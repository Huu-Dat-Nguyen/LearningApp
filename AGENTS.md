# LearningApp — Foreign Language Learning App

## Goal
An app that helps users learn and improve foreign languages through short daily practice: vocabulary, grammar, listening, speaking and reading.

## Target users
- Beginners starting a new language.
- Intermediate learners who want to keep and improve their level.
- Learners preparing for exams or travel.

## Core features (MVP)
1. **Onboarding** — pick native language, target language, level (placement quiz), daily goal (5/10/20 min).
2. **Vocabulary flashcards** — spaced repetition (SM-2 style), audio, example sentences, images.
3. **Lessons** — short units by topic (greetings, food, travel, work), each with a mini quiz.
4. **Exercises** — multiple choice, fill-in-the-blank, word ordering, listening (type what you hear), translation.
5. **Speaking practice** — record voice, speech-to-text, pronunciation score.
6. **Progress tracking** — streaks, XP, words learned, accuracy, weekly review.
7. **Reminders** — daily notification at a chosen time.

## Later features
- AI conversation partner (chat/voice role-play: ordering food, job interview).
- AI grammar correction with explanations.
- Reading mode: graded short stories with tap-to-translate.
- Custom word lists (import from text or web pages).
- Offline mode.
- Leaderboards and friends.

## Languages
- The user picks their **main (native) language** and the **language to learn** from the supported list.
- Any pair works (e.g. Vietnamese → Japanese, Spanish → English); the main language is used for UI text, translations and explanations.
- The user can change either language later in Settings; progress is kept per target language.
- Supported at start: English, Spanish, French, Japanese, Vietnamese. Add more via content files, not code changes.

## Pricing
Free. No ads, no paid tiers, no in-app purchases.

## Platform
Android first (min SDK 26 / Android 8.0). iOS and web later.

## Tech stack
- **App:** Kotlin + Jetpack Compose, MVVM, Hilt for DI, Navigation Compose.
- **Local data:** Room (offline words, lessons, review queue) + DataStore for settings.
- **Backend:** Firebase (Auth, Firestore sync, Cloud Messaging for reminders); custom API later if needed.
- **Auth:** email + Google sign-in.
- **Speech:** Android `SpeechRecognizer` and `TextToSpeech`.
- **Reminders:** WorkManager + notifications.
- **Build/release:** Gradle (Kotlin DSL), Android Studio, Google Play internal testing track.
- **AI features:** on-device only (free, offline, no API keys) — see "On-device AI" below.

## On-device AI (try; drop to out of scope if too hard)
Used for the AI conversation partner and grammar correction. No cloud AI, so no running cost.
- **Option A — Gemini Nano (built in):** via Google ML Kit GenAI / AICore. No download, but only on newer flagship phones (e.g. recent Pixel and Galaxy models).
- **Option B — Gemma (bundled/downloaded):** small Gemma model run with Google's on-device LLM runtime (MediaPipe LLM Inference / LiteRT). Works on more phones, but the model is a one-time download of roughly 0.5–3 GB and needs about 4 GB+ RAM.
- **Plan:** use Gemini Nano when the phone has it, else offer an optional Gemma download, else hide AI features. The app must work fully without AI.
- **Feasibility spike (milestone 5):** build a small prototype on one test phone and check speed (a reply in under ~5 s), answer quality in the 5 launch languages, app size and battery use.
- **If the spike fails** (too slow, poor quality in target languages, or too complex): move AI features to **out of scope** and ship without them.
- Check current library names and versions before coding; these APIs change quickly.

