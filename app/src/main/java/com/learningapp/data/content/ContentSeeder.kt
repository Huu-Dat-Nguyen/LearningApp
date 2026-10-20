package com.learningapp.data.content

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.learningapp.data.db.LessonDao
import com.learningapp.data.db.WordDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads bundled word lists and lessons into Room. Each file re-imports when its "version" goes up;
 * user progress (user_words, lesson_progress, daily_activity) is never touched.
 */
@Singleton
class ContentSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: WordDao,
    private val lessonDao: LessonDao,
    private val prefs: DataStore<Preferences>,
) {
    suspend fun seed(lang: String) = withContext(Dispatchers.IO) {
        seedWords(lang)
        seedLessons(lang)
    }

    private fun asset(path: String): String? = runCatching {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrNull()

    private suspend fun seedWords(lang: String) {
        val text = asset("content/$lang/words.json") ?: return
        val file = ContentParser.parse(text)
        val key = intPreferencesKey("content_version_$lang")
        val imported = prefs.data.first()[key] ?: 0
        if (file.version <= imported && dao.wordCount(lang) > 0) return
        val (words, translations) = ContentParser.toEntities(file)
        dao.upsertContent(words, translations)
        prefs.edit { it[key] = file.version }
    }

    private suspend fun seedLessons(lang: String) {
        val text = asset("content/$lang/lessons.json") ?: return
        val file = LessonParser.parse(text)
        val key = intPreferencesKey("lessons_version_$lang")
        val imported = prefs.data.first()[key] ?: 0
        if (file.version <= imported && lessonDao.lessonCount(lang) > 0) return
        lessonDao.upsertLessons(LessonParser.toEntities(file))
        prefs.edit { it[key] = file.version }
    }
}
