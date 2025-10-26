package ru.ibakaidov.distypepro.data

import ru.ibakaidov.distypepro.utils.Callback

actual abstract class Manager<T> {

    actual abstract fun getList(callback: Callback<Map<String, String>>)
    actual abstract fun remove(key: String, callback: Callback<Unit>)
    actual abstract fun edit(key: String, value: String, callback: Callback<Unit>)
    actual abstract fun create(value: String, callback: Callback<Unit>)
}