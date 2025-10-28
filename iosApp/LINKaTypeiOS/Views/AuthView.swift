import SwiftUI

struct AuthView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var isSignUpMode = false
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showResetPasswordAlert = false
    @State private var resetPasswordMessage: String?
    
    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [Color.blue.opacity(0.6), Color.purple.opacity(0.6)]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            VStack(spacing: 24) {
                Spacer()
                
                VStack(spacing: 8) {
                    Text(NSLocalizedString("welcome_back", comment: ""))
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.white)
                    
                    Text(NSLocalizedString(isSignUpMode ? "auth_subtitle_sign_up" : "auth_subtitle_sign_in", comment: ""))
                        .font(.system(size: 16))
                        .foregroundColor(.white.opacity(0.9))
                }
                .padding(.bottom, 32)
                
                VStack(spacing: 16) {
                    TextField(NSLocalizedString("auth_email_hint", comment: ""), text: $email)
                        .textFieldStyle(AuthTextFieldStyle())
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                    
                    SecureField(NSLocalizedString("auth_password_hint", comment: ""), text: $password)
                        .textFieldStyle(AuthTextFieldStyle())
                        .textContentType(.password)
                    
                    if isSignUpMode {
                        SecureField(NSLocalizedString("auth_confirm_password_hint", comment: ""), text: $confirmPassword)
                            .textFieldStyle(AuthTextFieldStyle())
                            .textContentType(.password)
                    }
                    
                    if let error = errorMessage {
                        Text(error)
                            .font(.system(size: 14))
                            .foregroundColor(.red)
                            .padding(.horizontal)
                    }
                    
                    Button(action: attemptAuth) {
                        if isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text(NSLocalizedString(isSignUpMode ? "auth_action_sign_up" : "auth_action_sign_in", comment: ""))
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.white)
                    .foregroundColor(.blue)
                    .cornerRadius(12)
                    .padding(.horizontal, 32)
                    .disabled(isLoading)
                    
                    if !isSignUpMode {
                        Button(action: { showResetPasswordAlert = true }) {
                            Text(NSLocalizedString("auth_action_reset_password", comment: ""))
                                .font(.system(size: 14))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 8)
                    }
                    
                    Button(action: { isSignUpMode.toggle() }) {
                        Text(NSLocalizedString(isSignUpMode ? "auth_toggle_to_sign_in" : "auth_toggle_to_sign_up", comment: ""))
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                    }
                    .padding(.top, 16)
                }
                .padding(.horizontal, 32)
                
                Spacer()
            }
        }
        .alert(NSLocalizedString("auth_action_reset_password", comment: ""), isPresented: $showResetPasswordAlert) {
            TextField(NSLocalizedString("auth_email_hint", comment: ""), text: $email)
            Button(NSLocalizedString("ok", comment: "")) {
                attemptPasswordReset()
            }
            Button(NSLocalizedString("cancel", comment: ""), role: .cancel) {}
        } message: {
            if let message = resetPasswordMessage {
                Text(message)
            }
        }
    }
    
    private func attemptAuth() {
        errorMessage = nil
        
        guard isValidEmail(email) else {
            errorMessage = NSLocalizedString("auth_error_invalid_email", comment: "")
            return
        }
        
        guard password.count >= 6 else {
            errorMessage = NSLocalizedString("auth_error_password_length", comment: "")
            return
        }
        
        if isSignUpMode && password != confirmPassword {
            errorMessage = NSLocalizedString("auth_error_password_mismatch", comment: "")
            return
        }
        
        isLoading = true
        
        Task {
            do {
                if isSignUpMode {
                    try await authManager.signUp(email: email, password: password)
                } else {
                    try await authManager.signIn(email: email, password: password)
                }
                await MainActor.run {
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    private func attemptPasswordReset() {
        guard isValidEmail(email) else {
            resetPasswordMessage = NSLocalizedString("auth_error_invalid_email", comment: "")
            return
        }
        
        Task {
            do {
                try await authManager.resetPassword(email: email)
                await MainActor.run {
                    resetPasswordMessage = NSLocalizedString("auth_message_password_reset_sent", comment: "")
                }
            } catch {
                await MainActor.run {
                    resetPasswordMessage = error.localizedDescription
                }
            }
        }
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }
}

struct AuthTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding()
            .background(Color.white.opacity(0.2))
            .cornerRadius(12)
            .foregroundColor(.white)
            .accentColor(.white)
    }
}

