package ru.ibakaidov.distypepro.shared.auth

interface TokenStorage {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun clear()
}

expect abstract class PlatformContext

expect class SecureTokenStorage(context: PlatformContext) {
    fun get(): String?
    fun set(value: String?)
    fun clear()
}

class DefaultTokenStorage(
    private val secureStorage: SecureTokenStorage,
) : TokenStorage {
    private var accessToken: String? = null

    override fun getAccessToken(): String? = accessToken

    override fun setAccessToken(token: String?) {
        accessToken = token
    }

    override fun getRefreshToken(): String? = secureStorage.get()

    override fun setRefreshToken(token: String?) {
        secureStorage.set(token)
    }

    override fun clear() {
        accessToken = null
        secureStorage.clear()
    }
}
