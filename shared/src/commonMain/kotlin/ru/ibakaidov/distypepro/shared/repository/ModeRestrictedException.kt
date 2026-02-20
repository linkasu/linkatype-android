package ru.ibakaidov.distypepro.shared.repository

import ru.ibakaidov.distypepro.shared.session.AppMode

class ModeRestrictedException(
    val feature: String,
    val requiredMode: AppMode = AppMode.ONLINE,
) : IllegalStateException(
    "Feature '$feature' is available only in ${requiredMode.name.lowercase()} mode.",
)
