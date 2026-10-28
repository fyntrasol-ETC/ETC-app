package com.evolvingtheconversation.app.ui.screens.learning

import java.util.UUID

// MARK: - Grammar MCQ Question Model
data class GrammarMCQQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)

// MARK: - Shuffle helpers
// Shuffles the order of questions in a quiz, and also shuffles the option order
// within each question (correctAnswer is unaffected since it's matched by value,
// not by index).
fun List<GrammarMCQQuestion>.shuffledQuiz(): List<GrammarMCQQuestion> {
    return this.shuffled().map { q -> q.copy(options = q.options.shuffled()) }
}

// MARK: - Tenses
val tensesMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "She _____ to school every day.",
        options = listOf("go", "goes", "going", "gone"),
        correctAnswer = "goes"
    ),
    GrammarMCQQuestion(
        question = "They _____ dinner when the phone rang.",
        options = listOf("had", "were having", "have", "are having"),
        correctAnswer = "were having"
    ),
    GrammarMCQQuestion(
        question = "I _____ my homework before I went to bed.",
        options = listOf("finish", "have finished", "had finished", "was finishing"),
        correctAnswer = "had finished"
    ),
    GrammarMCQQuestion(
        question = "Look! The baby _____.",
        options = listOf("cry", "cries", "is crying", "cried"),
        correctAnswer = "is crying"
    ),
    GrammarMCQQuestion(
        question = "By this time next year, she _____ her degree.",
        options = listOf("completes", "completed", "will have completed", "has completed"),
        correctAnswer = "will have completed"
    ),
    GrammarMCQQuestion(
        question = "We _____ in Karachi since 2018.",
        options = listOf("lived", "have lived", "live", "had lived"),
        correctAnswer = "have lived"
    ),
    GrammarMCQQuestion(
        question = "If he arrives early, we _____ the meeting on time.",
        options = listOf("start", "started", "will start", "have started"),
        correctAnswer = "will start"
    ),
    GrammarMCQQuestion(
        question = "The train _____ before we reached the station.",
        options = listOf("leaves", "has left", "had left", "will leave"),
        correctAnswer = "had left"
    ),
    GrammarMCQQuestion(
        question = "At 9 p.m. tomorrow, I _____ for my exam.",
        options = listOf("study", "studied", "will be studying", "have studied"),
        correctAnswer = "will be studying"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is grammatically correct?",
        options = listOf(
            "She don't like coffee.",
            "She isn't liking coffee.",
            "She doesn't like coffee.",
            "She not likes coffee."
        ),
        correctAnswer = "She doesn't like coffee."
    )
)

// MARK: - Articles
val articlesMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "She bought _____ umbrella because it was raining.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "an"
    ),
    GrammarMCQQuestion(
        question = "_____ sun rises in the east.",
        options = listOf("A", "An", "The", "No article"),
        correctAnswer = "The"
    ),
    GrammarMCQQuestion(
        question = "I have _____ pet dog.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "a"
    ),
    GrammarMCQQuestion(
        question = "Ali is _____ honest man.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "an"
    ),
    GrammarMCQQuestion(
        question = "We visited _____ Eiffel Tower during our trip.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "the"
    ),
    GrammarMCQQuestion(
        question = "My sister wants to become _____ engineer.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "an"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is correct?",
        options = listOf(
            "She is a best student.",
            "She is the best student.",
            "She is an best student.",
            "She is best student."
        ),
        correctAnswer = "She is the best student."
    ),
    GrammarMCQQuestion(
        question = "I usually drink _____ milk before going to bed.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "no article"
    ),
    GrammarMCQQuestion(
        question = "He found _____ old coin in the garden.",
        options = listOf("a", "an", "the", "no article"),
        correctAnswer = "an"
    ),
    GrammarMCQQuestion(
        question = "Which sentence uses the article correctly?",
        options = listOf(
            "She adopted an cat.",
            "He bought a apple.",
            "I saw a bird on the tree.",
            "She is the engineer at Google."
        ),
        correctAnswer = "I saw a bird on the tree."
    )
)

