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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Android implementation of UDP socket using Java's DatagramSocket.
 */
internal actual class UdpSocket {
    private var socket: DatagramSocket? = null

    actual suspend fun sendReceive(
        host: String,
        port: Int,
        data: ByteArray,
        timeoutMs: Long,
    ): ByteArray = withContext(Dispatchers.IO) {
        val address = InetAddress.getByName(host)
        val socket = DatagramSocket().also {
            it.soTimeout = timeoutMs.toInt()
            this@UdpSocket.socket = it
        }

        try {
            // Send request
            val sendPacket = DatagramPacket(data, data.size, address, port)
            socket.send(sendPacket)

            // Receive response
            val buffer = ByteArray(NtpPacket.PACKET_SIZE)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            buffer.copyOf(receivePacket.length)
        } finally {
            socket.close()
        }
    }

    actual fun close() {
        socket?.close()
        socket = null
    }
}
