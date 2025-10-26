package ru.ibakaidov.distypepro.data

import com.google.firebase.database.DatabaseReference
import ru.ibakaidov.distypepro.utils.Callback

abstract class Manager<T> {

    abstract fun getList(callback: Callback<Map<String, String>>)
    protected abstract fun getRoot(): DatabaseReference

    fun remove(key: String, callback: Callback<Unit>) {
        getRoot().child(key).removeValue { error, _ ->
            if (error != null) {
                callback.onError(error.toException())
            } else {
                callback.onDone(Unit)
            }
        }
    }

    abstract fun edit(key: String, value: String, callback: Callback<Unit>)

    abstract fun create(value: String, callback: Callback<Unit>)
}
