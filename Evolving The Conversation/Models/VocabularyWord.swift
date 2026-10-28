import SwiftUI
import Combine
import FirebaseFirestore
import FirebaseAuth

// MARK: - Vocabulary Word Model
struct VocabularyWord: Identifiable, Codable, Equatable {
    let id: String
    let word: String
    let phonetic: String
    let partOfSpeech: String
    let definition: String
    let example: String
    let synonyms: String
    let antonyms: String
    let level: VocabularyLevel
}

// MARK: - Vocabulary Level
enum VocabularyLevel: String, Codable, CaseIterable, Identifiable {
    case beginner, intermediate, advanced

    var id: String { rawValue }

    var title: String {
        switch self {
        case .beginner:     return "Beginner"
        case .intermediate: return "Intermediate"
        case .advanced:     return "Advanced"
        }
    }

    var subtitle: String {
        switch self {
        case .beginner:     return "Everyday words to build your foundation"
        case .intermediate: return "Expand your vocabulary with common terms"
        case .advanced:     return "Challenge yourself with complex words"
        }
    }

    var screenTitle: String { "\(title) Vocabulary" }

    var gradientColors: [Color] {
        switch self {
        case .beginner:
            return [Color(red: 0.169, green: 0.357, blue: 0.910),
                    Color(red: 0.102, green: 0.247, blue: 0.749)]
        case .intermediate:
            return [Color(red: 0.176, green: 0.741, blue: 0.494),
                    Color(red: 0.098, green: 0.545, blue: 0.365)]
        case .advanced:
            return [Color(red: 0.608, green: 0.435, blue: 0.910),
                    Color(red: 0.412, green: 0.243, blue: 0.749)]
        }
    }
}
final class SavedVocabularyStore: ObservableObject {
    static let shared = SavedVocabularyStore()

    @Published private(set) var savedIDs: Set<String> = []
    @Published private(set) var savedWordsFromServer: [VocabularyWord] = []   // ← NEW
    @Published private(set) var isLoading: Bool = false

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?
    private var authHandle: AuthStateDidChangeListenerHandle?
    private var currentUID: String?
    private var attachToken = UUID()

