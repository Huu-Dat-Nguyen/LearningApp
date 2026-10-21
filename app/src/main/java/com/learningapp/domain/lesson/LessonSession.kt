package com.learningapp.domain.lesson

/**
 * Runs one lesson: shows exercises in order, and each one answered wrong
 * comes back once at the end. Pure state, no Android.
 */
class LessonSession(private val exercises: List<Exercise>, private val targetLang: String = "es") {

    private data class Item(val index: Int, val isRetry: Boolean)

    private val queue = ArrayDeque(exercises.indices.map { Item(it, false) })
    private var current: Item? = queue.removeFirstOrNull()
    private val firstTryCorrect = mutableSetOf<Int>()
    private val retried = mutableSetOf<Int>()

    /** Number of answers given (first tries and retries). */
    var answered = 0
        private set

    /** Answers judged correct (first tries and retries). */
    var correct = 0
        private set

    val exercise: Exercise? get() = current?.let { exercises[it.index] }
    val isRetry: Boolean get() = current?.isRetry == true
    val finished: Boolean get() = current == null

    /** Steps done out of the total, counting retries that were added. */
    val progress: Float
        get() {
            val total = exercises.size + retried.size
            val left = queue.size + if (current != null) 1 else 0
            return if (total == 0) 1f else (total - left).toFloat() / total
        }

    /** Checks [answer] for the current exercise. Call [next] after showing feedback. */
    fun submit(answer: Answer): CheckResult {
        val item = current ?: error("Lesson finished")
        val result = AnswerChecker.check(exercises[item.index], answer, targetLang)
        answered++
        if (result.isCorrect) {
            correct++
            if (!item.isRetry) firstTryCorrect += item.index
        } else if (!item.isRetry && retried.add(item.index)) {
            queue.addLast(Item(item.index, true))
        }
        return result
    }

    fun next() {
        current = queue.removeFirstOrNull()
    }

    /** Fraction of exercises right the first time. */
    val accuracy: Double get() = if (exercises.isEmpty()) 1.0 else firstTryCorrect.size.toDouble() / exercises.size

    /** 10 XP for finishing plus 1 per exercise right the first time. */
    val xp: Int get() = XP_FOR_LESSON + firstTryCorrect.size

    companion object {
        const val XP_FOR_LESSON = 10
    }
}
