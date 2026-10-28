package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.ui.graphics.Color

/**
 * Difficulty level used across Audio Lessons and Dictation exercises.
 * Each level carries its own badge color, per the design spec.
 */
enum class Level(val label: String, val badgeColor: Color) {
    BEGINNER("Beginner", Color(0xFF1E5EF3)),
    INTERMEDIATE("Intermediate", Color(0xFF0CBD85)),
    ADVANCED("Advanced", Color(0xFF7C3AED))
}

/** A single audio lesson inside the "Audio lessons" section. */
data class AudioLesson(
    val id: String,
    val title: String,
    val duration: String,
    val level: Level,
    val audioFile: String
)

/** One episode inside a podcast series. */
data class PodcastEpisode(
    val id: String,
    val title: String,
    val duration: String,
    val audioFile: String
)

/** A podcast series, shown as a card in the "Podcasts" section. */
data class PodcastSeries(
    val id: String,
    val title: String,
    val gradient: List<Color>,
    val episodes: List<PodcastEpisode>
)

/** A single dialogue (e.g. "Airport", "Restaurant") that plays immediately when tapped. */
data class DialogueItem(
    val id: String,
    val title: String,
    val duration: String,
    val audioFile: String
)

/** One sentence used in the dictation exercise. */
data class DictationExercise(
    val id: String,
    val level: Level,
    val sentence: String,
    val duration: String,
    val audioFile: String
)

// ─────────────────────────────────────────────────────────────────────────
// AUDIO LESSONS — 3 levels x 4 lessons = 12
// ─────────────────────────────────────────────────────────────────────────
val audioLessons = listOf(
    AudioLesson("al_b1", "Meeting Someone New", "2:25", Level.BEGINNER, "lesson_beginner1.wav"),
    AudioLesson("al_b2", "First Day at Work", "1:12", Level.BEGINNER, "lesson_beginner2.wav"),
    AudioLesson("al_b3", "Ordering Breakfast at a Café", "1:51", Level.BEGINNER, "lesson_beginner3.wav"),
    AudioLesson("al_b4", "Shopping at the Supermarket", "1:53", Level.BEGINNER, "lesson_beginner4.wav"),

    AudioLesson("al_i1", "Planning a Weekend Trip", "1:11", Level.INTERMEDIATE, "lesson_intermediate1.wav"),
    AudioLesson("al_i2", "Working on a University Project", "2:07", Level.INTERMEDIATE, "lesson_intermediate2.wav"),
    AudioLesson("al_i3", "Preparing for a Job Interview", "1:27", Level.INTERMEDIATE, "lesson_intermediate3.wav"),
    AudioLesson("al_i4", "Making Plans with Friends", "1:08", Level.INTERMEDIATE, "lesson_intermediate4.wav"),

    AudioLesson("al_a1", "The Future of Artificial Intelligence", "2:37", Level.ADVANCED, "lesson_advanced1.wav"),
    AudioLesson("al_a2", "Building a Sustainable City", "2:32", Level.ADVANCED, "lesson_advanced2.wav"),
    AudioLesson("al_a3", "Leading Through Change", "1:21", Level.ADVANCED, "lesson_advanced3.wav"),
    AudioLesson("al_a4", "A Journey of Lifelong Learning", "1:53", Level.ADVANCED, "lesson_advanced4.wav"),
)

// ─────────────────────────────────────────────────────────────────────────
// PODCASTS — 2 series x 5 episodes
// ─────────────────────────────────────────────────────────────────────────
val podcastSeries = listOf(
    PodcastSeries(
        id = "daily_english",
        title = "Daily English",
        gradient = listOf(Color(0xFF0CBD85), Color(0xFF059669)),
        episodes = listOf(
            PodcastEpisode("de_ep1", "Morning Routines", "6:12", "daily_english_ep1.wav"),
            PodcastEpisode("de_ep2", "At the Coffee Shop", "5:48", "daily_english_ep2.wav"),
            PodcastEpisode("de_ep3", "Weekend Plans", "6:30", "daily_english_ep3.wav"),
            PodcastEpisode("de_ep4", "Talking About Weather", "5:20", "daily_english_ep4.wav"),
            PodcastEpisode("de_ep5", "Everyday Errands", "6:05", "daily_english_ep5.wav"),
        )
    ),
    PodcastSeries(
        id = "real_talk",
        title = "Real Talk",
        gradient = listOf(Color(0xFFFFA800), Color(0xFFE67E00)),
        episodes = listOf(
            PodcastEpisode("rt_ep1", "Modern Friendships", "7:15", "real_talk_ep1.wav"),
            PodcastEpisode("rt_ep2", "Work-Life Balance", "7:40", "real_talk_ep2.wav"),
            PodcastEpisode("rt_ep3", "Social Media Habits", "6:55", "real_talk_ep3.wav"),
            PodcastEpisode("rt_ep4", "Money Talk", "7:20", "real_talk_ep4.wav"),
            PodcastEpisode("rt_ep5", "Personal Growth", "7:05", "real_talk_ep5.wav"),
        )
    ),
)

