import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ru.ibakaidov.distypepro.ui.MainScreen
import ru.ibakaidov.distypepro.viewmodel.MainViewModel

@Composable
fun MainActivity() {
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