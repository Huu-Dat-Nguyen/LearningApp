package com.learningapp.data.content

import com.learningapp.data.db.LessonEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shape of assets/content/<lang>/lessons.json. Text is keyed by main (native) language code. */
@Serializable
data class LessonFile(val lang: String, val version: Int, val units: List<ContentUnit>)

@Serializable
data class ContentUnit(
    val id: String,
    val topic: String,
    val level: String,
    val title: Map<String, String>,
    val lessons: List<ContentLesson>,
)

@Serializable
data class ContentLesson(
    val id: String,
    val title: Map<String, String>,
    val words: List<String>,
    val exercises: List<ContentExercise>,
)

/** One exercise as written in the content file. */
@Serializable
sealed interface ContentExercise {
    @Serializable @SerialName("mc")
    data class Mc(val wordId: String) : ContentExercise

    @Serializable @SerialName("fill")
    data class Fill(val sentence: String, val answer: String, val hint: Map<String, String>) : ContentExercise

    @Serializable @SerialName("order")
    data class Order(val answer: String, val translation: Map<String, String>) : ContentExercise

    @Serializable @SerialName("listen")
    data class Listen(val text: String, val translation: Map<String, String>) : ContentExercise

    @Serializable @SerialName("translate")
    data class Translate(val source: Map<String, String>, val answers: List<String>) : ContentExercise
}

object LessonParser {
    val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    fun parse(text: String): LessonFile = json.decodeFromString(text)

    fun toEntities(file: LessonFile): List<LessonEntity> {
        var order = 0
        return file.units.flatMap { unit ->
            unit.lessons.map { lesson ->
                LessonEntity(
                    id = lesson.id,
                    lang = file.lang,
                    unitId = unit.id,
                    unitTitleJson = json.encodeToString(unit.title),
                    topic = unit.topic,
                    level = unit.level,
                    titleJson = json.encodeToString(lesson.title),
                    sortOrder = order++,
                    wordIdsJson = json.encodeToString(lesson.words),
                    exercisesJson = json.encodeToString(lesson.exercises),
                )
            }
        }
    }

    fun exercises(entity: LessonEntity): List<ContentExercise> = json.decodeFromString(entity.exercisesJson)
    fun wordIds(entity: LessonEntity): List<String> = json.decodeFromString(entity.wordIdsJson)
    fun text(textJson: String, mainLang: String): String {
        val map: Map<String, String> = json.decodeFromString(textJson)
        return map[mainLang] ?: map["en"] ?: map.values.firstOrNull().orEmpty()
    }
}