// MARK: - Prepositions
val prepositionsMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "The book is _____ the table.",
        options = listOf("in", "on", "at", "under"),
        correctAnswer = "on"
    ),
    GrammarMCQQuestion(
        question = "She arrived _____ the airport at 6 a.m.",
        options = listOf("in", "at", "on", "by"),
        correctAnswer = "at"
    ),
    GrammarMCQQuestion(
        question = "We have lived here _____ 2020.",
        options = listOf("for", "since", "during", "from"),
        correctAnswer = "since"
    ),
    GrammarMCQQuestion(
        question = "The children are playing _____ the park.",
        options = listOf("in", "on", "at", "over"),
        correctAnswer = "in"
    ),
    GrammarMCQQuestion(
        question = "He walked _____ the bridge to reach the village.",
        options = listOf("across", "between", "among", "inside"),
        correctAnswer = "across"
    ),
    GrammarMCQQuestion(
        question = "She has been studying _____ three hours.",
        options = listOf("since", "for", "from", "at"),
        correctAnswer = "for"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is correct?",
        options = listOf(
            "He is good in mathematics.",
            "He is good on mathematics.",
            "He is good at mathematics.",
            "He is good with mathematics."
        ),
        correctAnswer = "He is good at mathematics."
    ),
    GrammarMCQQuestion(
        question = "The cat jumped _____ the wall.",
        options = listOf("over", "under", "between", "through"),
        correctAnswer = "over"
    ),
    GrammarMCQQuestion(
        question = "The teacher stood _____ the two students.",
        options = listOf("among", "between", "over", "through"),
        correctAnswer = "between"
    ),
    GrammarMCQQuestion(
        question = "Choose the correct sentence.",
        options = listOf(
            "She is interested on music.",
            "She is interested at music.",
            "She is interested in music.",
            "She is interested with music."
        ),
        correctAnswer = "She is interested in music."
    )
)

// MARK: - Passive Voice
val passiveVoiceMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "The letter _____ yesterday.",
        options = listOf("was written", "wrote", "is writing", "has wrote"),
        correctAnswer = "was written"
    ),
    GrammarMCQQuestion(
        question = "English _____ in many countries.",
        options = listOf("speaks", "is spoken", "was speaking", "has spoken"),
        correctAnswer = "is spoken"
    ),
    GrammarMCQQuestion(
        question = "The cake _____ by Sarah tomorrow.",
        options = listOf("will bake", "will be baked", "is baking", "has baked"),
        correctAnswer = "will be baked"
    ),
    GrammarMCQQuestion(
        question = "The homework _____ before the deadline.",
        options = listOf("had completed", "had been completed", "was completing", "has complete"),
        correctAnswer = "had been completed"
    ),
    GrammarMCQQuestion(
        question = "A new hospital _____ in our city last year.",
        options = listOf("built", "was built", "is built", "has building"),
        correctAnswer = "was built"
    ),
    GrammarMCQQuestion(
        question = "The windows _____ every Saturday.",
        options = listOf("clean", "are cleaned", "were cleaning", "has cleaned"),
        correctAnswer = "are cleaned"
    ),
    GrammarMCQQuestion(
        question = "Choose the passive form of: 'The teacher praised the student.'",
        options = listOf(
            "The student praised the teacher.",
            "The student was praised by the teacher.",
            "The teacher was praised by the student.",
            "The student is praised the teacher."
        ),
        correctAnswer = "The student was praised by the teacher."
    ),
    GrammarMCQQuestion(
        question = "Which sentence is in the passive voice?",
        options = listOf(
            "Ali repaired the car.",
            "The car was repaired by Ali.",
            "Ali is repairing the car.",
            "Ali repairs the car every week."
        ),
        correctAnswer = "The car was repaired by Ali."
    ),
    GrammarMCQQuestion(
        question = "The project _____ by the team before the meeting.",
        options = listOf("finished", "had been finished", "has finish", "was finishing"),
        correctAnswer = "had been finished"
    ),
    GrammarMCQQuestion(
        question = "Choose the correct passive sentence.",
        options = listOf(
            "The song was sung by the singer.",
            "The song sung by the singer.",
            "The song was sang by the singer.",
            "The song is sunged by the singer."
        ),
        correctAnswer = "The song was sung by the singer."
    )
)