    private init() {
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            self?.attachListener(uid: user?.uid)
        }
        attachListener(uid: Auth.auth().currentUser?.uid)
    }

    deinit {
        listener?.remove()
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    private func attachListener(uid: String?) {
        let token = UUID()
        attachToken = token
        currentUID = uid

        listener?.remove()
        listener = nil
        savedIDs = []
        savedWordsFromServer = []   // ← NEW

        guard let uid else {
            isLoading = false
            return
        }

        isLoading = true
        listener = db.collection("users").document(uid)
            .collection("savedVocabulary")
            .addSnapshotListener { [weak self] snapshot, error in
                guard let self else { return }
                guard self.attachToken == token, self.currentUID == uid else { return }

                if let error = error {
                    print("SavedVocabularyStore listener error: \(error.localizedDescription)")
                    DispatchQueue.main.async {
                        guard self.attachToken == token else { return }
                        self.isLoading = false
                    }
                    return
                }

                // ← CHANGED: decode full word data instead of just IDs
                let docs = snapshot?.documents ?? []
                let ids = docs.map { $0.documentID }
                let words: [VocabularyWord] = docs.compactMap { doc in
                    let d = doc.data()
                    guard let word = d["word"] as? String else {
                        print("⚠️ Skipping \(doc.documentID): missing 'word' field. Raw data: \(d)")
                        return nil
                    }
                    guard let levelRaw = d["level"] as? String,
                          let level = VocabularyLevel(rawValue: levelRaw.lowercased()) else {   // ← normalize casing
                        print("⚠️ Skipping \(doc.documentID): bad 'level' value \(d["level"] ?? "nil"). Raw data: \(d)")
                        return nil
                    }
                    return VocabularyWord(
                        id: doc.documentID,
                        word: word,
                        phonetic: d["phonetic"] as? String ?? "",
                        partOfSpeech: d["partOfSpeech"] as? String ?? "",
                        definition: d["definition"] as? String ?? "",
                        example: d["example"] as? String ?? "",
                        synonyms: d["synonyms"] as? String ?? "",
                        antonyms: d["antonyms"] as? String ?? "",
                        level: level
                    )
                }

                DispatchQueue.main.async {
                    guard self.attachToken == token else { return }
                    self.savedIDs = Set(ids)
                    self.savedWordsFromServer = words   // ← NEW
                    self.isLoading = false
                }
            }
    }

    func isSaved(_ word: VocabularyWord) -> Bool {
        savedIDs.contains(word.id)
    }

    func toggle(_ word: VocabularyWord) {
        guard let uid = Auth.auth().currentUser?.uid, uid == currentUID else {
            print("SavedVocabularyStore: no signed-in user, ignoring toggle for \(word.id)")
            return
        }

        let docRef = db.collection("users").document(uid)
            .collection("savedVocabulary").document(word.id)

        if savedIDs.contains(word.id) {
            savedIDs.remove(word.id)
            docRef.delete { error in
                if let error = error {
                    print("Failed to unsave '\(word.word)': \(error.localizedDescription)")
                }
            }
        } else {
            savedIDs.insert(word.id)
            docRef.setData([
                "word": word.word,
                "phonetic": word.phonetic,          // ← NEW
                "partOfSpeech": word.partOfSpeech,  // ← NEW
                "definition": word.definition,      // ← NEW
                "example": word.example,            // ← NEW
                "synonyms": word.synonyms,           // ← NEW
                "antonyms": word.antonyms,           // ← NEW
                "level": word.level.rawValue,
                "savedAt": FieldValue.serverTimestamp()
            ]) { error in
                if let error = error {
                    print("Failed to save '\(word.word)': \(error.localizedDescription)")
                }
            }
        }
    }

    var savedWords: [VocabularyWord] {
        savedWordsFromServer   // ← CHANGED (was: VocabularyData.all.filter { savedIDs.contains($0.id) })
    }
}// MARK: - Vocabulary Data (20 words per level)
enum VocabularyData {

    static func words(for level: VocabularyLevel) -> [VocabularyWord] {
        switch level {
        case .beginner:     return beginner
        case .intermediate: return intermediate
        case .advanced:     return advanced
        }
    }

    static var all: [VocabularyWord] { beginner + intermediate + advanced }

