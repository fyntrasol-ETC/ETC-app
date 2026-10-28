//import FirebaseFirestore
//import FirebaseAuth
//
//class FirestoreSeeder {
//    static let db = Firestore.firestore()
//    
//    // MARK: - Call this ONCE from a dev button in Settings or AppDelegate
//    static func seedAll() async {
//        do {
//            try await seedTutors()
//            try await seedCourses()
//            try await seedLessons()
//            try await seedGrammarTopics()
//            try await seedQuizzes()
//            try await seedVocabulary()
//            try await seedDailyChallenges()
//            print("✅ Firestore seeding complete")
//        } catch {
//            print("❌ Seeding error: \(error)")
//        }
//    }
//    
//    static func seedTutors() async throws {
//        let tutors: [[String: Any]] = [
//            [
//                "id": "tutor_daniel",
//                "name": "Daniel Reed",
//                "title": "Spoken English Expert",
//                "bio": "10 years teaching English to Spanish speakers. Former BBC journalist.",
//                "photoURL": "",
//                "rating": 4.9,
//                "totalStudents": 12400
//            ],
//            [
//                "id": "tutor_sara",
//                "name": "Sara Lin",
//                "title": "Grammar Specialist",
//                "bio": "MA in Linguistics from Oxford. Specialist in English grammar for non-native speakers.",
//                "photoURL": "",
//                "rating": 4.8,
//                "totalStudents": 9100
//            ],
//            [
//                "id": "tutor_mark",
//                "name": "Mark Davis",
//                "title": "Business English Coach",
//                "bio": "Corporate trainer with 15 years experience in business communication.",
//                "photoURL": "",
//                "rating": 4.7,
//                "totalStudents": 7800
//            ],
//            [
//                "id": "tutor_aisha",
//                "name": "Aisha Khan",
//                "title": "IELTS & Academic English",
//                "bio": "Certified IELTS examiner with a 98% Band 7+ success rate.",
//                "photoURL": "",
//                "rating": 4.9,
//                "totalStudents": 15200
//            ]
//        ]
//        
//        for tutor in tutors {
//            try await db.collection("tutors")
//                .document(tutor["id"] as! String)
//                .setData(tutor)
//        }
//        print("✅ Tutors seeded")
//    }
//    static func seedCourses() async throws {
//        let courses: [[String: Any]] = [
//            [
//                "id": "course_spoken_english",
//                "title": "Spoken English Mastery",
//                "tutorId": "tutor_daniel",
//                "tutorName": "Daniel Reed",
//                "description": "Master everyday spoken English with fluency drills, pronunciation coaching, and real conversations.",
//                "category": "Spoken",
//                "price": 49.00,
//                "thumbnailColor": "blue",
//                "totalDurationMinutes": 500,
//                "enrollmentCount": 12400,
//                "isFeatured": true,
//                "isBestseller": true,
//                "lessonIds": [
//                    "lesson_se_1", "lesson_se_2", "lesson_se_3",
//                    "lesson_se_4", "lesson_se_5", "lesson_se_6"
//                ],
//                "tags": ["spoken", "fluency", "pronunciation", "conversation", "english"],
//                "searchTokens": ["spoken", "english", "mastery", "fluency", "pronunciation"]
//            ],
//            [
//                "id": "course_grammar_pro",
//                "title": "English Grammar Pro",
//                "tutorId": "tutor_sara",
//                "tutorName": "Sara Lin",
//                "description": "Complete grammar course covering all tenses, articles, and advanced structures.",
//                "category": "Grammar",
//                "price": 39.00,
//                "thumbnailColor": "green",
//                "totalDurationMinutes": 365,
//                "enrollmentCount": 9100,
//                "isFeatured": true,
//                "isBestseller": false,
//                "lessonIds": [
//                    "lesson_gp_1", "lesson_gp_2", "lesson_gp_3",
//                    "lesson_gp_4", "lesson_gp_5"
//                ],
//                "tags": ["grammar", "tenses", "articles", "english", "writing"],
//                "searchTokens": ["english", "grammar", "pro", "tenses", "articles"]
//            ],
//            [
//                "id": "course_business_english",
//                "title": "Business English",
//                "tutorId": "tutor_mark",
//                "tutorName": "Mark Davis",
//                "description": "Professional English for meetings, emails, presentations, and negotiations.",
//                "category": "Business",
//                "price": 59.00,
//                "thumbnailColor": "amber",
//                "totalDurationMinutes": 340,
//                "enrollmentCount": 7800,
//                "isFeatured": false,
//                "isBestseller": false,
//                "lessonIds": [
//                    "lesson_be_1", "lesson_be_2", "lesson_be_3",
//                    "lesson_be_4"
//                ],
//                "tags": ["business", "english", "professional", "emails", "meetings"],
//                "searchTokens": ["business", "english", "professional", "meetings", "emails"]
//            ],
//            [
//                "id": "course_ielts",
//                "title": "IELTS Band 8 Prep",
//                "tutorId": "tutor_aisha",
//                "tutorName": "Aisha Khan",
//                "description": "Full IELTS preparation: Reading, Writing, Listening, and Speaking. Aimed at Band 7.5-8.",
//                "category": "IELTS",
//                "price": 69.00,
//                "thumbnailColor": "purple",
//                "totalDurationMinutes": 615,
//                "enrollmentCount": 15200,
//                "isFeatured": true,
//                "isBestseller": false,
//                "lessonIds": [
//                    "lesson_ielts_1", "lesson_ielts_2", "lesson_ielts_3",
//                    "lesson_ielts_4", "lesson_ielts_5"
//                ],
//                "tags": ["ielts", "exam", "band8", "academic", "english"],
//                "searchTokens": ["ielts", "band", "prep", "exam", "academic", "english"]
//            ]
//        ]
//        
//        for course in courses {
//            try await db.collection("courses")
//                .document(course["id"] as! String)
//                .setData(course)
//        }
//        print("✅ Courses seeded")
//    }
//    
//    static func seedGrammarTopics() async throws {
//        let topics: [[String: Any]] = [
//            ["id": "topic_tenses",         "title": "Tenses",          "lessonCount": 4, "quizId": "quiz_tenses",    "searchTokens": ["tenses", "grammar", "past", "present", "future"]],
//            ["id": "topic_articles",       "title": "Articles",        "lessonCount": 5, "quizId": "quiz_articles",  "searchTokens": ["articles", "grammar", "a", "an", "the"]],
//            ["id": "topic_prepositions",   "title": "Prepositions",    "lessonCount": 6, "quizId": "quiz_prepositions", "searchTokens": ["prepositions", "grammar", "in", "on", "at"]],
//            ["id": "topic_passive",        "title": "Passive Voice",   "lessonCount": 7, "quizId": "quiz_passive",   "searchTokens": ["passive", "voice", "grammar"]],
//            ["id": "topic_active",         "title": "Active Voice",    "lessonCount": 4, "quizId": "quiz_active",    "searchTokens": ["active", "voice", "grammar"]],
//            ["id": "topic_modals",         "title": "Modals",          "lessonCount": 5, "quizId": "quiz_modals",    "searchTokens": ["modals", "grammar", "can", "must", "should"]],
//            ["id": "topic_conditionals",   "title": "Conditionals",    "lessonCount": 6, "quizId": "quiz_conditionals", "searchTokens": ["conditionals", "grammar", "if", "would"]],
//            ["id": "topic_reported",       "title": "Reported Speech", "lessonCount": 7, "quizId": "quiz_reported",  "searchTokens": ["reported", "speech", "grammar"]],
//            ["id": "topic_punctuation",    "title": "Punctuation",     "lessonCount": 4, "quizId": "quiz_punctuation", "searchTokens": ["punctuation", "grammar", "comma", "period"]]
//        ]
//        
//        for topic in topics {
//            try await db.collection("grammarTopics")
//                .document(topic["id"] as! String)
//                .setData(topic)
//        }
//        print("✅ Grammar topics seeded")
//    }
//    
//    static func seedQuizzes() async throws {
//        let quizzes: [[String: Any]] = [
//            [
//                "id": "quiz_tenses",
//                "title": "Tenses Quiz",
//                "topicId": "topic_tenses",
//                "category": "grammar",
//                "questions": [
//                    [
//                        "id": "q1",
//                        "type": "multiple_choice",
//                        "prompt": "Choose the correct form: \"She ______ learning English for two years.\"",
//                        "options": ["have been", "has been", "had being", "have being"],
//                        "correctIndex": 1,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q2",
//                        "type": "multiple_choice",
//                        "prompt": "Which sentence uses the past perfect correctly?",
//                        "options": [
//                            "She had finished her homework before dinner.",
//                            "She has finished her homework before dinner.",
//                            "She finished her homework before had dinner.",
//                            "She was finished her homework before dinner."
//                        ],
//                        "correctIndex": 0,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q3",
//                        "type": "multiple_choice",
//                        "prompt": "\"I ______ to the gym every morning.\" Choose the correct tense.",
//                        "options": ["am going", "go", "goes", "have gone"],
//                        "correctIndex": 1,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q4",
//                        "type": "multiple_choice",
//                        "prompt": "Complete: \"By 2030, scientists ______ a cure.\"",
//                        "options": ["will discover", "will have discovered", "are discovering", "discovered"],
//                        "correctIndex": 1,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q5",
//                        "type": "multiple_choice",
//                        "prompt": "Which is the correct present continuous?",
//                        "options": ["She is study English.", "She studying English.", "She is studying English.", "She are studying English."],
//                        "correctIndex": 2,
//                        "xpReward": 20
//                    ]
//                ]
//            ],
//            [
//                "id": "quiz_articles",
//                "title": "Articles Quiz",
//                "topicId": "topic_articles",
//                "category": "grammar",
//                "questions": [
//                    [
//                        "id": "q1",
//                        "type": "multiple_choice",
//                        "prompt": "Choose the correct article: \"I saw ______ elephant at the zoo.\"",
//                        "options": ["a", "an", "the", "no article"],
//                        "correctIndex": 1,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q2",
//                        "type": "multiple_choice",
//                        "prompt": "\"______ sun rises in the east.\" Choose the correct article.",
//                        "options": ["A", "An", "The", "No article"],
//                        "correctIndex": 2,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q3",
//                        "type": "multiple_choice",
//                        "prompt": "\"She is ______ doctor.\" Which is correct?",
//                        "options": ["a", "an", "the", "no article"],
//                        "correctIndex": 0,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q4",
//                        "type": "multiple_choice",
//                        "prompt": "\"I love ______ music.\" Which is correct?",
//                        "options": ["a", "an", "the", "no article"],
//                        "correctIndex": 3,
//                        "xpReward": 20
//                    ],
//                    [
//                        "id": "q5",
//                        "type": "multiple_choice",
//                        "prompt": "\"He is ______ best student in class.\"",
//                        "options": ["a", "an", "the", "no article"],
//                        "correctIndex": 2,
//                        "xpReward": 20
//                    ]
//                ]
//            ]
//        ]
//        
//        for quiz in quizzes {
//            try await db.collection("quizzes")
//                .document(quiz["id"] as! String)
//                .setData(quiz)
//        }
//        print("✅ Quizzes seeded")
//    }
//    static func seedVocabulary() async throws {
//            let words: [[String: Any]] = [
//                [
//                    "id": "word_eloquent",
//                    "word": "Eloquent",
//                    "phonetic": "/ˈeləkwənt/",
//                    "partOfSpeech": "adjective",
//                    "definition": "Fluent or persuasive in speaking or writing.",
//                    "exampleSentence": "She gave an eloquent speech that moved the entire room.",
//                    "synonyms": ["fluent", "articulate"],
//                    "antonyms": ["inarticulate", "halting"],
//                    "isWordOfDay": true,
//                    "wordOfDayDate": "2026-06-19",
//                    "searchTokens": ["eloquent", "fluent", "articulate", "speaking"]
//                ],
//                [
//                    "id": "word_resilient",
//                    "word": "Resilient",
//                    "phonetic": "/rɪˈzɪliənt/",
//                    "partOfSpeech": "adjective",
//                    "definition": "Able to recover quickly from difficult conditions.",
//                    "exampleSentence": "She was resilient despite all the challenges she faced.",
//                    "synonyms": ["tough", "adaptable"],
//                    "antonyms": ["fragile", "weak"],
//                    "isWordOfDay": false,
//                    "wordOfDayDate": nil,
//                    "searchTokens": ["resilient", "tough", "adaptable", "recover"]
//                ],
//                [
//                    "id": "word_candid",
//                    "word": "Candid",
//                    "phonetic": "/ˈkændɪd/",
//                    "partOfSpeech": "adjective",
//                    "definition": "Truthful and straightforward; frank.",
//                    "exampleSentence": "He gave a candid assessment of the situation.",
//                    "synonyms": ["frank", "honest"],
//                    "antonyms": ["dishonest", "evasive"],
//                    "isWordOfDay": false,
//                    "wordOfDayDate": nil,
//                    "searchTokens": ["candid", "frank", "honest", "truthful"]
//                ],
//                [
//                    "id": "word_meticulous",
//                    "word": "Meticulous",
//                    "phonetic": "/məˈtɪkjʊləs/",
//                    "partOfSpeech": "adjective",
//                    "definition": "Showing great attention to detail; very careful and precise.",
//                    "exampleSentence": "The engineer was meticulous in checking every component.",
//                    "synonyms": ["thorough", "precise"],
//                    "antonyms": ["careless", "sloppy"],
//                    "isWordOfDay": false,
//                    "wordOfDayDate": nil,
//                    "searchTokens": ["meticulous", "thorough", "precise", "careful"]
//                ],
//                [
//                    "id": "word_persevere",
//                    "word": "Persevere",
//                    "phonetic": "/ˌpɜːsɪˈvɪə/",
//                    "partOfSpeech": "verb",
//                    "definition": "Continue in a course of action despite difficulty.",
//                    "exampleSentence": "You must persevere even when learning feels hard.",
//                    "synonyms": ["persist", "endure"],
//                    "antonyms": ["give up", "quit"],
//                    "isWordOfDay": false,
//                    "wordOfDayDate": nil,
//                    "searchTokens": ["persevere", "persist", "endure", "continue"]
//                ]
//            ]
//            
//            for word in words {
//                var data = word
//                if data["wordOfDayDate"] == nil { data.removeValue(forKey: "wordOfDayDate") }
//                try await db.collection("vocabulary")
//                    .document(word["id"] as! String)
//                    .setData(data)
//            }
//            print("✅ Vocabulary seeded")
//        }
//        
//        static func seedDailyChallenges() async throws {
//            let challenges: [[String: Any]] = [
//                [
//                    "date": "2026-06-19",
//                    "title": "Learn 5 new words",
//                    "description": "Study 5 vocabulary flashcards from today's set.",
//                    "xpReward": 50,
//                    "type": "vocabulary"
//                ],
//                [
//                    "date": "2026-06-20",
//                    "title": "Complete a grammar quiz",
//                    "description": "Take any grammar topic quiz and score above 60%.",
//                    "xpReward": 75,
//                    "type": "quiz"
//                ],
//                [
//                    "date": "2026-06-21",
//                    "title": "Watch a full lesson",
//                    "description": "Complete any video lesson and mark it as done.",
//                    "xpReward": 100,
//                    "type": "lesson"
//                ]
//            ]
//            
//            for challenge in challenges {
//                try await db.collection("dailyChallenges")
//                    .document(challenge["date"] as! String)
//                    .setData(challenge)
//            }
//            print("✅ Daily challenges seeded")
//        }
//    } // end FirestoreSeeder
