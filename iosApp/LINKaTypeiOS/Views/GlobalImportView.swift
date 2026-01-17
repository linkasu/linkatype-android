import SwiftUI
import Shared

struct GlobalImportView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var manager = GlobalImportManager()
    @State private var snackbarMessage: String?
    @State private var showSnackbar = false

    var body: some View {
        List {
            if manager.isLoading {
                HStack {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
            } else if manager.categories.isEmpty {
                Text(NSLocalizedString("global_import_empty", comment: ""))
                    .foregroundColor(.secondary)
            } else {
                ForEach(manager.categories, id: \.id) { category in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(category.label)
                                .font(.headline)
                            Text(globalCountLabel(for: category))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        if manager.importingId == category.id {
                            ProgressView()
                        } else {
                            Button(NSLocalizedString("global_import_import", comment: "")) {
                                Task {
                                    let status = await manager.importCategory(id: category.id)
                                    let message = statusMessage(status)
                                    if let message = message {
                                        showSnackbarMessage(message)
                                    }
                                    NotificationCenter.default.post(name: .realtimeDidUpdate, object: nil)
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                }
            }
        }
        .navigationTitle(NSLocalizedString("global_import_title", comment: ""))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(NSLocalizedString("ok", comment: "")) {
                    dismiss()
                }
            }
        }
        .overlay(alignment: .bottom) {
            if showSnackbar, let message = snackbarMessage {
                SnackbarView(message: message)
                    .padding()
                    .transition(.move(edge: .bottom))
            }
        }
        .task {
            await manager.loadCategories()
        }
    }

    private func globalCountLabel(for category: GlobalCategory) -> String {
        let count = category.statements?.count ?? 0
        let format = NSLocalizedString("global_import_count", comment: "")
        return String(format: format, count)
    }

    private func statusMessage(_ status: String?) -> String? {
        switch status {
        case "exists":
            return NSLocalizedString("global_import_exists", comment: "")
        case "ok":
            return NSLocalizedString("global_import_done", comment: "")
        case .none:
            return NSLocalizedString("global_import_failed", comment: "")
        default:
            return NSLocalizedString("global_import_done", comment: "")
        }
    }

    private func showSnackbarMessage(_ message: String) {
        snackbarMessage = message
        withAnimation {
            showSnackbar = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            withAnimation {
                showSnackbar = false
            }
        }
    }
}
