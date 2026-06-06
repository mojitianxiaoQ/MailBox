package org.system.changedMailBox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final ChangedMailBoxPlugin plugin;
    private final Object saveLock = new Object();
    private volatile boolean dirty = false;
    private volatile File dataFile;
    private volatile FileConfiguration dataConfig;

    public DataManager(ChangedMailBoxPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadConfig() {
        synchronized (saveLock) {
            if (this.dataFile == null) {
                this.dataFile = new File(this.plugin.getDataFolder(), "data.yml");
            }
            if (!this.dataFile.exists()) {
                this.plugin.getDataFolder().mkdirs();
            }
            this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
        }
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null) {
            synchronized (saveLock) {
                if (this.dataConfig == null) {
                    reloadConfig();
                }
            }
        }
        return this.dataConfig;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void saveConfig() {
        if (!dirty) {
            return;
        }
        if (this.dataConfig == null || this.dataFile == null) {
            return;
        }
        synchronized (saveLock) {
            if (!dirty) {
                return;
            }
            try {
                File tempFile = new File(dataFile.getParentFile(), "data.yml.tmp");
                this.dataConfig.save(tempFile);

                if (dataFile.exists()) {
                    File backupFile = new File(dataFile.getParentFile(), "data.yml.bak");
                    Files.move(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.dirty = false;
            } catch (IOException e) {
                this.plugin.getLogger().severe("数据保存失败: " + e.getMessage());
            }
        }
    }

    public void startAutoSave() {
        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::saveConfig,
                6000L,
                6000L
        );
    }

    public void setMailBoxLocation(UUID playerUUID, Location location) {
        String path = "players." + playerUUID + ".mailbox_location";
        getConfig().set(path, location);
        markDirty();
    }

    public Location getMailBoxLocation(UUID playerUUID) {
        String path = "players." + playerUUID + ".mailbox_location";
        return (Location) getConfig().get(path);
    }

    public void removeMailBox(UUID playerUUID) {
        String path = "players." + playerUUID + ".mailbox_location";
        getConfig().set(path, null);
        markDirty();
    }

    public void setBagContents(UUID playerUUID, ItemStack[] contents) {
        String basePath = "data." + playerUUID + ".type.bag.";
        for (int i = 0; i < contents.length; i++) {
            getConfig().set(basePath + (i + 1), contents[i]);
        }
        markDirty();
    }

    public ItemStack[] getBagContents(UUID playerUUID) {
        String basePath = "data." + playerUUID + ".type.bag.";
        List<ItemStack> items = new ArrayList<>();
        for (int i = 1; i <= 27; i++) {
            ItemStack item = (ItemStack) getConfig().get(basePath + i);
            items.add(item);
        }
        return items.toArray(new ItemStack[0]);
    }

    public void setMailBoxContents(UUID playerUUID, int boxIndex, ItemStack[] contents) {
        if (boxIndex < 1 || boxIndex > 9) {
            return;
        }
        int startIndex = (boxIndex - 1) * 27 + 1;
        String basePath = "data." + playerUUID + ".type.mailbox.";
        for (int i = 0; i < contents.length; i++) {
            getConfig().set(basePath + (startIndex + i), contents[i]);
        }
        markDirty();
    }

    public ItemStack[] getMailBoxContents(UUID playerUUID, int boxIndex) {
        if (boxIndex < 1 || boxIndex > 9) {
            return new ItemStack[0];
        }
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

    public int getUnlockedMailboxCount(UUID playerUUID) {
        String path = "players." + playerUUID + ".unlocked_mailboxes";
        int count = getConfig().getInt(path);
        return count > 0 ? count : 1;
    }

    public void setUnlockedMailboxCount(UUID playerUUID, int count) {
        String path = "players." + playerUUID + ".unlocked_mailboxes";
        getConfig().set(path, count);
        markDirty();
    }

    public boolean isMailBoxUnlocked(UUID playerUUID, int boxIndex) {
        return boxIndex <= getUnlockedMailboxCount(playerUUID);
    }
}
