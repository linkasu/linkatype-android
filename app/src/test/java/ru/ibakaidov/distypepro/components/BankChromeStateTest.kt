package ru.ibakaidov.distypepro.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankChromeStateTest {

    @Test
    fun categoriesState_showsCategoryActionsAndImport() {
        val state = BankChromeState(
            title = "Categories",
            canNavigateBack = false,
            showingStatements = false,
            isDownloading = false,
        )

        assertTrue(state.showSortAction)
        assertTrue(state.showAddCategoryAction)
        assertFalse(state.showAddStatementAction)
        assertTrue(state.showImportGlobalAction)
        assertFalse(state.showDownloadCacheAction)
        assertFalse(state.isDownloadCacheActionEnabled)
    }

    @Test
    fun statementsState_showsStatementActionsAndDownload() {
        val state = BankChromeState(
            title = "Statements",
            canNavigateBack = true,
            showingStatements = true,
            isDownloading = false,
        )

        assertTrue(state.showSortAction)
        assertFalse(state.showAddCategoryAction)
        assertTrue(state.showAddStatementAction)
        assertFalse(state.showImportGlobalAction)
        assertTrue(state.showDownloadCacheAction)
        assertTrue(state.isDownloadCacheActionEnabled)
    }

    @Test
    fun downloadingState_disablesDownloadAction() {
        val state = BankChromeState(
            title = "Statements",
            canNavigateBack = true,
            showingStatements = true,
            isDownloading = true,
        )

        assertTrue(state.showDownloadCacheAction)
        assertFalse(state.isDownloadCacheActionEnabled)
    }
}
