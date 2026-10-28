import Foundation

// MARK: - Question Type
enum QuestionType {
    case multipleChoice
    case fillInTheBlank
    case matchThePairs
}

// MARK: - Quiz Question Model
struct QuizQuestion: Identifiable {
    let id = UUID()
    let type: QuestionType
    let question: String

    // Multiple choice
    var options: [String] = []
    var correctOptionIndex: Int = 0

    // Fill in the blank
    var blankAnswer: String = ""

    // Match the pairs
    var leftItems: [String] = []
    var rightItems: [String] = []
    var correctPairs: [Int: Int] = [:]   // leftIndex : rightIndex
}

// MARK: - Question Pools (matches your Android dev's data)
enum QuizData {

    static let mcqPool: [QuizQuestion] = [
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct form: \"She ___ learning English for two years.\"",
            options: ["have been", "has been", "had being", "have being"], correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Which sentence uses the passive voice correctly?",
            options: ["She was given the award.", "She gave the award.",
                       "The award was giving to her.", "They have given she the award."],
            correctOptionIndex: 0),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct article: \"___ Eiffel Tower is in Paris.\"",
            options: ["A", "An", "The", "No article"], correctOptionIndex: 2),
        QuizQuestion(type: .multipleChoice,
            question: "Select the correct conditional: \"If I ___ you, I would study harder.\"",
            options: ["am", "was", "were", "will be"], correctOptionIndex: 2),
        QuizQuestion(type: .multipleChoice,
            question: "Which modal expresses strong obligation?",
            options: ["might", "could", "must", "would"], correctOptionIndex: 2),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct sentence.",
            options: ["She don't like coffee.", "She doesn't like coffee.",
                       "She doesn't likes coffee.", "She not like coffee."],
            correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct verb: \"My brother ___ to school by bus every day.\"",
            options: ["go", "goes", "going", "gone"], correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct preposition: \"The keys are ___ the table.\"",
            options: ["in", "on", "at", "between"], correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Which word is the synonym of \"rapid\"?",
            options: ["Slow", "Quick", "Heavy", "Bright"], correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct article: \"I saw ___ elephant at the zoo.\"",
            options: ["a", "an", "the", "no article"], correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Which sentence is in the Present Continuous tense?",
            options: ["She reads every night.", "She is reading a book.",
                       "She read a book.", "She has read a book."],
            correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct modal verb: \"You ___ wear a seatbelt while driving.\"",
            options: ["can", "must", "may", "might"], correctOptionIndex: 1),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the opposite of \"expensive\".",
            options: ["Cheap", "Costly", "Large", "Valuable"], correctOptionIndex: 0),
        QuizQuestion(type: .multipleChoice,
            question: "Complete the sentence: \"If it rains tomorrow, we ___ at home.\"",
            options: ["stay", "stayed", "will stay", "staying"], correctOptionIndex: 2),
        QuizQuestion(type: .multipleChoice,
            question: "Choose the correct question.",
            options: ["Where you live?", "Where do you live?",
                       "Where does you live?", "Where living you?"],
            correctOptionIndex: 1),
    ]

    static let fillBlankPool: [QuizQuestion] = [
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete the sentence: \"I ___ (go) to the market every Sunday.\"",
            blankAnswer: "go"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"Water boils at 100 degrees ___.\"",
            blankAnswer: "celsius"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete: \"The opposite of 'happy' is ___.\"",
            blankAnswer: "sad"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"She has been living here ___ 2019.\"",
            blankAnswer: "since"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete: \"An animal that meows is called a ___.\"",
            blankAnswer: "cat"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete the sentence: \"She ___ (study) English every evening.\"",
            blankAnswer: "studies"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"We arrived ___ the airport early.\"",
            blankAnswer: "at"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete the sentence: \"I have lived here ___ five years.\"",
            blankAnswer: "for"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"The sun ___ in the east.\"",
            blankAnswer: "rises"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete the sentence: \"Yesterday, they ___ (watch) a movie.\"",
            blankAnswer: "watched"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"Please turn ___ the lights before leaving.\"",
            blankAnswer: "off"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete the sentence: \"My sister is ___ than me.\"",
            blankAnswer: "taller"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"He has ___ his homework already.\"",
            blankAnswer: "finished"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Complete the sentence: \"There ___ three apples on the table.\"",
            blankAnswer: "are"),
        QuizQuestion(type: .fillInTheBlank,
            question: "Fill in the blank: \"The opposite of 'early' is ___.\"",
            blankAnswer: "late"),
    ]

    static let matchPairsPool: [QuizQuestion] = [
        QuizQuestion(type: .matchThePairs,
            question: "Match each word with its correct meaning.",
            leftItems: ["Happy", "Big", "Fast"], rightItems: ["Quick", "Large", "Joyful"],
            correctPairs: [0: 2, 1: 1, 2: 0]),
        QuizQuestion(type: .matchThePairs,
            question: "Match each country with its capital.",
            leftItems: ["France", "Japan", "Egypt"], rightItems: ["Cairo", "Tokyo", "Paris"],
            correctPairs: [0: 2, 1: 1, 2: 0]),
        QuizQuestion(type: .matchThePairs,
            question: "Match each verb with its past tense form.",
            leftItems: ["Go", "Eat", "See"], rightItems: ["Saw", "Went", "Ate"],
            correctPairs: [0: 1, 1: 2, 2: 0]),
        QuizQuestion(type: .matchThePairs,
            question: "Match each word with its synonym.",
            leftItems: ["Smart", "Angry", "Tiny"], rightItems: ["Furious", "Small", "Clever"],
            correctPairs: [0: 2, 1: 0, 2: 1]),
        QuizQuestion(type: .matchThePairs,
            question: "Match each tool with what it's used for.",
            leftItems: ["Hammer", "Scissors", "Pencil"],
            rightItems: ["Writing", "Cutting", "Hammering nails"],
            correctPairs: [0: 2, 1: 1, 2: 0]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the word with its meaning.",
            leftItems: ["Honest", "Brave", "Tiny"], rightItems: ["Truthful", "Courageous", "Very small"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the country with its capital.",
            leftItems: ["Canada", "Italy", "Australia"], rightItems: ["Ottawa", "Rome", "Canberra"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the verb with its past tense.",
            leftItems: ["Write", "Buy", "Speak"], rightItems: ["Wrote", "Bought", "Spoke"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the adjective with its opposite.",
            leftItems: ["Strong", "Empty", "Clean"], rightItems: ["Weak", "Full", "Dirty"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the word with its category.",
            leftItems: ["Apple", "Carrot", "Salmon"], rightItems: ["Fruit", "Vegetable", "Fish"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the occupation with the workplace.",
            leftItems: ["Doctor", "Teacher", "Chef"], rightItems: ["Hospital", "School", "Restaurant"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the sentence with its tense.",
            leftItems: ["I eat lunch.", "I ate lunch.", "I will eat lunch."],
            rightItems: ["Present Simple", "Past Simple", "Future Simple"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the phrasal verb with its meaning.",
            leftItems: ["Wake up", "Sit down", "Turn off"],
            rightItems: ["Stop sleeping", "Take a seat", "Switch off"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the word with its synonym.",
            leftItems: ["Begin", "End", "Silent"], rightItems: ["Start", "Finish", "Quiet"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
        QuizQuestion(type: .matchThePairs,
            question: "Match the sentence with the correct punctuation.",
            leftItems: ["What is your name", "I love English", "Wow"],
            rightItems: ["?", ".", "!"],
            correctPairs: [0: 0, 1: 1, 2: 2]),
    ]

    /// Builds a fresh 15-question quiz: 5 random MCQs, then 5 random fill-in-the-blanks,
    /// then 5 random match-the-pairs. Block order is fixed; contents within each block
    /// are shuffled and re-picked every time this is called (i.e. every screen open).
    static func buildQuiz() -> [QuizQuestion] {
        let mcqs    = Array(mcqPool.shuffled().prefix(5))
        let blanks  = Array(fillBlankPool.shuffled().prefix(5))
        let matches = Array(matchPairsPool.shuffled().prefix(5))
        return mcqs + blanks + matches
    }
}
