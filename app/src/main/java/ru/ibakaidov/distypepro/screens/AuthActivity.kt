package ru.ibakaidov.distypepro.screens

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityAuthBinding
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.shared.session.AppMode

class AuthActivity : AppCompatActivity() {

    private enum class AuthMode { SignIn, SignUp }

    private enum class LaunchMode { Online, Offline }

    private lateinit var binding: ActivityAuthBinding
    private var authMode: AuthMode = AuthMode.SignIn
    private var launchMode: LaunchMode = LaunchMode.Online
    private val sdk by lazy { SharedSdkProvider.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLaunchMode()
        setupListeners()
        updateModeUi()
    }

    private fun initLaunchMode() = with(binding) {
        val forceOnline = intent.getBooleanExtra(EXTRA_FORCE_ONLINE_MODE, false)
        launchMode = when {
            forceOnline -> LaunchMode.Online
            sdk.sessionRepository.getMode() == AppMode.OFFLINE -> LaunchMode.Offline
            else -> LaunchMode.Online
        }
        val selectedId = if (launchMode == LaunchMode.Online) {
            R.id.modeOnlineButton
        } else {
            R.id.modeOfflineButton
        }
        authModeToggleGroup.check(selectedId)
    }

    private fun setupListeners() = with(binding) {
        authPrimaryButton.setOnClickListener { attemptAuth() }
        toggleAuthMode.setOnClickListener { toggleMode() }
        resetPasswordButton.setOnClickListener { attemptPasswordReset() }

        authModeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            launchMode = if (checkedId == R.id.modeOfflineButton) {
                LaunchMode.Offline
            } else {
                LaunchMode.Online
            }
            updateModeUi()
        }

        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptAuth()
                true
            } else {
                false
            }
        }
        confirmPasswordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptAuth()
                true
            } else {
                false
            }
        }
    }

    private fun toggleMode() {
        authMode = if (authMode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
        updateModeUi()
    }

    private fun updateModeUi() = with(binding) {
        val isOnline = launchMode == LaunchMode.Online
        val isSignUp = authMode == AuthMode.SignUp

        authSubtitle.setText(
            when {
                !isOnline -> R.string.auth_subtitle_offline
                isSignUp -> R.string.auth_subtitle_sign_up
                else -> R.string.auth_subtitle_sign_in
            },
        )
        authPrimaryButton.setText(
            if (isOnline) {
                if (isSignUp) R.string.auth_action_sign_up else R.string.auth_action_sign_in
            } else {
                R.string.auth_action_continue_offline
            },
        )

        emailLayout.isVisible = isOnline
        passwordLayout.isVisible = isOnline
        confirmPasswordLayout.isVisible = isOnline && isSignUp
        resetPasswordButton.isVisible = isOnline && !isSignUp
        toggleAuthMode.isVisible = isOnline
        offlineInfoText.isVisible = !isOnline

        if (isOnline) {
            toggleAuthMode.setText(if (isSignUp) R.string.auth_toggle_to_sign_in else R.string.auth_toggle_to_sign_up)
        } else {
            clearErrors()
            confirmPasswordInput.text = null
        }
    }

    private fun attemptAuth() {
        if (launchMode == LaunchMode.Offline) {
            enterOfflineMode()
            return
        }
        attemptOnlineAuth()
    }

    private fun enterOfflineMode() {
        sdk.sessionRepository.setMode(AppMode.OFFLINE)
        sdk.sessionRepository.getOrCreateDeviceId()
        navigateToMain()
    }

    private fun attemptOnlineAuth() {
        clearErrors()

        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirmPassword = binding.confirmPasswordInput.text?.toString().orEmpty()

        var hasError = false

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.auth_error_invalid_email)
            hasError = true
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            binding.passwordLayout.error = getString(R.string.auth_error_password_length)
            hasError = true
        }

        if (authMode == AuthMode.SignUp && password != confirmPassword) {
            binding.confirmPasswordLayout.error = getString(R.string.auth_error_password_mismatch)
            hasError = true
        }

        if (hasError) return

        val previousMode = sdk.sessionRepository.getMode()
        sdk.sessionRepository.setMode(AppMode.ONLINE)
        sdk.sessionRepository.getOrCreateDeviceId()

        setLoading(true)

        lifecycleScope.launch {
            val result = runCatching {
                when (authMode) {
                    AuthMode.SignIn -> sdk.authRepository.login(email, password)
                    AuthMode.SignUp -> sdk.authRepository.register(email, password)
                }
            }

            result.onSuccess {
                val hasLocalData = previousMode == AppMode.OFFLINE &&
                    runCatching { sdk.localDataMigrationService.hasLocalDataForMigration() }.getOrDefault(false)

                if (hasLocalData) {
                    setLoading(false)
                    showMigrationChoiceDialog()
                    return@onSuccess
                }

                if (authMode == AuthMode.SignUp) {
                    Snackbar.make(binding.root, R.string.auth_message_account_created, Snackbar.LENGTH_SHORT).show()
                }
                navigateToMain()
            }.onFailure { error ->
                sdk.sessionRepository.setMode(previousMode)
                setLoading(false)
                Snackbar.make(
                    binding.root,
                    error.localizedMessage ?: getString(R.string.auth_error_generic),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun showMigrationChoiceDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.auth_migration_title)
            .setMessage(R.string.auth_migration_message)
            .setPositiveButton(R.string.auth_migration_sync) { _, _ ->
                runMigration(syncLocalData = true)
            }
            .setNegativeButton(R.string.auth_migration_replace) { _, _ ->
                runMigration(syncLocalData = false)
            }
            .setCancelable(false)
            .show()
    }

    private fun runMigration(syncLocalData: Boolean) {
        setLoading(true)
        lifecycleScope.launch {
            val migration = runCatching {
                if (syncLocalData) {
                    sdk.localDataMigrationService.syncLocalDataToRemote()
                } else {
                    sdk.localDataMigrationService.replaceLocalDataWithRemote()
                }
            }
            migration.onFailure {
                Snackbar.make(binding.root, R.string.auth_migration_failed, Snackbar.LENGTH_LONG).show()
            }
            setLoading(false)
            navigateToMain()
        }
    }

    private fun attemptPasswordReset() {
        clearErrors()

        val email = binding.emailInput.text?.toString()?.trim().orEmpty()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.auth_error_invalid_email)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = runCatching { sdk.authRepository.resetPassword(email) }
            setLoading(false)
            result.onSuccess {
                Snackbar.make(binding.root, R.string.auth_message_password_reset_sent, Snackbar.LENGTH_LONG).show()
            }.onFailure { error ->
                Snackbar.make(
                    binding.root,
                    error.localizedMessage ?: getString(R.string.auth_error_generic),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun clearErrors() {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        authProgress.isVisible = isLoading
        authPrimaryButton.isEnabled = !isLoading
        emailLayout.isEnabled = !isLoading
        passwordLayout.isEnabled = !isLoading
        confirmPasswordLayout.isEnabled = !isLoading
        resetPasswordButton.isEnabled = !isLoading
        toggleAuthMode.isEnabled = !isLoading
        modeOnlineButton.isEnabled = !isLoading
        modeOfflineButton.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_FORCE_ONLINE_MODE = "extra_force_online_mode"
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
