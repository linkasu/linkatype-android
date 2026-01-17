package ru.ibakaidov.distypepro.shared.testing

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import ru.ibakaidov.distypepro.shared.db.LinkaDatabase

actual fun createTestDatabase(): LinkaDatabase {
    val driver = NativeSqliteDriver(LinkaDatabase.Schema, ":memory:")
    return LinkaDatabase(driver)
}