    // MARK: Beginner
    static let beginner: [VocabularyWord] = [
        VocabularyWord(id: "beg_1", word: "Happy", phonetic: "/ˈhæpi/", partOfSpeech: "adjective",
            definition: "Feeling or showing pleasure and contentment.",
            example: "She felt happy after passing her exam.",
            synonyms: "joyful, glad", antonyms: "sad, unhappy", level: .beginner),
        VocabularyWord(id: "beg_2", word: "Brave", phonetic: "/breɪv/", partOfSpeech: "adjective",
            definition: "Ready to face danger or pain without fear.",
            example: "The brave firefighter ran into the burning building.",
            synonyms: "courageous, bold", antonyms: "cowardly, timid", level: .beginner),
        VocabularyWord(id: "beg_3", word: "Honest", phonetic: "/ˈɒnɪst/", partOfSpeech: "adjective",
            definition: "Free of deceit; truthful.",
            example: "He gave an honest answer to the question.",
            synonyms: "truthful, sincere", antonyms: "dishonest, deceitful", level: .beginner),
        VocabularyWord(id: "beg_4", word: "Careful", phonetic: "/ˈkɛəfʊl/", partOfSpeech: "adjective",
            definition: "Making sure of avoiding mistakes or harm.",
            example: "She was careful while crossing the busy road.",
            synonyms: "cautious, attentive", antonyms: "careless, reckless", level: .beginner),
        VocabularyWord(id: "beg_5", word: "Curious", phonetic: "/ˈkjʊəriəs/", partOfSpeech: "adjective",
            definition: "Eager to know or learn something.",
            example: "The curious child asked many questions.",
            synonyms: "inquisitive, interested", antonyms: "indifferent, uninterested", level: .beginner),
        VocabularyWord(id: "beg_6", word: "Gentle", phonetic: "/ˈdʒɛntl/", partOfSpeech: "adjective",
            definition: "Mild and kind in manner.",
            example: "She has a gentle way of speaking to children.",
            synonyms: "mild, tender", antonyms: "harsh, rough", level: .beginner),
        VocabularyWord(id: "beg_7", word: "Strong", phonetic: "/strɒŋ/", partOfSpeech: "adjective",
            definition: "Having great physical power.",
            example: "He is strong enough to lift heavy boxes.",
            synonyms: "powerful, sturdy", antonyms: "weak, feeble", level: .beginner),
        VocabularyWord(id: "beg_8", word: "Bright", phonetic: "/braɪt/", partOfSpeech: "adjective",
            definition: "Giving out a lot of light; intelligent.",
            example: "The bright student solved the puzzle quickly.",
            synonyms: "intelligent, radiant", antonyms: "dull, dim", level: .beginner),
        VocabularyWord(id: "beg_9", word: "Calm", phonetic: "/kɑːm/", partOfSpeech: "adjective",
            definition: "Not showing or feeling nervousness or anger.",
            example: "She stayed calm during the emergency.",
            synonyms: "peaceful, relaxed", antonyms: "anxious, agitated", level: .beginner),
        VocabularyWord(id: "beg_10", word: "Clever", phonetic: "/ˈklɛvər/", partOfSpeech: "adjective",
            definition: "Quick to understand and learn.",
            example: "The clever fox found a way out of the trap.",
            synonyms: "smart, intelligent", antonyms: "foolish, dull", level: .beginner),
        VocabularyWord(id: "beg_11", word: "Friendly", phonetic: "/ˈfrɛndli/", partOfSpeech: "adjective",
            definition: "Kind and pleasant toward others.",
            example: "Our new neighbor is very friendly.",
            synonyms: "kind, sociable", antonyms: "unfriendly, hostile", level: .beginner),
        VocabularyWord(id: "beg_12", word: "Patient", phonetic: "/ˈpeɪʃənt/", partOfSpeech: "adjective",
            definition: "Able to wait calmly without complaint.",
            example: "The teacher was patient with the slow learners.",
            synonyms: "tolerant, calm", antonyms: "impatient, restless", level: .beginner),
        VocabularyWord(id: "beg_13", word: "Polite", phonetic: "/pəˈlaɪt/", partOfSpeech: "adjective",
            definition: "Having good manners and respect for others.",
            example: "It's polite to say thank you.",
            synonyms: "courteous, respectful", antonyms: "rude, impolite", level: .beginner),
        VocabularyWord(id: "beg_14", word: "Simple", phonetic: "/ˈsɪmpl/", partOfSpeech: "adjective",
            definition: "Easily understood or done.",
            example: "The instructions were simple to follow.",
            synonyms: "easy, plain", antonyms: "complex, difficult", level: .beginner),
        VocabularyWord(id: "beg_15", word: "Quick", phonetic: "/kwɪk/", partOfSpeech: "adjective",
            definition: "Moving fast or done in a short time.",
            example: "She gave a quick reply to the message.",
            synonyms: "fast, swift", antonyms: "slow, sluggish", level: .beginner),
        VocabularyWord(id: "beg_16", word: "Kind", phonetic: "/kaɪnd/", partOfSpeech: "adjective",
            definition: "Having a friendly, generous nature.",
            example: "It was kind of him to help the old man.",
            synonyms: "caring, generous", antonyms: "unkind, cruel", level: .beginner),
        VocabularyWord(id: "beg_17", word: "Cheerful", phonetic: "/ˈtʃɪərfʊl/", partOfSpeech: "adjective",
            definition: "Noticeably happy and optimistic.",
            example: "Her cheerful smile brightened the room.",
            synonyms: "happy, upbeat", antonyms: "gloomy, sad", level: .beginner),
        VocabularyWord(id: "beg_18", word: "Helpful", phonetic: "/ˈhɛlpfʊl/", partOfSpeech: "adjective",
            definition: "Giving useful help or assistance.",
            example: "The helpful staff guided us to the right room.",
            synonyms: "supportive, useful", antonyms: "unhelpful, useless", level: .beginner),
        VocabularyWord(id: "beg_19", word: "Confident", phonetic: "/ˈkɒnfɪdənt/", partOfSpeech: "adjective",
            definition: "Feeling sure of oneself and one's abilities.",
            example: "She spoke with a confident voice during the interview.",
            synonyms: "self-assured, certain", antonyms: "insecure, doubtful", level: .beginner),
        VocabularyWord(id: "beg_20", word: "Grateful", phonetic: "/ˈɡreɪtfʊl/", partOfSpeech: "adjective",
            definition: "Feeling or showing thankfulness.",
            example: "I am grateful for your support.",
            synonyms: "thankful, appreciative", antonyms: "ungrateful, unappreciative", level: .beginner),
    ]

