package com.evolvingtheconversation.app.data

data class LessonContent(
    val id: String,
    val number: Int,
    val title: String,
    val duration: String,
    val videoAsset: String   // path inside assets/, e.g. "videos/spoken_english_intro_1.mp4"
)

data class CourseContent(
    val id: String,
    val title: String,
    val instructor: String,
    val category: String,
    val duration: String,
    val students: String,
    val rating: String,
    val price: String,
    val description: String,
    val lessons: List<LessonContent>
)

object CourseCatalog {

    val courses = listOf(
        CourseContent(
            id = "spoken_english_mastery",
            title = "Spoken English Mastery",
            instructor = "Daniel Reed",
            category = "SPOKEN ENGLISH",
            duration = "8h 20m",
            students = "12.4k",
            rating = "4.9 (2.1k)",
            price = "$149.00",
            description = "Build natural, confident speaking habits through guided practice, real conversations, and instant feedback on pronunciation.",
            lessons = listOf(
                LessonContent("spoken_english_intro_1",      1, "Introduction & goals",       "8 min",  "videos/spoken_english_intro_1.mp4"),
                LessonContent("spoken_english_everyday_2",   2, "Everyday conversations",      "22 min", "videos/spoken_english_intro_2.mp4"),
                LessonContent("spoken_english_fluency_3",    3, "Fluency & pronunciation",      "18 min", "videos/spoken_english_intro_1.mp4"),
                LessonContent("spoken_english_pressure_4",   4, "Speaking under pressure",      "25 min", "videos/spoken_english_intro_2.mp4"),
                LessonContent("spoken_english_review_5",     5, "Final practice & review",      "30 min", "videos/spoken_english_intro_1.mp4")
            )
        ),
        CourseContent(
            id = "english_grammar_pro",
            title = "English Grammar Pro",
            instructor = "Sara Lin",
            category = "GRAMMAR",
            duration = "6h 05m",
            students = "9.1k",
            rating = "4.7 (1.3k)",
            price = "$99.00",
            description = "Master English grammar fundamentals with structured drills and real-world examples.",
            lessons = listOf(
                LessonContent("grammar_pro_tenses_1",   1, "Tenses foundations",   "10 min", "videos/spoken_english_intro_1.mp4"),
                LessonContent("grammar_pro_sentence_2", 2, "Sentence structure",   "14 min", "videos/spoken_english_intro_2.mp4"),
                LessonContent("grammar_pro_common_3",   3, "Common mistakes",      "12 min", "videos/spoken_english_intro_1.mp4")
            )
        ),
        CourseContent(
            id = "business_english",
            title = "Business English",
            instructor = "Mark Davis",
            category = "BUSINESS",
            duration = "5h 40m",
            students = "7.8k",
            rating = "4.6 (900)",
            price = "$129.00",
            description = "Professional communication for meetings, emails, and negotiations.",
            lessons = listOf(
                LessonContent("business_english_emails_1",   1, "Professional emails", "15 min", "videos/spoken_english_intro_1.mp4"),
                LessonContent("business_english_meetings_2", 2, "Meeting etiquette",   "18 min", "videos/spoken_english_intro_2.mp4")
            )
        ),
        CourseContent(
            id = "ielts_band8_prep",   // ← was "ielts_band_8_prep" — fixed to match iOS
            title = "IELTS Band 8 Prep",
            instructor = "Aisha Khan",
            category = "IELTS",
            duration = "10h 15m",
            students = "15.2k",
            rating = "4.9 (2.5k)",
            price = "$179.00",
            description = "Targeted strategies for Reading, Writing, Listening, and Speaking to hit Band 8.",
            lessons = listOf(
                LessonContent("ielts_band8_overview_1", 1, "Exam overview",           "12 min", "videos/spoken_english_intro_1.mp4"),
                LessonContent("ielts_band8_writing_2",  2, "Writing task strategies", "20 min", "videos/spoken_english_intro_2.mp4")
            )
        )
    )

    fun getCourse(id: String): CourseContent? = courses.find { it.id == id }
}