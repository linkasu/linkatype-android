package ru.ibakaidov.distypepro.shared.testing

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ru.ibakaidov.distypepro.shared.db.LinkaDatabase

actual fun createTestDatabase(): LinkaDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    LinkaDatabase.Schema.create(driver)
    return LinkaDatabase(driver)
}
