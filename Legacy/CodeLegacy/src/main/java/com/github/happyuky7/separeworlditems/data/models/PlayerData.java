package com.github.happyuky7.separeworlditems.data.models;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * This class holds all the relevant data for a player.
 */
public class PlayerData {

    private GameMode gameMode;
    private boolean isFlying;
    private double health;
    private int hunger;
    private float experience;
    private int expLevel;
    private List<PotionEffect> potionEffects;
    private ItemStack[] inventory;
    private ItemStack[] enderChest;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack offHandItem;

    /**
     * Constructor to initialize all fields.
     *
     * @param gameMode      The player's game mode.
     * @param isFlying      Whether the player is flying.
     * @param health        The player's health.
     * @param hunger        The player's hunger level.
     * @param experience    The player's experience.
     * @param expLevel      The player's experience level.
     * @param potionEffects The player's active potion effects.
     * @param inventory     The player's inventory contents.
     * @param enderChest    The player's ender chest contents.
     * @param helmet        The player's helmet item.
     * @param chestplate    The player's chestplate item.
     * @param leggings      The player's leggings item.
     * @param boots         The player's boots item.
     * @param offHandItem   The player's off-hand item.
     */
    public PlayerData(GameMode gameMode, boolean isFlying, double health, int hunger, float experience, int expLevel,
                      List<PotionEffect> potionEffects, ItemStack[] inventory, ItemStack[] enderChest,
                      ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots, ItemStack offHandItem) {
        this.gameMode = gameMode;
        this.isFlying = isFlying;
        this.health = health;
        this.hunger = hunger;
        this.experience = experience;
        this.expLevel = expLevel;
        this.potionEffects = potionEffects;
        this.inventory = inventory;
        this.enderChest = enderChest;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
        this.offHandItem = offHandItem;
    }

    // Getters and Setters

    /**
     * Gets the player's game mode.
     *
     * @return The player's game mode.
     */
    public GameMode getGameMode() {
        return gameMode;
    }

    /**
     * Sets the player's game mode.
     *
     * @param gameMode The player's new game mode.
     */
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * Checks if the player is flying.
     *
     * @return True if the player is flying, otherwise false.
     */
    public boolean isFlying() {
        return isFlying;
    }

    /**
     * Sets the player's flying state.
     *
     * @param flying The player's new flying state.
     */
    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    /**
     * Gets the player's health.
     *
     * @return The player's health.
     */
    public double getHealth() {
        return health;
    }

    /**
     * Sets the player's health.
     *
     * @param health The player's new health.
     */
    public void setHealth(double health) {
        this.health = health;
    }

    /**
     * Gets the player's hunger level.
     *
     * @return The player's hunger level.
     */
    public int getHunger() {
        return hunger;
    }

    /**
     * Sets the player's hunger level.
     *
     * @param hunger The player's new hunger level.
     */
    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    /**
     * Gets the player's experience.
     *
     * @return The player's experience.
     */
    public float getExperience() {
        return experience;
    }

    /**
     * Sets the player's experience.
     *
     * @param experience The player's new experience.
     */
    public void setExperience(float experience) {
        this.experience = experience;
    }

    /**
     * Gets the player's experience level.
     *
     * @return The player's experience level.
     */
    public int getExpLevel() {
        return expLevel;
    }

    /**
     * Sets the player's experience level.
     *
     * @param expLevel The player's new experience level.
     */
    public void setExpLevel(int expLevel) {
        this.expLevel = expLevel;
    }

    /**
     * Gets the player's active potion effects.
     *
     * @return A list of the player's active potion effects.
     */
    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }

    /**
     * Sets the player's active potion effects.
     *
     * @param potionEffects The player's new active potion effects.
     */
    public void setPotionEffects(List<PotionEffect> potionEffects) {
        this.potionEffects = potionEffects;
    }

    /**
     * Gets the player's inventory contents.
     *
     * @return The player's inventory contents.
     */
    public ItemStack[] getInventory() {
        return inventory;
    }

    /**
     * Sets the player's inventory contents.
     *
     * @param inventory The player's new inventory contents.
     */
    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets the player's ender chest contents.
     *
     * @return The player's ender chest contents.
     */
    public ItemStack[] getEnderChest() {
        return enderChest;
    }

    /**
     * Sets the player's ender chest contents.
     *
     * @param enderChest The player's new ender chest contents.
     */
    public void setEnderChest(ItemStack[] enderChest) {
        this.enderChest = enderChest;
    }

    /**
     * Gets the player's helmet item.
     *
     * @return The player's helmet item.
     */
    public ItemStack getHelmet() {
        return helmet;
    }

    /**
     * Sets the player's helmet item.
     *
     * @param helmet The player's new helmet item.
     */
    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    /**
     * Gets the player's chestplate item.
     *
     * @return The player's chestplate item.
     */
    public ItemStack getChestplate() {
        return chestplate;
    }

    /**
     * Sets the player's chestplate item.
     *
     * @param chestplate The player's new chestplate item.
     */
    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    /**
     * Gets the player's leggings item.
     *
     * @return The player's leggings item.
     */
    public ItemStack getLeggings() {
        return leggings;
    }

    /**
     * Sets the player's leggings item.
     *
     * @param leggings The player's new leggings item.
     */
    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    /**
     * Gets the player's boots item.
     *
     * @return The player's boots item.
     */
    public ItemStack getBoots() {
        return boots;
    }

    /**
     * Sets the player's boots item.
     *
     * @param boots The player's new boots item.
     */
    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    /**
     * Gets the player's off-hand item.
     *
     * @return The player's off-hand item.
     */
    public ItemStack getOffHandItem() {
        return offHandItem;
    }

    /**
     * Sets the player's off-hand item.
     *
     * @param offHandItem The player's new off-hand item.
     */
    public void setOffHandItem(ItemStack offHandItem) {
        this.offHandItem = offHandItem;
    }
}
