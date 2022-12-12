package com.github.happyrogelio7.separeworlditems.listeners;

/*
 * Code by: HappyRogelio7
 * Github: https://github.com/happyrogelio7
 * License: Custom
 * Link: https://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS
 */

import com.github.happyrogelio7.separeworlditems.SepareWorldItems;
import com.github.happyrogelio7.separeworlditems.filemanagers.FileManager2;
import com.github.happyrogelio7.separeworlditems.utils.MessageColors;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;

public class WorldChangeEvent implements Listener {

    private SepareWorldItems plugin;

    public WorldChangeEvent(SepareWorldItems plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event){

        Player p = event.getPlayer();

            if (plugin.playerlist1.contains(p.getUniqueId())) {

                if (plugin.getConfig().getBoolean("Options.bypass-world-options.use_bypass", true)) {

                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-warning-alert")));
                    return;
                }
                plugin.playerlist1.remove(p.getUniqueId());
            }


        String from = event.getFrom().getName();
        String to = event.getPlayer().getWorld().getName();

        FileConfiguration config1 = plugin.getConfig();

        if (config1.contains("worlds." + from) && config1.contains("worlds." + to)){

            String fromGroup = config1.getString("worlds." + from);
            String toGroup = config1.getString("worlds." + to);

            if (!fromGroup.equals(toGroup)){

                File fromFile = new File(plugin.getDataFolder() + File.separator + "groups"
                        + File.separator + fromGroup + File.separator + p.getName() + "-" + p.getUniqueId() + ".yml");

                FileConfiguration fromConfig = FileManager2.getYaml(fromFile);

                int pos = 0;
                for (ItemStack icontains : p.getInventory().getContents()){
                    fromConfig.set("inventory." + pos, icontains);
                    pos++;
                }

                if (plugin.getConfig().getBoolean("Options.ender-chest", true)){
                    pos = 0;
                    for (ItemStack is : p.getEnderChest().getContents()) {
                        fromConfig.set("ender_chest." + pos, is);
                        pos++;
                    }
                }

                fromConfig.set("armor_contents.helmet", p.getInventory().getHelmet());
                fromConfig.set("armor_contents.chestplate", p.getInventory().getChestplate());
                fromConfig.set("armor_contents.leggings", p.getInventory().getLeggings());
                fromConfig.set("armor_contents.boots", p.getInventory().getBoots());

                p.getInventory().setHelmet(new ItemStack(Material.AIR));
                p.getInventory().setChestplate(new ItemStack(Material.AIR));
                p.getInventory().setLeggings(new ItemStack(Material.AIR));
                p.getInventory().setBoots(new ItemStack(Material.AIR));

                p.getInventory().clear();
                p.getEnderChest().clear();

                if (plugin.getConfig().getBoolean("Options.gamemode", true)){
                    fromConfig.set("gamemode", p.getGameMode().toString());
                    p.setGameMode(GameMode.SURVIVAL);
                }

                if (plugin.getConfig().getBoolean("Options.flying", true)) {
                    fromConfig.set("flying", p.isFlying());
                    p.setFlying(false);
                }

                pos = 0;
                fromConfig.set("potion_effect", null);

                for (PotionEffect pe : p.getActivePotionEffects()) {

                    fromConfig.set("potion_effect." + pos + ".type", pe.getType().getName());
                    fromConfig.set("potion_effect." + pos + ".level", Integer.valueOf(pe.getAmplifier()));
                    fromConfig.set("potion_effect." + pos + ".duration", Integer.valueOf(pe.getDuration()));

                    p.removePotionEffect(pe.getType());
                    pos++;

                }

                if (plugin.getConfig().getBoolean("Options.health-options.health-default-save", true)) {
                    fromConfig.set("health", p.getHealth());
                }

                fromConfig.set("hunger", Integer.valueOf(p.getFoodLevel()));
                fromConfig.set("exp", Float.valueOf(p.getExp()));
                fromConfig.set("exp-level", Integer.valueOf(p.getLevel()));


                ///////////////////////////////////

                FileManager2.saveConfiguraton(fromFile, fromConfig);

                File toFile = new File(plugin.getDataFolder() + File.separator + "groups"
                        + File.separator + toGroup + File.separator + p.getName() + "-" + p.getUniqueId() + ".yml");

                FileConfiguration toConfig = FileManager2.getYaml(toFile);

                if (toConfig.contains("inventory"))

                    for (String s : toConfig.getConfigurationSection("inventory").getKeys(false))

                        p.getInventory().setItem(Integer.parseInt(s), toConfig.getItemStack("inventory." + s));

                if (plugin.getConfig().getBoolean("Options.ender-chest", true)){
                    if (toConfig.contains("ender_chest"))
                    for (String se : toConfig.getConfigurationSection("ender_chest").getKeys(false)){
                        p.getEnderChest().setItem(Integer.parseInt(se), toConfig.getItemStack("ender_chest." + se));
                    }
                }

                if (toConfig.contains("potion_effect"))
                    for (String s : toConfig.getConfigurationSection("potion_effect").getKeys(false)) {

                        PotionEffect effect = new PotionEffect(PotionEffectType.getByName(toConfig.getString(
                                "potion_effect." + s + ".type")), toConfig.getInt("potion_effect." + s +
                                ".duration"), toConfig.getInt("potion_effect." + s + ".level"));

                        p.addPotionEffect(effect);

                    }

                if (toConfig.contains("armor_contents")) {

                    p.getInventory().setHelmet(toConfig.getItemStack("armor_contents.helmet"));
                    p.getInventory().setChestplate(toConfig.getItemStack("armor_contents.chestplate"));
                    p.getInventory().setLeggings(toConfig.getItemStack("armor_contents.leggings"));
                    p.getInventory().setBoots(toConfig.getItemStack("armor_contents.boots"));

                }

                if (plugin.getConfig().getBoolean("Options.gamemode", true)){

                    if (toConfig.contains("gamemode")) {

                        p.setGameMode(GameMode.valueOf(toConfig.getString("gamemode")));//GameMode.valueOf(toConfig.getString("gamemode")

                    }else{

                        p.setGameMode(GameMode.SURVIVAL);

                    }
                }

                if (plugin.getConfig().getBoolean("Options.flying", true)){

                    if (toConfig.contains("flying")) {

                        p.setFlying(toConfig.getBoolean("flying"));

                    }else{

                        p.setFlying(false);

                    }

                }

                if (plugin.getConfig().getBoolean("Options.health-options.health-default-save", true)) {
                    if (toConfig.contains("health")) {

                        p.setHealth(toConfig.getDouble("health"));

                    } else {

                        p.setHealth(20.0D);

                    }
                }

                if (toConfig.contains("hunger")) {

                    p.setFoodLevel(toConfig.getInt("hunger"));

                } else {

                    p.setFoodLevel(20);

                }

                if (toConfig.contains("exp") && !toConfig.contains("exp-level")) {

                    int total = toConfig.getInt("exp");

                    p.setTotalExperience(total);
                    p.setLevel(0);
                    p.setExp(0.0F);

                    while (total > p.getExpToLevel()) {

                        total -= p.getExpToLevel();
                        p.setLevel(p.getLevel() + 1);

                    }

                    float xp = (total / p.getExpToLevel());

                    p.setExp(xp);

                } else if (toConfig.contains("exp-level") && toConfig.contains("exp")) {

                    p.setExp((float)toConfig.getDouble("exp"));
                    p.setLevel(toConfig.getInt("exp-level"));

                } else {

                    p.setExp(0.0F);
                    p.setLevel(0);

                }
            }
        }

    }


}
