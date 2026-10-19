package com.learningapp.data.content

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.learningapp.data.db.WordDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads bundled word lists into Room. Re-imports when the file's "version" goes up;
 * review progress (user_words) is never touched.
 */
@Singleton
class ContentSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: WordDao,
    private val prefs: DataStore<Preferences>,
) {
    suspend fun seed(lang: String) = withContext(Dispatchers.IO) {
        val text = context.assets.open("content/$lang/words.json").bufferedReader().use { it.readText() }
        val file = ContentParser.parse(text)
        val key = intPreferencesKey("content_version_$lang")
        val imported = prefs.data.first()[key] ?: 0
        if (file.version <= imported && dao.wordCount(lang) > 0) return@withContext
        val (words, translations) = ContentParser.toEntities(file)
        dao.upsertContent(words, translations)
        prefs.edit { it[key] = file.version }
    }
}
