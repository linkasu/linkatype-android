import SwiftUI

struct MigrationDecisionView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    @State private var isLoading = false

    var body: some View {
        VStack(spacing: 20) {
            Text(NSLocalizedString("auth_migration_title", comment: ""))
                .font(.title2)
                .fontWeight(.semibold)

            Text(NSLocalizedString("auth_migration_message", comment: ""))
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)

            if let error = authManager.migrationError {
                Text(error)
                    .font(.footnote)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
            }

            Button(action: { resolveMigration(syncLocalData: true) }) {
                if isLoading {
                    ProgressView().frame(maxWidth: .infinity)
                } else {
                    Text(NSLocalizedString("auth_migration_sync", comment: ""))
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading)

            Button(action: { resolveMigration(syncLocalData: false) }) {
                Text(NSLocalizedString("auth_migration_replace", comment: ""))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .disabled(isLoading)
        }
        .padding(24)
    }

    private func resolveMigration(syncLocalData: Bool) {
        isLoading = true
        Task {
            await authManager.resolveMigration(syncLocalData: syncLocalData)
            await MainActor.run {
                isLoading = false
            }
        }
    }
}
