# ChronoTask: 100% Offline To-Do & Samsung-Style Full-Screen Reminder Experience (Stitch Design Edition)

A completely offline, local-first Android task and reminder application modeled after the **Samsung Reminders** full-screen experience and styled with the **Stitch Design System** — featuring Apple-style continuous curved cards (`24dp`–`32dp`), glowing app opening entrance animations, interactive zero-states with quick-starter templates, in-app pop alerts, shimmer loading, and animated weekly completion metrics.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Refined Design & Interactive Capabilities Incorporated:**
> - **Stitch Frontend System & Apple-Style Curves:** Deep glass surfaces (`SurfaceColor` with soft semi-transparent overlay), continuous curved squircles (`RoundedCornerShape(26.dp)`), delicate border strokes (`1.dp` rgba white highlight), and vibrant priority glow rings.
> - **Animated App Opening & Logo Intro:** Fluid splash entrance featuring a glowing geometric brand emblem, spring scaling (`spring(dampingRatio = 0.65f)`), and staggered reveal into the main dashboard.
> - **Interactive Zero-States (Never Boring):** When lists are empty, instead of blank screens, display pulsing glowing illustrations, motivational copy, a primary "Create Your First Task" button, and 1-tap quick starter pills (e.g. *"💧 Drink Water"*, *"⚡ Morning Workout"*, *"📚 30m Reading"*, *"💼 Daily Standup"*).
> - **Multi-Layer Reminder Delivery:**
>   1. **Full-Screen Samsung Alert:** The exact overlay from your screenshot (`image_4dca4b2b.png`) with ambient violet glow, "Dismiss", "Complete", and dynamic `[-] Snooze 15m [+]` pill waking up the phone even when locked or screen off.
>   2. **System Push Notifications:** Heads-up banner with quick actions.
>   3. **In-App Pop Banners:** Tactile floating alert pill when tasks trigger while the app is active.
> - **100% Completely Offline:** Pure Room SQLite database with zero network permissions or cloud requirements.

---

### 1. Overview & Core Concept

- **What It Does:** ChronoTask is an offline daily productivity and reminder powerhouse. It combines rapid task capture, customizable categories, 3-tier priorities (Urgent, Medium, Normal), and scheduled alarm-grade reminders that wake the device and launch a dedicated full-screen overlay just like Samsung Reminders.
- **Stitch Visual Philosophy:** Elevates standard utility tools into a polished sensory experience through Apple-style rounded cards, responsive touch ripples, fluid spring physics, and vibrant ambient color accents.

---

### 2. User Experience & Visual Design (Stitch System)

#### Screen 1: Fluid Launch & Logo Entrance
- **Branded Emblem:** Animated circular progress icon with a glowing checkmark that springs into place.
- **Dynamic Greeting:** Transitions seamlessly into today's dashboard with staggered card entries (`slideInVertically` + `fadeIn`).

#### Screen 2: Today's Dashboard (Apple-Style Stitch Cards)
- **Header Card:**
  - Ambient gradient glow backdrop.
  - Large display typography for current day & date.
  - Circular animated completion ring showing daily progress (e.g. "80% Completed") with celebratory pulse when reaching 100%.
- **Filter Carousel:** Pill buttons with smooth sliding indicator for "All", "Today", "Upcoming", and Priority tags.
- **Task Cards:**
  - `26.dp` continuous squircle corners with frosted glass feel.
  - Color-coded priority ribbon: Coral Red (`#FF5370`), Amber Gold (`#FFA23A`), Teal Mint (`#00E5A3`).
  - Haptic-enabled checkmark: Spring bounce animation + subtle particle burst upon completion.
  - Swipe-to-action gestures: Swipe right to complete, swipe left to snooze/delete.
- **Interactive Empty State:**
  - Soft glowing floating calendar/bell illustration.
  - "No tasks scheduled for today" with upbeat tagline.
  - Big vibrant "Create First Task" button.
  - Quick-start recommendation chips that populate a new task with one tap.

#### Screen 3: Samsung-Style Full-Screen Reminder (`ReminderAlertActivity`)
*(Faithful to user reference: `image_4dca4b2b.png`)*
- Deep midnight plum & violet radial aura.
- "Scheduled reminder" bold white header (`28sp`) with tapable "Details" foldout.
- Centered capsules:
  - **Dismiss** translucent frosted pill (`#432670`).
  - **Complete** radiant purple action button (`#58349E`).
- Bottom Snooze Controller:
  - `[-]` button (decrements snooze interval).
  - Central capsule: `Snooze 15 mins` (adjustable to 5, 10, 15, 30, 45, 60m).
  - `[+]` button (increments snooze interval).
