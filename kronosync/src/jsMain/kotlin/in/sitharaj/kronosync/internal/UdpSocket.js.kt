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

package `in`.sitharaj.kronosync.internal

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*

/**
 * JavaScript implementation using HTTP-based time API.
 *
 * Since browsers don't support raw UDP sockets, we fall back to
 * HTTP-based time APIs like WorldTimeAPI.
 */
internal actual class UdpSocket {
    private val httpClient = HttpClient()

    actual suspend fun sendReceive(
        host: String,
        port: Int,
        data: ByteArray,
        timeoutMs: Long,
    ): ByteArray {
        // For JS, we use HTTP API instead of NTP
        // This is a workaround since browsers don't support UDP
        val response = httpClient.get("https://worldtimeapi.org/api/ip")
        val json = Json.parseToJsonElement(response.bodyAsText())
        val unixtime = json.jsonObject["unixtime"]?.jsonPrimitive?.long ?: 0L

        // Create a fake NTP response packet with the HTTP time
        return createFakeNtpResponse(unixtime * 1000)
    }

    private fun createFakeNtpResponse(epochMillis: Long): ByteArray {
        val timestamp = NtpTimestamp.fromEpochMillis(epochMillis)
        val packet = NtpPacket(
            leapIndicator = 0,
            version = 4,
            mode = 4, // Server mode
            stratum = 2,
            receiveTimestamp = timestamp,
            transmitTimestamp = timestamp,
        )
        return packet.toByteArray()
    }

    actual fun close() {
        httpClient.close()
    }
}
