package com.learningapp.data.repo

/** What the lesson player needs from storage. [LessonRepository] implements it; tests use a fake. */
interface LessonStore {
    suspend fun lesson(id: String): LessonData?
    suspend fun completeLesson(result: LessonResult, lessonWordIds: List<String>)
}