// ─────────────────────────────────────────────────────────────────────────
// DIALOGUES — 17 total. The first 7 match the audio files you originally
// specified. The remaining 10 are new categories added for the "Show more"
// grid — you'll need to add matching mp3 files under assets/audio/ for them
// (file names are listed below, following the same naming pattern).
// ─────────────────────────────────────────────────────────────────────────
val dialogues = listOf(
    // Original 7
    DialogueItem("dlg_airport", "Airport", "4:00", "airport.wav"),
    DialogueItem("dlg_restaurant", "Restaurant", "3:00", "restaurant.wav"),
    DialogueItem("dlg_hotel", "Hotel", "4:00", "hotel.wav"),
    DialogueItem("dlg_shopping", "Shopping", "3:00", "shopping.wav"),
    DialogueItem("dlg_hospital", "Hospital", "4:00", "hospital.wav"),
    DialogueItem("dlg_office", "Office", "5:00", "office.wav"),
    DialogueItem("dlg_interview", "Interview", "6:00", "interview.wav"),

    // New — add these 10 mp3 files under assets/audio/
    DialogueItem("dlg_bank", "Bank", "4:00", "bank.wav"),
    DialogueItem("dlg_pharmacy", "Pharmacy", "3:00", "pharmacy.wav"),
    DialogueItem("dlg_taxi", "Taxi", "3:00", "taxi.wav"),
    DialogueItem("dlg_gym", "Gym", "4:00", "gym.wav"),
    DialogueItem("dlg_school", "School", "5:00", "school.wav"),
    DialogueItem("dlg_post_office", "Post Office", "4:00", "post_office.wav"),
    DialogueItem("dlg_library", "Library", "4:00", "library.wav"),
    DialogueItem("dlg_salon", "Salon", "3:00", "salon.wav"),
    DialogueItem("dlg_bakery", "Bakery", "3:00", "bakery.wav"),
    DialogueItem("dlg_gas_station", "Gas Station", "3:00", "gas_station.wav"),
)

/** Number of dialogues shown before the user taps "Show more". */
const val DIALOGUES_COLLAPSED_COUNT = 7

// ─────────────────────────────────────────────────────────────────────────
// DICTATION — 3 levels x 4 sentences = 12
// ─────────────────────────────────────────────────────────────────────────
val dictationExercises = listOf(
    DictationExercise("dc_b1", Level.BEGINNER, "I would like a cup of coffee please.", "0:06", "dictation_beginner1.wav"),
    DictationExercise("dc_b2", Level.BEGINNER, "What time does the store open today.", "0:06", "dictation_beginner2.wav"),
    DictationExercise("dc_b3", Level.BEGINNER, "Can you help me find the bus station.", "0:07", "dictation_beginner3.wav"),
    DictationExercise("dc_b4", Level.BEGINNER, "She usually wakes up early in the morning.", "0:07", "dictation_beginner4.wav"),

    DictationExercise("dc_i1", Level.INTERMEDIATE, "I have been trying to reach you all week.", "0:08", "dictation_intermediate1.wav"),
    DictationExercise("dc_i2", Level.INTERMEDIATE, "The meeting was rescheduled to next Thursday.", "0:08", "dictation_intermediate2.wav"),
    DictationExercise("dc_i3", Level.INTERMEDIATE, "He apologized for arriving late to the interview.", "0:09", "dictation_intermediate3.wav"),
    DictationExercise("dc_i4", Level.INTERMEDIATE, "We should double check the numbers before sending.", "0:08", "dictation_intermediate4.wav"),

    DictationExercise("dc_a1", Level.ADVANCED, "The committee reached a consensus after lengthy deliberation.", "0:10", "dictation_advanced1.wav"),
    DictationExercise("dc_a2", Level.ADVANCED, "Economic uncertainty continues to influence investor confidence.", "0:10", "dictation_advanced2.wav"),
    DictationExercise("dc_a3", Level.ADVANCED, "Her argument was both persuasive and well documented.", "0:09", "dictation_advanced3.wav"),
    DictationExercise("dc_a4", Level.ADVANCED, "The policy sparked debate among lawmakers and analysts alike.", "0:10", "dictation_advanced4.wav"),
)