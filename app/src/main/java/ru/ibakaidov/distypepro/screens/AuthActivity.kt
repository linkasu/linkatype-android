package ru.ibakaidov.distypepro.screens

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private enum class AuthMode { SignIn, SignUp }

    private lateinit var binding: ActivityAuthBinding
    private var authMode: AuthMode = AuthMode.SignIn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Firebase.auth.currentUser != null) {
            navigateToMain()
            return
        }

        setupListeners()
        updateModeUi()
    }

    private fun setupListeners() = with(binding) {
        authPrimaryButton.setOnClickListener { attemptAuth() }
        toggleAuthMode.setOnClickListener { toggleMode() }
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

        val task = when (authMode) {
            AuthMode.SignIn -> Firebase.auth.signInWithEmailAndPassword(email, password)
            AuthMode.SignUp -> Firebase.auth.createUserWithEmailAndPassword(email, password)
        }

        task.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                if (authMode == AuthMode.SignUp) {
                    Snackbar.make(binding.root, R.string.auth_message_account_created, Snackbar.LENGTH_SHORT).show()
                }
                navigateToMain()
            } else {
                setLoading(false)
                Snackbar.make(
                    binding.root,
                    result.exception?.localizedMessage ?: getString(R.string.auth_error_generic),
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
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
