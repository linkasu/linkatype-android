package ru.ibakaidov.distypepro.data

import com.google.firebase.database.DatabaseReference
import ru.ibakaidov.distypepro.utils.Callback

actual abstract class Manager<T> {

    actual abstract fun getList(callback: Callback<Map<String, String>>)
    protected abstract fun getRoot(): DatabaseReference

    actual fun remove(key: String, callback: Callback<Unit>) {
        getRoot().child(key).removeValue { error, _ ->
            if (error != null) {
                callback.onError(error.toException())
            } else {
                callback.onDone(Unit)
            }
        }
    }

    actual abstract fun edit(key: String, value: String, callback: Callback<Unit>)
    actual abstract fun create(value: String, callback: Callback<Unit>)
}