// MARK: - Active Voice
val activeVoiceMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "Which sentence is in the active voice?",
        options = listOf(
            "The cake was baked by Sara.",
            "Sara baked the cake.",
            "The cake is being baked by Sara.",
            "The cake had been baked by Sara."
        ),
        correctAnswer = "Sara baked the cake."
    ),
    GrammarMCQQuestion(
        question = "Choose the active form of: 'The book was written by Mark.'",
        options = listOf(
            "Mark wrote the book.",
            "Mark was writing the book.",
            "The book wrote Mark.",
            "Mark has wrote the book."
        ),
        correctAnswer = "Mark wrote the book."
    ),
    GrammarMCQQuestion(
        question = "The chef _____ a delicious meal every evening.",
        options = listOf("cooks", "is cooked", "was cooked", "has been cooked"),
        correctAnswer = "cooks"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is written in the active voice?",
        options = listOf(
            "The match was won by our team.",
            "The room was cleaned yesterday.",
            "Our team won the match.",
            "The homework was completed."
        ),
        correctAnswer = "Our team won the match."
    ),
    GrammarMCQQuestion(
        question = "Choose the active form of: 'The email was sent by John.'",
        options = listOf(
            "John sent the email.",
            "John was sent the email.",
            "The email sent John.",
            "John has been sent the email."
        ),
        correctAnswer = "John sent the email."
    ),
    GrammarMCQQuestion(
        question = "Every morning, my father _____ the newspaper.",
        options = listOf("reads", "is read", "was read", "has been read"),
        correctAnswer = "reads"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is NOT in the active voice?",
        options = listOf(
            "She painted the wall.",
            "The wall was painted by her.",
            "They organized the event.",
            "He fixed the computer."
        ),
        correctAnswer = "The wall was painted by her."
    ),
    GrammarMCQQuestion(
        question = "Choose the active form of: 'The project will be completed by the team.'",
        options = listOf(
            "The team will complete the project.",
            "The team completed the project.",
            "The project completes the team.",
            "The project will complete the team."
        ),
        correctAnswer = "The team will complete the project."
    ),
    GrammarMCQQuestion(
        question = "The students _____ the classroom before the teacher arrived.",
        options = listOf("had cleaned", "had been cleaned", "were cleaned", "are cleaned"),
        correctAnswer = "had cleaned"
    ),
    GrammarMCQQuestion(
        question = "Choose the active sentence.",
        options = listOf(
            "The decision was made by the manager.",
            "The report was prepared yesterday.",
            "The manager made the decision.",
            "The documents were signed."
        ),
        correctAnswer = "The manager made the decision."
    )
)

// MARK: - Modals
val modalsMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "You _____ wear a seat belt while driving.",
        options = listOf("must", "can", "might", "could"),
        correctAnswer = "must"
    ),
    GrammarMCQQuestion(
        question = "_____ you help me carry these bags?",
        options = listOf("May", "Can", "Must", "Should"),
        correctAnswer = "Can"
    ),
    GrammarMCQQuestion(
        question = "It looks cloudy. It _____ rain later.",
        options = listOf("might", "must", "should", "can"),
        correctAnswer = "might"
    ),
    GrammarMCQQuestion(
        question = "You _____ apologize for being late.",
        options = listOf("should", "could", "may", "can"),
        correctAnswer = "should"
    ),
    GrammarMCQQuestion(
        question = "When I was younger, I _____ swim very fast.",
        options = listOf("can", "could", "must", "may"),
        correctAnswer = "could"
    ),
    GrammarMCQQuestion(
        question = "Students _____ use calculators during the exam.",
        options = listOf("mustn't", "can", "should", "could"),
        correctAnswer = "mustn't"
    ),
    GrammarMCQQuestion(
        question = "Choose the sentence that expresses strong certainty.",
        options = listOf(
            "She might be at home.",
            "She could be at home.",
            "She must be at home.",
            "She may be at home."
        ),
        correctAnswer = "She must be at home."
    ),
    GrammarMCQQuestion(
        question = "Which sentence is grammatically correct?",
        options = listOf(
            "He can to drive.",
            "He musts leave now.",
            "He should study harder.",
            "He coulds swim well."
        ),
        correctAnswer = "He should study harder."
    ),
    GrammarMCQQuestion(
        question = "_____ I borrow your pen, please?",
        options = listOf("May", "Must", "Should", "Couldn't"),
        correctAnswer = "May"
    ),
    GrammarMCQQuestion(
        question = "Choose the correct sentence.",
        options = listOf(
            "You must to finish your homework.",
            "You should finishing your homework.",
            "You must finish your homework.",
            "You can finished your homework."
        ),
        correctAnswer = "You must finish your homework."
    )
)

