# rgaBingo — RGA Minigame Companion Plugin

`rgaBingo` (`com.ronlab.bingo`) is the official Ronlab Game Assistant (RGA) companion minigame plugin for Ultimate Bingo. Built under the **CPM Architecture (CPMK)** as a self-contained, event-driven module, it targets **PaperMC 26.2** running on **Java 25**.

---

## 🏛️ CPMK Architectural Alignment (5 Core Pillars)

`rgaBingo` fully adheres to the five core CPMK pillars established in `rga-core`:

1. **Core Gameplay Function Retention**: Preserves 100% of native minigame mechanics, including dynamic 54-slot chest GUI card generation, item pickup/crafting acquisition pathways, actionbar HUD updates, pre-match freeze countdowns, and spectator victory transitions.
2. **Ronlab Integration Standard**: Listens strictly for CPMK event payloads (`MinigameStartEvent` and `MinigameConcludeEvent`) over `rga-api`. Descriptor `paper-plugin.yml` targets `api-version: '26.2'` and declares dependency on `RonlabGameAssistant` (`required: true`, `join-classpath: true`, `load: BEFORE`).
3. **Baseline Structure & Rules Provision**: FastBoard scoreboard lines suppress margin numbers using PaperMC blank number formatting. Scoreboard creation and assignment occur strictly post-teleport to eliminate chunk-loading hangs. Teardown routines restore the main scoreboard and unregister objectives on session conclusion.
4. **Companion-Type Agnostic Design**: Operates as an independent module decoupled from `rga-core` internals, communicating solely through `rga-api` event payloads.
5. **Feature & QA Specification**: Features zero player-facing command clutter and includes **Solo QA Developer Mode** (`initialPlayerCount == 1`) for continuous map reset and mechanics testing.

---

## ⚡ Quick Start & Deployment

### 1. Build Prerequisites
- **JDK 25** (Eclipse Adoptium OpenJDK 25 or equivalent)
- **Apache Maven 3.9+**
- **PaperMC 26.2** server instance with `RonlabGameAssistant` installed

### 2. Compilation
```bash
cd rgaBingo
mvn clean package
```
The compiled, shaded artifact will be produced at:
`rgaBingo/target/rgaBingo-1.0.0.jar`

### 3. Installation
1. Copy `rgaBingo-1.0.0.jar` into your Paper server's `plugins/` folder alongside `RonlabGameAssistant.jar`.
2. Start the server. RGA will discover `rgaBingo` via `paper-plugin.yml` dependency management.

---

## 🎮 Gameplay Mechanics & Features

- **Zero Command Execution**: Players join via RGA central minigame GUI (`rga:join_minigame bingo`). Hotbar Slot 8 item opens the 54-slot Bingo Card GUI.
- **3-Second Pre-Match Countdown**: Movement is frozen at spawn location, note block audio cues play, and a title countdown appears (`3... 2... 1... GO!`).
- **Item Acquisition Pathways**: Unified detection across physical item pickups (`EntityPickupItemEvent`), crafting (`CraftItemEvent`), and container extractions (`InventoryClickEvent`/`InventoryDragEvent`).
- **Real-Time FastBoard HUD**: Displays elapsed match time (`BINGO (MM:SS)`), player progress, completed lines, and top 5 leaderboard rankings.
- **Victory & Spectator Flow**: Completing a 5-item row, column, or diagonal line triggers victory announcements, plays sound toasts, transitions the winner to Spectator mode, and fires `MinigameConcludeEvent`.

---

## 🧪 Solo QA Developer Mode (`initialPlayerCount == 1`)

When a session starts with `initialPlayerCount == 1` (e.g. single-player QA test):
- **Win Condition Freeze**: The match does not auto-conclude or kick the player upon completing a Bingo line.
- **Continuous QA Testing**: Enables developers and admins to continuously test map resets, item pickups, actionbar notifications, and chest GUI interactions.
- **Score Reporting**: Final item completion counts are preserved and delivered in the final event payload when manually concluded or shut down.

---

## 📜 Permissions & Administrative Control

| Permission Node | Description | Default |
|---|---|---|
| `rga.player` | Grants access to join Bingo minigame sessions via RGA | `true` |
| `rga.admin` | Grants administrative control to conclude or reset sessions | `op` |
| `rga.bingo.admin` | Local administrative node for Bingo companion management | `op` |

---

## 📄 Manifest Metadata (`paper-plugin.yml`)

```yaml
name: rgaBingo
version: 1.0.0
main: com.ronlab.bingo.BingoPlugin
api-version: '26.2'

dependencies:
  server:
    RonlabGameAssistant:
      load: BEFORE
      required: true
      join-classpath: true
```
