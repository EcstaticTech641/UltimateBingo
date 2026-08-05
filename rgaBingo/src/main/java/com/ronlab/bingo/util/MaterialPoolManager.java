package com.ronlab.bingo.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.*;
import java.util.logging.Logger;

public class MaterialPoolManager {

    private static final Logger LOGGER = Bukkit.getLogger();

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

    private static final List<Material> HARD_MATERIALS;

    static {
        List<Material> hardList = new ArrayList<>(List.of(
                Material.LAPIS_LAZULI, Material.BOOKSHELF, Material.MOSS_BLOCK,
                Material.BAMBOO, Material.PUMPKIN, Material.MOSSY_COBBLESTONE,
                Material.ITEM_FRAME, Material.LEATHER_BOOTS, Material.LEATHER_HELMET,
                Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS,
                Material.IRON_PICKAXE, Material.DIORITE, Material.POINTED_DRIPSTONE,
                Material.MELON, Material.POPPY, Material.BLACK_WOOL, Material.KELP,
                Material.GUNPOWDER, Material.GLASS_PANE, Material.WHITE_DYE
        ));

        // "chain" may not exist as a Material enum constant in all Paper 26.2 builds.
        // Probe multiple candidate name strings; skip gracefully if none resolve.
        // We intentionally avoid any compile-time Material.CHAIN reference to prevent
        // NoSuchFieldError on API versions where the constant is absent.
        Material chainMat = probeMatchMaterial("chain", "CHAIN", "chain_armor");
        if (chainMat != null) {
            hardList.add(chainMat);
        } else {
            LOGGER.warning("[rgaBingo] Could not resolve any chain material variant on this server version. Skipping.");
        }

        HARD_MATERIALS = Collections.unmodifiableList(hardList);
    }

    /**
     * Probes a list of candidate name strings via {@link Material#matchMaterial(String)}
     * and returns the first non-null result. Returns null if none of the candidates resolve.
     * Callers are responsible for handling the null case (skip or substitute).
     *
     * <p>This avoids any compile-time Material enum constant reference, which would cause
     * a {@link NoSuchFieldError} on API versions where the constant is absent.
     *
     * @param candidates one or more name strings to attempt in order
     * @return the first resolved Material, or null if all candidates fail
     */
    public static Material probeMatchMaterial(String... candidates) {
        for (String name : candidates) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                return mat;
            }
        }
        return null;
    }

    /**
     * Resolves a material by name string, falling back to the provided enum constant if
     * {@link Material#matchMaterial(String)} returns null. Only use this overload when the
     * fallback enum constant is guaranteed to exist in the target server's API version.
     *
     * @param name     the material name string to attempt resolution for
     * @param fallback the compile-time verified enum constant to use if resolution fails
     * @return the resolved Material (never null when a valid fallback is supplied)
     */
    public static Material resolveSafeMaterial(String name, Material fallback) {
        Material resolved = Material.matchMaterial(name);
        if (resolved != null) {
            return resolved;
        }
        LOGGER.warning("[rgaBingo] Material.matchMaterial(\"" + name + "\") returned null. " +
                "Falling back to enum constant: " + fallback.name());
        return fallback;
    }

    public static List<Material> getRandomMaterials(int count) {
        List<Material> pool = new ArrayList<>();
        pool.addAll(EASY_MATERIALS);
        pool.addAll(NORMAL_MATERIALS);
        pool.addAll(HARD_MATERIALS);

        Collections.shuffle(pool);

        Set<Material> selected = new LinkedHashSet<>();
        for (Material mat : pool) {
            if (mat != null) {
                selected.add(mat);
                if (selected.size() >= count) {
                    break;
                }
            }
        }
        return new ArrayList<>(selected);
    }
}