// MARK: - Conditionals
val conditionalsMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "If it rains, we _____ at home.",
        options = listOf("stay", "stayed", "will stay", "would stay"),
        correctAnswer = "will stay"
    ),
    GrammarMCQQuestion(
        question = "If I _____ enough money, I would buy a new car.",
        options = listOf("have", "had", "will have", "am having"),
        correctAnswer = "had"
    ),
    GrammarMCQQuestion(
        question = "If she studies hard, she _____ the exam.",
        options = listOf("passes", "passed", "will pass", "would pass"),
        correctAnswer = "will pass"
    ),
    GrammarMCQQuestion(
        question = "If they had left earlier, they _____ the train.",
        options = listOf("would catch", "would have caught", "caught", "will catch"),
        correctAnswer = "would have caught"
    ),
    GrammarMCQQuestion(
        question = "If you heat ice, it _____.",
        options = listOf("melted", "melts", "will melt", "would melt"),
        correctAnswer = "melts"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is a First Conditional?",
        options = listOf(
            "If I had wings, I would fly.",
            "If it rains, I will take an umbrella.",
            "If I had studied, I would have passed.",
            "If water reached 100°C, it boiled."
        ),
        correctAnswer = "If it rains, I will take an umbrella."
    ),
    GrammarMCQQuestion(
        question = "Choose the correct Second Conditional sentence.",
        options = listOf(
            "If I am rich, I will travel the world.",
            "If I were rich, I would travel the world.",
            "If I had been rich, I would have traveled the world.",
            "If I was rich, I will travel the world."
        ),
        correctAnswer = "If I were rich, I would travel the world."
    ),
    GrammarMCQQuestion(
        question = "If you _____ the instructions, you wouldn't have made that mistake.",
        options = listOf("follow", "followed", "had followed", "will follow"),
        correctAnswer = "had followed"
    ),
    GrammarMCQQuestion(
        question = "Which sentence is grammatically correct?",
        options = listOf(
            "If he will come, we will start.",
            "If he comes, we will start.",
            "If he came, we will start.",
            "If he come, we start."
        ),
        correctAnswer = "If he comes, we will start."
    ),
    GrammarMCQQuestion(
        question = "If I _____ you, I would accept the job offer.",
        options = listOf("am", "was", "were", "had been"),
        correctAnswer = "were"
    )
)

// MARK: - Reported Speech
val reportedSpeechMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "Change into reported speech: He said, 'I am tired.'",
        options = listOf(
            "He said that he is tired.",
            "He said that he was tired.",
            "He says that he was tired.",
            "He said that I was tired."
        ),
        correctAnswer = "He said that he was tired."
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: Sara said, 'I will help you.'",
        options = listOf(
            "Sara said that she would help me.",
            "Sara said that she will help me.",
            "Sara says that she would help me.",
            "Sara said that I would help her."
        ),
        correctAnswer = "Sara said that she would help me."
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: Ali said, 'I have completed my work.'",
        options = listOf(
            "Ali said that he completed his work.",
            "Ali said that he had completed his work.",
            "Ali said that he has completed my work.",
            "Ali says that he had completed his work."
        ),
        correctAnswer = "Ali said that he had completed his work."
    ),
    GrammarMCQQuestion(
        question = "Change into direct speech: She said that she was happy.",
        options = listOf(
            "She said, 'I am happy.'",
            "She said, 'She is happy.'",
            "She says, 'I was happy.'",
            "She said, 'I were happy.'"
        ),
        correctAnswer = "She said, 'I am happy.'"
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: He said, 'Do you like tea?'",
        options = listOf(
            "He asked if I liked tea.",
            "He asked do I like tea.",
            "He said if I like tea.",
            "He asked that I liked tea."
        ),
        correctAnswer = "He asked if I liked tea."
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: The teacher said, 'Open your books.'",
        options = listOf(
            "The teacher told us to open our books.",
            "The teacher said us open our books.",
            "The teacher told that open books.",
            "The teacher asked if we opened books."
        ),
        correctAnswer = "The teacher told us to open our books."
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: He said, 'I can swim.'",
        options = listOf(
            "He said that he can swim.",
            "He said that he could swim.",
            "He said that I could swim.",
            "He says that he could swim."
        ),
        correctAnswer = "He said that he could swim."
    ),
    GrammarMCQQuestion(
        question = "Which sentence is correctly reported?",
        options = listOf(
            "She said that she will come tomorrow.",
            "She said that she would come the next day.",
            "She said that she come next day.",
            "She said she would came tomorrow."
        ),
        correctAnswer = "She said that she would come the next day."
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: Ahmed said, 'Where are you going?'",
        options = listOf(
            "Ahmed asked where I was going.",
            "Ahmed asked where was I going.",
            "Ahmed said where I am going.",
            "Ahmed asked where I going."
        ),
        correctAnswer = "Ahmed asked where I was going."
    ),
    GrammarMCQQuestion(
        question = "Change into reported speech: She said, 'Don't touch the fire.'",
        options = listOf(
            "She told me not to touch the fire.",
            "She said me don't touch the fire.",
            "She told that I don't touch the fire.",
            "She asked me touching the fire."
        ),
        correctAnswer = "She told me not to touch the fire."
    )
)

