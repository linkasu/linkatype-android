package ru.ibakaidov.distypepro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.data.CategoryManager
import ru.ibakaidov.distypepro.data.StatementManager
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.structures.Statement
import ru.ibakaidov.distypepro.utils.ProgressState

actual class MainViewModel : ViewModel() {
    private val categoryManager = CategoryManager()
    private val statementManager = StatementManager()
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    actual val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _statements = MutableStateFlow<List<Statement>>(emptyList())
    actual val statements: StateFlow<List<Statement>> = _statements.asStateFlow()
    
    private val _currentCategory = MutableStateFlow<Category?>(null)
    actual val currentCategory: StateFlow<Category?> = _currentCategory.asStateFlow()
    
    private val _progressState = MutableStateFlow<ProgressState>(ProgressState.Idle)
    actual val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()
    
    actual fun loadCategories() {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            categoryManager.getList { result ->
                when (result) {
                    is ProgressState.Success -> {
                        _categories.value = result.data
                        _progressState.value = ProgressState.Idle
                    }
                    is ProgressState.Error -> {
                        _progressState.value = result
                    }
                    else -> {}
                }
            }
        }
    }
    
    actual fun loadStatements(categoryId: String) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            statementManager.getList { result ->
                when (result) {
                    is ProgressState.Success -> {
                        _statements.value = result.data
                        _progressState.value = ProgressState.Idle
                    }
                    is ProgressState.Error -> {
                        _progressState.value = result
                    }
                    else -> {}
                }
            }
        }
    }
    
    actual fun addCategory(label: String) {
        viewModelScope.launch {
            categoryManager.create(label) { result ->
                when (result) {
                    is ProgressState.Success -> loadCategories()
                    is ProgressState.Error -> _progressState.value = result
                    else -> {}
                }
            }
        }
    }
    
    actual fun addStatement(categoryId: String, text: String) {
        viewModelScope.launch {
            statementManager.create(text) { result ->
                when (result) {
                    is ProgressState.Success -> loadStatements(categoryId)
                    is ProgressState.Error -> _progressState.value = result
                    else -> {}
                }
            }
        }
    }
    
    actual fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryManager.remove(categoryId) { result ->
                when (result) {
                    is ProgressState.Success -> loadCategories()
                    is ProgressState.Error -> _progressState.value = result
                    else -> {}
                }
            }
        }
    }
    
    actual fun deleteStatement(statementId: String) {
        viewModelScope.launch {
            statementManager.remove(statementId) { result ->
                when (result) {
                    is ProgressState.Success -> loadStatements(_currentCategory.value?.id ?: "")
                    is ProgressState.Error -> _progressState.value = result
                    else -> {}
                }
            }
        }
    }
    
    actual fun selectCategory(category: Category?) {
        _currentCategory.value = category
        category?.let { loadStatements(it.id) }
    }
}