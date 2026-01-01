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

import kotlinx.cinterop.*
import platform.darwin.*
import platform.posix.*

/**
 * iOS implementation of UDP socket using Darwin sockets.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class UdpSocket {
    private var socketFd: Int = -1

    actual suspend fun sendReceive(
        host: String,
        port: Int,
        data: ByteArray,
        timeoutMs: Long,
    ): ByteArray = memScoped {
        // Resolve hostname
        val hints = alloc<addrinfo>()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_DGRAM
        hints.ai_protocol = IPPROTO_UDP

        val result = allocPointerTo<addrinfo>()
        val resolveResult = getaddrinfo(host, port.toString(), hints.ptr, result.ptr)
        if (resolveResult != 0) {
            throw Exception("Failed to resolve host: $host")
        }

        val addrInfo = result.value ?: throw Exception("No address info returned")

        try {
            // Create socket
            socketFd = socket(addrInfo.pointed.ai_family, addrInfo.pointed.ai_socktype, addrInfo.pointed.ai_protocol)
            if (socketFd < 0) {
                throw Exception("Failed to create socket")
            }

            // Set timeout
            val timeout = alloc<timeval>()
            timeout.tv_sec = (timeoutMs / 1000).convert()
            timeout.tv_usec = ((timeoutMs % 1000) * 1000).convert()
            setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())

            // Send data
            val sendResult = sendto(
                socketFd,
                data.refTo(0),
                data.size.convert(),
                0,
                addrInfo.pointed.ai_addr,
                addrInfo.pointed.ai_addrlen
            )
            if (sendResult < 0) {
                throw Exception("Failed to send data")
            }

            // Receive response
            val buffer = ByteArray(NtpPacket.PACKET_SIZE)
            val received = recv(socketFd, buffer.refTo(0), buffer.size.convert(), 0)
            if (received < 0) {
                throw Exception("Failed to receive data or timeout")
            }

            buffer.copyOf(received.toInt())
        } finally {
            if (socketFd >= 0) {
                close(socketFd)
            }
            freeaddrinfo(addrInfo)
        }
    }

    actual fun close() {
        if (socketFd >= 0) {
            close(socketFd)
            socketFd = -1
        }
    }
}
