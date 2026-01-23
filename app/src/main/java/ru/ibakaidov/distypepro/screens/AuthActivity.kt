package ru.ibakaidov.distypepro.screens

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
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private enum class AuthMode { SignIn, SignUp }

    private lateinit var binding: ActivityAuthBinding
    private var authMode: AuthMode = AuthMode.SignIn
    private val sdk by lazy { SharedSdkProvider.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateModeUi()
    }

    private fun setupListeners() = with(binding) {
        authPrimaryButton.setOnClickListener { attemptAuth() }
        toggleAuthMode.setOnClickListener { toggleMode() }
        resetPasswordButton.setOnClickListener { attemptPasswordReset() }
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
        val isSignUp = authMode == AuthMode.SignUp
        authSubtitle.setText(if (isSignUp) R.string.auth_subtitle_sign_up else R.string.auth_subtitle_sign_in)
        authPrimaryButton.setText(if (isSignUp) R.string.auth_action_sign_up else R.string.auth_action_sign_in)
        toggleAuthMode.setText(if (isSignUp) R.string.auth_toggle_to_sign_in else R.string.auth_toggle_to_sign_up)
        confirmPasswordLayout.isVisible = isSignUp
        resetPasswordButton.isVisible = !isSignUp
        if (!isSignUp) {
            confirmPasswordInput.text = null
            confirmPasswordLayout.error = null
        }
    }

    private fun attemptAuth() {
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

        setLoading(true)

        lifecycleScope.launch {
            val result = runCatching {
                when (authMode) {
                    AuthMode.SignIn -> sdk.authRepository.login(email, password)
                    AuthMode.SignUp -> sdk.authRepository.register(email, password)
                }
            }

            result.onSuccess {
                if (authMode == AuthMode.SignUp) {
                    Snackbar.make(binding.root, R.string.auth_message_account_created, Snackbar.LENGTH_SHORT).show()
                }
                navigateToMain()
            }.onFailure { error ->
                setLoading(false)
                Snackbar.make(
                    binding.root,
                    error.localizedMessage ?: getString(R.string.auth_error_generic),
                    Snackbar.LENGTH_LONG
                ).show()
            }
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
                    Snackbar.LENGTH_LONG
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
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
