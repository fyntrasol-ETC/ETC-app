import SwiftUI
import FirebaseAuth
import FirebaseFirestore

struct CommentsSheet: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @ObservedObject private var store = CommunityStore.shared
    @Environment(\.appSizeClass) var sc

    let postId: String

    @State private var comments: [CommunityComment] = []
    @State private var latestFirestoreComments: [FirestoreComment] = []
    @State private var listener: ListenerRegistration?
    @State private var authListenerHandle: AuthStateDidChangeListenerHandle?

    @State private var newCommentText = ""
    @State private var replyingTo: CommunityComment? = nil
    @FocusState private var isInputFocused: Bool

    private var currentUid: String? { Auth.auth().currentUser?.uid }

    private var authorName: String {
        authVM.currentUser?.displayName?.components(separatedBy: " ").first
        ?? authVM.currentUser?.email?.components(separatedBy: "@").first
        ?? "You"
    }

    private var topLevelComments: [CommunityComment] {
        comments.filter { $0.parentId == nil }.sorted { $0.timestamp < $1.timestamp }
    }
    private func replies(to commentId: String) -> [CommunityComment] {
        comments.filter { $0.parentId == commentId }.sorted { $0.timestamp < $1.timestamp }
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(spacing: 0) {
            VStack(spacing: 4) {
                Capsule()
                    .fill(AppTheme.Colors.borderFallback)
                    .frame(width: 40, height: 5)
                    .padding(.top, 8)

                HStack {
                    Text("Comments")
                        .font(AppTheme.Typography.headline(sc))
                        .fontWeight(.bold)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                    Text("\(comments.count)")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
                .padding(.horizontal, pad.lg)
                .padding(.top, 10)
                .padding(.bottom, 10)

                Divider()
            }

            if topLevelComments.isEmpty {
                Spacer()
                VStack(spacing: 10) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 34))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback.opacity(0.5))
                    Text("No comments yet. Be the first to reply!")
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: pad.md) {
                        ForEach(topLevelComments) { comment in
                            VStack(alignment: .leading, spacing: pad.sm) {
                                CommentRow(
                                    comment: comment,
                                    onLike:     { toggleCommentLike(comment) },
                                    onReply:    { replyingTo = comment; isInputFocused = true },
                                    onSaveEdit: { newText in editCommentText(comment, newText: newText) },
                                    onDelete:   { deleteComment(comment) }
                                )

                                ForEach(replies(to: comment.id)) { reply in
                                    CommentRow(
                                        comment: reply,
                                        isReply: true,
                                        onLike:     { toggleCommentLike(reply) },
                                        onReply:    { replyingTo = comment; isInputFocused = true },
                                        onSaveEdit: { newText in editCommentText(reply, newText: newText) },
                                        onDelete:   { deleteComment(reply) }
                                    )
                                    .padding(.leading, pad.lg)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, pad.lg)
                    .padding(.vertical, pad.md)
                }
            }

            Divider()

            // ── Input area (new comments / replies only — edit is inline now) ──
            VStack(spacing: 8) {
                if let replying = replyingTo {
                    HStack {
                        Text(String(format: NSLocalizedString("Replying to %@", comment: ""), replying.authorName))
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                        Spacer()
                        Button { replyingTo = nil } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        }
                    }
                    .padding(.horizontal, pad.lg)
                }

                HStack(spacing: 10) {
                    ZStack {
                        Circle().fill(AppTheme.Colors.primaryFallback).frame(width: 34, height: 34)
                        Text(String(authorName.prefix(1)).uppercased())
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.white)
                    }

                    HStack {
                        TextField("Add a comment…", text: $newCommentText, axis: .vertical)
                            .font(AppTheme.Typography.body(sc))
                            .focused($isInputFocused)
                            .lineLimit(1...4)

                        Button(action: submitComment) {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(AppTheme.Colors.primaryFallback)
                        }
                        .disabled(newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .opacity(newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.4 : 1)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                     .background(AppTheme.Colors.surface)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(AppTheme.Colors.borderFallback, lineWidth: 1)
                    )
                    .cornerRadius(20)
                }
                .padding(.horizontal, pad.lg)
                .padding(.bottom, pad.sm)
                .padding(.top, 4)
            }
            .background(AppTheme.Colors.backgroundFallback)
        }
         .background(AppTheme.Colors.surface.ignoresSafeArea())
        .frame(
            idealWidth:  sc == .compact ? nil : UIScreen.main.bounds.width  * 0.75,
            idealHeight: sc == .compact ? nil : UIScreen.main.bounds.height * 0.85
        )
        .presentationDetents(sc == .compact ? [.medium, .large] : [.large])
        .presentationDragIndicator(.hidden)
        .onAppear { startListening() }
        .onDisappear { stopListening() }
    }

    private func startListening() {
        listener = FirestoreCommunityService.listenToComments(postId: postId) { firestoreComments in
            latestFirestoreComments = firestoreComments
            remapCommentsForCurrentUser()
        }
        // Re-derive likedByMe/isMine when the signed-in user changes — a
        // snapshot won't refire just because you switched accounts.
        authListenerHandle = Auth.auth().addStateDidChangeListener { _, _ in
            remapCommentsForCurrentUser()
        }
    }

    private func stopListening() {
        listener?.remove()
        if let authListenerHandle {
            Auth.auth().removeStateDidChangeListener(authListenerHandle)
        }
    }

    private func remapCommentsForCurrentUser() {
        let uid = currentUid
        comments = latestFirestoreComments.map { fc in
            CommunityComment(
                id: fc.id ?? UUID().uuidString,
                authorUid: fc.authorUid,
                authorName: fc.authorName,
                text: fc.text,
                timestamp: fc.createdAt.dateValue(),
                parentId: fc.parentId,
                likeCount: fc.likeCount,
                likedByMe: uid.map { fc.likedByUids.contains($0) } ?? false,
                isMine: uid == fc.authorUid,
                authorColorHex: fc.authorColorHex,
                isEdited: fc.isEdited
            )
        }
    }

    private func toggleCommentLike(_ comment: CommunityComment) {
        guard let idx = comments.firstIndex(where: { $0.id == comment.id }) else { return }
        comments[idx].likedByMe.toggle()
        comments[idx].likeCount += comments[idx].likedByMe ? 1 : -1
        Task {
            do {
                try await FirestoreCommunityService.toggleCommentLike(postId: postId, commentId: comment.id)
            } catch {
                print("toggleCommentLike failed: \(error.localizedDescription)")
            }
        }
    }

    private func submitComment() {
        let trimmed = newCommentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        // Capture NOW, before replyingTo gets cleared below — the Task body
        // runs asynchronously and would otherwise see replyingTo == nil.
        let parentId = replyingTo?.id

        Task {
            do {
                try await FirestoreCommunityService.addComment(
                    postId: postId, text: trimmed, authorName: authorName, parentId: parentId
                )
                await store.refreshCommentCount(postId: postId)
            } catch {
                print("addComment failed: \(error.localizedDescription)")
            }
        }
        newCommentText = ""
        replyingTo = nil
        isInputFocused = false
    }

    private func editCommentText(_ comment: CommunityComment, newText: String) {
        // Optimistic local update.
        if let idx = comments.firstIndex(where: { $0.id == comment.id }) {
            comments[idx].text = newText
        }
        Task {
            do {
                try await FirestoreCommunityService.editComment(postId: postId, commentId: comment.id, newText: newText)
            } catch {
                print("editComment failed: \(error.localizedDescription)")
            }
        }
    }

    private func deleteComment(_ comment: CommunityComment) {
        Task {
            do {
                try await FirestoreCommunityService.deleteComment(postId: postId, commentId: comment.id)
                await store.refreshCommentCount(postId: postId)
            } catch {
                print("deleteComment failed: \(error.localizedDescription)")
            }
        }
    }
}

