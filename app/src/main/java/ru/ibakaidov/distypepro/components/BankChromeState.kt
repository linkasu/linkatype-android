package ru.ibakaidov.distypepro.components

data class BankChromeState(
    val title: String,
    val canNavigateBack: Boolean,
    val showingStatements: Boolean,
    val isDownloading: Boolean,
) {
    val showSortAction: Boolean
        get() = true

    val showAddCategoryAction: Boolean
        get() = !showingStatements

    val showAddStatementAction: Boolean
        get() = showingStatements

    val showImportGlobalAction: Boolean
        get() = !showingStatements

    val showDownloadCacheAction: Boolean
        get() = showingStatements

    val isDownloadCacheActionEnabled: Boolean
        get() = showDownloadCacheAction && !isDownloading
}
