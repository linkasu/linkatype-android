package ru.ibakaidov.distypepro.desktopapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.ibakaidov.distypepro.ui.MainScreen
import ru.ibakaidov.distypepro.viewmodel.MainViewModel

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LINKa Type",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        MainContent()
    }
}

@Composable
fun MainContent() {
    val viewModel = MainViewModel()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "LINKa Type",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(16.dp)
        )
        
        MainScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}