// MARK: - Comment Row
private struct CommentRow: View {
    let comment: CommunityComment
    var isReply: Bool = false
    let onLike: () -> Void
    let onReply: () -> Void
    let onSaveEdit: (String) -> Void
    let onDelete: () -> Void

    @Environment(\.appSizeClass) var sc
    @State private var isEditing = false
    @State private var editText = ""
    @FocusState private var isEditFocused: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            ZStack {
                Circle()
                    .fill(AppTheme.Colors.greenFallback)
                    .frame(width: isReply ? 28 : 34, height: isReply ? 28 : 34)
                Text(comment.authorInitial)
                    .font(.system(size: isReply ? 11 : 13, weight: .bold))
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(comment.authorName)
                        .font(AppTheme.Typography.subheadline(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text(timeAgo(comment.timestamp))
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                    Spacer()

                    // Edit / Delete dropdown — own comments only
                    if comment.isMine && !isEditing {
                        Menu {
                            Button {
                                editText = comment.text
                                isEditing = true
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive, action: onDelete) {
                                Label("Delete", systemImage: "trash")
                            }
                        } label: {
                            Image(systemName: "ellipsis")
                                .font(.system(size: 13))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                .rotationEffect(.degrees(90))   // NEW — makes it vertical
                                .padding(6)
                                .contentShape(Rectangle())
                        }
                    }
                }

                if isEditing {
                    VStack(alignment: .leading, spacing: 6) {
                        TextField("Edit comment…", text: $editText, axis: .vertical)
                            .font(AppTheme.Typography.body(sc))
                            .focused($isEditFocused)
                            .lineLimit(1...4)
                            .padding(8)
                            .background(AppTheme.Colors.surface)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(AppTheme.Colors.primaryFallback, lineWidth: 1.5)
                            )
                            .cornerRadius(8)

                        HStack {
                            Spacer()
                            Button("Cancel") { isEditing = false }
                                .font(AppTheme.Typography.caption(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                            Button("Save") {
                                let trimmed = editText.trimmingCharacters(in: .whitespacesAndNewlines)
                                guard !trimmed.isEmpty else { return }
                                onSaveEdit(trimmed)
                                isEditing = false
                            }
                            .font(AppTheme.Typography.caption(sc))
                            .fontWeight(.semibold)
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                        }
                    }
                    .onAppear { isEditFocused = true }
                } else {
                    Text(comment.text)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                    HStack(spacing: 16) {
                        Button(action: onLike) {
                            HStack(spacing: 4) {
                                Image(systemName: comment.likedByMe ? "heart.fill" : "heart")
                                    .foregroundColor(comment.likedByMe ? AppTheme.Colors.red : AppTheme.Colors.textSecondaryFallback)
                                if comment.likeCount > 0 {
                                    Text("\(comment.likeCount)")
                                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                }
                            }
                            .font(AppTheme.Typography.caption(sc))
                        }
                        .buttonStyle(.plain)

                        Button("Reply", action: onReply)
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                }
            }
            Spacer()
        }
    }

    private func timeAgo(_ date: Date) -> String {
        let s = Date().timeIntervalSince(date)
        if s < 60 { return "now" }
        if s < 3600 { return "\(Int(s / 60))m" }
        if s < 86400 { return "\(Int(s / 3600))h" }
        return "\(Int(s / 86400))d"
    }
}
