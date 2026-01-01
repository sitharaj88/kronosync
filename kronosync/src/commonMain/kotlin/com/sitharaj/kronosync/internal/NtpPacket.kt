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

package com.sitharaj.kronosync.internal

import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Represents an NTP packet according to RFC 4330 (SNTP).
 *
 * The NTP packet is 48 bytes with the following structure:
 * - Byte 0: LI (2 bits), VN (3 bits), Mode (3 bits)
 * - Byte 1: Stratum
 * - Byte 2: Poll interval
 * - Byte 3: Precision
 * - Bytes 4-7: Root delay
 * - Bytes 8-11: Root dispersion
 * - Bytes 12-15: Reference identifier
 * - Bytes 16-23: Reference timestamp
 * - Bytes 24-31: Originate timestamp
 * - Bytes 32-39: Receive timestamp
 * - Bytes 40-47: Transmit timestamp
 *
 * @property leapIndicator Leap second warning (0 = no warning, 1 = +1 sec, 2 = -1 sec, 3 = unsync)
 * @property version NTP version number (currently 4)
 * @property mode NTP mode (3 = client, 4 = server)
 * @property stratum Server stratum level (0 = unspecified, 1 = primary, 2-15 = secondary)
 * @property pollInterval Maximum interval between messages as power of 2
 * @property precision Clock precision as power of 2 seconds
 * @property rootDelay Total round-trip delay to reference clock
 * @property rootDispersion Maximum error relative to reference clock
 * @property referenceIdentifier Identifier of reference source
 * @property referenceTimestamp Time when system clock was last set
 * @property originateTimestamp Time when request was sent by client
 * @property receiveTimestamp Time when request was received by server
 * @property transmitTimestamp Time when reply was sent by server
 */
internal data class NtpPacket(
    val leapIndicator: Int = 0,
    val version: Int = 4,
    val mode: Int = 3,
    val stratum: Int = 0,
    val pollInterval: Int = 0,
    val precision: Int = 0,
    val rootDelay: Long = 0,
    val rootDispersion: Long = 0,
    val referenceIdentifier: Long = 0,
    val referenceTimestamp: NtpTimestamp = NtpTimestamp.ZERO,
    val originateTimestamp: NtpTimestamp = NtpTimestamp.ZERO,
    val receiveTimestamp: NtpTimestamp = NtpTimestamp.ZERO,
    val transmitTimestamp: NtpTimestamp = NtpTimestamp.ZERO,
) {
    /**
     * Converts this packet to a 48-byte array for transmission.
     *
     * @return ByteArray containing the encoded NTP packet.
     */
    fun toByteArray(): ByteArray {
        val buffer = ByteArray(PACKET_SIZE)

        // Byte 0: LI (2 bits) | VN (3 bits) | Mode (3 bits)
        buffer[0] = ((leapIndicator shl 6) or (version shl 3) or mode).toByte()

        // Bytes 1-3: Stratum, Poll, Precision
        buffer[1] = stratum.toByte()
        buffer[2] = pollInterval.toByte()
        buffer[3] = precision.toByte()

        // Bytes 4-7: Root Delay (32-bit fixed-point)
        writeInt(buffer, 4, rootDelay.toInt())

        // Bytes 8-11: Root Dispersion (32-bit fixed-point)
        writeInt(buffer, 8, rootDispersion.toInt())

        // Bytes 12-15: Reference Identifier
        writeInt(buffer, 12, referenceIdentifier.toInt())

        // Bytes 16-23: Reference Timestamp
        writeTimestamp(buffer, 16, referenceTimestamp)

        // Bytes 24-31: Originate Timestamp
        writeTimestamp(buffer, 24, originateTimestamp)

        // Bytes 32-39: Receive Timestamp
        writeTimestamp(buffer, 32, receiveTimestamp)

        // Bytes 40-47: Transmit Timestamp
        writeTimestamp(buffer, 40, transmitTimestamp)

        return buffer
    }

    companion object {
        /** Standard NTP packet size in bytes */
        const val PACKET_SIZE = 48

        /** NTP port number */
        const val NTP_PORT = 123

        /**
         * Creates a client request packet with the current time as transmit timestamp.
         *
         * @param transmitTime Current system time in milliseconds since epoch.
         * @return NTP packet ready for transmission.
         */
        fun createRequest(transmitTime: Long): NtpPacket {
            return NtpPacket(
                leapIndicator = 0,
                version = 4,
                mode = 3, // Client mode
                transmitTimestamp = NtpTimestamp.fromEpochMillis(transmitTime),
            )
        }

        /**
         * Parses a received byte array into an NTP packet.
         *
         * @param buffer 48-byte array received from NTP server.
         * @return Parsed NTP packet.
         * @throws IllegalArgumentException if buffer size is incorrect.
         */
        fun fromByteArray(buffer: ByteArray): NtpPacket {
            require(buffer.size >= PACKET_SIZE) {
                "NTP packet must be at least $PACKET_SIZE bytes, got ${buffer.size}"
            }

            val firstByte = buffer[0].toInt() and 0xFF

            return NtpPacket(
                leapIndicator = (firstByte shr 6) and 0x03,
                version = (firstByte shr 3) and 0x07,
                mode = firstByte and 0x07,
                stratum = buffer[1].toInt() and 0xFF,
                pollInterval = buffer[2].toInt(),
                precision = buffer[3].toInt(),
                rootDelay = readInt(buffer, 4).toLong(),
                rootDispersion = readInt(buffer, 8).toLong(),
                referenceIdentifier = readInt(buffer, 12).toLong(),
                referenceTimestamp = readTimestamp(buffer, 16),
                originateTimestamp = readTimestamp(buffer, 24),
                receiveTimestamp = readTimestamp(buffer, 32),
                transmitTimestamp = readTimestamp(buffer, 40),
            )
        }

        private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
            buffer[offset] = (value shr 24).toByte()
            buffer[offset + 1] = (value shr 16).toByte()
            buffer[offset + 2] = (value shr 8).toByte()
            buffer[offset + 3] = value.toByte()
        }

        private fun readInt(buffer: ByteArray, offset: Int): Int {
            return ((buffer[offset].toInt() and 0xFF) shl 24) or
                    ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
                    ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
                    (buffer[offset + 3].toInt() and 0xFF)
        }

        private fun writeTimestamp(buffer: ByteArray, offset: Int, timestamp: NtpTimestamp) {
            writeInt(buffer, offset, timestamp.seconds.toInt())
            writeInt(buffer, offset + 4, timestamp.fraction.toInt())
        }

        private fun readTimestamp(buffer: ByteArray, offset: Int): NtpTimestamp {
            val seconds = readInt(buffer, offset).toLong() and 0xFFFFFFFFL
            val fraction = readInt(buffer, offset + 4).toLong() and 0xFFFFFFFFL
            return NtpTimestamp(seconds, fraction)
        }
    }
}

