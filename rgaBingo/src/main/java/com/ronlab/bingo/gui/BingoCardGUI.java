package com.ronlab.bingo.gui;

import com.ronlab.bingo.model.BingoCard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BingoCardGUI {

    public static final String GUI_TITLE_PREFIX = "§2§lBINGO CARD §8| §7Progress: ";

    // Slot layout for 5x5 grid in a 54-slot inventory
    private static final int[] GRID_SLOTS_5X5 = {
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42,
            47, 48, 49, 50, 51
    };

    public static Inventory createGUI(BingoCard card, Player player) {
        int completed = card.getCompletedCount();
        int total = card.getTotalCount();
        String title = GUI_TITLE_PREFIX + completed + "/" + total;

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));

        // Fill background/borders with dark gray stained glass panes
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.text(" "));
            border.setItemMeta(borderMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // Place card items into the grid slots
        List<BingoCard.CardSlot> cardSlots = card.getSlots();
        for (int i = 0; i < cardSlots.size() && i < GRID_SLOTS_5X5.length; i++) {
            BingoCard.CardSlot slot = cardSlots.get(i);
            int invSlot = GRID_SLOTS_5X5[i];

            if (slot.isCompleted()) {
                ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(getMaterialName(slot.getTargetMaterial()), NamedTextColor.GREEN, TextDecoration.BOLD));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("✔ Completed by " + (slot.getCompletedByName() != null ? slot.getCompletedByName() : player.getName()), NamedTextColor.GREEN));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(invSlot, item);
            } else {
                ItemStack item = new ItemStack(slot.getTargetMaterial());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(getMaterialName(slot.getTargetMaterial()), NamedTextColor.GOLD, TextDecoration.BOLD));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("✘ Not Collected", NamedTextColor.RED));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(invSlot, item);
            }
        }

        return inv;
    }

    public static void openGUI(Player player, BingoCard card) {
        Inventory gui = createGUI(card, player);
        player.openInventory(gui);
    }

    private static String getMaterialName(Material material) {
        String name = material.name().replace('_', ' ').toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
