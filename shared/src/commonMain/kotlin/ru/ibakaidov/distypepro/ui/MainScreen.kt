package ru.ibakaidov.distypepro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.structures.Statement
import ru.ibakaidov.distypepro.utils.ProgressState
import ru.ibakaidov.distypepro.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val statements by viewModel.statements.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCategories()
    }
    
    when (progressState) {
        is ProgressState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ProgressState.Error -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Error: ${(progressState as ProgressState.Error).throwable.message}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        else -> {
            Row(
                modifier = modifier.fillMaxSize()
            ) {
                // Categories panel
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryItem(
                                category = category,
                                isSelected = currentCategory?.id == category.id,
                                onClick = { viewModel.selectCategory(category) }
                            )
                        }
                    }
                }
                
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                
                // Statements panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentCategory != null) "Statements for ${currentCategory!!.label}" else "Select a category",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    if (currentCategory != null) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(statements) { statement ->
                                StatementItem(statement = statement)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = category.label,
            modifier = Modifier.padding(16.dp),
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatementItem(
    statement: Statement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = statement.text,
            modifier = Modifier.padding(16.dp)
        )
    }
}