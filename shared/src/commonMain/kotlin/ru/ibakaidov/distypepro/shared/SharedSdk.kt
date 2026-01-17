package ru.ibakaidov.distypepro.shared

import ru.ibakaidov.distypepro.shared.api.ApiClient
import ru.ibakaidov.distypepro.shared.auth.DefaultTokenStorage
import ru.ibakaidov.distypepro.shared.auth.PlatformContext
import ru.ibakaidov.distypepro.shared.auth.SecureTokenStorage
import ru.ibakaidov.distypepro.shared.db.DatabaseDriverFactory
import ru.ibakaidov.distypepro.shared.db.LinkaDatabaseFactory
import ru.ibakaidov.distypepro.shared.db.LocalStore
import ru.ibakaidov.distypepro.shared.repository.AuthRepository
import ru.ibakaidov.distypepro.shared.repository.AuthRepositoryImpl
import ru.ibakaidov.distypepro.shared.repository.AccountRepository
import ru.ibakaidov.distypepro.shared.repository.AccountRepositoryImpl
import ru.ibakaidov.distypepro.shared.repository.CategoriesRepository
import ru.ibakaidov.distypepro.shared.repository.CategoriesRepositoryImpl
import ru.ibakaidov.distypepro.shared.repository.DialogRepository
import ru.ibakaidov.distypepro.shared.repository.DialogRepositoryImpl
import ru.ibakaidov.distypepro.shared.repository.GlobalRepository
import ru.ibakaidov.distypepro.shared.repository.GlobalRepositoryImpl
import ru.ibakaidov.distypepro.shared.repository.StatementsRepository
import ru.ibakaidov.distypepro.shared.repository.StatementsRepositoryImpl
import ru.ibakaidov.distypepro.shared.repository.UserStateRepository
import ru.ibakaidov.distypepro.shared.repository.UserStateRepositoryImpl
import ru.ibakaidov.distypepro.shared.sync.ChangesSyncer
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor

class SharedSdk(
    baseUrl: String,
    platformContext: PlatformContext,
) {
    val tokenStorage = DefaultTokenStorage(SecureTokenStorage(platformContext))
    private val apiClient = ApiClient(baseUrl, tokenStorage)
    private val database = LinkaDatabaseFactory(DatabaseDriverFactory(platformContext)).create()
    private val localStore = LocalStore(database)

    val authRepository: AuthRepository = AuthRepositoryImpl(apiClient, tokenStorage)
    val accountRepository: AccountRepository = AccountRepositoryImpl(apiClient)
    val categoriesRepository: CategoriesRepository = CategoriesRepositoryImpl(apiClient, localStore)
    val statementsRepository: StatementsRepository = StatementsRepositoryImpl(apiClient, localStore)
    val userStateRepository: UserStateRepository = UserStateRepositoryImpl(apiClient, localStore)
    val dialogRepository: DialogRepository = DialogRepositoryImpl(apiClient, localStore)
    val globalRepository: GlobalRepository = GlobalRepositoryImpl(apiClient)

    val offlineQueueProcessor = OfflineQueueProcessor(apiClient, localStore)
    val changesSyncer = ChangesSyncer(apiClient, localStore)
}
