import SwiftUI

enum CourseCatalog {
    static let all: [Course] = [
        Course(
            id: "spoken_english_mastery", title: "Spoken English Mastery", author: "Daniel Reed",
            duration: "8h 20m", students: "12.4k", category: "Spoken", icon: "book.fill",
            accentColorHex: "#4B5BF5", accentColorDarkHex: "#2B5BE8",
            lessons: [
                CourseLesson(id: "spoken_english_intro_1", order: 0, title: "Introduction & goals", duration: "8 min", videoURL: "spoken_english_intro_1"),
                CourseLesson(id: "spoken_english_everyday_2", order: 1, title: "Everyday conversations", duration: "22 min", videoURL:  "spoken_english_intro_2"),

                CourseLesson(id: "spoken_english_fluency_3", order: 2, title: "Fluency & pronunciation", duration: "18 min", videoURL:  "spoken_english_intro_1"),

                CourseLesson(id: "spoken_english_pressure_4", order: 3, title: "Speaking under pressure", duration: "25 min", videoURL:  "spoken_english_intro_2"),

                CourseLesson(id: "spoken_english_review_5", order: 4, title: "Final practice & review", duration: "30 min", videoURL:  "spoken_english_intro_1"),

            ]
        ),
        Course(
            id: "english_grammar_pro", title: "English Grammar Pro", author: "Sara Lin",
            duration: "6h 05m", students: "9.1k", category: "Grammar", icon: "doc.text.fill",
            accentColorHex: "#2DBD7E", accentColorDarkHex: "#1A9460",
            lessons: [
                CourseLesson(id: "grammar_pro_tenses_1", order: 0, title: "Tenses foundations", duration: "10 min", videoURL:  "spoken_english_intro_1"),

                CourseLesson(id: "grammar_pro_sentence_2", order: 1, title: "Sentence structure", duration: "14 min", videoURL:  "spoken_english_intro_2"),

                CourseLesson(id: "grammar_pro_common_3", order: 2, title: "Common mistakes", duration: "12 min", videoURL:  "spoken_english_intro_1"),

            ]
        ),
        Course(
            id: "business_english", title: "Business English", author: "Mark Davis",
            duration: "5h 40m", students: "7.8k", category: "Business", icon: "briefcase.fill",
            accentColorHex: "#F5A623", accentColorDarkHex: "#E08A0F",
            lessons: [
                CourseLesson(id: "business_english_emails_1", order: 0, title: "Professional emails", duration: "15 min", videoURL:  "spoken_english_intro_1"),

                CourseLesson(id: "business_english_meetings_2", order: 1, title: "Meeting etiquette", duration: "18 min", videoURL:  "spoken_english_intro_2"),

            ]
        ),
        Course(
            id: "ielts_band8_prep", title: "IELTS Band 8 Prep", author: "Aisha Khan",
            duration: "10h 15m", students: "15.2k", category: "Beginner", icon: "target",
            accentColorHex: "#9B6FE8", accentColorDarkHex: "#7C3AED",
            lessons: [
                CourseLesson(id: "ielts_band8_overview_1", order: 0, title: "Exam overview", duration: "12 min", videoURL:  "spoken_english_intro_1"),

                CourseLesson(id: "ielts_band8_writing_2", order: 1, title: "Writing task strategies", duration: "20 min", videoURL:  "spoken_english_intro_2"),

            ]
        ),
    ]
}
