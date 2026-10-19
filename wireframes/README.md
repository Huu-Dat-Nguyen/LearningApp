# Wireframes (Milestone 1)

Clickable wireframes for the Android app, styled with the app theme (see "Theme" below). No mascot.

## Theme
Reference: [GEL – Guided E-learning (Dribbble)](https://dribbble.com/shots/25344541-GEL-Guided-E-learning), without the mascot.
- Background `#efeef3` (soft lavender-grey), cards `#fafafc`, 18px corners, very light shadows.
- Text: headings deep purple `#2e1760`, body `#3d3557`, muted `#9a95ab`.
- Accent: violet gradient `#a24cf0 → #c98bff` on main buttons (pill shape, uppercase, soft glow), progress bars and the mic button. Secondary accents: pink `#f0a0d8`, teal `#4cc3d9` for progress rings.
- Font: Plus Jakarta Sans (500–800); bold uppercase for hero titles.
- Home: greeting, two stat cards with progress rings, week strip with the current day in a pill, "Ready to learn?" hero with the main button.
- The grey-box version is replaced; the theme tokens are the CSS variables at the top of `index.html`.

## How to open
Open `index.html` in a browser (double-click), or run `python -m http.server 8765 --directory wireframes` and go to http://localhost:8765. Use the "Jump to screen" menu above the phone to go to any screen.

## Main flows
- **First-time setup:** Welcome → Languages → Placement quiz → Daily goal → Home
- **Daily lesson:** Home → Multiple choice → Fill in the blank → Word order → Listening → Translation → Lesson done → Home
- **Review:** Home → Flashcards (show answer, rate Again/Hard/Good/Easy)
- **Speaking:** Speak → AI chat (optional, on-device)
- **Settings:** Profile → Progress → Settings

## Screens
| id | Screen | Shows | Taps |
|---|---|---|---|
| welcome | Welcome | Logo, app name, tagline | Get started → languages; I have an account → home |
| languages | Languages | Main language + language to learn (5 languages) | Continue → placement |
| placement | Placement quiz | Progress bar, one question, 4 options | Skip / Next → goal |
| goal | Daily goal | 5/10/20 min, reminder time | Start learning → home |
| home | Home | Streak, XP, today's lesson, cards due, speak card | Lesson → exercise-mc; Cards → review; Speak → speak |
| learn | Lessons | Units by topic with level, locked/done states | Unlocked lesson → exercise-mc |
| exercise-mc | Multiple choice | Question, 4 options, feedback panel | Check → feedback → Continue → exercise-fill |
| exercise-fill | Fill in the blank | Sentence with blank | → exercise-order |
| exercise-order | Word order | Word chips to arrange | → exercise-listen |
| exercise-listen | Listening | Play button, type what you hear | → exercise-translate |
| exercise-translate | Translation | Sentence to translate | → lesson-done |
| lesson-done | Lesson done | XP earned, accuracy | Continue → home |
| review | Flashcards | Card front/back, counter, 4 grade buttons | Show answer → grade → next card |
| speak | Speaking | Target sentence, mic, what you said, score | AI conversation → ai-chat |
| ai-chat | AI chat | Role-play bubbles with a grammar correction, on-device note | Back |
| progress | Progress | Words learned, accuracy, streak, 7-day chart | Settings → settings |
| settings | Settings | Languages, goal, reminder, AI model download, account | Back → progress |

Bottom nav (Home, Learn, Review, Speak, Profile) is on the five main screens; Profile opens Progress.

## Known simplifications
- Exercise feedback always shows "Correct!" (no real answer checking).
- The same language can be picked as both main and target.
- Locked lessons do nothing when tapped.
