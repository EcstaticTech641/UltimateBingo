package com.ronlab.bingo.util;

import org.bukkit.Material;

import java.util.*;

public class MaterialPoolManager {

    private static final List<Material> EASY_MATERIALS = List.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.COAL, Material.IRON_INGOT, Material.IRON_NUGGET,
            Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.FEATHER,
            Material.CHICKEN, Material.BEEF, Material.PORKCHOP, Material.MUTTON, Material.COD,
            Material.SUGAR_CANE, Material.CHEST, Material.BARREL, Material.WHITE_WOOL,
            Material.APPLE, Material.FLINT, Material.FLINT_AND_STEEL, Material.FLOWER_POT,
            Material.STICK, Material.TORCH, Material.CRAFTING_TABLE, Material.FURNACE
    );

    private static final List<Material> NORMAL_MATERIALS = List.of(
            Material.GOLD_INGOT, Material.REDSTONE, Material.REDSTONE_TORCH,
            Material.SPIDER_EYE, Material.NOTE_BLOCK, Material.COOKED_CHICKEN,
            Material.COOKED_PORKCHOP, Material.BOOK, Material.CAULDRON,
            Material.LIGHTNING_ROD, Material.RAIL, Material.SNOW_BLOCK,
            Material.GLASS, Material.COAL_BLOCK, Material.RED_BED,
            Material.ORANGE_WOOL, Material.YELLOW_WOOL, Material.RED_WOOL,
            Material.SMOOTH_STONE, Material.RED_DYE, Material.YELLOW_DYE, Material.BRICK
    );

    private static final List<Material> HARD_MATERIALS = List.of(
            Material.LAPIS_LAZULI, Material.BOOKSHELF, Material.MOSS_BLOCK,
            Material.BAMBOO, Material.PUMPKIN, Material.MOSSY_COBBLESTONE,
            Material.ITEM_FRAME, Material.LEATHER_BOOTS, Material.LEATHER_HELMET,
            Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.CHAIN,
            Material.IRON_PICKAXE, Material.DIORITE, Material.POINTED_DRIPSTONE,
            Material.MELON, Material.POPPY, Material.BLACK_WOOL, Material.KELP,
            Material.GUNPOWDER, Material.GLASS_PANE, Material.WHITE_DYE
    );

    public static List<Material> getRandomMaterials(int count) {
        List<Material> pool = new ArrayList<>();
        pool.addAll(EASY_MATERIALS);
        pool.addAll(NORMAL_MATERIALS);
        pool.addAll(HARD_MATERIALS);

        Collections.shuffle(pool);

        Set<Material> selected = new LinkedHashSet<>();
        for (Material mat : pool) {
            selected.add(mat);
            if (selected.size() >= count) {
                break;
            }
        }
        return new ArrayList<>(selected);
    }
}
