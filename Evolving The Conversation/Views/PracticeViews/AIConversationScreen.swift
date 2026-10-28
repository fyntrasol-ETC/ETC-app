import SwiftUI
import FirebaseFirestore

struct AIConversationScreen: View {
    var onBack: (() -> Void)? = nil
    @Environment(\.appSizeClass) var sc
    @EnvironmentObject private var authVM: AuthViewModel

    struct Bubble: Identifiable {
        let id: String
        let text: String
        let isUser: Bool
    }

    private let aiService = AIService()
    private let conversationService = AIConversationService()
    private let speechPlayer = AISpeechPlayer()
    @StateObject private var audioRecorder = AudioRecorderManager()

    // Persisted history, loaded from / synced with Firestore.
    @State private var messages: [AIMessage] = []
    @State private var isLoadingHistory = true
    @State private var listenerRegistration: ListenerRegistration?

    // Transient, not persisted (e.g. "didn't catch that").
    @State private var transientErrorText: String? = nil

    @State private var isProcessing = false

    private static let greeting = "Hi! I'm your English conversation partner. Tap the mic and say something to start."

    private var bubbles: [Bubble] {
        var result = messages.map { Bubble(id: $0.id, text: $0.text, isUser: $0.isUser) }
        if result.isEmpty && !isLoadingHistory {
            result.append(Bubble(id: "greeting", text: Self.greeting, isUser: false))
        }
        if let transientErrorText {
            result.append(Bubble(id: "transient-error", text: transientErrorText, isUser: false))
        }
        return result
    }

    var body: some View {
        VStack(spacing: 0) {
            ETCScreenBackBar(title: "AI Conversation") { onBack?() }

            ScrollViewReader { proxy in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: AppSizing.Padding.forClass(sc).sm) {
                        if isLoadingHistory {
                            ProgressView("Loading conversation…")
                                .padding(.top, 40)
                        } else {
                            ForEach(bubbles) { bubble in
                                chatBubble(bubble).id(bubble.id)
                            }
                        }
                        if isProcessing {
                            HStack {
                                ProgressView().padding(.leading, 8)
                                Spacer()
                            }
                        }
                    }
                    .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                    .padding(.top, AppSizing.Padding.forClass(sc).md)
                    .padding(.bottom, 140)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
                .onChange(of: bubbles.count) { _ in
                    if let last = bubbles.last {
                        withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                    }
                }
            }

            micBar
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear { startListeningIfNeeded() }
        .onDisappear {
            listenerRegistration?.remove()
            listenerRegistration = nil
        }
    }

    // MARK: - Firestore sync
    private func startListeningIfNeeded() {
        guard listenerRegistration == nil, let uid = authVM.currentUser?.uid else {
            isLoadingHistory = false
            return
        }
        listenerRegistration = conversationService.listen(uid: uid) { updated in
            messages = updated
            isLoadingHistory = false
            // Reseed the Gemini chat session so the AI keeps full context,
            // including turns that may have arrived from another device.
            aiService.startConversation(history: updated)
        }
    }

    // MARK: - Chat Bubble
    @ViewBuilder
    private func chatBubble(_ bubble: Bubble) -> some View {
        HStack {
            if bubble.isUser { Spacer(minLength: 40) }

            Text(bubble.text)
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(bubble.isUser ? .white : AppTheme.Colors.textPrimaryFallback)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
             .background(bubble.isUser ? AppTheme.Colors.primaryFallback : AppTheme.Colors.surface)
                .cornerRadius(18)
                .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                        x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)

            if !bubble.isUser { Spacer(minLength: 40) }
        }
    }

    // MARK: - Mic Bar
    private var micBar: some View {
        let size = AppSizing.iconSize(base: 70, sizeClass: sc)
        return VStack(spacing: 8) {
            Button {
                if audioRecorder.isRecording {
                    handleStopRecording()
                } else {
                    audioRecorder.requestPermission { granted in
                        guard granted else { return }
                        speechPlayer.stop() // stop AI talking if user interrupts
                        transientErrorText = nil
                        audioRecorder.startRecording()
                    }
                }
            } label: {
                ZStack {
                    Circle()
                        .fill(audioRecorder.isRecording ? AppTheme.Colors.red : AppTheme.Colors.primaryFallback)
                        .frame(width: size, height: size)
                        .shadow(color: AppTheme.Colors.primaryFallback.opacity(0.35), radius: 14, x: 0, y: 6)
                    Image(systemName: audioRecorder.isRecording ? "stop.fill" : "mic.fill")
                        .font(.system(size: AppSizing.fontSize(base: 26, sizeClass: sc), weight: .medium))
                        .foregroundColor(.white)
                }
            }
            .buttonStyle(.plain)
            .disabled(isProcessing || isLoadingHistory)

            Text(audioRecorder.isRecording ? "Listening…" : (isProcessing ? "Thinking…" : "Tap to speak"))
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
        .padding(.vertical, AppSizing.Padding.forClass(sc).md)
        .frame(maxWidth: .infinity)
         .background(AppTheme.Colors.surface.shadow(color: .black.opacity(0.06), radius: 12, x: 0, y: -4))
    }

    // MARK: - Handle Recording
    private func handleStopRecording() {
        guard let audioData = audioRecorder.stopRecording() else { return }
        guard let uid = authVM.currentUser?.uid else {
            transientErrorText = "You need to be signed in to chat with your tutor."
            return
        }
        isProcessing = true
        transientErrorText = nil
        Task {
            do {
                let turn = try await aiService.converse(audioData: audioData)
                speechPlayer.speak(turn.aiReply)
                try await conversationService.addMessage(uid: uid, text: turn.userTranscript, isUser: true)
                try await conversationService.addMessage(uid: uid, text: turn.aiReply, isUser: false)
                XPService.shared.recordActivity(.aiSpeaking)   // ← add this
            } catch {
                transientErrorText = "Sorry, I didn't catch that — could you try again?"
            }
            isProcessing = false
        }
    }
}
