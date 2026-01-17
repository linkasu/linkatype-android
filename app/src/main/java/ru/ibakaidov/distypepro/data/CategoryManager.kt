package ru.ibakaidov.distypepro.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.shared.repository.CategoriesRepository
import ru.ibakaidov.distypepro.utils.Callback

class CategoryManager(
    context: Context,
    private val repository: CategoriesRepository = SharedSdkProvider.get(context).categoriesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : Manager<ru.ibakaidov.distypepro.shared.model.Category>() {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override fun getList(callback: Callback<Map<String, String>>) {
        scope.launch {
            try {
                val list = repository.list()
                val result = list
                    .sortedByDescending { it.created }
                    .associate { it.id to it.label }
                withContext(mainDispatcher) {
                    callback.onDone(result)
                }
            } catch (ex: Exception) {
                withContext(mainDispatcher) {
                    callback.onError(ex)
                }
            }
        }
    }

    override fun edit(key: String, value: String, callback: Callback<Unit>) {
        scope.launch {
            try {
                repository.update(id = key, label = value, aiUse = null)
                withContext(mainDispatcher) {
                    callback.onDone(Unit)
                }
            } catch (ex: Exception) {
                withContext(mainDispatcher) {
                    callback.onError(ex)
                }
            }
        }
    }

    override fun create(value: String, callback: Callback<Unit>) {
        scope.launch {
            try {
                repository.create(label = value)
                withContext(mainDispatcher) {
                    callback.onDone(Unit)
                }
            } catch (ex: Exception) {
                withContext(mainDispatcher) {
                    callback.onError(ex)
                }
            }
        }
    }

    override fun remove(key: String, callback: Callback<Unit>) {
        scope.launch {
            try {
                repository.delete(key)
                withContext(mainDispatcher) {
                    callback.onDone(Unit)
                }
            } catch (ex: Exception) {
                withContext(mainDispatcher) {
                    callback.onError(ex)
                }
            }
        }
    }
}
