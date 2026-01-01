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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class NtpConfigTest {

    @Test
    fun testDefaultConfig() {
        val config = NtpConfig.DEFAULT

        assertEquals(5, config.ntpServers.size)
        assertEquals(10.seconds, config.timeout)
        assertEquals(3, config.retryCount)
        assertTrue(config.syncOnInit)
    }

    @Test
    fun testBuilderPattern() {
        val servers = listOf("time.google.com")
        val config = NtpConfig.Builder()
            .ntpServers(servers)
            .timeout(5.seconds)
            .retryCount(1)
            .syncOnInit(false)
            .build()

        assertEquals(servers, config.ntpServers)
        assertEquals(5.seconds, config.timeout)
        assertEquals(1, config.retryCount)
        assertFalse(config.syncOnInit)
    }

    @Test
    fun testDataClassEquality() {
        val config1 = NtpConfig(ntpServers = listOf("time.google.com"))
        val config2 = NtpConfig(ntpServers = listOf("time.google.com"))

        assertEquals(config1, config2)
    }
}
