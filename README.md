# UltimateBingo & rgaBingo Companion Plugin

This repository contains the legacy **UltimateBingo** codebase as well as the modern CPMK Greenfield Companion plugin **rgaBingo** (`com.ronlab.bingo`).

---

## 📦 rgaBingo (RGA Companion Minigame Plugin)

`rgaBingo` is a pure event-driven minigame companion plugin engineered for the **RonlabGameAssistant (RGA)** orchestrator ecosystem. It targets **Paper 26.2** and **Java 25**, featuring dynamic 54-slot chest GUI card rendering and a packet-based FastBoard sidebar HUD.

### 🎯 Key Architectural Rules & Principles

- **Stateless Orchestration**: World copying, session lifecycle, inventory isolation, and player state management are handled entirely by RGA.
- **Zero Command Layer**: No player-facing `/bingo` commands. Session entry is handled via RGA (`rga:join_minigame bingo`), and mid-game card access is bound to a hotbar item (Slot 8) or offhand right-click.
- **Event-Driven Engine**: Listens to `MinigameStartEvent` to initialize 5x5 card grids and run a 3-second pre-match countdown (frozen XYZ movement, note block audio cues).
- **Fast Short-Circuiting**: Inventory listeners (`EntityPickupItemEvent`, `CraftItemEvent`, `InventoryClickEvent`) short-circuit early for non-participants to preserve server performance.
- **Packet-Based Scoreboard**: Uses shaded `fr.mrmicky:fastboard` (relocated to `com.ronlab.bingo.fastboard`) for real-time progress and match timing.
- **Spectator & Win Flow**: On Bingo line or blackout completion, winner transitions to Spectator mode via `rgaControl.setSpectator(player, true)` and fires `GameSessionRequestConcludeEvent`.

### 🚀 Building rgaBingo

#### Prerequisites
- JDK 25
- Apache Maven 3.9+

#### Build Command
```bash
cd rgaBingo
mvn clean package
```

The compiled, shaded JAR will be produced at:
`rgaBingo/target/rgaBingo-1.0.0.jar`

---

## 📋 Plugin Manifest (`paper-plugin.yml`)

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

---

## 📂 Project Structure

```text
UltimateBingo/
├── .gitignore                      # Standard Java/Maven/IDE gitignore
├── README.md                       # Repository documentation
├── pom.xml                         # Legacy UltimateBingo POM
├── src/                            # Legacy UltimateBingo source code
└── rgaBingo/                       # RGA CPMK Companion Plugin
    ├── pom.xml                     # Java 25 Maven build specification
    ├── README.md                   # Companion plugin quickstart & documentation
    └── src/
        └── main/
            ├── java/
            │   ├── com/ronlab/bingo/      # Companion core (Plugin, listeners, model, GUI, HUD)
            │   └── com/ronlab/rga/api/    # RGA Orchestrator API & events
            └── resources/
                └── paper-plugin.yml   # Paper 26.2 plugin manifest
```

---

## 📜 License & Governance

This project adheres to Ronlab ecosystem standards and software engineering best practices.
