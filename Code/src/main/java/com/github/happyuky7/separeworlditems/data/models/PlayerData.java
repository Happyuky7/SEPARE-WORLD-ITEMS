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

    // Constructor to initialize all fields
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

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public int getHunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    public float getExperience() {
        return experience;
    }

    public void setExperience(float experience) {
        this.experience = experience;
    }

    public int getExpLevel() {
        return expLevel;
    }

    public void setExpLevel(int expLevel) {
        this.expLevel = expLevel;
    }

    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }

    public void setPotionEffects(List<PotionEffect> potionEffects) {
        this.potionEffects = potionEffects;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public ItemStack[] getEnderChest() {
        return enderChest;
    }

    public void setEnderChest(ItemStack[] enderChest) {
        this.enderChest = enderChest;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public ItemStack getOffHandItem() {
        return offHandItem;
    }

    public void setOffHandItem(ItemStack offHandItem) {
        this.offHandItem = offHandItem;
    }
}
