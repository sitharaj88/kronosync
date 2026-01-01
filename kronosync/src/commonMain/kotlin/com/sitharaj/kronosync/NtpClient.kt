/*
 * Copyright 2024 Sitharaj Seenivasan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sitharaj.kronosync

import com.sitharaj.kronosync.internal.NtpPacket
import com.sitharaj.kronosync.internal.SystemClock
import com.sitharaj.kronosync.internal.UdpSocket
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main client for Network Time Protocol (NTP) synchronization.
 *
 * KronoSync provides accurate network time by communicating with NTP servers
 * and calculating the offset between the local system clock and the true time.
 *
 * ## Features
 * - Supports multiple NTP server pools with automatic fallback
 * - Configurable retry policies and timeouts
 * - Thread-safe with atomic operations
 * - Caches time offset to minimize network requests
 * - Works offline using cached offset
 *
 * ## Basic Usage
 * ```kotlin
 * val client = NtpClient()
 *
 * // Synchronize with NTP servers
 * when (val result = client.sync()) {
 *     is SyncResult.Success -> {
 *         println("Synced! Offset: ${result.offset}")
 *     }
 *     is SyncResult.Failure -> {
 *         println("Sync failed: ${result.error}")
 *     }
 * }
 *
 * // Get the current network time
 * val networkTime: Instant? = client.now()
 * ```
 *
 * ## Advanced Usage
 * ```kotlin
 * val client = NtpClient(
 *     NtpConfig.Builder()
 *         .ntpServers(listOf("time.google.com", "time.apple.com"))
 *         .timeout(5.seconds)
 *         .retryCount(3)
 *         .build()
 * )
 * ```
 *
 * @param config Configuration for the NTP client.
 *
 * @see NtpConfig
 * @see SyncResult
 * @see KronoSync
 */
public class NtpClient(
    private val config: NtpConfig = NtpConfig.DEFAULT,
) {
    private val lock = SynchronizedObject()
    private val offsetMillis = atomic(0L)
    private val isSynced = atomic(false)
    private val lastSyncElapsedTime = atomic(0L)
    private val lastSyncTimeMillis = atomic(0L)

    /**
     * Returns the current network-synchronized time as an [Instant].
     *
     * If the client has not yet successfully synced, this returns `null`.
     * Use [nowOrSystem] to get the system time as a fallback.
     *
     * @return The current network time, or null if not synced.
     */
    public fun now(): Instant? {
        if (!isSynced.value) return null
        return Instant.fromEpochMilliseconds(currentTimeMillis())
    }

    /**
     * Returns the current network-synchronized time, falling back to system time.
     *
     * If the client has successfully synced, returns the network time.
     * Otherwise, returns the local system time.
     *
     * @return The current time as an [Instant].
     */
    public fun nowOrSystem(): Instant {
        return Instant.fromEpochMilliseconds(currentTimeMillis())
    }

    /**
     * Returns the current time in milliseconds since Unix epoch.
     *
     * Applies the calculated offset to the system time.
     *
     * @return Milliseconds since January 1, 1970 00:00:00 UTC.
     */
    public fun currentTimeMillis(): Long {
        return SystemClock.currentTimeMillis() + offsetMillis.value
    }

    /**
     * Returns the current time offset between local and network time.
     *
     * - Positive offset: local clock is ahead of network time
     * - Negative offset: local clock is behind network time
     *
     * @return The time offset, or [Duration.ZERO] if not synced.
     */
    public fun offset(): Duration {
        return offsetMillis.value.milliseconds
    }

    /**
     * Returns whether the client has successfully synchronized.
     *
     * @return `true` if at least one successful sync has occurred.
     */
    public fun isSynchronized(): Boolean = isSynced.value

    /**
     * Returns a snapshot of the current time state.
     *
     * @return Current [TimeSnapshot] with all timing information.
     */
    public fun snapshot(): TimeSnapshot {
        return TimeSnapshot(
            epochMillis = currentTimeMillis(),
            offset = offset(),
            isSynced = isSynced.value,
            lastSyncTimeMillis = if (isSynced.value) lastSyncTimeMillis.value else null,
        )
    }

    /**
     * Synchronizes with NTP servers to update the time offset.
     *
     * This method attempts to contact each configured NTP server in order
     * until one responds successfully. The calculated offset is then stored
     * for use by [now] and [currentTimeMillis].
     *
     * @return [SyncResult] indicating success or failure.
     */
    public suspend fun sync(): SyncResult {
        var lastError: Throwable? = null

        for (server in config.ntpServers) {
            for (attempt in 0..config.retryCount) {
                try {
                    val result = syncWithServer(server)
                    if (result is SyncResult.Success) {
                        synchronized(lock) {
                            offsetMillis.value = result.offset.inWholeMilliseconds
                            isSynced.value = true
                            lastSyncElapsedTime.value = SystemClock.elapsedRealtime()
                            lastSyncTimeMillis.value = SystemClock.currentTimeMillis()
                        }
                        return result
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < config.retryCount) {
                        delay(config.retryDelay)
                    }
                }
            }
        }

        return SyncResult.Failure(
            error = "Failed to sync with any NTP server",
            exception = lastError,
        )
    }

    private suspend fun syncWithServer(server: String): SyncResult {
        val socket = UdpSocket()
        try {
            // Record time before sending
            val t0 = SystemClock.currentTimeMillis()

            // Create and send NTP request
            val request = NtpPacket.createRequest(t0)
            val response = socket.sendReceive(
                host = server,
                port = NtpPacket.NTP_PORT,
                data = request.toByteArray(),
                timeoutMs = config.timeout.inWholeMilliseconds,
            )

            // Record time after receiving
            val t3 = SystemClock.currentTimeMillis()

            // Parse response
            val packet = NtpPacket.fromByteArray(response)

            // Validate response
            if (packet.stratum == 0) {
                return SyncResult.Failure("Server stratum is 0 (kiss-of-death)")
            }

            // Extract timestamps
            val t1 = packet.receiveTimestamp.toEpochMillis()  // Server receive time
            val t2 = packet.transmitTimestamp.toEpochMillis() // Server transmit time

            // Calculate offset: ((t1 - t0) + (t2 - t3)) / 2
            val offset = ((t1 - t0) + (t2 - t3)) / 2

            // Calculate round-trip delay: (t3 - t0) - (t2 - t1)
            val roundTripDelay = (t3 - t0) - (t2 - t1)

            return SyncResult.Success(
                offset = offset.milliseconds,
                roundTripDelay = roundTripDelay.milliseconds,
                serverAddress = server,
            )
        } finally {
            socket.close()
        }
    }

    /**
     * Resets the synchronization state.
     *
     * After calling this, [isSynchronized] will return `false` and
     * [now] will return `null` until [sync] is called again.
     */
    public fun reset() {
        synchronized(lock) {
            offsetMillis.value = 0
            isSynced.value = false
            lastSyncElapsedTime.value = 0
            lastSyncTimeMillis.value = 0
        }
    }
}