- Wakes device, shows over lockscreen (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).

#### Screen 4: Reminders & Alarms Timeline
- Visual chronological timeline with glowing time nodes.
- Instant "Test Alert (5s)" floating button so you can test and demonstrate the full-screen reminder alert immediately on your device.

#### Screen 5: Weekly Progress & Visual Analytics
- Custom Compose Canvas weekly bar charts showing task velocity and completion trends.
- Productivity streak badge tracking unbroken consecutive days of completed tasks.
- Category distribution breakdown.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Stitch Component Aesthetics**
  - *Chosen Approach:* Custom Material 3 theme extensions with `RoundedCornerShape(26.dp)`, custom gradient brushes, and subtle border strokes.
  - *Why:* Replaces generic default card lists with an Apple/OneUI premium feel that looks high-end and modern.
- **Decision 2: Interactive Empty States with Quick Starter Templates**
  - *Chosen Approach:* Beautiful zero-state layouts with quick pre-filled template chips (*"💧 Hydrate"*, *"🏃 20m Walk"*, *"📖 Read chapter"*, *"💼 Work review"*).
  - *Why:* Prevents blank, boring screens on first launch and gives the user instant one-tap utility.
- **Decision 3: Triple-Layer Notification Engine**
  - *Chosen Approach:* Full-screen lockscreen activity + System notification channel + In-app pop toast banner.
  - *Why:* Ensures you never miss a reminder regardless of whether your phone is asleep, locked, or currently being used in-app.
- **Decision 4: 100% Offline & Reboot Resilience**
  - *Chosen Approach:* Room SQLite database + `RECEIVE_BOOT_COMPLETED` receiver to restore scheduled alarms automatically after device restart.

---

### 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ChronoTask Stitch App                           │
│                                                                        │
│   ┌────────────────────┐   ┌───────────────────┐   ┌────────────────┐  │
│   │  Animated Splash   │   │  DashboardScreen  │   │ AnalyticsScreen│  │
│   │    Logo Intro      │   │ (Apple-Style Card)│   │ (Weekly Canvas)│  │
│   └─────────┬──────────┘   └─────────┬─────────┘   └────────┬───────┘  │
│             │                        │                      │          │
│             └────────────────┬───────┴──────────────────────┘          │
│                              ▼                                         │
│                    ┌───────────────────┐                               │
│                    │   TaskViewModel   │                               │
│                    │(StateFlow, Filter)│                               │
│                    └─────────┬─────────┘                               │
│                              ▼                                         │
│                    ┌───────────────────┐                               │
│                    │  TaskRepository   │                               │
│                    └─────────┬─────────┘                               │
│                              │                                         │
│             ┌────────────────┴────────────────┐                        │
│             ▼                                 ▼                        │
│   ┌──────────────────┐              ┌──────────────────┐               │
│   │   Room Database  │              │TaskAlarmScheduler│               │
│   │   (Local SQLite) │              │  (AlarmManager)  │               │
│   └──────────────────┘              └────────┬─────────┘               │
└──────────────────────────────────────────────┼─────────────────────────┘
                                               ▼ (When time triggers)
┌────────────────────────────────────────────────────────────────────────┐
│                       Lockscreen Alert Trigger                         │
│                                                                        │
│   ┌───────────────────────────┐      ┌──────────────────────────────┐  │
│   │ TaskAlarmReceiver         │ ───► │ In-App Pop & System Notify   │  │
│   │ (Wakeful Broadcast)       │      │ (Heads-Up High Priority)     │  │
│   └─────────────┬─────────────┘      └──────────────┬───────────────┘  │
│                 │                                   │                  │
│                 └───────────────────────────────────┤                  │
│                                                     ▼                  │
│                                     ┌───────────────────────────────┐  │
│                                     │     ReminderAlertActivity     │  │
│                                     │ (Samsung Purple Ambient View: │  │
│                                     │ Dismiss, Complete, [-] Snooze)│  │
│                                     └───────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### AndroidManifest Permissions & Declarations
- `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
- `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />`
- `<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />`
- `<uses-permission android:name="android.permission.WAKE_LOCK" />`
- `<uses-permission android:name="android.permission.VIBRATE" />`
- `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`
- Activity declaration for `ReminderAlertActivity`:
  - `android:showWhenLocked="true"`
  - `android:turnScreenOn="true"`
  - `android:showOnLockScreen="true"`
  - `android:launchMode="singleTask"`
  - `android:excludeFromRecents="true"`