## Visual style
Based on [GEL – Guided E-learning](https://dribbble.com/shots/25344541-GEL-Guided-E-learning), no mascot: soft lavender-grey background, white rounded cards, deep purple headings, violet gradient pill buttons, progress rings, Plus Jakarta Sans. Color tokens are in `wireframes/README.md` (Theme); reuse them for the Compose theme.

## Data model (draft)
- `User` — id, native_lang, target_lang, level, daily_goal, streak, xp
- `Lesson` — id, lang, level, topic, order
- `Exercise` — id, lesson_id, type, prompt, answer, audio_url
- `Word` — id, lang, text, translation, example, audio_url
- `UserWord` — user_id, word_id, ease, interval, due_date
- `Attempt` — user_id, exercise_id, correct, time_ms, created_at

## Project structure
```
app/src/main/assets/content/<lang>/  # words.json, lessons.json (each with its own "version")
app/src/main/java/com/learningapp/
  data/content  # JSON parsing + ContentSeeder (imports on app start)
  data/db       # Room v3: words, translations, user_words, review_log, lessons, lesson_progress, daily_activity, speak_practice
  data/speech   # AndroidSpeechInput (SpeechRecognizer)
  data/repo     # WordRepository, LessonRepository, ProgressRepository, UserPrefs (+ ReviewRepository/LessonStore interfaces for tests)
  domain/srs    # SM-2 scheduler
  domain/lesson # exercise types, AnswerChecker, ExerciseBuilder, LessonSession
  domain/speech # SpeechInput interface, PronunciationScorer
  ui/           # home, learn, lesson, review, speak, profile (Progress), components, nav, theme
app/schemas/    # exported Room schemas (needed for migration tests)
wireframes/     # clickable HTML wireframes (milestone 1)
```
Reports and plans (`report/`) live in the OneDrive folder.

## Milestones
1. Spec + wireframes — done 2026-10-19 (`wireframes/index.html`, see `wireframes/README.md`).
2. Flashcards with spaced repetition — **done 2026-10-20** (plan: `report/milestone-2-plan.md`). 50 Spanish A1 words; SM-2 scheduling; review screen with flip, text-to-speech and grade buttons showing the next interval; due cards first, then up to 10 new a day; Again re-queues in the session. Known: a brand-new card gets 1 day for every grade (standard SM-2).
3. Lessons + exercises + progress — **done 2026-10-20** (plan: `report/milestone-3-plan.md`). 10 lessons (5 units × 2), each with multiple choice, fill in the blank, word order, listen and translate; lessons unlock in order; wrong answers come back once at the end; a missing accent counts as right with a note; finishing a lesson adds its words to the flashcards. XP = 10 per lesson + 1 per first-try correct + 1 per flashcard; streak counts days with XP; study time counted while a lesson or review is open. Home and the Progress tab show real numbers. Checked: 53 unit + 8 device tests pass (incl. Room v1→v2 migration); on the emulator a lesson with one wrong answer and two missing accents gave +16 XP / 86%, and all progress survived an app restart.
4. Speaking practice — **done 2026-10-20** (plan: `report/milestone-4-plan.md`). Speak tab: sentences from started words and finished lessons (lesson 1 as fallback), playback normal/slow, phone speech recognition (`data/speech/AndroidSpeechInput`, prefers on-device, retries online when no offline pack), word-level score (`domain/speech/PronunciationScorer`: right / close / missed, 0–100), XP 2/1 once per sentence per day, mic permission with settings link, listen-only mode without recognition. Room v3 adds `speak_practice`. Checked: 72 unit + 9 device tests pass; on the emulator the permission prompt, listening state and "didn't catch that" path work. **Not checked:** real recognition and scoring with a voice (the emulator has no voice input) — try on a real phone.
5. On-device AI spike (conversation + correction); keep or move out of scope.
6. Beta release on Google Play (internal/closed testing).

## Team
Solo project (one developer). Keep scope small, prefer managed services (Firebase) over custom backend.

## Build and run
- Repo: `C:\Users\nguye\repos\LearningApp` (this folder). The OneDrive folder keeps reports only.
- JDK: Android Studio's bundled one. In bash: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- Build: `./gradlew :app:assembleDebug` · Unit tests: `./gradlew :app:testDebugUnitTest` · Device tests (emulator running): `./gradlew :app:connectedDebugAndroidTest` — this uninstalls the app afterwards, which wipes its data on the emulator.
- Emulator: AVD `Medium_Phone_API_37.0` (start it in a Terminal tab, not a session-bound task).
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (adb in `%LOCALAPPDATA%\Android\Sdk\platform-tools`).
- Stack: AGP 9.4 (built-in Kotlin), Compose BOM 2026.02, Hilt 2.60, Room 2.8, KSP 2.3, Navigation 2.10 (type-safe routes). Versions in `gradle/libs.versions.toml`.
- Font: system default for now; Plus Jakarta Sans still to add in `res/font`.

## Rules for agents
- Keep secrets in env vars (`.env`, never committed).
- Keep the git repo outside OneDrive (`C:\Users\nguye\repos\LearningApp`).
- Lesson content lives in `content/`, not hard-coded.
