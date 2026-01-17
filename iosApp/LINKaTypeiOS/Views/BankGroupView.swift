import SwiftUI

struct BankGroupView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    @StateObject private var categoryManager = FirebaseCategoryManager()
    @StateObject private var statementManager = FirebaseStatementManager()
    @ObservedObject var ttsManager: TtsManager
    
    @State private var showingStatements = false
    @State private var currentCategoryId: String?
    @State private var currentCategoryTitle = ""
    @State private var showInputDialog = false
    @State private var inputDialogTitle = ""
    @State private var inputDialogValue = ""
    @State private var inputDialogAction: ((String) -> Void)?
    @State private var showConfirmDialog = false
    @State private var confirmDialogAction: (() -> Void)?
    @State private var sortMode: SortMode = .alphabetAsc
    @State private var showSortSheet = false
    @State private var isDownloading = false
    @State private var downloadProgress = 0
    @State private var downloadTotal = 0
    @State private var showDownloadAlert = false
    @State private var showGlobalImport = false
    
    private let gridColumns = [
        GridItem(.adaptive(minimum: 150), spacing: 12)
    ]
    
    var body: some View {
        VStack(spacing: 0) {
            toolbar
            
            ScrollView {
                LazyVGrid(columns: gridColumns, spacing: 12) {
                    if showingStatements {
                        ForEach(sortedStatements, id: \.key) { key, value in
                            BankItemView(label: value)
                                .onTapGesture {
                                    ttsManager.speak(value)
                                }
                                .contextMenu {
                                    Button("edit") {
                                        showEditDialog(key: key, value: value)
                                    }
                                    Button("remove", role: .destructive) {
                                        confirmRemoval(key: key)
                                    }
                                }
                        }
                    } else {
                        ForEach(sortedCategories, id: \.key) { key, value in
                            BankItemView(label: value)
                                .onTapGesture {
                                    selectCategory(key: key, title: value)
                                }
                                .contextMenu {
                                    Button("edit") {
                                        showEditDialog(key: key, value: value)
                                    }
                                    Button("remove", role: .destructive) {
                                        confirmRemoval(key: key)
                                    }
                                }
                        }
                    }
                }
                .padding()
            }
        }
        .onAppear {
            if let userId = authManager.getUserId() {
                categoryManager.startListening(userId: userId)
            }
        }
        .onDisappear {
            categoryManager.stopListening()
            statementManager.stopListening()
        }
        .sheet(isPresented: $showInputDialog) {
            InputDialogView(
                title: inputDialogTitle,
                value: $inputDialogValue,
                onSave: { value in
                    inputDialogAction?(value)
                    inputDialogValue = ""
                }
            )
        }
        .sheet(isPresented: $showGlobalImport) {
            NavigationStack {
                GlobalImportView()
            }
        }
        .alert(NSLocalizedString("remove", comment: ""), isPresented: $showConfirmDialog) {
            Button(NSLocalizedString("remove", comment: ""), role: .destructive) {
                confirmDialogAction?()
            }
            Button(NSLocalizedString("cancel", comment: ""), role: .cancel) {}
        }
        .alert(NSLocalizedString("bank_download_cache_title", comment: ""), isPresented: $showDownloadAlert) {
            Button(NSLocalizedString("ok", comment: "")) {
                isDownloading = false
            }
        } message: {
            Text(String(format: NSLocalizedString("bank_download_cache_progress", comment: ""), downloadProgress, downloadTotal))
        }
        .actionSheet(isPresented: $showSortSheet) {
            ActionSheet(
                title: Text(NSLocalizedString("bank_action_sort", comment: "")),
                buttons: [
                    .default(Text(NSLocalizedString("bank_sort_alpha_asc", comment: ""))) {
                        sortMode = .alphabetAsc
                    },
                    .default(Text(NSLocalizedString("bank_sort_alpha_desc", comment: ""))) {
                        sortMode = .alphabetDesc
                    },
                    .cancel(Text(NSLocalizedString("cancel", comment: "")))
                ]
            )
        }
        .onReceive(NotificationCenter.default.publisher(for: .realtimeDidUpdate)) { _ in
            if showingStatements {
                statementManager.refresh()
            } else {
                categoryManager.refresh()
            }
        }
    }
    
    private var toolbar: some View {
        HStack {
            if showingStatements {
                Button(action: { showingStatements = false }) {
                    Image(systemName: "chevron.left")
                }
            }
            
            Text(showingStatements ? String(format: NSLocalizedString("bank_toolbar_title_statements", comment: ""), currentCategoryTitle) : NSLocalizedString("bank_toolbar_title_categories", comment: ""))
                .font(.headline)
            
            Spacer()
            
            Button(action: { showSortSheet = true }) {
                Image(systemName: "arrow.up.arrow.down")
            }

            if !showingStatements {
                Button(action: { showGlobalImport = true }) {
                    Image(systemName: "tray.and.arrow.down")
                }
            }

            Button(action: showAddDialog) {
                Image(systemName: "plus")
            }
            
            if showingStatements {
                Button(action: downloadCurrentCategoryToCache) {
                    Image(systemName: "arrow.down.circle")
                }
                .disabled(isDownloading)
            }
        }
        .padding()
        .background(Color(.systemGray6))
    }
    
    private var sortedCategories: [(key: String, value: String)] {
        let sorted = categoryManager.categories.sorted { item1, item2 in
            switch sortMode {
            case .alphabetAsc:
                return item1.value.localizedCaseInsensitiveCompare(item2.value) == .orderedAscending
            case .alphabetDesc:
                return item1.value.localizedCaseInsensitiveCompare(item2.value) == .orderedDescending
            }
        }
        return sorted
    }
    
    private var sortedStatements: [(key: String, value: String)] {
        let sorted = statementManager.statements.sorted { item1, item2 in
            switch sortMode {
            case .alphabetAsc:
                return item1.value.localizedCaseInsensitiveCompare(item2.value) == .orderedAscending
            case .alphabetDesc:
                return item1.value.localizedCaseInsensitiveCompare(item2.value) == .orderedDescending
            }
        }
        return sorted
    }
    
    private func selectCategory(key: String, title: String) {
        currentCategoryId = key
        currentCategoryTitle = title
        showingStatements = true
        
        if let userId = authManager.getUserId() {
            statementManager.startListening(userId: userId, categoryId: key)
        }
    }
    
    private func showAddDialog() {
        inputDialogTitle = NSLocalizedString("create", comment: "")
        inputDialogValue = ""
        inputDialogAction = { value in
            guard let userId = authManager.getUserId() else { return }
            
            Task {
                do {
                    if showingStatements, let categoryId = currentCategoryId {
                        try await statementManager.create(label: value, userId: userId, categoryId: categoryId)
                    } else {
                        try await categoryManager.create(label: value, userId: userId)
                    }
                } catch { }
            }
        }
        showInputDialog = true
    }
    
    private func showEditDialog(key: String, value: String) {
        inputDialogTitle = NSLocalizedString("edit", comment: "")
        inputDialogValue = value
        inputDialogAction = { newValue in
            guard let userId = authManager.getUserId() else { return }
            
            Task {
                do {
                    if showingStatements, let categoryId = currentCategoryId {
                        try await statementManager.edit(id: key, label: newValue, userId: userId, categoryId: categoryId)
                    } else {
                        try await categoryManager.edit(id: key, label: newValue, userId: userId)
                    }
                } catch { }
            }
        }
        showInputDialog = true
    }
    
    private func confirmRemoval(key: String) {
        confirmDialogAction = {
            guard let userId = authManager.getUserId() else { return }
            
            Task {
                do {
                    if showingStatements, let categoryId = currentCategoryId {
                        try await statementManager.remove(id: key, userId: userId, categoryId: categoryId)
                    } else {
                        try await categoryManager.remove(id: key, userId: userId)
                    }
                } catch {
                    print("Error removing: \(error)")
                }
            }
        }
        showConfirmDialog = true
    }
    
    private func downloadCurrentCategoryToCache() {
        let phrases = Array(statementManager.statements.values)
        guard !phrases.isEmpty else { return }
        
        FirebaseAnalyticsManager.shared.logDownloadCategoryCacheEvent()
        
        isDownloading = true
        downloadProgress = 0
        downloadTotal = phrases.count
        showDownloadAlert = true
        
        ttsManager.downloadPhrasesToCache(phrases, voice: "current") { current, total in
            downloadProgress = current
            downloadTotal = total
            if current >= total {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    isDownloading = false
                    showDownloadAlert = false
                }
            }
        }
    }
}

struct BankItemView: View {
    let label: String
    
    var body: some View {
        Text(label)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color(.systemGray5))
            .cornerRadius(12)
            .lineLimit(2)
    }
}

struct InputDialogView: View {
    let title: String
    @Binding var value: String
    let onSave: (String) -> Void
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationStack {
            VStack {
                TextField("", text: $value)
                    .textFieldStyle(.roundedBorder)
                    .padding()
                
                Spacer()
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("cancel", comment: "")) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("ok", comment: "")) {
                        onSave(value)
                        dismiss()
                    }
                    .disabled(value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

enum SortMode {
    case alphabetAsc
    case alphabetDesc
}
