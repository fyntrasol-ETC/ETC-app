import SwiftUI

struct CommunityScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @ObservedObject private var store = CommunityStore.shared
    @Environment(\.appSizeClass) var sc
    @Binding var selectedTab: ETCTab
    @Binding var showTabBar: Bool

    @State private var selectedFilter: CommunityFilter = .forYou
    @State private var showSearch        = false
    @State private var showNotifications = false
    @State private var showAddPost       = false
    @State private var activeCommentsPostId: String? = nil

    enum CommunityFilter: String, CaseIterable {
        case forYou    = "For you"
        case questions = "Questions"
        case tips      = "Tips"
        case featured  = "Featured"
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                ETCScreenHeader(
                    title: "Community",
                    subtitle: "Learn together",
                    onSearch: { showSearch = true },
                    onNotification: { showNotifications = true },
                    notificationBadge: 3,
                    style: .plain
                )

                filterChips

                ScrollView(showsIndicators: false) {
                    VStack(spacing: AppSizing.Padding.forClass(sc).sm) {
                        ForEach(filteredPosts) { post in
                            CommunityPostCard(
                                post: post,
                                onLike: { store.toggleLike(postId: post.id) },
                                onComment: { activeCommentsPostId = post.id },
                                onDelete: { store.deletePost(postId: post.id) },
                                onEdit: { newText in store.editPost(postId: post.id, newText: newText) }   // NEW
                            )
                        }
                    }
                    .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                    .padding(.top, AppSizing.Padding.forClass(sc).md)
                    .padding(.bottom, 110)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
            .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())

            // ── Floating "add post" button ─────────────
            addPostFAB
                .padding(.trailing, AppSizing.Padding.forClass(sc).lg)
                .padding(.bottom, 90)

            ETCTabBar(selectedTab: $selectedTab)
                .ignoresSafeArea(edges: .bottom)
                .frame(maxHeight: .infinity, alignment: .bottom)
        }
        .fullScreenCover(isPresented: $showSearch) {
            ETCSearchScreen(isPresented: $showSearch)
                .environmentObject(authVM)
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showNotifications) {
            ETCNotificationsScreen(isPresented: $showNotifications)
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showAddPost) {
            AddPostScreen(isPresented: $showAddPost)
                .environmentObject(authVM)
                .appSizeClass()
        }
                .sheet(item: $activeCommentsPostId.asIdentifiableBinding()) { item in
                    CommentsSheet(postId: item.id)
                        .environmentObject(authVM)
                        .appSizeClass()
                }
        }
    
    private var filteredPosts: [CommunityPost] {
        switch selectedFilter {
        case .forYou, .featured:
            return store.posts
        case .questions:
            return store.posts.filter { $0.category == .question }
        case .tips:
            return store.posts.filter { $0.category == .tip }
        }
    }
    // MARK: - Floating Action Button
    private var addPostFAB: some View {
        let size: CGFloat = sc == .compact ? 58 : 74
        return Button {
            showAddPost = true
        } label: {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.greenFallback],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: size, height: size)
                Image(systemName: "plus")
                    .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc), weight: .bold))
                    .foregroundColor(.white)
            }
            .shadow(color: AppTheme.Colors.primaryFallback.opacity(0.35), radius: 12, y: 6)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Filter Chips
    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(CommunityFilter.allCases, id: \.self) { filter in
                    Button {
                        withAnimation(.easeInOut(duration: 0.18)) { selectedFilter = filter }
                    } label: {
                        Text(filter.rawValue)
                            .font(AppTheme.Typography.subheadline(sc))
                            .fontWeight(selectedFilter == filter ? .semibold : .regular)
                            .foregroundColor(selectedFilter == filter ? .white : AppTheme.Colors.textPrimaryFallback)
                            .padding(.horizontal, 18)
                            .padding(.vertical, 9)
                         .background(selectedFilter == filter ? AppTheme.Colors.primaryFallback : AppTheme.Colors.surface)
                            .overlay(
                                Capsule().stroke(
                                    selectedFilter == filter ? Color.clear : AppTheme.Colors.borderFallback,
                                    lineWidth: 1.5
                                )
                            )
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
            .padding(.vertical, AppSizing.Padding.forClass(sc).sm)
        }
        .background(AppTheme.Colors.backgroundFallback)
        .shadow(color: .black.opacity(0.04), radius: 6, x: 0, y: 2)
    }
}

