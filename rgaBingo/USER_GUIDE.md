# rgaBingo — Local User & Administrator Guide

Welcome to the official user and administrator guide for **rgaBingo**, the micro-companion minigame plugin built for the **Ronlab Game Assistant (RGA)** orchestrator framework under **PaperMC 26.2** and **Java 25**.

---

## 📑 Table of Contents
1. [Architectural Overview](#architectural-overview)
2. [CPMK Event Bus Lifecycle](#cpmk-event-bus-lifecycle)
3. [Scoreboard HUD & Display Specification](#scoreboard-hud--display-specification)
4. [Solo QA Developer Mode](#solo-qa-developer-mode)
5. [Configuration Reference (`config.yml`)](#configuration-reference-configyml)
6. [Commands & Permission Nodes](#commands--permission-nodes)
7. [Troubleshooting & Verification](#troubleshooting--verification)

---

## 1. Architectural Overview

`rgaBingo` (`com.ronlab.bingo`) is engineered using the **CPM Architecture (CPMK)**. It functions as a pure, stateless event listener companion to `RonlabGameAssistant` (`rga-core`).

Key design characteristics:
- **Stateless Orchestration**: World loading, player inventory saving/restoration, team allocation, and lobby teleportation are managed entirely by `rga-core`.
- **Zero Command Clutter**: No `/bingo` player commands are registered. Session lifecycle is driven via `rga-api` event payloads.
- **Card Mechanics**: Upon match start, each player receives a unique 5x5 grid of survival-obtainable items. Card progress can be inspected anytime via the Slot 8 hotbar item (`Bingo Card`).

---

## 2. CPMK Event Bus Lifecycle

`rgaBingo` communicates strictly through the `com.ronlab.rga.api.event` package:

### 🟢 `MinigameStartEvent`
Dispatched by `rga-core` when a Bingo session starts:
1. `rgaBingo` validates the `minigameId` (`"bingo"` or `"ultimatebingo"`).
2. Generates a randomized 5x5 `BingoCard` for each participant UUID.
3. Performs a 1-tick deferred inventory scan to silently credit items already held by players.
4. Initializes a post-teleport FastBoard sidebar for each player.
5. Teleports players to `spawn-location` and applies a **3-second freeze countdown** with note block audio feedback (`BLOCK_NOTE_BLOCK_PLING`).

### 🔴 `MinigameConcludeEvent`
Dispatched when a player completes a Bingo line or when an admin terminates the session:
1. Gathers final item completion scores (`Map<UUID, Number>`).
2. Cancels active timer and countdown tasks.
3. Reverts FastBoard scoreboards for all players back to Bukkit's main scoreboard (`Bukkit.getScoreboardManager().getMainScoreboard()`).
4. Unregisters session data and clears memory caches.

---

## 3. Scoreboard HUD & Display Specification

The FastBoard sidebar HUD (`BingoScoreboardManager`) provides real-time match tracking:

- **Margin Number Suppression**: Uses PaperMC `NumberFormat.blank()` / FastBoard blank line formatting across sidebar lines to eliminate ugly right-hand objective score numbers.
- **Chunk-Loading Hang Prevention**: Scoreboard initialization (`player.setScoreboard()`) is executed strictly during post-teleport spawn phases after chunk loading has completed.
- **Teardown Backstop**: On `MinigameConcludeEvent` or plugin disable, `removeAll()` is invoked to remove scoreboards even for disconnected or spectating players, preventing lingering sidebars in lobby worlds.

---

## 4. Solo QA Developer Mode

### Overview
When a session is launched with `initialPlayerCount == 1` (a single participant), `rgaBingo` automatically activates **Solo QA Developer Mode**.

### QA Testing Features
- **Win Condition Freeze**: Completing a 5-in-a-row Bingo line triggers victory announcements and title toasts, but **does not auto-conclude or kick the developer**.
- **Continuous Map & Mechanics QA**: Allows QA engineers to continuously test:
  - Inventory pickup listeners (`EntityPickupItemEvent`)
  - Crafting table detection (`CraftItemEvent`)
  - Container pulling and offhand transactions (`InventoryClickEvent` / `InventoryDragEvent`)
  - Map resets, fall thresholds, and vector spawns.
- **Payload Accuracy**: The final score map (`completedCount`) remains recorded and is accurately transmitted when the session is concluded via RGA admin controls (`/rga conclude`).

---

## 5. Configuration Reference (`config.yml`)

The default configuration is saved at `rgaBingo/src/main/resources/config.yml`:

```yaml
# Vector & Spatial Settings
spawn-location:
  x: 0.5
  y: 64.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

pedestal-location:
  x: 0.5
  y: 65.0
  z: 10.5
  yaw: 180.0
  pitch: 0.0

fall-threshold-y: -64.0   # Safe teleport back to spawn if player falls below Y

# Match Timing
time-limit-seconds: 1200  # Max match duration (20 minutes)
countdown-seconds: 3      # Pre-match frozen countdown

# Card Setup
card-size: 5              # 5x5 grid (25 items)
card-item-slot: 8         # Hotbar slot for Bingo Card item
```

---

## 6. Commands & Permission Nodes

### Commands
`rgaBingo` operates in **Zero Command Mode**. All session operations (join, leave, start, conclude) are handled through `RonlabGameAssistant`:
- Join Bingo: `/rga join bingo` or via RGA Minigame GUI
- Admin Conclude: `/rga conclude`

### Permission Nodes
- `rga.player`: Required for players to join Bingo minigame sessions (Default: `true`).
- `rga.admin`: Grants administrative session override and conclude rights (Default: `op`).
- `rga.bingo.admin`: Local companion permission node for admin overrides (Default: `op`).

---

## 7. Troubleshooting & Verification

### Build & Package Verification
```bash
cd rgaBingo
mvn clean package
```
Ensure `rgaBingo-1.0.0.jar` contains `paper-plugin.yml` and `config.yml` at root.

### Runtime Log Checklist
Upon server boot, look for the following console logs:
- `[rgaBingo] Initializing rgaBingo v1.0.0 (CPMK Companion Mode)`
- `[rgaBingo] Successfully hooked into RGASessionControl service.`
- `[rgaBingo] Handshake successful. Initializing Bingo session for world: <world_name>`
