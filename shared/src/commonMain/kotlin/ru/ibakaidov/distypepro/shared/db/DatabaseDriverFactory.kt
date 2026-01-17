package ru.ibakaidov.distypepro.shared.db

import app.cash.sqldelight.db.SqlDriver
import ru.ibakaidov.distypepro.shared.auth.PlatformContext

expect class DatabaseDriverFactory(context: PlatformContext) {
    fun createDriver(): SqlDriver
}

class LinkaDatabaseFactory(private val driverFactory: DatabaseDriverFactory) {
    fun create(): LinkaDatabase = LinkaDatabase(driverFactory.createDriver())
}
