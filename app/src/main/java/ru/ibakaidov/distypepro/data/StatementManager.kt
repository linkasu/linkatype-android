package ru.ibakaidov.distypepro.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.shared.repository.StatementsRepository
import ru.ibakaidov.distypepro.utils.Callback

class StatementManager(
    context: Context,
    private val categoryId: String,
    private val repository: StatementsRepository = SharedSdkProvider.get(context).statementsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : Manager<ru.ibakaidov.distypepro.shared.model.Statement>() {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override fun getList(callback: Callback<Map<String, String>>) {
        scope.launch {
            try {
                val list = repository.listByCategory(categoryId)
                val result = list
                    .sortedByDescending { it.created }
                    .associate { it.id to it.text }
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
                repository.update(id = key, text = value)
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
                repository.create(categoryId = categoryId, text = value)
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
