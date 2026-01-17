package ru.ibakaidov.distypepro.data

import ru.ibakaidov.distypepro.utils.Callback

abstract class Manager<T> {

    abstract fun getList(callback: Callback<Map<String, String>>)

    abstract fun remove(key: String, callback: Callback<Unit>)

    abstract fun edit(key: String, value: String, callback: Callback<Unit>)

    abstract fun create(value: String, callback: Callback<Unit>)
}
