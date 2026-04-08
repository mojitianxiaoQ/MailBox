package org.system.changedMailBox;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final ChangedMailBoxPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(ChangedMailBoxPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadConfig() {
        if (this.dataConfig == null) {
            this.dataFile = new File(this.plugin.getDataFolder(), "data.yml");
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null) {
            reloadConfig();
        }
        return this.dataConfig;
    }

    public void saveConfig() {
        if (this.dataConfig == null || this.dataFile == null) {
            return;
        }
        try {
            this.dataConfig.save(this.dataFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("无法保存配置文件: " + this.dataFile);
            e.printStackTrace();
        }
    }

    // --- 邮箱位置管理 ---
    public void setMailBoxLocation(UUID playerUUID, Location location) {
        String path = "players." + playerUUID + ".mailbox_location";
        getConfig().set(path, location);
        saveConfig();
    }

    public Location getMailBoxLocation(UUID playerUUID) {
        String path = "players." + playerUUID + ".mailbox_location";
        return (Location) getConfig().get(path);
    }

    public void removeMailBox(UUID playerUUID) {
        String path = "players." + playerUUID + ".mailbox_location";
        getConfig().set(path, null);
        saveConfig();
    }

    // --- Bag (暂存箱) 物品管理 ---
    public void setBagContents(UUID playerUUID, ItemStack[] contents) {
        String basePath = "data." + playerUUID + ".type.bag.";
        for (int i = 0; i < contents.length; i++) {
            getConfig().set(basePath + (i + 1), contents[i]);
        }
        saveConfig();
    }

    public ItemStack[] getBagContents(UUID playerUUID) {
        String basePath = "data." + playerUUID + ".type.bag.";
        List<ItemStack> items = new ArrayList<>();
        for (int i = 1; i <= 27; i++) { // 暂存箱是27格
            ItemStack item = (ItemStack) getConfig().get(basePath + i);
            items.add(item);
        }
        return items.toArray(new ItemStack[0]);
    }

    // --- Mailbox (邮箱) 物品管理 ---
    public void setMailBoxContents(UUID playerUUID, int boxIndex, ItemStack[] contents) {
        int startIndex = (boxIndex - 1) * 27 + 1; // 计算在总列表中的起始序号
        String basePath = "data." + playerUUID + ".type.mailbox.";
        for (int i = 0; i < contents.length; i++) {
            getConfig().set(basePath + (startIndex + i), contents[i]);
        }
        saveConfig();
    }

    public ItemStack[] getMailBoxContents(UUID playerUUID, int boxIndex) {
        int startIndex = (boxIndex - 1) * 27 + 1;
        int endIndex = startIndex + 26;
        String basePath = "data." + playerUUID + ".type.mailbox.";

        List<ItemStack> items = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            ItemStack item = (ItemStack) getConfig().get(basePath + i);
            items.add(item);
        }
        return items.toArray(new ItemStack[0]);
    }
}