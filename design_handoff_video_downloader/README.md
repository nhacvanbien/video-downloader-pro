# Handoff: Video Downloader — Design System Directions + Pinboard App

## Overview
Two linked design artifacts for the Video Downloader Android app:
1. **Design System (4 directions)** — an exploration comparing four visual directions (Focus, Calm, Signal, Pinboard) applied to two sample screens, used to pick the app's visual language.
2. **Pinboard App (backup)** — the full 19-screen app redesign built on the chosen "Pinboard" direction: warm berry palette, large rounded corners, pill/soft buttons, fully flat (no shadows), Plus Jakarta Sans. This is a saved snapshot of that direction — the project's current "Video Downloader App.dc.html" has since evolved past this snapshot toward a more Material-style token set, so treat this backup file as the definitive Pinboard reference if that's the direction you're building.

## About the Design Files
The bundled `.dc.html` files are **design references built in HTML** — prototypes showing intended look, states, and copy. They are NOT production code to copy into the app. The task is to **recreate this UI natively in the existing Android codebase** (Jetpack Compose or XML/View system — match whatever the project already uses) using its existing architecture, navigation, and component patterns.

## Fidelity
**High-fidelity.** Colors, corner radii, spacing, typography, and copy shown are final for the Pinboard app file — recreate pixel-close using the tokens below. The Design System file is a **comparison/exploration** document (not a screen to build); use it only to understand why Pinboard's tokens were chosen over the other three directions. Icons in both files are placeholder line-art SVGs or flat-color blocks — replace with the app's icon set (or new icons matching ~1.7–2px stroke weight) rather than copying the SVGs.

## Note on language
Some labels/copy inside `Video Downloader Design System.dc.html` are in Vietnamese (it was an internal exploration doc). Any user-facing strings you implement should be in English, matching the Pinboard app file's copy.

---

## File 1 — Video Downloader Design System.dc.html
Four candidate visual directions, each with a light/dark color palette, semantic colors (success/warning/error), a typography pairing, and sample components (buttons, chip, card), plus the same two screens (Browser Home, Progress) re-skinned:

| ID | Name | Character | Fonts | Radii | Shadow |
|---|---|---|---|---|---|
| 1a | Focus | Indigo/violet, restrained accent | Space Grotesk / Instrument Sans | 14–20px | soft, on cards/sheets only |
| 1b | Calm | Warm paper bg, deep green accent | Bricolage Grotesque / Public Sans | 10–16px | none (flat) |
| 1c | Signal | Ink on warm white, amber accent for progress only | IBM Plex Sans / IBM Plex Mono (numerics) | 6–10px | none (flat) |
| 1d | **Pinboard (chosen)** | Warm berry accent, pill shapes | Plus Jakarta Sans | 14–18px | none (flat) |

**Pinboard was selected** as the app-wide direction — see File 2 for its full implementation.

## File 2 — Video Downloader App (Pinboard backup).dc.html
Full UI redesign covering onboarding, browser, video detection & format selection, download & progress (merged with library into one "Downloads" tab), player, history/bookmark, private area (PIN), and settings, all in the Pinboard visual language.

### Design Tokens (Pinboard)

#### Colors
| Token | Value | Usage |
|---|---|---|
| --bg | oklch(0.985 0.006 40) | Screen background |
| --surface | #ffffff | Cards, sheets, nav bar |
| --text | oklch(0.24 0.02 30) | Primary text |
| --muted | oklch(0.55 0.02 30) | Secondary text |
| --pri | #e60023 (≈ oklch(0.55 0.2 18)) | Primary berry accent (buttons, active states, selected) |
| --pri-ink | #ffffff | Text/icon on primary |
| --pri-soft | oklch(0.95 0.04 20) | Primary tint background (chips, thumbnail placeholders, active row bg) |
| --border | oklch(0.9 0.008 40) | Hairline borders |
| --warn | oklch(0.7 0.15 65) | Warning accent token (persistent Wi-Fi banner was removed from Downloads screen) |
| --warn-soft | oklch(0.96 0.06 80) | Warning background |
| --warn-ink | oklch(0.45 0.11 55) | Warning text |
| --success | oklch(0.58 0.13 155) | Success/done state |
| --error | #c2410c | Errors, destructive actions (Delete, Retry, Failed) |

