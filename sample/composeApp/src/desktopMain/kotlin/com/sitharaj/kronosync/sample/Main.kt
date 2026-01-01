/*
 * Copyright 2024 Sitharaj Seenivasan
 */

package com.sitharaj.kronosync.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KronoSync Sample",
        state = rememberWindowState(width = 400.dp, height = 700.dp),
    ) {
        App()
    }
}