    // MARK: Intermediate
    static let intermediate: [VocabularyWord] = [
        VocabularyWord(id: "int_1", word: "Resilient", phonetic: "/rɪˈzɪliənt/", partOfSpeech: "adjective",
            definition: "Able to withstand or recover quickly from difficulties.",
            example: "The team remained resilient despite the setbacks.",
            synonyms: "tough, adaptable", antonyms: "fragile, weak", level: .intermediate),
        VocabularyWord(id: "int_2", word: "Candid", phonetic: "/ˈkændɪd/", partOfSpeech: "adjective",
            definition: "Truthful and straightforward; frank.",
            example: "He gave a candid opinion about the proposal.",
            synonyms: "honest, forthright", antonyms: "evasive, deceptive", level: .intermediate),
        VocabularyWord(id: "int_3", word: "Eloquent", phonetic: "/ˈɛləkwənt/", partOfSpeech: "adjective",
            definition: "Fluent or persuasive in speaking or writing.",
            example: "She gave an eloquent speech that moved the entire room.",
            synonyms: "fluent, articulate", antonyms: "inarticulate, halting", level: .intermediate),
        VocabularyWord(id: "int_4", word: "Tenacious", phonetic: "/təˈneɪʃəs/", partOfSpeech: "adjective",
            definition: "Holding firmly to a course of action; persistent.",
            example: "His tenacious attitude helped him finish the marathon.",
            synonyms: "persistent, determined", antonyms: "irresolute, weak-willed", level: .intermediate),
        VocabularyWord(id: "int_5", word: "Pragmatic", phonetic: "/præɡˈmætɪk/", partOfSpeech: "adjective",
            definition: "Dealing with things sensibly and practically.",
            example: "She took a pragmatic approach to solving the budget crisis.",
            synonyms: "practical, realistic", antonyms: "idealistic, impractical", level: .intermediate),
        VocabularyWord(id: "int_6", word: "Ambiguous", phonetic: "/æmˈbɪɡjuəs/", partOfSpeech: "adjective",
            definition: "Open to more than one interpretation.",
            example: "The contract's wording was ambiguous and confused both parties.",
            synonyms: "unclear, vague", antonyms: "clear, definite", level: .intermediate),
        VocabularyWord(id: "int_7", word: "Diligent", phonetic: "/ˈdɪlɪdʒənt/", partOfSpeech: "adjective",
            definition: "Showing care and effort in one's work.",
            example: "The diligent student reviewed her notes every night.",
            synonyms: "hardworking, industrious", antonyms: "lazy, negligent", level: .intermediate),
        VocabularyWord(id: "int_8", word: "Empathetic", phonetic: "/ˌɛmpəˈθɛtɪk/", partOfSpeech: "adjective",
            definition: "Able to understand and share the feelings of others.",
            example: "The empathetic nurse comforted the worried family.",
            synonyms: "compassionate, understanding", antonyms: "indifferent, callous", level: .intermediate),
        VocabularyWord(id: "int_9", word: "Frugal", phonetic: "/ˈfruːɡəl/", partOfSpeech: "adjective",
            definition: "Careful with money or resources; economical.",
            example: "They lived a frugal life to save for their house.",
            synonyms: "thrifty, economical", antonyms: "wasteful, extravagant", level: .intermediate),
        VocabularyWord(id: "int_10", word: "Genuine", phonetic: "/ˈdʒɛnjuɪn/", partOfSpeech: "adjective",
            definition: "Truly what it is said to be; sincere.",
            example: "Her genuine concern for others made her a great friend.",
            synonyms: "authentic, sincere", antonyms: "fake, insincere", level: .intermediate),
        VocabularyWord(id: "int_11", word: "Humble", phonetic: "/ˈhʌmbl/", partOfSpeech: "adjective",
            definition: "Having a modest view of one's own importance.",
            example: "Despite his success, he remained humble and kind.",
            synonyms: "modest, unassuming", antonyms: "arrogant, boastful", level: .intermediate),
        VocabularyWord(id: "int_12", word: "Innovative", phonetic: "/ˈɪnəveɪtɪv/", partOfSpeech: "adjective",
            definition: "Introducing new ideas or methods.",
            example: "The startup's innovative product changed the industry.",
            synonyms: "creative, original", antonyms: "conventional, unoriginal", level: .intermediate),
        VocabularyWord(id: "int_13", word: "Meticulous", phonetic: "/məˈtɪkjʊləs/", partOfSpeech: "adjective",
            definition: "Showing great attention to detail.",
            example: "The meticulous editor caught every small error.",
            synonyms: "precise, thorough", antonyms: "careless, sloppy", level: .intermediate),
        VocabularyWord(id: "int_14", word: "Optimistic", phonetic: "/ˌɒptɪˈmɪstɪk/", partOfSpeech: "adjective",
            definition: "Hopeful and confident about the future.",
            example: "She stayed optimistic even after the setback.",
            synonyms: "hopeful, positive", antonyms: "pessimistic, hopeless", level: .intermediate),
        VocabularyWord(id: "int_15", word: "Persistent", phonetic: "/pəˈsɪstənt/", partOfSpeech: "adjective",
            definition: "Continuing firmly despite difficulty.",
            example: "His persistent efforts finally paid off.",
            synonyms: "determined, steadfast", antonyms: "irresolute, hesitant", level: .intermediate),
        VocabularyWord(id: "int_16", word: "Reluctant", phonetic: "/rɪˈlʌktənt/", partOfSpeech: "adjective",
            definition: "Unwilling and hesitant to do something.",
            example: "He was reluctant to share his true feelings.",
            synonyms: "hesitant, unwilling", antonyms: "eager, willing", level: .intermediate),
        VocabularyWord(id: "int_17", word: "Skeptical", phonetic: "/ˈskɛptɪkəl/", partOfSpeech: "adjective",
            definition: "Not easily convinced; having doubts.",
            example: "She remained skeptical about the salesman's claims.",
            synonyms: "doubtful, dubious", antonyms: "trusting, credulous", level: .intermediate),
        VocabularyWord(id: "int_18", word: "Versatile", phonetic: "/ˈvɜːsətaɪl/", partOfSpeech: "adjective",
            definition: "Able to adapt to many different functions.",
            example: "He is a versatile player who can fill several positions.",
            synonyms: "adaptable, flexible", antonyms: "limited, inflexible", level: .intermediate),
        VocabularyWord(id: "int_19", word: "Witty", phonetic: "/ˈwɪti/", partOfSpeech: "adjective",
            definition: "Showing clever and quick humor.",
            example: "Her witty remarks kept everyone laughing.",
            synonyms: "humorous, sharp", antonyms: "dull, humorless", level: .intermediate),
        VocabularyWord(id: "int_20", word: "Zealous", phonetic: "/ˈzɛləs/", partOfSpeech: "adjective",
            definition: "Having great energy or enthusiasm for a cause.",
            example: "The zealous volunteers worked all night to help.",
            synonyms: "enthusiastic, passionate", antonyms: "apathetic, indifferent", level: .intermediate),
    ]

