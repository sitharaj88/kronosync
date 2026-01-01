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

package in.sitharaj.kronosync.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.sitharaj.kronosync.KronoSync
import in.sitharaj.kronosync.NtpConfig
import in.sitharaj.kronosync.SyncResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun App() {
    // Initialize KronoSync
    LaunchedEffect(Unit) {
        if (!KronoSync.isInitialized()) {
            KronoSync.initialize(
                NtpConfig.Builder()
                    .ntpServers(listOf(
                        "time.google.com",
                        "time.apple.com",
                        "time.cloudflare.com"
                    ))
                    .timeout(10.seconds)
                    .build()
            )
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6366F1),
            secondary = Color(0xFF22D3EE),
            tertiary = Color(0xFFA855F7),
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            onBackground = Color.White,
            onSurface = Color.White,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B),
                            Color(0xFF0F172A),
                        )
                    )
                )
        ) {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var syncState by remember { mutableStateOf<SyncState>(SyncState.Idle) }
    var currentTime by remember { mutableStateOf<Instant?>(null) }
    var systemTime by remember { mutableStateOf<Instant?>(null) }
    var offset by remember { mutableStateOf(0L) }
    var syncServer by remember { mutableStateOf<String?>(null) }
    var roundTrip by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()

    // Update time every 100ms
    LaunchedEffect(Unit) {
        while (isActive) {
            if (KronoSync.isInitialized()) {
                currentTime = KronoSync.nowOrSystem()
                systemTime = Instant.fromEpochMilliseconds(currentTimeMillis())
                offset = KronoSync.snapshot().offset.inWholeMilliseconds
            }
            delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Text(
            text = "KronoSync",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Text(
            text = "Network Time Synchronization",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Status indicator
        SyncStatusIndicator(syncState)

        Spacer(modifier = Modifier.height(32.dp))

        // Main clock display
        ClockDisplay(
            time = currentTime,
            label = "Network Time",
            isPrimary = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ClockDisplay(
            time = systemTime,
            label = "System Time",
            isPrimary = false,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Metrics
        MetricsRow(
            offset = offset,
            roundTrip = roundTrip,
            server = syncServer,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Sync button
        Button(
            onClick = {
                scope.launch {
                    syncState = SyncState.Syncing
                    when (val result = KronoSync.sync()) {
                        is SyncResult.Success -> {
                            syncState = SyncState.Success
                            syncServer = result.serverAddress
                            roundTrip = result.roundTripDelay.inWholeMilliseconds
                        }
                        is SyncResult.Failure -> {
                            syncState = SyncState.Error(result.error)
                        }
                    }
                }
            },
            enabled = syncState !is SyncState.Syncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1),
            )
        ) {
            if (syncState is SyncState.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "Sync Now",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        AnimatedVisibility(visible = syncState is SyncState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF7F1D1D),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = (syncState as? SyncState.Error)?.message ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SyncStatusIndicator(state: SyncState) {
    val color by animateColorAsState(
        targetValue = when (state) {
            is SyncState.Idle -> Color(0xFF64748B)
            is SyncState.Syncing -> Color(0xFFFBBF24)
            is SyncState.Success -> Color(0xFF22C55E)
            is SyncState.Error -> Color(0xFFEF4444)
        }
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (state) {
                is SyncState.Idle -> "Not synced"
                is SyncState.Syncing -> "Syncing..."
                is SyncState.Success -> "Synchronized"
                is SyncState.Error -> "Sync failed"
            },
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ClockDisplay(
    time: Instant?,
    label: String,
    isPrimary: Boolean,
) {
    val localTime = time?.toLocalDateTime(TimeZone.currentSystemDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) Color(0xFF1E293B) else Color(0xFF0F172A),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = localTime?.let {
                    String.format(
                        "%02d:%02d:%02d",
                        it.hour,
                        it.minute,
                        it.second
                    )
                } ?: "--:--:--",
                fontSize = if (isPrimary) 56.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isPrimary) Color.White else Color(0xFF64748B),
            )

            if (isPrimary && localTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(
                        "%04d-%02d-%02d",
                        localTime.year,
                        localTime.monthNumber,
                        localTime.dayOfMonth
                    ),
                    fontSize = 16.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun MetricsRow(
    offset: Long,
    roundTrip: Long?,
    server: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MetricCard(
            label = "Offset",
            value = "${if (offset >= 0) "+" else ""}${offset}ms",
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(12.dp))

        MetricCard(
            label = "RTT",
            value = roundTrip?.let { "${it}ms" } ?: "--",
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(12.dp))

        MetricCard(
            label = "Server",
            value = server?.substringBefore(".") ?: "--",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

// Platform-specific expect for currentTimeMillis
expect fun currentTimeMillis(): Long