private struct CommunityPostCard: View {
    let post: CommunityPost
    let onLike: () -> Void
    let onComment: () -> Void
    let onDelete: () -> Void
    let onEdit: (String) -> Void          // NEW
    @Environment(\.appSizeClass) var sc
    @State private var showDeleteConfirm = false
    @State private var isEditing = false      // NEW
    @State private var editText = ""          // NEW
    @FocusState private var isEditFocused: Bool // NEW

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(alignment: .leading, spacing: pad.md) {

            // ── Author row ──────────────────────────────
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(post.avatarColor)
                        .frame(width:  AppSizing.iconSize(base: 44, sizeClass: sc),
                               height: AppSizing.iconSize(base: 44, sizeClass: sc))

                    Text(post.initial)
                        .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc), weight: .bold))
                        .foregroundColor(.white)
                }

                Spacer()

                VStack(alignment: .center, spacing: 2) {
                    Text(post.name)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text(post.time)
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Spacer()

                // Always reserves the same width, whether or not the menu is shown —
                // this is what keeps Zezo's row and Lilly's row visually identical.
                Group {
                    if post.isMine && !isEditing {
                        Menu {
                            Button {
                                editText = post.body
                                isEditing = true
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive) {
                                showDeleteConfirm = true
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        } label: {
                            Image(systemName: "ellipsis")
                                .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                .rotationEffect(.degrees(90))
                                .padding(8)
                                .contentShape(Rectangle())
                        }
                    } else {
                        Color.clear
                    }
                }
                .frame(width: 32, height: 32)
            }
            // ── Body / inline editor ────────────────────
            if isEditing {
                VStack(alignment: .leading, spacing: 8) {
                    TextField("Edit your post…", text: $editText, axis: .vertical)
                        .font(AppTheme.Typography.body(sc))
                        .focused($isEditFocused)
                        .lineLimit(1...8)
                        .padding(10)
                     .background(AppTheme.Colors.surface)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(AppTheme.Colors.primaryFallback, lineWidth: 1.5)
                        )
                        .cornerRadius(10)

                    HStack {
                        Spacer()
                        Button("Cancel") { isEditing = false }
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                        Button("Save") {
                            let trimmed = editText.trimmingCharacters(in: .whitespacesAndNewlines)
                            guard !trimmed.isEmpty else { return }
                            onEdit(trimmed)
                            isEditing = false
                        }
                        .font(AppTheme.Typography.subheadline(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                }
                .onAppear { isEditFocused = true }
            } else {
                Text(post.body)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            // ── Actions ─────────────────────────────────
            HStack(spacing: pad.lg) {
                Button(action: onLike) {
                    HStack(spacing: 6) {
                        Image(systemName: post.likedByMe ? "heart.fill" : "heart")
                            .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                            .foregroundColor(post.likedByMe ? AppTheme.Colors.red : AppTheme.Colors.textSecondaryFallback)
                        Text("\(post.likeCount)")
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                }
                .buttonStyle(.plain)

                Button(action: onComment) {
                    HStack(spacing: 6) {
                        Image(systemName: "bubble.right")
                            .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        Text("\(post.commentCount)")
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                }
                .buttonStyle(.plain)

                Spacer()
            }
        }
        .padding(AppSizing.Padding.forClass(sc).md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
        .confirmationDialog(
            "Delete this post?",
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive, action: onDelete)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This can't be undone.")
        }
    }
}

// MARK: - Small helper: turn a String? binding into an Identifiable binding for .sheet(item:)
private struct IdentifiableString: Identifiable { let id: String }

private extension Binding where Value == String? {
    func asIdentifiableBinding() -> Binding<IdentifiableString?> {
        Binding<IdentifiableString?>(
            get: { self.wrappedValue.map(IdentifiableString.init) },
            set: { self.wrappedValue = $0?.id }
        )
    }
}
