package com.ronlab.bingo.model;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class BingoCard {

    public static class CardSlot {
        private final int gridIndex;
        private final Material targetMaterial;
        private boolean completed;
        private UUID completedBy;
        private String completedByName;

        public CardSlot(int gridIndex, Material targetMaterial) {
            this.gridIndex = gridIndex;
            this.targetMaterial = targetMaterial;
            this.completed = false;
        }

        public int getGridIndex() {
            return gridIndex;
        }

        public Material getTargetMaterial() {
            return targetMaterial;
        }

        public boolean isCompleted() {
            return completed;
        }

        public UUID getCompletedBy() {
            return completedBy;
        }

        public String getCompletedByName() {
            return completedByName;
        }

        public void markCompleted(Player player) {
            this.completed = true;
            this.completedBy = player.getUniqueId();
            this.completedByName = player.getName();
        }
    }

    private final int gridSize; // 5 (5x5) or 3 (3x3)
    private final List<CardSlot> slots;
    private final Map<Material, CardSlot> materialMap;

    public BingoCard(int gridSize, List<Material> materials) {
        this.gridSize = gridSize;
        this.slots = new ArrayList<>();
        this.materialMap = new HashMap<>();

        int count = Math.min(gridSize * gridSize, materials.size());
        for (int i = 0; i < count; i++) {
            Material mat = materials.get(i);
            CardSlot slot = new CardSlot(i, mat);
            slots.add(slot);
            materialMap.put(mat, slot);
        }
    }

    public int getGridSize() {
        return gridSize;
    }

    public List<CardSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public CardSlot getSlot(int index) {
        if (index >= 0 && index < slots.size()) {
            return slots.get(index);
        }
        return null;
    }

    public CardSlot getSlotForMaterial(Material material) {
        return materialMap.get(material);
    }

    public boolean markItemCompleted(Material material, Player player) {
        CardSlot slot = materialMap.get(material);
        if (slot != null && !slot.isCompleted()) {
            slot.markCompleted(player);
            return true;
        }
        return false;
    }

    public int getCompletedCount() {
        int count = 0;
        for (CardSlot slot : slots) {
            if (slot.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount() {
        return slots.size();
    }

    public int countCompletedLines() {
        int completedLines = 0;

        // Check horizontal rows
        for (int row = 0; row < gridSize; row++) {
            boolean rowComplete = true;
            for (int col = 0; col < gridSize; col++) {
                int idx = row * gridSize + col;
                if (idx >= slots.size() || !slots.get(idx).isCompleted()) {
                    rowComplete = false;
                    break;
                }
            }
            if (rowComplete) completedLines++;
        }

        // Check vertical columns
        for (int col = 0; col < gridSize; col++) {
            boolean colComplete = true;
            for (int row = 0; row < gridSize; row++) {
                int idx = row * gridSize + col;
                if (idx >= slots.size() || !slots.get(idx).isCompleted()) {
                    colComplete = false;
                    break;
                }
            }
            if (colComplete) completedLines++;
        }

        // Check diagonal 1 (top-left to bottom-right)
        boolean diag1Complete = true;
        for (int i = 0; i < gridSize; i++) {
            int idx = i * gridSize + i;
            if (idx >= slots.size() || !slots.get(idx).isCompleted()) {
                diag1Complete = false;
                break;
            }
        }
        if (diag1Complete) completedLines++;

        // Check diagonal 2 (top-right to bottom-left)
        boolean diag2Complete = true;
        for (int i = 0; i < gridSize; i++) {
            int idx = i * gridSize + (gridSize - 1 - i);
            if (idx >= slots.size() || !slots.get(idx).isCompleted()) {
                diag2Complete = false;
                break;
            }
        }
        if (diag2Complete) completedLines++;

        return completedLines;
    }

    public boolean hasBingoLine() {
        return countCompletedLines() > 0;
    }

    public boolean isFullCard() {
        return getCompletedCount() == getTotalCount();
    }
}
