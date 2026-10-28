import SwiftUI

struct AddPostScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @ObservedObject private var store = CommunityStore.shared
    @Environment(\.appSizeClass) var sc
    
    @Binding var isPresented: Bool
    @State private var selectedCategory: PostCategory = .other
    @State private var postText = ""
    @FocusState private var isFocused: Bool
    
    private let maxChars = 500
    
    private var authorName: String {
        authVM.currentUser?.displayName?.components(separatedBy: " ").first
        ?? authVM.currentUser?.email?.components(separatedBy: "@").first
        ?? "You"
    }
    private var authorInitial: String { String(authorName.prefix(1)).uppercased() }
    
    private var isEmpty: Bool {
        postText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let tf  = AppSizing.TextField.forClass(sc)
        
        VStack(spacing: 0) {
            ETCScreenBackBar(title: "New post") { isPresented = false }
            
            ScrollView {
                VStack(spacing: pad.lg) {
                    
                    // ── Author row ─────────────────────────
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(AppTheme.Colors.primaryFallback)
                                .frame(width:  AppSizing.iconSize(base: 44, sizeClass: sc),
                                       height: AppSizing.iconSize(base: 44, sizeClass: sc))
                            Text(authorInitial)
                                .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc), weight: .bold))
                                .foregroundColor(.white)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text(authorName)
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            Text("Posting to Community")
                                .font(AppTheme.Typography.caption(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        }
                        Spacer()
                    }
                    
                    // ── Composer field (styled like ETCTextField) ──
                    VStack(alignment: .leading, spacing: 6) {
                        Text("What's on your mind?")
                            .font(AppTheme.Typography.subheadline(sc))
                            .fontWeight(.semibold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        // ── Category picker ─────────────────────────
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Post type")
                                .font(AppTheme.Typography.subheadline(sc))
                                .fontWeight(.semibold)
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            
                            HStack(spacing: 10) {
                                ForEach(PostCategory.allCases, id: \.self) { cat in
                                    Button {
                                        withAnimation(.easeInOut(duration: 0.15)) { selectedCategory = cat }
                                    } label: {
                                        // ✅ fixed — remove the stray inner .background(), swap the outer Color.white → surface
                                        HStack(spacing: 6) {
                                            Image(systemName: cat.icon)
                                                .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc)))
                                            Text(cat.rawValue)
                                                .font(AppTheme.Typography.subheadline(sc))
                                        }
                                        .foregroundColor(selectedCategory == cat ? .white : AppTheme.Colors.textPrimaryFallback)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 9)
                                        .frame(maxWidth: .infinity)
                                        .background(selectedCategory == cat ? cat.color : AppTheme.Colors.surface)
                                        .overlay(
                                            Capsule().stroke(
                                                selectedCategory == cat ? Color.clear : AppTheme.Colors.borderFallback,
                                                lineWidth: 1.5
                                            )
                                        )
                                        .clipShape(Capsule())
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        ZStack(alignment: .topLeading) {
                            if postText.isEmpty {
                                Text("Share a tip, ask a question, or celebrate a win…")
                                    .font(AppTheme.Typography.body(sc))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback.opacity(0.7))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 14)
                            }
                            TextEditor(text: $postText)
                                .focused($isFocused)
                                .font(AppTheme.Typography.body(sc))
                                .scrollContentBackground(.hidden)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .frame(minHeight: sc == .compact ? 160 : 220)
                                .onChange(of: postText) { _, newValue in
                                    if newValue.count > maxChars {
                                        postText = String(newValue.prefix(maxChars))
                                    }
                                }
                        }
                        .background(AppTheme.Colors.surface)
                        .overlay(
                            RoundedRectangle(cornerRadius: tf.cornerRadius)
                                .stroke(isFocused ? AppTheme.Colors.primaryFallback : AppTheme.Colors.borderFallback,
                                        lineWidth: isFocused ? 1.5 : 1)
                        )
                        .cornerRadius(tf.cornerRadius)
                        
                        HStack {
                            Spacer()
                            Text(String(format: NSLocalizedString("%d/%d", comment: ""), postText.count, maxChars))
                                .font(AppTheme.Typography.caption(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        }
                    }
                    
                    ETCButton(title: "Post", icon: "paperplane.fill") {
                        submit()
                    }
                    .disabled(isEmpty)
                    .opacity(isEmpty ? 0.5 : 1)
                    
                    Spacer(minLength: pad.xl)
                }
                .padding(.horizontal, pad.lg)
                .padding(.top, pad.md)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear { isFocused = true }
    }
    
    private func submit() {
        guard !isEmpty else { return }
        store.addPost(
            body: postText.trimmingCharacters(in: .whitespacesAndNewlines),
            category: selectedCategory,
            authorName: authorName
        )
        isPresented = false
    }
}
