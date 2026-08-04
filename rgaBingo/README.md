# rgaBingo — RGA Minigame Companion Plugin

`rgaBingo` (`com.ronlab.bingo`) is the official Ronlab Game Assistant (RGA) companion plugin for the Ultimate Bingo minigame. Built as a pure event-driven engine, it hooks directly into `com.ronlab:rga-api`, utilizing **Paper 26.2** (Java 25), dynamic 54-slot chest GUI card generation, and packet-based FastBoard sidebar scoreboards.

---

## ⚡ Quick Start / Setup How-To

### 1. Requirements
- **JDK 25** (Eclipse Adoptium OpenJDK 25 or equivalent)
- **Apache Maven 3.9+**
- **Paper 26.2** server instance with `RonlabGameAssistant` installed

### 2. Compilation & Building
To build the shaded, deployable companion JAR:

```bash
cd rgaBingo
mvn clean package
```

The compiled artifact will be generated at:
`rgaBingo/target/rgaBingo-1.0.0.jar`

### 3. Installation
1. Copy `rgaBingo-1.0.0.jar` into your Paper server's `plugins/` folder alongside `RonlabGameAssistant.jar`.
2. Start or restart the server.
3. RGA will automatically discover `rgaBingo` via `paper-plugin.yml` dependency management.

---

## 🎮 How It Works

### Game Entry
Players join Bingo sessions through RGA's central minigame GUI (`rga:join_minigame bingo`). No `/bingo` or chat commands are required.

### In-Game Features
- **3-Second Pre-Match Countdown**: Movement is frozen, note block audio cues play, and a title countdown appears (`3... 2... 1... GO!`).
- **Bingo Card Item**: Slot 8 hotbar item (`Bingo Card`) allows opening the dynamic 54-slot chest GUI anytime with right-click.
- **Real-Time HUD**: FastBoard sidebar displays match timer (`BINGO (MM:SS)`) and top player item completion counts.
- **Item Pickups & Crafting**: Acquiring target items plays an experience chime (`entity.experience_orb.pickup`), updates the actionbar (`✔ Found: <Item Name>!`), and refreshes HUD/GUI instantly.
- **Win Condition & Teardown**: Completing a Bingo line transitions the winner to Spectator mode and triggers session conclusion with RGA.

---

## 📦 Versioning & Metadata

- **Plugin Name**: `rgaBingo`
- **Version**: `1.0.0`
- **Package**: `com.ronlab.bingo`
- **API Target**: Paper `26.2` (Java 25)
- **Dependency**: `RonlabGameAssistant` (load BEFORE, required)
