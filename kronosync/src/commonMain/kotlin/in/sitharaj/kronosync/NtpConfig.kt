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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the NTP client.
 *
 * This class provides a builder pattern for configuring the NTP synchronization
 * behavior including server pools, timeouts, and retry policies.
 *
 * @property ntpServers List of NTP server hostnames to use for synchronization.
 * @property timeout Maximum time to wait for a response from an NTP server.
 * @property retryCount Number of times to retry a failed synchronization.
 * @property retryDelay Delay between retry attempts.
 * @property syncOnInit Whether to automatically sync when the client is initialized.
 * @property cacheDuration How long to cache the time offset before requiring a new sync.
 *
 * @sample in.sitharaj.kronosync.samples.configSample
 */
public data class NtpConfig(
    val ntpServers: List<String> = DEFAULT_NTP_SERVERS,
    val timeout: Duration = 10.seconds,
    val retryCount: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val syncOnInit: Boolean = true,
    val cacheDuration: Duration = Duration.INFINITE,
) {
    /**
     * Builder for creating [NtpConfig] instances.
     *
     * Example usage:
     * ```kotlin
     * val config = NtpConfig.Builder()
     *     .ntpServers(listOf("time.google.com", "time.apple.com"))
     *     .timeout(5.seconds)
     *     .build()
     * ```
     */
    public class Builder {
        private var ntpServers: List<String> = DEFAULT_NTP_SERVERS
        private var timeout: Duration = 10.seconds
        private var retryCount: Int = 3
        private var retryDelay: Duration = 1.seconds
        private var syncOnInit: Boolean = true
        private var cacheDuration: Duration = Duration.INFINITE

        /**
         * Sets the list of NTP servers to use.
         *
         * @param servers List of NTP server hostnames.
         * @return This builder for chaining.
         */
        public fun ntpServers(servers: List<String>): Builder = apply {
            ntpServers = servers
        }

        /**
         * Sets the timeout for NTP requests.
         *
         * @param duration Maximum time to wait for a response.
         * @return This builder for chaining.
         */
        public fun timeout(duration: Duration): Builder = apply {
            timeout = duration
        }

        /**
         * Sets the number of retry attempts for failed syncs.
         *
         * @param count Number of retries (0 for no retries).
         * @return This builder for chaining.
         */
        public fun retryCount(count: Int): Builder = apply {
            retryCount = count
        }

        /**
         * Sets the delay between retry attempts.
         *
         * @param duration Delay duration.
         * @return This builder for chaining.
         */
        public fun retryDelay(duration: Duration): Builder = apply {
            retryDelay = duration
        }

        /**
         * Sets whether to sync automatically on initialization.
         *
         * @param sync True to sync on init, false otherwise.
         * @return This builder for chaining.
         */
        public fun syncOnInit(sync: Boolean): Builder = apply {
            syncOnInit = sync
        }

        /**
         * Sets how long to cache the synchronized time offset.
         *
         * @param duration Cache duration. Use [Duration.INFINITE] to cache forever.
         * @return This builder for chaining.
         */
        public fun cacheDuration(duration: Duration): Builder = apply {
            cacheDuration = duration
        }

        /**
         * Builds the [NtpConfig] instance.
         *
         * @return Configured [NtpConfig].
         */
        public fun build(): NtpConfig = NtpConfig(
            ntpServers = ntpServers,
            timeout = timeout,
            retryCount = retryCount,
            retryDelay = retryDelay,
            syncOnInit = syncOnInit,
            cacheDuration = cacheDuration,
        )
    }

    public companion object {
        /**
         * Default NTP servers used when none are specified.
         * Includes major public NTP pools for reliability.
         */
        public val DEFAULT_NTP_SERVERS: List<String> = listOf(
            "time.google.com",
            "time.apple.com",
            "time.cloudflare.com",
            "pool.ntp.org",
            "time.windows.com",
        )

        /**
         * Default configuration with sensible defaults.
         */
        public val DEFAULT: NtpConfig = NtpConfig()
    }
}
