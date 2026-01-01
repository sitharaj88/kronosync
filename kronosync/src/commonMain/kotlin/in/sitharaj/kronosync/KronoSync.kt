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

package `in`.sitharaj.kronosync

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

/**
 * Global singleton for convenient access to network-synchronized time.
 *
 * KronoSync provides a simple, static-like interface for applications that
 * need a single shared NTP client throughout the app.
 *
 * ## Quick Start
 * ```kotlin
 * // Initialize once at app startup
 * KronoSync.initialize(
 *     NtpConfig.Builder()
 *         .ntpServers(listOf("time.google.com"))
 *         .build()
 * )
 *
 * // Sync with NTP servers
 * KronoSync.sync()
 *
 * // Get network time anywhere in your app
 * val networkTime = KronoSync.now()
 * ```
 *
 * ## Thread Safety
 * All methods are thread-safe and can be called from any thread or coroutine.
 *
 * @see NtpClient
 * @see NtpConfig
 */
public object KronoSync {
    private val mutex = Mutex()
    private val _client = atomic<NtpClient?>(null)

    /**
     * Returns the underlying [NtpClient] instance.
     *
     * @throws IllegalStateException if [initialize] has not been called.
     */
    public val client: NtpClient
        get() = _client.value
            ?: throw IllegalStateException("KronoSync not initialized. Call initialize() first.")

    /**
     * Initializes KronoSync with the given configuration.
     *
     * This should be called once at application startup, typically in:
     * - Android: `Application.onCreate()`
     * - iOS: App initialization
     * - Desktop: Main function
     *
     * Multiple calls to initialize will replace the existing client.
     *
     * @param config Configuration for the NTP client.
     */
    public fun initialize(config: NtpConfig = NtpConfig.DEFAULT) {
        _client.value = NtpClient(config)
    }

    /**
     * Returns whether KronoSync has been initialized.
     *
     * @return `true` if [initialize] has been called.
     */
    public fun isInitialized(): Boolean = _client.value != null

    /**
     * Returns the current network-synchronized time.
     *
     * @return The current network time, or null if not synced.
     * @throws IllegalStateException if not initialized.
     */
    public fun now(): Instant? = client.now()

    /**
     * Returns the current network-synchronized time, or system time as fallback.
     *
     * @return The current time as an [Instant].
     * @throws IllegalStateException if not initialized.
     */
    public fun nowOrSystem(): Instant = client.nowOrSystem()

    /**
     * Returns the current time in milliseconds since Unix epoch.
     *
     * @return Milliseconds since January 1, 1970 00:00:00 UTC.
     * @throws IllegalStateException if not initialized.
     */
    public fun currentTimeMillis(): Long = client.currentTimeMillis()

    /**
     * Returns whether a successful sync has occurred.
     *
     * @return `true` if synchronized.
     * @throws IllegalStateException if not initialized.
     */
    public fun isSynchronized(): Boolean = client.isSynchronized()

    /**
     * Synchronizes with NTP servers.
     *
     * This is a suspend function that should be called from a coroutine.
     *
     * @return [SyncResult] indicating success or failure.
     * @throws IllegalStateException if not initialized.
     */
    public suspend fun sync(): SyncResult = mutex.withLock {
        client.sync()
    }

    /**
     * Resets the synchronization state.
     *
     * @throws IllegalStateException if not initialized.
     */
    public fun reset() {
        client.reset()
    }

    /**
     * Returns a snapshot of the current time state.
     *
     * @return Current [TimeSnapshot].
     * @throws IllegalStateException if not initialized.
     */
    public fun snapshot(): TimeSnapshot = client.snapshot()
}
