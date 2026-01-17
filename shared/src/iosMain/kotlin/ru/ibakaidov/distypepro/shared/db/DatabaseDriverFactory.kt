package ru.ibakaidov.distypepro.shared.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import ru.ibakaidov.distypepro.shared.auth.PlatformContext

actual class DatabaseDriverFactory actual constructor(context: PlatformContext) {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(LinkaDatabase.Schema, "linka.db")
    }
}
