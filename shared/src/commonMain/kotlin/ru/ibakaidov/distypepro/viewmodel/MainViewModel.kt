package ru.ibakaidov.distypepro.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.structures.Statement
import ru.ibakaidov.distypepro.utils.ProgressState

expect class MainViewModel {
    val categories: StateFlow<List<Category>>
    val statements: StateFlow<List<Statement>>
    val currentCategory: StateFlow<Category?>
    val progressState: StateFlow<ProgressState>
    
    fun loadCategories()
    fun loadStatements(categoryId: String)
    fun addCategory(label: String)
    fun addStatement(categoryId: String, text: String)
    fun deleteCategory(categoryId: String)
    fun deleteStatement(statementId: String)
    fun selectCategory(category: Category?)
}