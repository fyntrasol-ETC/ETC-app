package com.evolvingtheconversation.app.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
// NOTE: adjust these two imports if your SDK version puts Chat/Content in a
// different package — they live alongside GenerativeBackend/content above,
// so check com.google.firebase.ai.type first if this doesn't resolve.
import com.google.firebase.ai.Chat
import com.google.firebase.ai.type.Content
import org.json.JSONArray
import org.json.JSONObject

class AIServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Android counterpart to the iOS AIService.swift. Same model, same system
 * instruction, same prompts — so both apps behave identically.
 */

object AIService {

    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val TUTOR_SYSTEM_INSTRUCTION =
        "You are a friendly, encouraging English tutor helping a beginner-to-intermediate learner. " +
                "You genuinely care about their progress, so you always point out their mistakes clearly and " +
                "specifically, the way a good teacher does — being warm never means letting errors slide. " +
                "Praise what they did well, but never at the cost of skipping real corrections."

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content(role = "system") { text(TUTOR_SYSTEM_INSTRUCTION) }
        )
    }

    // Screen 3's chat session. Keyed to the signed-in user and (re)created
    // seeded with that user's saved history from Firestore, so the model
    // still "remembers" past turns even after the screen/app was closed and reopened.
    private var conversationChat: Chat? = null
    private var conversationChatUid: String? = null

    private fun chatFor(uid: String, seedHistory: List<StoredMessage>): Chat {
        if (conversationChat == null || conversationChatUid != uid) {
            conversationChat = model.startChat(history = buildHistoryContent(seedHistory))
            conversationChatUid = uid
        }
        return conversationChat!!
    }

    private fun buildHistoryContent(messages: List<StoredMessage>): List<Content> =
        messages.map { msg ->
            content(role = if (msg.isUser) "user" else "model") { text(msg.text) }
        }

    /** Call this on logout / account switch so the next user doesn't inherit this chat. */
    fun resetConversation() {
        conversationChat = null
        conversationChatUid = null
    }

    // MARK: - Screen 3: AI Conversation (voice-to-voice)
    suspend fun converse(
        uid: String,
        seedHistory: List<StoredMessage>,
        audioData: ByteArray
    ): ConversationTurn {
        val chat = chatFor(uid, seedHistory)

        val prompt = """
            Listen to the audio. Respond with ONLY valid JSON (no markdown, no backticks):
            {
              "userTranscript": "what the student said, transcribed",
              "aiReply": "your natural conversational reply, 2-3 sentences, gently weaving in any grammar/pronunciation correction if needed"
            }
        """.trimIndent()

        val message = content(role = "user") {
            inlineData(audioData, "audio/mp4")
            text(prompt)
        }
        val response = chat.sendMessage(message)
        val text = response.text ?: throw AIServiceException("Empty response from model")
        return parseConversationTurn(text)
    }

    // MARK: - Screen 2: Speaking Practice
    suspend fun judgePronunciation(audioData: ByteArray, targetPrompt: String): SpeakingFeedback {
        val prompt = """
            You are an English pronunciation coach. The student was asked to speak about:
            "$targetPrompt"

            Listen to their recording and respond with ONLY valid JSON (no markdown, no backticks):
            {
              "transcript": "exact transcription of what the student said, word for word",
              "correctedLine": "the same sentence(s) rewritten in correct, natural English — fix any grammar, word choice, or phrasing issues. If the student's line was already correct, repeat it unchanged.",
              "confidenceScore": integer 0-100,
              "summary": "ONE short sentence, max 15 words, encouraging tone",
              "mistakes": [
                {"issue": "max 6 words, e.g. 'Mispronounced -ed endings'", "tip": "max 10 words, one concrete fix"}
              ]
            }
            Return AT MOST 3 mistakes — only the most important ones. If pronunciation and grammar were solid, return an empty mistakes array. Do not include minor or nitpicky issues.
        """.trimIndent()

        val message = content {
            inlineData(audioData, "audio/mp4")
            text(prompt)
        }
        val response = model.generateContent(message)
        val text = response.text ?: throw AIServiceException("Empty response from model")
        return parseSpeakingFeedback(text)
    }

    // MARK: - Screen 1: Writing Practice
    suspend fun analyzeWriting(essay: String, userPrompt: String): WritingFeedbackResponse {
        val prompt = """
            You are an English writing tutor. A student was given this prompt:
            "$userPrompt"

            They wrote this essay in response:

            ""${'"'}
            $essay
            ""${'"'}

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
        """.trimIndent()

        val response = model.generateContent(prompt)
        val text = response.text ?: throw AIServiceException("Empty response from model")
        return parseWritingFeedback(text)
    }

    // MARK: - JSON parsing helpers (Android has no Codable, so we parse with org.json —
    // no extra dependency needed, it ships with the platform)

    private fun cleanJson(raw: String): String =
        raw.replace("```json", "").replace("```", "").trim()

    private fun parseConversationTurn(raw: String): ConversationTurn {
        val obj = JSONObject(cleanJson(raw))
        return ConversationTurn(
            userTranscript = obj.getString("userTranscript"),
            aiReply = obj.getString("aiReply")
        )
    }

    private fun parseSpeakingFeedback(raw: String): SpeakingFeedback {
        val obj = JSONObject(cleanJson(raw))
        val mistakesJson: JSONArray = obj.optJSONArray("mistakes") ?: JSONArray()
        val mistakes = (0 until mistakesJson.length()).map { i ->
            val m = mistakesJson.getJSONObject(i)
            MistakeDTO(issue = m.getString("issue"), tip = m.getString("tip"))
        }
        return SpeakingFeedback(
            transcript = obj.optString("transcript", ""),
            correctedLine = obj.optString("correctedLine", ""),
            confidenceScore = obj.getInt("confidenceScore"),
            summary = obj.getString("summary"),
            mistakes = mistakes
        )
    }

    private fun parseWritingFeedback(raw: String): WritingFeedbackResponse {
        val obj = JSONObject(cleanJson(raw))
        val suggestionsJson: JSONArray = obj.optJSONArray("suggestions") ?: JSONArray()
        val suggestions = (0 until suggestionsJson.length()).map { i ->
            val s = suggestionsJson.getJSONObject(i)
            SuggestionDTO(
                original = s.getString("original"),
                corrected = s.getString("corrected"),
                type = s.getString("type"),
                line = s.optInt("line", 0),
                severity = s.getString("severity")
            )
        }
        return WritingFeedbackResponse(
            feedbackTitle = obj.getString("feedbackTitle"),
            feedbackMessage = obj.getString("feedbackMessage"),
            suggestions = suggestions
        )
    }
}

// MARK: - Response Models (Android equivalents of the iOS Codable structs)

data class ConversationTurn(
    val userTranscript: String,
    val aiReply: String
)

data class SpeakingFeedback(
    val transcript: String,
    val correctedLine: String,
    val confidenceScore: Int,
    val summary: String,
    val mistakes: List<MistakeDTO>
)

data class MistakeDTO(
    val issue: String,
    val tip: String
)

data class WritingFeedbackResponse(
    val feedbackTitle: String,
    val feedbackMessage: String,
    val suggestions: List<SuggestionDTO>
)

data class SuggestionDTO(
    val original: String,
    val corrected: String,
    val type: String,
    val line: Int,
    val severity: String
)