#### Typography
- Font family (all roles — heading, body, numeric): **Plus Jakarta Sans**
- Screen titles: 19px / weight 800
- Body/labels: 12.5–13.5px / weight 600–700
- Secondary/meta text: 10.5–11px / weight 400–600, color `--muted`
- Numeric (%, speed, file size, timers): tabular, weight 700

#### Radii
- `--r-lg`: 18px — cards, active-item rows, bottom-sheet top corners
- `--r-md`: 14px — thumbnails, inputs, standard buttons/rows
- `--r-sm`: 999px (pill) — primary CTA buttons, chips, toggles

#### Spacing
Screen horizontal padding: 16px. Section vertical rhythm: 8–14px between stacked elements, 9–10px gap in lists.

### Screens / Views

1. **Onboarding** — Splash, Intro, Language picker (selected language = 2px `--pri` border outline, not filled), Guide/permissions carousel, Grant access, Privacy notice.
2. **Browser** — Home (search bar, tab count badge); WebView (address bar with search icon on the right; bottom nav + floating "Download" button); Tabs (grid of cards, close X top-right).
3. **Video Detection & Format Selection** — Bottom sheet, detected media list, chip row incl. Audio chip (P1), primary "Download video" pill button.
4. **Downloads** (merged Download/Progress + Library) — Header ("Downloads" title + circular Select icon; a Delete icon appears once items are selected). Type tabs (underline style): All/Video/Audio/Image — no separate status filter. Search bar. List meta row ("N files" / "Sort: Newest", or "N selected" in selection mode). Mixed rows: Downloading (pri-soft card, progress bar), Failed (outlined card, Retry pill — P0), Completed (plain row, 3-dot actions). Selection mode adds leading checkboxes. Empty state with illustration placeholder. Selection action bar: Move to Private / Delete + storage readout (P0). Item actions sheet: Rename/Share/Move to Private/Delete. System notification mocks (progress + failed w/ Retry — P1).
5. **Media Player** — Dark chrome (#141210), scrubber, speed/loop/crop/rotate controls, brightness/volume swipe gestures (P2, invisible layer).
6. **History & Bookmark** — Segmented History/Bookmark switch. Bookmark empty state has floating "+". History grouped by day, "Clear history" link.
7. **Private area (PIN)** — PIN entry/setup. Copy must never imply encryption ("hidden from the main library", never "secured"/"encrypted").
8. **Settings** — Download location, Wi-Fi Only toggle (P1), Languages, Search Engine (P2, bottom sheet: Google/Bing/Yahoo/DuckDuckGo with same selection pattern as Language picker), Rate app, Share app, Privacy Policy.

### Interactions & Behavior
- Select icon toggles selection mode (becomes an X while active); Delete icon in header appears only once ≥1 item selected.
- Retry re-attempts only that item's download.
- Tapping a completed row opens the Player; tapping its 3-dot icon opens the actions sheet instead (must stop propagation).
- A prior filter-sheet (modal Status + Type pickers) and a persistent Wi-Fi-waiting banner were explored and explicitly removed in favor of inline type tabs + interleaved list — don't reintroduce without checking with product.

### State Management
- `downloads: DownloadItem[]` — `status: 'downloading'|'failed'|'completed'`, `type: 'video'|'audio'|'image'`, `progress: number` (0–100).
- `activeTypeTab: 'all'|'video'|'audio'|'image'`
- `selectionMode: boolean`, `selectedIds: Set<string>`
- `searchQuery: string`
- Wi-Fi Only is a persisted setting; confirm with product whether a transient "waiting for Wi-Fi" toast should replace the removed persistent banner.

## Assets
No real icons/illustrations included — placeholders are flat-color blocks or inline placeholder line-art SVGs. Replace with the app's real icon set and illustrations before shipping. Font: Plus Jakarta Sans (Google Fonts).

## Files
- `Video Downloader Design System.dc.html` — 4-direction comparison exploration (open in a browser; pan/zoom canvas).
- `Video Downloader App (Pinboard backup).dc.html` — full Pinboard app reference, all screens/states.
- `PhoneScreens.dc.html` — shared phone-frame component both files import.
- `screenshots/` — static previews of both files' top sections.
