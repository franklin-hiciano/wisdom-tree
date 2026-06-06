# Wisdom Tree — Android App

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
