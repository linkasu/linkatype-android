package ru.ibakaidov.distypepro.shared.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import ru.ibakaidov.distypepro.shared.auth.PlatformContext

actual class DatabaseDriverFactory actual constructor(private val context: PlatformContext) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(LinkaDatabase.Schema, context, "linka.db")
    }
}
