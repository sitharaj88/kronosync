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

import kotlin.time.Duration

/**
 * Result of an NTP synchronization operation.
 *
 * This sealed class represents the possible outcomes of attempting to
 * synchronize time with NTP servers.
 */
public sealed class SyncResult {
    /**
     * Synchronization succeeded.
     *
     * @property offset The calculated time offset between local and network time.
     *                  Positive values mean local clock is ahead, negative means behind.
     * @property roundTripDelay The total round-trip delay for the NTP exchange.
     * @property serverAddress The address of the NTP server that responded.
     */
    public data class Success(
        val offset: Duration,
        val roundTripDelay: Duration,
        val serverAddress: String,
    ) : SyncResult()

    /**
     * Synchronization failed.
     *
     * @property error Description of what went wrong.
     * @property exception The underlying exception, if any.
     */
    public data class Failure(
        val error: String,
        val exception: Throwable? = null,
    ) : SyncResult()
}

/**
 * Snapshot of the current synchronized time state.
 *
 * @property epochMillis Current time in milliseconds since Unix epoch.
 * @property offset The time offset from the last successful sync.
 * @property isSynced Whether a successful sync has occurred.
 * @property lastSyncTimeMillis Time of last successful sync in epoch millis, or null.
 */
public data class TimeSnapshot(
    val epochMillis: Long,
    val offset: Duration,
    val isSynced: Boolean,
    val lastSyncTimeMillis: Long?,
)
