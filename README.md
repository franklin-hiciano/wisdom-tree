# Wisdom Tree — Android App

your agency in an app. architectable reflection framework so you reflect how you want

for a quickstart, copy and paste this config. syntax for a 'wisdom tree' should be intuitive enough

```
How did it go? Did you meet your goal?
  Yes, absoutely >> Great. Why do you think that was?
  I got some of the way there >> Hmm. What happened?
  No >> Oh no, what happened?

Great. Why do you think that was?
  >> Now look at your goal. If you keep going at this pace, will you meet it on time?

Now look at your goal. If you keep going at this pace, will you meet it on time?
  Absolutely, yes >> Great, improve in other domains. What could you be working on that increases your leverage?
  No, not really >> Great, let's pick up the pace. What could you be working on that increases your leverage?

Great, improve in other domains. What could you be working on that increases your leverage?
  I burned a lot of time on low priority items today. I could be spending more time on what directly increases my KPI. >> Great, elaborate. What's your solution?
  I want to learn something new, or I was bored today. I should find inspiring online content by people at the level I want to be at. >> Great, elaborate. What's your solution?
  I don't think this project is worth it anymore. I want to pivot to working on something else. >> Great, elaborate. What's your solution?
  The KPI needs to be redefined for sure. I could have produced more results had the metric been more relevant to making progress towards the goal. >> Great, elaborate. What's your solution?
  >> Great, your job now is to keep this pace. Think back to today, and imagine everything that could have possibly been the reason you produced less results. How can you guarantee that you always work as well as you did today? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.

Great, your job now is to keep this pace. Think back to today, and imagine everything that could have possibly been the reason you produced less results. How can you guarantee that you always work as well as you did today? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.
  >> End of the tree. Reflect on what you wrote, sleep well, and see you tomorrow night.

Great, let's pick up the pace. What could you be working on that increases your leverage?
  @set slow = 1.0
  I was out of it today, basically going through the motions. I need to do some hard physical exercise so I can rebuild my grit and produce more results. >> Great, elaborate. What's your solution?
  I burned too much time on certain tasks today, and I lack a sense of conviction. I need to watch videos by people already living my dream, question if my KPI accurately indicates whether I'm producing results, and track my it more often while I work. >> Great, elaborate. What's your solution?
  I honestly don't know. I need to talk with someone above my level. >> Great, elaborate. What's your solution?
  The KPI needs to be redefined for sure. I could have produced more results had the metric been more relevant to making progress towards the goal. >> Great, elaborate. What's your solution?

Great, elaborate. What's your solution?
  @if slow == 1.0 >> Great. Even though you didn't achieve everything you wanted, make sure you don't regress tomorrow either. Think back to today, and imagine everything that could have possibly been the reason you produced less results. How can you guarantee that you always work just as well, if not better, than you did today? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.
  >> End of the tree. Reflect on what you wrote, sleep well, and see you tomorrow night.

Hmm. What happened?
  @set partial = 1.0
  >> Hmm. have you had any similar days recently? Elaborate.

Oh no, what happened?
  >> Hmm. have you had any similar days recently? Elaborate.

Hmm. have you had any similar days recently? Elaborate.
  >> Give yourself the best possible chance at permanent redemption. Two questions: (1) How can you guarantee you never have to deal with this issue again? (2) How can you guarantee you meet your KPI target tomorrow? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.

Give yourself the best possible chance at permanent redemption. Two questions: (1) How can you guarantee you never have to deal with this issue again? (2) How can you guarantee you meet your KPI target tomorrow? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.
  @if partial == 1.0 >> Great. Even though you didn't achieve everything you wanted, make sure you don't regress tomorrow either. Think back to today, and imagine everything that could have possibly been the reason you produced less results. How can you guarantee that you always work just as well, if not better, than you did today? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.
  >> End of the tree. Reflect on what you wrote, sleep well, and see you tomorrow night.

Great. Even though you didn't achieve everything you wanted, make sure you don't regress tomorrow either. Think back to today, and imagine everything that could have possibly been the reason you produced less results. How can you guarantee that you always work just as well, if not better, than you did today? Leave no doubts that you've thought of everything. Repeating mistakes will cost you progress in the future.
  >> End of the tree. Reflect on what you wrote, sleep well, and see you tomorrow night.

End of the tree. Reflect on what you wrote, sleep well, and see you tomorrow night.

```

# Rest of readme

A native Android port of `wisdom-tree-v3.html`, built with Kotlin + Jetpack Compose.

## Structure

```
app/src/main/java/com/wisdomtree/
├── MainActivity.kt              # App shell, navigation, topbar, overlays
├── parser/
│   └── WisdomTreeParser.kt      # Kotlin port of the DSL parser + syntax highlighter
├── data/
│   ├── model/Models.kt          # Room entities, DAOs, type converters
│   ├── db/WisdomTreeDatabase.kt # Room database
│   └── repository/              # Data access layer
├── viewmodel/
│   └── WisdomTreeViewModel.kt   # All state, business logic, run management
└── ui/
    ├── theme/Theme.kt            # Colors (exact HTML vars), typography
    └── screens/
        ├── editor/EditorScreen.kt   # Syntax-highlighted editor with line numbers
        ├── canvas/TreeCanvas.kt     # Pan/zoom Canvas graph renderer
        ├── test/TestScreen.kt       # Card-by-card test mode + save banner
        ├── runs/RunsScreen.kt       # Run history with filter (7d/30d/all)
        └── vars/VarsScreen.kt       # Variable inspector + node stats
```

## Setup

1. Open in **Android Studio Hedgehog** (or newer)
2. Let Gradle sync
3. Run on device/emulator (API 26+, Android 8+)

No API keys or external services needed — all data is local via Room.

## Updating the tree DSL / HTML

The parser lives entirely in `WisdomTreeParser.kt`. If you update the DSL syntax
in the HTML, mirror the changes in the `parse()` function there.

## Adding Android-native features

The ViewModel (`WisdomTreeViewModel.kt`) is the right place to add:
- **Notifications** — inject `NotificationManager`, schedule via `AlarmManager`
- **Widgets** — read from Room in a `GlanceAppWidget`
- **Shortcuts** — use `ShortcutManager` in `MainActivity`
- **Export/share** — add a `shareRuns()` fun that serializes to JSON and fires an Intent

The screens are pure Compose — they receive state and fire callbacks upward,
so adding a new screen is just: new `@Composable` file + new `AnimatedVisibility`
block in `MainActivity`.

## Feature parity with HTML

| HTML feature                      | Android                         | Status |
|-----------------------------------|---------------------------------|--------|
| Syntax-highlighted editor         | BasicTextField + SpannableString | ✅     |
| Line numbers                      | Synced Column of Text            | ✅     |
| Pan/zoom canvas graph             | Canvas + gesture detectors       | ✅     |
| Test mode (single/multi/text)     | Full-screen Compose screen       | ✅     |
| Save banner                       | Compose dialog overlay           | ✅     |
| Runs overlay (7d/30d/all filter)  | Full-screen Compose screen       | ✅     |
| Vars / node stats overlay         | Full-screen Compose screen       | ✅     |
| @set / @if / @inline directives   | Fully parsed in ViewModel        | ✅     |
| localStorage persistence          | Room database                    | ✅     |
| Minimap                           | Roadmap                          | ⏳     |
| Split editor+canvas view          | Tablet / landscape layout        | ⏳     |
