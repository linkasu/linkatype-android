package ru.ibakaidov.distypepro.components

import ru.ibakaidov.distypepro.R

object BankActionHandler {
    fun handle(itemId: Int, bankGroup: BankGroup): Boolean {
        return when (itemId) {
            R.id.action_sort -> {
                bankGroup.onSortClicked()
                true
            }

            R.id.action_add_category,
            R.id.action_add_statement -> {
                bankGroup.onAddClicked()
                true
            }

            R.id.action_download_cache -> {
                bankGroup.onDownloadCacheClicked()
                true
            }

            R.id.action_import_global -> {
                bankGroup.onImportGlobalClicked()
                true
            }

            else -> false
        }
    }
}
