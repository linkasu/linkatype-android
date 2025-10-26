package ru.ibakaidov.distypepro.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.structures.Statement
import ru.ibakaidov.distypepro.utils.ProgressState

actual class MainViewModel {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    actual val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _statements = MutableStateFlow<List<Statement>>(emptyList())
    actual val statements: StateFlow<List<Statement>> = _statements.asStateFlow()
    
    private val _currentCategory = MutableStateFlow<Category?>(null)
    actual val currentCategory: StateFlow<Category?> = _currentCategory.asStateFlow()
    
    private val _progressState = MutableStateFlow<ProgressState>(ProgressState.Idle)
    actual val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()
    
    actual fun loadCategories() {
        // TODO: Implement desktop-specific data loading
        _progressState.value = ProgressState.Loading
        // Mock data for now
        _categories.value = listOf(
            Category("1", "Sample Category 1", System.currentTimeMillis()),
            Category("2", "Sample Category 2", System.currentTimeMillis())
        )
        _progressState.value = ProgressState.Idle
    }
    
    actual fun loadStatements(categoryId: String) {
        // TODO: Implement desktop-specific data loading
        _progressState.value = ProgressState.Loading
        // Mock data for now
        _statements.value = listOf(
            Statement("1", categoryId, "Sample statement 1", System.currentTimeMillis()),
            Statement("2", categoryId, "Sample statement 2", System.currentTimeMillis())
        )
        _progressState.value = ProgressState.Idle
    }
    
    actual fun addCategory(label: String) {
        // TODO: Implement desktop-specific data creation
        val newCategory = Category(
            id = System.currentTimeMillis().toString(),
            label = label,
            created = System.currentTimeMillis()
        )
        _categories.value = _categories.value + newCategory
    }
    
    actual fun addStatement(categoryId: String, text: String) {
        // TODO: Implement desktop-specific data creation
        val newStatement = Statement(
            id = System.currentTimeMillis().toString(),
            categoryId = categoryId,
            text = text,
            created = System.currentTimeMillis()
        )
        _statements.value = _statements.value + newStatement
    }
    
    actual fun deleteCategory(categoryId: String) {
        // TODO: Implement desktop-specific data deletion
        _categories.value = _categories.value.filter { it.id != categoryId }
    }
    
    actual fun deleteStatement(statementId: String) {
        // TODO: Implement desktop-specific data deletion
        _statements.value = _statements.value.filter { it.id != statementId }
    }
    
    actual fun selectCategory(category: Category?) {
        _currentCategory.value = category
        category?.let { loadStatements(it.id) }
    }
}