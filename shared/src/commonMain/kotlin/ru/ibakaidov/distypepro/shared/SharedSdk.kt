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
import ru.ibakaidov.distypepro.shared.session.DefaultSessionRepository
import ru.ibakaidov.distypepro.shared.session.SessionStorage
import ru.ibakaidov.distypepro.shared.sync.ChangesSyncer
import ru.ibakaidov.distypepro.shared.sync.OfflineQueueProcessor

class SharedSdk(
    baseUrl: String,
    platformContext: PlatformContext,
) {
    val tokenStorage = DefaultTokenStorage(SecureTokenStorage(platformContext))
    val sessionRepository = DefaultSessionRepository(SessionStorage(platformContext))
    private val apiClient = ApiClient(baseUrl, tokenStorage)
    private val database = LinkaDatabaseFactory(DatabaseDriverFactory(platformContext)).create()
    private val localStore = LocalStore(database)

    val authRepository: AuthRepository = AuthRepositoryImpl(apiClient, tokenStorage, sessionRepository)
    val accountRepository: AccountRepository = AccountRepositoryImpl(apiClient, sessionRepository)
    val categoriesRepository: CategoriesRepository = CategoriesRepositoryImpl(apiClient, localStore, sessionRepository = sessionRepository)
    val statementsRepository: StatementsRepository = StatementsRepositoryImpl(apiClient, localStore, sessionRepository = sessionRepository)
    val userStateRepository: UserStateRepository = UserStateRepositoryImpl(apiClient, localStore, sessionRepository = sessionRepository)
    val dialogRepository: DialogRepository = DialogRepositoryImpl(apiClient, localStore, sessionRepository = sessionRepository)
    val globalRepository: GlobalRepository = GlobalRepositoryImpl(apiClient, sessionRepository)

    val offlineQueueProcessor = OfflineQueueProcessor(apiClient, localStore, sessionRepository)
    val changesSyncer = ChangesSyncer(apiClient, localStore, sessionRepository = sessionRepository)
}