    // MARK: Advanced
    static let advanced: [VocabularyWord] = [
        VocabularyWord(id: "adv_1", word: "Ephemeral", phonetic: "/ɪˈfɛmərəl/", partOfSpeech: "adjective",
            definition: "Lasting for a very short time.",
            example: "The beauty of cherry blossoms is ephemeral.",
            synonyms: "fleeting, transient", antonyms: "permanent, lasting", level: .advanced),
        VocabularyWord(id: "adv_2", word: "Ubiquitous", phonetic: "/juːˈbɪkwɪtəs/", partOfSpeech: "adjective",
            definition: "Present or found everywhere.",
            example: "Smartphones have become ubiquitous in modern life.",
            synonyms: "omnipresent, widespread", antonyms: "rare, scarce", level: .advanced),
        VocabularyWord(id: "adv_3", word: "Serendipitous", phonetic: "/ˌsɛrənˈdɪpɪtəs/", partOfSpeech: "adjective",
            definition: "Occurring by happy chance.",
            example: "Their meeting was a serendipitous encounter at the airport.",
            synonyms: "fortuitous, chance", antonyms: "planned, intentional", level: .advanced),
        VocabularyWord(id: "adv_4", word: "Cacophonous", phonetic: "/kəˈkɒfənəs/", partOfSpeech: "adjective",
            definition: "Harsh and jarring in sound.",
            example: "The cacophonous traffic noise kept her awake.",
            synonyms: "discordant, harsh", antonyms: "harmonious, melodious", level: .advanced),
        VocabularyWord(id: "adv_5", word: "Ineffable", phonetic: "/ɪnˈɛfəbl/", partOfSpeech: "adjective",
            definition: "Too great to be expressed in words.",
            example: "She felt an ineffable joy at the birth of her child.",
            synonyms: "indescribable, inexpressible", antonyms: "describable, expressible", level: .advanced),
        VocabularyWord(id: "adv_6", word: "Perfunctory", phonetic: "/pəˈfʌŋktəri/", partOfSpeech: "adjective",
            definition: "Carried out with minimal effort or interest.",
            example: "He gave a perfunctory nod and walked away.",
            synonyms: "cursory, half-hearted", antonyms: "thorough, careful", level: .advanced),
        VocabularyWord(id: "adv_7", word: "Obfuscate", phonetic: "/ˈɒbfʌskeɪt/", partOfSpeech: "verb",
            definition: "To make something unclear or confusing.",
            example: "The report seemed designed to obfuscate the real numbers.",
            synonyms: "confuse, obscure", antonyms: "clarify, illuminate", level: .advanced),
        VocabularyWord(id: "adv_8", word: "Ostentatious", phonetic: "/ˌɒstɛnˈteɪʃəs/", partOfSpeech: "adjective",
            definition: "Showy in a way meant to impress others.",
            example: "The ostentatious mansion had gold-plated gates.",
            synonyms: "flashy, showy", antonyms: "modest, understated", level: .advanced),
        VocabularyWord(id: "adv_9", word: "Pernicious", phonetic: "/pəˈnɪʃəs/", partOfSpeech: "adjective",
            definition: "Having a harmful effect, especially gradually.",
            example: "The pernicious rumor damaged her reputation for years.",
            synonyms: "harmful, damaging", antonyms: "beneficial, harmless", level: .advanced),
        VocabularyWord(id: "adv_10", word: "Quintessential", phonetic: "/ˌkwɪntɪˈsɛnʃəl/", partOfSpeech: "adjective",
            definition: "Representing the most perfect example of a quality.",
            example: "He is the quintessential gentleman.",
            synonyms: "archetypal, classic", antonyms: "atypical, unrepresentative", level: .advanced),
        VocabularyWord(id: "adv_11", word: "Recalcitrant", phonetic: "/rɪˈkælsɪtrənt/", partOfSpeech: "adjective",
            definition: "Stubbornly resistant to authority or guidance.",
            example: "The recalcitrant employee refused to follow new policies.",
            synonyms: "defiant, obstinate", antonyms: "compliant, obedient", level: .advanced),
        VocabularyWord(id: "adv_12", word: "Sagacious", phonetic: "/səˈɡeɪʃəs/", partOfSpeech: "adjective",
            definition: "Having keen judgment and wisdom.",
            example: "The sagacious advisor guided the young king wisely.",
            synonyms: "wise, shrewd", antonyms: "foolish, unwise", level: .advanced),
        VocabularyWord(id: "adv_13", word: "Surreptitious", phonetic: "/ˌsʌrəpˈtɪʃəs/", partOfSpeech: "adjective",
            definition: "Done secretly or stealthily.",
            example: "She took a surreptitious glance at her phone during the meeting.",
            synonyms: "secretive, covert", antonyms: "open, overt", level: .advanced),
        VocabularyWord(id: "adv_14", word: "Taciturn", phonetic: "/ˈtæsɪtɜːn/", partOfSpeech: "adjective",
            definition: "Saying little; reserved in speech.",
            example: "The taciturn old man rarely spoke to strangers.",
            synonyms: "reserved, reticent", antonyms: "talkative, garrulous", level: .advanced),
        VocabularyWord(id: "adv_15", word: "Vicissitude", phonetic: "/vɪˈsɪsɪtjuːd/", partOfSpeech: "noun",
            definition: "A change of circumstances, often unpleasant.",
            example: "They faced the vicissitudes of life together.",
            synonyms: "fluctuation, upheaval", antonyms: "stability, constancy", level: .advanced),
        VocabularyWord(id: "adv_16", word: "Winsome", phonetic: "/ˈwɪnsəm/", partOfSpeech: "adjective",
            definition: "Attractive or appealing in a charming way.",
            example: "The child had a winsome smile that charmed everyone.",
            synonyms: "charming, endearing", antonyms: "repellent, unattractive", level: .advanced),
        VocabularyWord(id: "adv_17", word: "Magnanimous", phonetic: "/mæɡˈnænɪməs/", partOfSpeech: "adjective",
            definition: "Generous or forgiving, especially toward a rival.",
            example: "The champion was magnanimous in victory.",
            synonyms: "generous, noble", antonyms: "petty, vindictive", level: .advanced),
        VocabularyWord(id: "adv_18", word: "Fastidious", phonetic: "/fæˈstɪdiəs/", partOfSpeech: "adjective",
            definition: "Very attentive to detail; hard to please.",
            example: "He was fastidious about keeping his desk organized.",
            synonyms: "meticulous, particular", antonyms: "careless, sloppy", level: .advanced),
        VocabularyWord(id: "adv_19", word: "Inexorable", phonetic: "/ɪnˈɛksərəbl/", partOfSpeech: "adjective",
            definition: "Impossible to stop or prevent.",
            example: "The inexorable rise of technology reshaped the industry.",
            synonyms: "unstoppable, relentless", antonyms: "stoppable, avoidable", level: .advanced),
        VocabularyWord(id: "adv_20", word: "Ebullient", phonetic: "/ɪˈbʌliənt/", partOfSpeech: "adjective",
            definition: "Cheerful and full of energy.",
            example: "She was ebullient after hearing the good news.",
            synonyms: "exuberant, enthusiastic", antonyms: "gloomy, subdued", level: .advanced),
    ]
}
