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

/**
 * Platform-specific UDP socket for NTP communication.
 *
 * Each platform provides its own implementation using native networking APIs.
 */
internal expect class UdpSocket() {
    /**
     * Sends data to the specified host and port, and waits for a response.
     *
     * @param host The hostname or IP address of the NTP server.
     * @param port The port number (typically 123 for NTP).
     * @param data The data to send (NTP request packet).
     * @param timeoutMs Maximum time to wait for a response in milliseconds.
     * @return The response data from the server.
     * @throws Exception if the operation fails or times out.
     */
    suspend fun sendReceive(
        host: String,
        port: Int,
        data: ByteArray,
        timeoutMs: Long,
    ): ByteArray

    /**
     * Closes the socket and releases resources.
     */
    fun close()
}