/**
 * Represents an NTP timestamp (64-bit fixed-point number).
 *
 * NTP timestamps are measured from January 1, 1900, unlike Unix timestamps
 * which start from January 1, 1970.
 *
 * @property seconds Seconds since NTP epoch (1900-01-01 00:00:00)
 * @property fraction Fractional seconds (2^32 fractions per second)
 */
internal data class NtpTimestamp(
    val seconds: Long,
    val fraction: Long,
) {
    /**
     * Converts this NTP timestamp to milliseconds since Unix epoch (1970-01-01).
     *
     * @return Milliseconds since January 1, 1970 00:00:00 UTC.
     */
    fun toEpochMillis(): Long {
        // Subtract the 70 years between 1900 and 1970
        val unixSeconds = seconds - SECONDS_FROM_1900_TO_1970
        val fractionMillis = (fraction * 1000L) / FRACTION_PER_SECOND
        return (unixSeconds * 1000L) + fractionMillis
    }

    companion object {
        /** Zero timestamp */
        val ZERO = NtpTimestamp(0, 0)

        /** Seconds between NTP epoch (1900) and Unix epoch (1970) */
        private const val SECONDS_FROM_1900_TO_1970 = 2208988800L

        /** Number of fractions per second (2^32) */
        private const val FRACTION_PER_SECOND = 0x100000000L

        /**
         * Creates an NTP timestamp from Unix epoch milliseconds.
         *
         * @param epochMillis Milliseconds since January 1, 1970 00:00:00 UTC.
         * @return NTP timestamp.
         */
        fun fromEpochMillis(epochMillis: Long): NtpTimestamp {
            val epochSeconds = epochMillis / 1000L
            val millisPart = epochMillis % 1000L

            val ntpSeconds = epochSeconds + SECONDS_FROM_1900_TO_1970
            val ntpFraction = (millisPart * FRACTION_PER_SECOND) / 1000L

            return NtpTimestamp(ntpSeconds, ntpFraction)
        }
    }
}