// MARK: - Punctuation
val punctuationMCQs: List<GrammarMCQQuestion> = listOf(
    GrammarMCQQuestion(
        question = "Which sentence uses punctuation correctly?",
        options = listOf(
            "What is your name.",
            "What is your name?",
            "What is your name!",
            "What is your name,"
        ),
        correctAnswer = "What is your name?"
    ),
    GrammarMCQQuestion(
        question = "Choose the correctly punctuated sentence.",
        options = listOf(
            "I like apples oranges and bananas.",
            "I like apples, oranges, and bananas.",
            "I like apples oranges, and bananas.",
            "I like, apples oranges and bananas."
        ),
        correctAnswer = "I like apples, oranges, and bananas."
    ),
    GrammarMCQQuestion(
        question = "Which punctuation mark is used at the end of an exclamatory sentence?",
        options = listOf(".", "?", "!", ","),
        correctAnswer = "!"
    ),
    GrammarMCQQuestion(
        question = "Choose the correct sentence.",
        options = listOf(
            "Ali said I am ready.",
            "Ali said, 'I am ready.'",
            "Ali said 'I am ready'.",
            "Ali, said 'I am ready.'"
        ),
        correctAnswer = "Ali said, 'I am ready.'"
    ),
    GrammarMCQQuestion(
        question = "Which sentence uses an apostrophe correctly?",
        options = listOf(
            "The girls book is new.",
            "The girl's book is new.",
            "The girls's book is new.",
            "The girl book's is new."
        ),
        correctAnswer = "The girl's book is new."
    ),
    GrammarMCQQuestion(
        question = "Choose the correctly punctuated sentence.",
        options = listOf(
            "My favorite colors are blue green and red.",
            "My favorite colors are blue, green, and red.",
            "My favorite colors are blue green, and red",
            "My favorite colors are, blue green and red."
        ),
        correctAnswer = "My favorite colors are blue, green, and red."
    ),
    GrammarMCQQuestion(
        question = "Which punctuation mark separates two closely related independent clauses?",
        options = listOf("Comma", "Semicolon", "Apostrophe", "Question mark"),
        correctAnswer = "Semicolon"
    ),
    GrammarMCQQuestion(
        question = "Choose the correct sentence.",
        options = listOf(
            "Its raining outside.",
            "It's raining outside.",
            "Its' raining outside.",
            "It raining outside."
        ),
        correctAnswer = "It's raining outside."
    ),
    GrammarMCQQuestion(
        question = "Which sentence is punctuated correctly?",
        options = listOf(
            "Wow, that was amazing!",
            "Wow that was amazing.",
            "Wow; that was amazing!",
            "Wow that, was amazing!"
        ),
        correctAnswer = "Wow, that was amazing!"
    ),
    GrammarMCQQuestion(
        question = "Choose the correctly punctuated sentence.",
        options = listOf(
            "The teacher asked, 'Are you ready?'",
            "The teacher asked 'Are you ready'.",
            "The teacher asked, Are you ready?",
            "The teacher asked 'Are you ready?'"
        ),
        correctAnswer = "The teacher asked, 'Are you ready?'"
    )
)

// MARK: - Topic → Quiz map
// Keys match GrammarTopic.title exactly, so you can do:
//   val quiz = grammarMCQMap[topic.title]?.shuffledQuiz() ?: emptyList()
val grammarMCQMap: Map<String, List<GrammarMCQQuestion>> = mapOf(
    "Tenses" to tensesMCQs,
    "Articles" to articlesMCQs,
    "Prepositions" to prepositionsMCQs,
    "Passive Voice" to passiveVoiceMCQs,
    "Active Voice" to activeVoiceMCQs,
    "Modals" to modalsMCQs,
    "Conditionals" to conditionalsMCQs,
    "Reported Speech" to reportedSpeechMCQs,
    "Punctuation" to punctuationMCQs
)