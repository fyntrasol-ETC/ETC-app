package com.evolvingtheconversation.app.ui.screens.learning

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import android.widget.Toast
import com.evolvingtheconversation.app.ui.components.BackButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthViewModel
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val VSurface: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF2F3F7)
private val VCardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val VBlue        = Color(0xFF1E5EF3)
private val VGreen       = Color(0xFF0CBD85)
private val VTextPri: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val VTextSec: Color
@Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

@Composable
private fun levelAccentColor(level: String): Color = when (level.uppercase()) {
    "BEGINNER"     -> VBlue
    "INTERMEDIATE" -> VGreen
    "ADVANCED"     -> Color(0xFF7C3AED)
    else           -> VTextSec
}

private fun levelLabel(level: String): String =
    level.lowercase().replaceFirstChar { it.uppercase() }

// ─── Vocabulary level ──────────────────────────────────────────────────────
enum class VocabularyLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

private fun VocabularyLevel.accentGradient(): List<Color> = when (this) {
    VocabularyLevel.BEGINNER     -> listOf(Color(0xFF1E5EF3), Color(0xFF1E3A8A))
    VocabularyLevel.INTERMEDIATE -> listOf(Color(0xFF0CBD85), Color(0xFF059669))
    VocabularyLevel.ADVANCED     -> listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
}

private fun VocabularyLevel.subtitle(): String = when (this) {
    VocabularyLevel.BEGINNER     -> "Everyday words to build your foundation"
    VocabularyLevel.INTERMEDIATE -> "Expand your vocabulary with common terms"
    VocabularyLevel.ADVANCED     -> "Challenge yourself with complex words"
}

// ─── Vocabulary word data ──────────────────────────────────────────────────────
data class VocabularyWord(
    val id           : String,
    val word         : String,
    val phonetic     : String,
    val partOfSpeech : String,
    val definition   : String,
    val example      : String,
    val synonyms     : List<String>,
    val antonyms     : List<String>,
    val gradient     : List<Color>,
    val level        : VocabularyLevel
) {
    val pronunciation: String get() = "$phonetic · $partOfSpeech"
}

// ─── Saved vocabulary (Firestore-backed) ───────────────────────────────────────
data class SavedVocabularyItem(
    val id           : String,
    val word         : String,
    val phonetic     : String,
    val partOfSpeech : String,
    val definition   : String,
    val example      : String,
    val synonyms     : String,
    val antonyms     : String,
    val level        : String,
    val savedAt      : Long
)

private const val USERS_COLLECTION = "users"
private const val SAVED_VOCAB_COLLECTION = "savedVocabulary"
private const val TTS_TAG = "VocabTTS"

private fun saveWordToFirestore(uid: String, word: VocabularyWord) {
    val data = hashMapOf(
        "word" to word.word,
        "phonetic" to word.phonetic,
        "partOfSpeech" to word.partOfSpeech,
        "definition" to word.definition,
        "example" to word.example,
        "synonyms" to word.synonyms.joinToString(", "),
        "antonyms" to word.antonyms.joinToString(", "),
        "level" to word.level.name,
        "savedAt" to System.currentTimeMillis()
    )
    FirebaseFirestore.getInstance()
        .collection(USERS_COLLECTION).document(uid)
        .collection(SAVED_VOCAB_COLLECTION).document(word.id)
        .set(data)
}

private fun safeSavedAtMillis(raw: Any?): Long = when (raw) {
    is Long -> raw
    is Int -> raw.toLong()
    is Double -> raw.toLong()
    is com.google.firebase.Timestamp -> raw.toDate().time
    is java.util.Date -> raw.time
    is String -> raw.toLongOrNull() ?: 0L
    else -> 0L
}

private fun unsaveWordFromFirestore(uid: String, wordId: String) {
    FirebaseFirestore.getInstance()
        .collection(USERS_COLLECTION).document(uid)
        .collection(SAVED_VOCAB_COLLECTION).document(wordId)
        .delete()
}

