import FirebaseAI
import Foundation

final class AIService {
    private let model = FirebaseAI.firebaseAI(backend: .googleAI())
        .generativeModel(
            modelName: "gemini-2.5-flash",
            systemInstruction: ModelContent(
                role: "system",
                parts: [TextPart("You are a friendly, encouraging English tutor helping a beginner-to-intermediate learner. You genuinely care about their progress, so you always point out their mistakes clearly and specifically, the way a good teacher does — being warm never means letting errors slide. Praise what they did well, but never at the cost of skipping real corrections.")]
            )
        )

    // No longer created eagerly — it's built (and rebuilt) via
    // startConversation(history:) so it can be seeded with saved turns.
    private var conversationChat: Chat?

    // MARK: - Seed / reseed the chat session with saved history
    /// Call this once history has been loaded from Firestore (and again
    /// whenever new messages arrive from another device) so the model
    /// keeps full conversational context across app restarts.
    func startConversation(history: [AIMessage]) {
        let modelContents: [ModelContent] = history.map { message in
            ModelContent(role: message.isUser ? "user" : "model", parts: [TextPart(message.text)])
        }
        conversationChat = model.startChat(history: modelContents)
    }

    // MARK: - Screen 3: AI Conversation (voice-to-voice)
    func converse(audioData: Data) async throws -> ConversationTurn {
        // Fallback in case startConversation wasn't called yet.
        let chat = conversationChat ?? {
            let newChat = model.startChat()
            conversationChat = newChat
            return newChat
        }()

        let prompt = """
        Listen to the audio and respond in this exact plain-text format, nothing else — no markdown, no JSON, no extra commentary:
        TRANSCRIPT: <what the student said, transcribed>
        REPLY: <your natural conversational reply, 2-3 sentences, gently weaving in any grammar/pronunciation correction if needed>
        """
        let audioPart = InlineDataPart(data: audioData, mimeType: "audio/mp4")
        let response = try await chat.sendMessage(audioPart, prompt)
        guard let text = response.text else { throw AIServiceError.emptyResponse }
        return try parseConversationTurn(from: text)
    }

    // MARK: - Plain-text parser for conversation turns
    private func parseConversationTurn(from text: String) throws -> ConversationTurn {
        guard let replyRange = text.range(of: "REPLY:") else {
            throw AIServiceError.decodingFailed
        }

        let transcriptStart: String.Index
        if let transcriptLabelRange = text.range(of: "TRANSCRIPT:") {
            transcriptStart = transcriptLabelRange.upperBound
        } else {
            transcriptStart = text.startIndex
        }

        let transcript = text[transcriptStart..<replyRange.lowerBound]
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let reply = text[replyRange.upperBound...]
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard !transcript.isEmpty, !reply.isEmpty else { throw AIServiceError.decodingFailed }
        return ConversationTurn(userTranscript: transcript, aiReply: reply)
    }

    // MARK: - Screen 2: Speaking Practice
    func judgePronunciation(audioData: Data, targetPrompt: String) async throws -> SpeakingFeedback {
        let prompt = """
        You are an English pronunciation coach. The student was asked to speak about:
        "\(targetPrompt)"

        Listen to their recording and respond with ONLY valid JSON (no markdown, no backticks):
        {
          "transcript": "exact transcription of what the student said, word for word",
          "correctedText": "the same sentence(s) rewritten in correct, natural English — fix any grammar, word choice, or phrasing issues. If the student's line was already correct, repeat it unchanged.",
          "confidenceScore": integer 0-100,
          "summary": "ONE short sentence, max 15 words, encouraging tone",
          "mistakes": [
            {"issue": "max 6 words, e.g. 'Mispronounced -ed endings'", "tip": "max 10 words, one concrete fix"}
          ]
        }
        Return AT MOST 3 mistakes — only the most important ones. If pronunciation and grammar were solid, return an empty mistakes array. Do not include minor or nitpicky issues.
        """
        let audioPart = InlineDataPart(data: audioData, mimeType: "audio/mp4")
        let response = try await model.generateContent(audioPart, prompt)
        guard let text = response.text else { throw AIServiceError.emptyResponse }
        return try decode(SpeakingFeedback.self, from: text)
    }
    // MARK: - Screen 1: Writing Practice
    func analyzeWriting(essay: String, prompt userPrompt: String) async throws -> WritingFeedbackResponse {
        let prompt = """
        You are an English writing tutor. A student was given this prompt:
        "\(userPrompt)"

        They wrote this essay in response:

        \"\"\"
        \(essay)
        \"\"\"

        First, briefly consider whether the essay actually addresses the prompt (topic relevance, whether it covers what was asked — e.g. "both views" if requested). Then respond with ONLY valid JSON (no markdown, no backticks):
        {
          "feedbackTitle": "short encouraging headline, e.g. 'Strong structure!'",
          "feedbackMessage": "1-2 sentence overall feedback, mentioning relevance to the prompt if it's off-topic or incomplete",
          "suggestions": [
            {
              "original": "the mistaken word or phrase",
              "corrected": "the corrected word or phrase",
              "type": "Spelling" | "Grammar" | "Punctuation" | "Word choice",
              "line": approximate line number as integer,
              "severity": "error" | "warning"
            }
          ]
        }
        If there are no mistakes, return an empty suggestions array.
        """
        let response = try await model.generateContent(prompt)
        guard let text = response.text else { throw AIServiceError.emptyResponse }
        return try decode(WritingFeedbackResponse.self, from: text)
    }
    // MARK: - Shared JSON decode helper
    private func decode<T: Decodable>(_ type: T.Type, from text: String) throws -> T {
        let cleaned = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard let data = cleaned.data(using: .utf8) else { throw AIServiceError.decodingFailed }
        return try JSONDecoder().decode(T.self, from: data)
    }
}

enum AIServiceError: Error { case emptyResponse, decodingFailed }

// MARK: - Response Models
struct WritingFeedbackResponse: Codable {
    let feedbackTitle: String
    let feedbackMessage: String
    let suggestions: [SuggestionDTO]
    struct SuggestionDTO: Codable {
        let original: String, corrected: String, type: String, line: Int, severity: String
    }
}

struct SpeakingFeedback: Codable {
    let transcript: String
    let correctedText: String
    let confidenceScore: Int
    let summary: String
    let mistakes: [MistakeDTO]
    struct MistakeDTO: Codable { let issue: String, tip: String }
}

struct ConversationTurn: Codable {
    let userTranscript: String
    let aiReply: String
}
