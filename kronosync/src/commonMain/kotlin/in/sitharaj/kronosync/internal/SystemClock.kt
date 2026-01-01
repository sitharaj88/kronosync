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

package in.sitharaj.kronosync.internal

import kotlinx.datetime.Clock as KotlinClock

/**
 * Platform-specific system clock operations.
 */
internal expect object SystemClock {
    /**
     * Returns the current system time in milliseconds since Unix epoch.
     */
    fun currentTimeMillis(): Long

    /**
     * Returns the elapsed time since system boot in milliseconds.
     * This is useful for calculating time deltas independent of wall clock changes.
     */
    fun elapsedRealtime(): Long
}

/**
 * Default implementation using kotlinx-datetime.
 */
internal object DefaultSystemClock {
    fun currentTimeMillis(): Long = KotlinClock.System.now().toEpochMilliseconds()
}