@Composable
private fun rememberSavedVocabulary(uid: String?): State<List<SavedVocabularyItem>> {
    val state = remember { mutableStateOf<List<SavedVocabularyItem>>(emptyList()) }
    DisposableEffect(uid) {
        if (uid == null) {
            state.value = emptyList()
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION).document(uid)
                .collection(SAVED_VOCAB_COLLECTION)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        state.value = snapshot.documents.mapNotNull { doc ->
                            val wordValue = doc.getString("word") ?: return@mapNotNull null
                            SavedVocabularyItem(
                                id           = doc.id,
                                word         = wordValue,
                                phonetic     = doc.getString("phonetic") ?: "",
                                partOfSpeech = doc.getString("partOfSpeech") ?: "",
                                definition   = doc.getString("definition") ?: "",
                                example      = doc.getString("example") ?: "",
                                synonyms     = doc.getString("synonyms") ?: "",
                                antonyms     = doc.getString("antonyms") ?: "",
                                level        = doc.getString("level") ?: "",
                                savedAt      = safeSavedAtMillis(doc.get("savedAt"))
                            )
                        }.sortedByDescending { it.savedAt }
                    }
                }
            onDispose { registration.remove() }
        }
    }
    return state
}

@Composable
private fun rememberTextToSpeech(): (String) -> Unit {
    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val ttsRef = remember { arrayOfNulls<TextToSpeech>(1) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = ttsRef[0]?.setLanguage(Locale.US)
                ttsReady = langResult != TextToSpeech.LANG_MISSING_DATA &&
                        langResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ttsReady) {
                    Log.w(TTS_TAG, "US English voice data missing/unsupported (result=$langResult)")
                }
            } else {
                Log.w(TTS_TAG, "TextToSpeech init failed with status=$status")
            }
        }
        ttsRef[0] = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    return remember {
        { text: String ->
            val engine = ttsRef[0]
            if (ttsReady && engine != null) {
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vocab_word_$text")
            } else {
                Log.w(TTS_TAG, "speak() called before TTS was ready; ignoring \"$text\"")
                Toast.makeText(
                    context,
                    "Text-to-speech isn't available on this device yet",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
// ─── Word bank ─────────────────────────────────────────────────────────────────
private val gradientPalette = listOf(
    listOf(Color(0xFF1E5EF3), Color(0xFF1E3A8A)),
    listOf(Color(0xFF0CBD85), Color(0xFF059669)),
    listOf(Color(0xFFFFA800), Color(0xFFE67E00)),
    listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
    listOf(Color(0xFFE53935), Color(0xFFB71C1C)),
)

private fun gradientFor(index: Int) = gradientPalette[index % gradientPalette.size]

private fun vw(
    id: String, word: String, phonetic: String, pos: String,
    definition: String, example: String, synonyms: String, antonyms: String,
    level: VocabularyLevel, index: Int
) = VocabularyWord(
    id = id, word = word, phonetic = phonetic, partOfSpeech = pos,
    definition = definition, example = example,
    synonyms = synonyms.split(", "), antonyms = antonyms.split(", "),
    gradient = gradientFor(index), level = level
)

private val beginnerWords = listOf(
    vw("beg_1", "Happy", "/ˈhæpi/", "adjective", "Feeling or showing pleasure and contentment.", "She felt happy after passing her exam.", "joyful, glad", "sad, unhappy", VocabularyLevel.BEGINNER, 0),
    vw("beg_2", "Brave", "/breɪv/", "adjective", "Ready to face danger or pain without fear.", "The brave firefighter ran into the burning building.", "courageous, bold", "cowardly, timid", VocabularyLevel.BEGINNER, 1),
    vw("beg_3", "Honest", "/ˈɒnɪst/", "adjective", "Free of deceit; truthful.", "He gave an honest answer to the question.", "truthful, sincere", "dishonest, deceitful", VocabularyLevel.BEGINNER, 2),
    vw("beg_4", "Careful", "/ˈkɛəfʊl/", "adjective", "Making sure of avoiding mistakes or harm.", "She was careful while crossing the busy road.", "cautious, attentive", "careless, reckless", VocabularyLevel.BEGINNER, 3),
    vw("beg_5", "Curious", "/ˈkjʊəriəs/", "adjective", "Eager to know or learn something.", "The curious child asked many questions.", "inquisitive, interested", "indifferent, uninterested", VocabularyLevel.BEGINNER, 4),
    vw("beg_6", "Gentle", "/ˈdʒɛntl/", "adjective", "Mild and kind in manner.", "She has a gentle way of speaking to children.", "mild, tender", "harsh, rough", VocabularyLevel.BEGINNER, 0),
    vw("beg_7", "Strong", "/strɒŋ/", "adjective", "Having great physical power.", "He is strong enough to lift heavy boxes.", "powerful, sturdy", "weak, feeble", VocabularyLevel.BEGINNER, 1),
    vw("beg_8", "Bright", "/braɪt/", "adjective", "Giving out a lot of light; intelligent.", "The bright student solved the puzzle quickly.", "intelligent, radiant", "dull, dim", VocabularyLevel.BEGINNER, 2),
    vw("beg_9", "Calm", "/kɑːm/", "adjective", "Not showing or feeling nervousness or anger.", "She stayed calm during the emergency.", "peaceful, relaxed", "anxious, agitated", VocabularyLevel.BEGINNER, 3),
    vw("beg_10", "Clever", "/ˈklɛvər/", "adjective", "Quick to understand and learn.", "The clever fox found a way out of the trap.", "smart, intelligent", "foolish, dull", VocabularyLevel.BEGINNER, 4),
    vw("beg_11", "Friendly", "/ˈfrɛndli/", "adjective", "Kind and pleasant toward others.", "Our new neighbor is very friendly.", "kind, sociable", "unfriendly, hostile", VocabularyLevel.BEGINNER, 0),
    vw("beg_12", "Patient", "/ˈpeɪʃənt/", "adjective", "Able to wait calmly without complaint.", "The teacher was patient with the slow learners.", "tolerant, calm", "impatient, restless", VocabularyLevel.BEGINNER, 1),
    vw("beg_13", "Polite", "/pəˈlaɪt/", "adjective", "Having good manners and respect for others.", "It's polite to say thank you.", "courteous, respectful", "rude, impolite", VocabularyLevel.BEGINNER, 2),
    vw("beg_14", "Simple", "/ˈsɪmpl/", "adjective", "Easily understood or done.", "The instructions were simple to follow.", "easy, plain", "complex, difficult", VocabularyLevel.BEGINNER, 3),
    vw("beg_15", "Quick", "/kwɪk/", "adjective", "Moving fast or done in a short time.", "She gave a quick reply to the message.", "fast, swift", "slow, sluggish", VocabularyLevel.BEGINNER, 4),
    vw("beg_16", "Kind", "/kaɪnd/", "adjective", "Having a friendly, generous nature.", "It was kind of him to help the old man.", "caring, generous", "unkind, cruel", VocabularyLevel.BEGINNER, 0),
    vw("beg_17", "Cheerful", "/ˈtʃɪərfʊl/", "adjective", "Noticeably happy and optimistic.", "Her cheerful smile brightened the room.", "happy, upbeat", "gloomy, sad", VocabularyLevel.BEGINNER, 1),
    vw("beg_18", "Helpful", "/ˈhɛlpfʊl/", "adjective", "Giving useful help or assistance.", "The helpful staff guided us to the right room.", "supportive, useful", "unhelpful, useless", VocabularyLevel.BEGINNER, 2),
    vw("beg_19", "Confident", "/ˈkɒnfɪdənt/", "adjective", "Feeling sure of oneself and one's abilities.", "She spoke with a confident voice during the interview.", "self-assured, certain", "insecure, doubtful", VocabularyLevel.BEGINNER, 3),
    vw("beg_20", "Grateful", "/ˈɡreɪtfʊl/", "adjective", "Feeling or showing thankfulness.", "I am grateful for your support.", "thankful, appreciative", "ungrateful, unappreciative", VocabularyLevel.BEGINNER, 4),
)

private val intermediateWords = listOf(
    vw("int_1", "Resilient", "/rɪˈzɪliənt/", "adjective", "Able to withstand or recover quickly from difficulties.", "The team remained resilient despite the setbacks.", "tough, adaptable", "fragile, weak", VocabularyLevel.INTERMEDIATE, 0),
    vw("int_2", "Candid", "/ˈkændɪd/", "adjective", "Truthful and straightforward; frank.", "He gave a candid opinion about the proposal.", "honest, forthright", "evasive, deceptive", VocabularyLevel.INTERMEDIATE, 1),
    vw("int_3", "Eloquent", "/ˈɛləkwənt/", "adjective", "Fluent or persuasive in speaking or writing.", "She gave an eloquent speech that moved the entire room.", "fluent, articulate", "inarticulate, halting", VocabularyLevel.INTERMEDIATE, 2),
    vw("int_4", "Tenacious", "/təˈneɪʃəs/", "adjective", "Holding firmly to a course of action; persistent.", "His tenacious attitude helped him finish the marathon.", "persistent, determined", "irresolute, weak-willed", VocabularyLevel.INTERMEDIATE, 3),
    vw("int_5", "Pragmatic", "/præɡˈmætɪk/", "adjective", "Dealing with things sensibly and practically.", "She took a pragmatic approach to solving the budget crisis.", "practical, realistic", "idealistic, impractical", VocabularyLevel.INTERMEDIATE, 4),
    vw("int_6", "Ambiguous", "/æmˈbɪɡjuəs/", "adjective", "Open to more than one interpretation.", "The contract's wording was ambiguous and confused both parties.", "unclear, vague", "clear, definite", VocabularyLevel.INTERMEDIATE, 0),
    vw("int_7", "Diligent", "/ˈdɪlɪdʒənt/", "adjective", "Showing care and effort in one's work.", "The diligent student reviewed her notes every night.", "hardworking, industrious", "lazy, negligent", VocabularyLevel.INTERMEDIATE, 1),
    vw("int_8", "Empathetic", "/ˌɛmpəˈθɛtɪk/", "adjective", "Able to understand and share the feelings of others.", "The empathetic nurse comforted the worried family.", "compassionate, understanding", "indifferent, callous", VocabularyLevel.INTERMEDIATE, 2),
    vw("int_9", "Frugal", "/ˈfruːɡəl/", "adjective", "Careful with money or resources; economical.", "They lived a frugal life to save for their house.", "thrifty, economical", "wasteful, extravagant", VocabularyLevel.INTERMEDIATE, 3),
    vw("int_10", "Genuine", "/ˈdʒɛnjuɪn/", "adjective", "Truly what it is said to be; sincere.", "Her genuine concern for others made her a great friend.", "authentic, sincere", "fake, insincere", VocabularyLevel.INTERMEDIATE, 4),
    vw("int_11", "Humble", "/ˈhʌmbl/", "adjective", "Having a modest view of one's own importance.", "Despite his success, he remained humble and kind.", "modest, unassuming", "arrogant, boastful", VocabularyLevel.INTERMEDIATE, 0),
    vw("int_12", "Innovative", "/ˈɪnəveɪtɪv/", "adjective", "Introducing new ideas or methods.", "The startup's innovative product changed the industry.", "creative, original", "conventional, unoriginal", VocabularyLevel.INTERMEDIATE, 1),
    vw("int_13", "Meticulous", "/məˈtɪkjʊləs/", "adjective", "Showing great attention to detail.", "The meticulous editor caught every small error.", "precise, thorough", "careless, sloppy", VocabularyLevel.INTERMEDIATE, 2),
    vw("int_14", "Optimistic", "/ˌɒptɪˈmɪstɪk/", "adjective", "Hopeful and confident about the future.", "She stayed optimistic even after the setback.", "hopeful, positive", "pessimistic, hopeless", VocabularyLevel.INTERMEDIATE, 3),
    vw("int_15", "Persistent", "/pəˈsɪstənt/", "adjective", "Continuing firmly despite difficulty.", "His persistent efforts finally paid off.", "determined, steadfast", "irresolute, hesitant", VocabularyLevel.INTERMEDIATE, 4),
    vw("int_16", "Reluctant", "/rɪˈlʌktənt/", "adjective", "Unwilling and hesitant to do something.", "He was reluctant to share his true feelings.", "hesitant, unwilling", "eager, willing", VocabularyLevel.INTERMEDIATE, 0),
    vw("int_17", "Skeptical", "/ˈskɛptɪkəl/", "adjective", "Not easily convinced; having doubts.", "She remained skeptical about the salesman's claims.", "doubtful, dubious", "trusting, credulous", VocabularyLevel.INTERMEDIATE, 1),
    vw("int_18", "Versatile", "/ˈvɜːsətaɪl/", "adjective", "Able to adapt to many different functions.", "He is a versatile player who can fill several positions.", "adaptable, flexible", "limited, inflexible", VocabularyLevel.INTERMEDIATE, 2),
    vw("int_19", "Witty", "/ˈwɪti/", "adjective", "Showing clever and quick humor.", "Her witty remarks kept everyone laughing.", "humorous, sharp", "dull, humorless", VocabularyLevel.INTERMEDIATE, 3),
    vw("int_20", "Zealous", "/ˈzɛləs/", "adjective", "Having great energy or enthusiasm for a cause.", "The zealous volunteers worked all night to help.", "enthusiastic, passionate", "apathetic, indifferent", VocabularyLevel.INTERMEDIATE, 4),
)

private val advancedWords = listOf(
    vw("adv_1", "Ephemeral", "/ɪˈfɛmərəl/", "adjective", "Lasting for a very short time.", "The beauty of cherry blossoms is ephemeral.", "fleeting, transient", "permanent, lasting", VocabularyLevel.ADVANCED, 0),
    vw("adv_2", "Ubiquitous", "/juːˈbɪkwɪtəs/", "adjective", "Present or found everywhere.", "Smartphones have become ubiquitous in modern life.", "omnipresent, widespread", "rare, scarce", VocabularyLevel.ADVANCED, 1),
    vw("adv_3", "Serendipitous", "/ˌsɛrənˈdɪpɪtəs/", "adjective", "Occurring by happy chance.", "Their meeting was a serendipitous encounter at the airport.", "fortuitous, chance", "planned, intentional", VocabularyLevel.ADVANCED, 2),
    vw("adv_4", "Cacophonous", "/kəˈkɒfənəs/", "adjective", "Harsh and jarring in sound.", "The cacophonous traffic noise kept her awake.", "discordant, harsh", "harmonious, melodious", VocabularyLevel.ADVANCED, 3),
    vw("adv_5", "Ineffable", "/ɪnˈɛfəbl/", "adjective", "Too great to be expressed in words.", "She felt an ineffable joy at the birth of her child.", "indescribable, inexpressible", "describable, expressible", VocabularyLevel.ADVANCED, 4),
    vw("adv_6", "Perfunctory", "/pəˈfʌŋktəri/", "adjective", "Carried out with minimal effort or interest.", "He gave a perfunctory nod and walked away.", "cursory, half-hearted", "thorough, careful", VocabularyLevel.ADVANCED, 0),
    vw("adv_7", "Obfuscate", "/ˈɒbfʌskeɪt/", "verb", "To make something unclear or confusing.", "The report seemed designed to obfuscate the real numbers.", "confuse, obscure", "clarify, illuminate", VocabularyLevel.ADVANCED, 1),
    vw("adv_8", "Ostentatious", "/ˌɒstɛnˈteɪʃəs/", "adjective", "Showy in a way meant to impress others.", "The ostentatious mansion had gold-plated gates.", "flashy, showy", "modest, understated", VocabularyLevel.ADVANCED, 2),
    vw("adv_9", "Pernicious", "/pəˈnɪʃəs/", "adjective", "Having a harmful effect, especially gradually.", "The pernicious rumor damaged her reputation for years.", "harmful, damaging", "beneficial, harmless", VocabularyLevel.ADVANCED, 3),
    vw("adv_10", "Quintessential", "/ˌkwɪntɪˈsɛnʃəl/", "adjective", "Representing the most perfect example of a quality.", "He is the quintessential gentleman.", "archetypal, classic", "atypical, unrepresentative", VocabularyLevel.ADVANCED, 4),
    vw("adv_11", "Recalcitrant", "/rɪˈkælsɪtrənt/", "adjective", "Stubbornly resistant to authority or guidance.", "The recalcitrant employee refused to follow new policies.", "defiant, obstinate", "compliant, obedient", VocabularyLevel.ADVANCED, 0),
    vw("adv_12", "Sagacious", "/səˈɡeɪʃəs/", "adjective", "Having keen judgment and wisdom.", "The sagacious advisor guided the young king wisely.", "wise, shrewd", "foolish, unwise", VocabularyLevel.ADVANCED, 1),
    vw("adv_13", "Surreptitious", "/ˌsʌrəpˈtɪʃəs/", "adjective", "Done secretly or stealthily.", "She took a surreptitious glance at her phone during the meeting.", "secretive, covert", "open, overt", VocabularyLevel.ADVANCED, 2),
    vw("adv_14", "Taciturn", "/ˈtæsɪtɜːn/", "adjective", "Saying little; reserved in speech.", "The taciturn old man rarely spoke to strangers.", "reserved, reticent", "talkative, garrulous", VocabularyLevel.ADVANCED, 3),
    vw("adv_15", "Vicissitude", "/vɪˈsɪsɪtjuːd/", "noun", "A change of circumstances, often unpleasant.", "They faced the vicissitudes of life together.", "fluctuation, upheaval", "stability, constancy", VocabularyLevel.ADVANCED, 4),
    vw("adv_16", "Winsome", "/ˈwɪnsəm/", "adjective", "Attractive or appealing in a charming way.", "The child had a winsome smile that charmed everyone.", "charming, endearing", "repellent, unattractive", VocabularyLevel.ADVANCED, 0),
    vw("adv_17", "Magnanimous", "/mæɡˈnænɪməs/", "adjective", "Generous or forgiving, especially toward a rival.", "The champion was magnanimous in victory.", "generous, noble", "petty, vindictive", VocabularyLevel.ADVANCED, 1),
    vw("adv_18", "Fastidious", "/fæˈstɪdiəs/", "adjective", "Very attentive to detail; hard to please.", "He was fastidious about keeping his desk organized.", "meticulous, particular", "careless, sloppy", VocabularyLevel.ADVANCED, 2),
    vw("adv_19", "Inexorable", "/ɪnˈɛksərəbl/", "adjective", "Impossible to stop or prevent.", "The inexorable rise of technology reshaped the industry.", "unstoppable, relentless", "stoppable, avoidable", VocabularyLevel.ADVANCED, 3),
    vw("adv_20", "Ebullient", "/ɪˈbʌliənt/", "adjective", "Cheerful and full of energy.", "She was ebullient after hearing the good news.", "exuberant, enthusiastic", "gloomy, subdued", VocabularyLevel.ADVANCED, 4),
)

private val vocabularyWords: List<VocabularyWord> = beginnerWords + intermediateWords + advancedWords

// ─── Navigation destinations within this screen ───────────────────────────────
private sealed class VocabDestination {
    data object Selection : VocabDestination()
    data class Level(val level: VocabularyLevel) : VocabDestination()
    data object Saved : VocabDestination()
}

// ─── Entry point ────────────────────────────────────────────────────────────────
@Composable
fun VocabularyScreen(
    onBack: () -> Unit = {}
) {
    var destination by remember { mutableStateOf<VocabDestination>(VocabDestination.Selection) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val savedItems by rememberSavedVocabulary(uid)
    val savedIds = remember(savedItems) { savedItems.map { it.id }.toSet() }

    fun toggleSave(word: VocabularyWord) {
        val currentUid = uid ?: return
        if (savedIds.contains(word.id)) {
            unsaveWordFromFirestore(currentUid, word.id)
        } else {
            saveWordToFirestore(currentUid, word)
        }
    }

    when (val dest = destination) {
        is VocabDestination.Selection -> VocabularyLevelSelectionScreen(
            onBack = onBack,
            onLevelSelected = { level -> destination = VocabDestination.Level(level) },
            onSavedVocabularyClick = { destination = VocabDestination.Saved }
        )
        is VocabDestination.Level -> VocabularyWordScreen(
            level = dest.level,
            savedIds = savedIds,
            onToggleSave = ::toggleSave,
            onBack = { destination = VocabDestination.Selection }
        )
        is VocabDestination.Saved -> SavedVocabularyScreen(
            isSignedIn = uid != null,
            items = savedItems,
            onRemove = { wordId -> uid?.let { unsaveWordFromFirestore(it, wordId) } },
            onBack = { destination = VocabDestination.Selection }
        )
    }
}

// ─── Level selection screen ──────────────────────────────────────────────────────
@Composable
private fun VocabularyLevelSelectionScreen(
    onBack                  : () -> Unit,
    onLevelSelected         : (VocabularyLevel) -> Unit,
    onSavedVocabularyClick  : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VSurface)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "Vocabulary",
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = VTextPri
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Text(
                text     = "Choose a difficulty level to start practicing.",
                fontSize = 14.sp,
                color    = VTextSec
            )

            Spacer(Modifier.height(20.dp))

            VocabularyLevel.entries.forEach { level ->
                LevelCard(
                    level    = level,
                    onClick  = { onLevelSelected(level) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            SavedVocabularyEntryCard(
                onClick  = onSavedVocabularyClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LevelCard(
    level    : VocabularyLevel,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .background(
                brush = Brush.linearGradient(level.accentGradient()),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            Text(
                text       = level.label,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 22.sp,
                color      = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = level.subtitle(),
                fontSize   = 14.sp,
                color      = Color.White.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "${vocabularyWords.count { it.level == level }} words · Tap to practice",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SavedVocabularyEntryCard(
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(VCardWhite, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Filled.Bookmark,
                contentDescription = null,
                tint               = VBlue,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text       = "Saved Vocabulary",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp,
                color      = VTextPri
            )
        }
        Icon(
            imageVector        = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint               = VTextSec,
            modifier           = Modifier.size(20.dp)
        )
    }
}
// ─── Per-level vocabulary screen ─────────────────────────────────────────────────
@Composable
private fun VocabularyWordScreen(
    level        : VocabularyLevel,
    savedIds     : Set<String>,
    onToggleSave : (VocabularyWord) -> Unit,
    onBack       : () -> Unit
) {
    val scrollState = rememberScrollState()

    // Shuffled once per visit to this level's screen; stays fixed while browsing.
    val wordsForLevel = remember(level) {
        vocabularyWords.filter { it.level == level }.shuffled()
    }

    // ── Word of the Day: fixed for this entire visit to the level. ──────────
    // Only changes when the user leaves this level and picks it again
    // (this composable gets a fresh `level` key on re-entry).
    val wordOfDay = remember(level) { wordsForLevel.first() }

    // ── Flashcards: fully independent index, only moved by the arrows. ──────
    var flashcardIndex by remember(level) { mutableStateOf(0) }

    val speak = rememberTextToSpeech()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VSurface)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "${level.label} Vocabulary",
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = VTextPri
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {

            Text(
                text          = "WORD OF THE DAY",
                color         = VGreen,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier      = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )

            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = VCardWhite),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 3.dp
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text       = wordOfDay.word,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 32.sp,
                                color      = VTextPri
                            )
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text     = wordOfDay.pronunciation,
                                    fontSize = 15.sp,
                                    color    = VTextSec
                                )
                                Icon(
                                    imageVector        = Icons.Filled.VolumeUp,
                                    contentDescription = "Pronounce ${wordOfDay.word}",
                                    tint               = VTextPri,
                                    modifier           = Modifier
                                        .size(18.dp)
                                        .clickable { speak(wordOfDay.word) }
                                )
                            }
                        }

                        val isSaved = savedIds.contains(wordOfDay.id)
                        Icon(
                            imageVector        = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove from saved" else "Save word",
                            tint               = if (isSaved) VBlue else VTextPri,
                            modifier           = Modifier
                                .size(26.dp)
                                .clickable { onToggleSave(wordOfDay) }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text       = wordOfDay.definition,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 18.sp,
                        color      = VTextPri,
                        lineHeight = 26.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text       = wordOfDay.example,
                        fontStyle  = FontStyle.Italic,
                        fontSize   = 16.sp,
                        color      = VTextPri,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column {
                            Text(
                                text          = "SYNONYMS",
                                fontSize      = 12.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = VTextSec,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = wordOfDay.synonyms.joinToString(", "),
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = VTextPri
                            )
                        }
                        Column {
                            Text(
                                text          = "ANTONYMS",
                                fontSize      = 12.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = VTextSec,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = wordOfDay.antonyms.joinToString(", "),
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = VTextPri
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Flashcards",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = VTextPri
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Two flashcards side-by-side — driven ONLY by flashcardIndex ──
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(flashcardIndex, (flashcardIndex + 1) % wordsForLevel.size).forEach { idx ->
                    FlipCard(
                        vocabularyWord = wordsForLevel[idx],
                        modifier       = Modifier
                            .weight(1f)
                            .height(140.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Prev/Next arrows — only touch flashcardIndex now ─────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(5.dp, RoundedCornerShape(12.dp))
                        .background(VCardWhite, RoundedCornerShape(12.dp))
                        .clickable {
                            flashcardIndex = (flashcardIndex - 1 + wordsForLevel.size) % wordsForLevel.size
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Prev",
                        tint     = VTextPri,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(5.dp, RoundedCornerShape(12.dp))
                        .background(VCardWhite, RoundedCornerShape(12.dp))
                        .clickable {
                            flashcardIndex = (flashcardIndex + 1) % wordsForLevel.size
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next",
                        tint     = VTextPri,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Flip card ────────────────────────────────────────────────────────────────
@Composable
private fun FlipCard(
    vocabularyWord : VocabularyWord,
    modifier       : Modifier = Modifier
) {
    val authViewModel: AuthViewModel = viewModel()
    var flipped  by remember(vocabularyWord.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue   = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label         = "flip"
    )

    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .background(
                brush = Brush.linearGradient(vocabularyWord.gradient),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                if (!flipped) authViewModel.recordWordLearned() // record daily skill stat
                flipped = !flipped
            },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = vocabularyWord.word,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = "tap to flip",
                    fontSize = 11.sp,
                    color    = Color.White.copy(alpha = 0.75f)
                )
            }
        } else {
            Column(
                modifier            = Modifier
                    .graphicsLayer { rotationY = 180f }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text       = vocabularyWord.definition,
                    fontSize   = 11.sp,
                    color      = Color.White,
                    lineHeight = 16.sp
                )
            }
        }
    }
}// ─── Saved vocabulary screen ────────────────────────────────────────────────────
@Composable
private fun SavedVocabularyScreen(
    isSignedIn : Boolean,
    items      : List<SavedVocabularyItem>,
    onRemove   : (String) -> Unit,
    onBack     : () -> Unit
) {
    val scrollState = rememberScrollState()
    val speak = rememberTextToSpeech()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VSurface)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "Saved Vocabulary",
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = VTextPri
            )
        }

        when {
            !isSignedIn -> EmptyState(
                title    = "Sign in to save words",
                subtitle = "Log in to your account so your saved vocabulary syncs across devices."
            )
            items.isEmpty() -> EmptyState(
                title    = "No saved words yet",
                subtitle = "Tap the bookmark icon on any word to add it here."
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 24.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = "${items.size} word${if (items.size == 1) "" else "s"} saved",
                    fontSize = 13.sp,
                    color    = VTextSec
                )
                Spacer(Modifier.height(14.dp))

                items.forEach { item ->
                    SavedWordCard(
                        item     = item,
                        onRemove = { onRemove(item.id) },
                        onSpeak  = { speak(item.word) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier             = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        Icon(
            imageVector        = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint               = VTextSec,
            modifier           = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text       = title,
            fontWeight = FontWeight.Bold,
            fontSize   = 17.sp,
            color      = VTextPri
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text       = subtitle,
            fontSize   = 14.sp,
            color      = VTextSec,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SavedWordCard(
    item     : SavedVocabularyItem,
    onRemove : () -> Unit,
    onSpeak  : () -> Unit,
    modifier : Modifier = Modifier
) {
    ElevatedCard(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = VCardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    if (item.level.isNotBlank()) {
                        Text(
                            text          = levelLabel(item.level).uppercase(),
                            color         = levelAccentColor(item.level),
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Text(
                        text       = item.word,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 22.sp,
                        color      = VTextPri
                    )
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text     = "${item.phonetic} · ${item.partOfSpeech}",
                            fontSize = 13.sp,
                            color    = VTextSec
                        )
                        Icon(
                            imageVector        = Icons.Filled.VolumeUp,
                            contentDescription = "Pronounce ${item.word}",
                            tint               = VTextPri,
                            modifier           = Modifier
                                .size(16.dp)
                                .clickable { onSpeak() }
                        )
                    }
                }
                Icon(
                    imageVector        = Icons.Filled.Bookmark,
                    contentDescription = "Remove from saved",
                    tint               = VBlue,
                    modifier           = Modifier
                        .size(24.dp)
                        .clickable { onRemove() }
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text       = item.definition,
                fontSize   = 15.sp,
                color      = VTextPri,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = item.example,
                fontStyle  = FontStyle.Italic,
                fontSize   = 14.sp,
                color      = VTextSec,
                lineHeight = 20.sp
            )

            if (item.synonyms.isNotBlank() || item.antonyms.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (item.synonyms.isNotBlank()) {
                        Column {
                            Text("SYNONYMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VTextSec)
                            Text(item.synonyms, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VTextPri)
                        }
                    }
                    if (item.antonyms.isNotBlank()) {
                        Column {
                            Text("ANTONYMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VTextSec)
                            Text(item.antonyms, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VTextPri)
                        }
                    }
                }
            }
        }
    }
}