package ru.ibakaidov.distypepro.components

import android.view.Menu
import ru.ibakaidov.distypepro.R

object BankChromeMenuBinder {
    fun bind(menu: Menu, state: BankChromeState) {
        menu.findItem(R.id.action_add_category)?.isVisible = state.showAddCategoryAction
        menu.findItem(R.id.action_add_statement)?.isVisible = state.showAddStatementAction
        menu.findItem(R.id.action_sort)?.isVisible = state.showSortAction
        menu.findItem(R.id.action_import_global)?.isVisible = state.showImportGlobalAction
        menu.findItem(R.id.action_download_cache)?.apply {
            isVisible = state.showDownloadCacheAction
            isEnabled = state.isDownloadCacheActionEnabled
        }
    }
}
