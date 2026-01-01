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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NtpPacketTest {

    @Test
    fun testPacketCreation() {
        val transmitTime = 1704067200000L // 2024-01-01 00:00:00 UTC
        val packet = NtpPacket.createRequest(transmitTime)

        assertEquals(0, packet.leapIndicator)
        assertEquals(4, packet.version)
        assertEquals(3, packet.mode) // Client mode
    }

    @Test
    fun testPacketSerialization() {
        val packet = NtpPacket(
            leapIndicator = 0,
            version = 4,
            mode = 3,
            stratum = 0,
            pollInterval = 0,
            precision = 0,
        )

        val bytes = packet.toByteArray()

        assertEquals(48, bytes.size)
        assertEquals(0b00100011.toByte(), bytes[0]) // LI=0, VN=4, Mode=3
    }

    @Test
    fun testPacketDeserialization() {
        val bytes = ByteArray(48)
        bytes[0] = 0b00100100.toByte() // LI=0, VN=4, Mode=4 (server)
        bytes[1] = 2 // Stratum 2

        val packet = NtpPacket.fromByteArray(bytes)

        assertEquals(0, packet.leapIndicator)
        assertEquals(4, packet.version)
        assertEquals(4, packet.mode)
        assertEquals(2, packet.stratum)
    }

    @Test
    fun testRoundTrip() {
        val transmitTime = 1704067200000L
        val original = NtpPacket.createRequest(transmitTime)

        val bytes = original.toByteArray()
        val deserialized = NtpPacket.fromByteArray(bytes)

        assertEquals(original.leapIndicator, deserialized.leapIndicator)
        assertEquals(original.version, deserialized.version)
        assertEquals(original.mode, deserialized.mode)
        assertEquals(original.transmitTimestamp, deserialized.transmitTimestamp)
    }
}

class NtpTimestampTest {

    @Test
    fun testTimestampConversion() {
        val epochMillis = 1704067200000L // 2024-01-01 00:00:00 UTC

        val ntpTimestamp = NtpTimestamp.fromEpochMillis(epochMillis)
        val converted = ntpTimestamp.toEpochMillis()

        // Allow 1ms tolerance due to fractional precision
        assertTrue(kotlin.math.abs(epochMillis - converted) <= 1)
    }

    @Test
    fun testZeroTimestamp() {
        assertEquals(0, NtpTimestamp.ZERO.seconds)
        assertEquals(0, NtpTimestamp.ZERO.fraction)
    }

    @Test
    fun testFractionalSeconds() {
        val epochMillis = 1704067200500L // 2024-01-01 00:00:00.500 UTC

        val ntpTimestamp = NtpTimestamp.fromEpochMillis(epochMillis)

        // Fraction should be approximately half of 2^32
        assertTrue(ntpTimestamp.fraction > 0)
    }
}
