package org.system.mailBox; // 请确保与你的文件夹名和其它文件一致

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final JavaPlugin plugin;
    private File dataFile;
    public FileConfiguration dataConfig;
//初始创建
    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDataFile();
    }
//也是初始创建
    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            plugin.saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 信箱相关 ---
    public void setMailBoxLocation(UUID playerUUID, Location loc) {
        String path = "players." + playerUUID.toString() + ".mailbox";
        dataConfig.set(path + ".world", loc.getWorld().getName());
        dataConfig.set(path + ".x", loc.getX());
        dataConfig.set(path + ".y", loc.getY());
        dataConfig.set(path + ".z", loc.getZ());
        saveData();
    }

    public Location getMailBoxLocation(UUID playerUUID) {
        String path = "players." + playerUUID.toString() + ".mailbox";
        if (!dataConfig.contains(path)) return null;

        String worldName = dataConfig.getString(path + ".world");
        double x = dataConfig.getDouble(path + ".x");
        double y = dataConfig.getDouble(path + ".y");
        double z = dataConfig.getDouble(path + ".z");

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(world, x, y, z);
    }

    public boolean hasMailBox(UUID playerUUID) {
        return dataConfig.contains("players." + playerUUID.toString() + ".mailbox");
    }
//删除信箱
    public void removeMailBox(UUID playerUUID) {
        dataConfig.set("players." + playerUUID.toString() + ".mailbox", null);
        saveData();
    }
//神秘地无效代码
    public void addMailToBox(UUID playerUUID, ItemStack shulkerBox) {
        String path = "players." + playerUUID.toString() + ".mails";
        List<ItemStack> mails = getMails(playerUUID);
        mails.add(shulkerBox);
        dataConfig.set(path, mails);
        saveData();
    }

    public List<ItemStack> getMails(UUID playerUUID) {
        String path = "players." + playerUUID.toString() + ".mails";
        List<?> rawList = dataConfig.getList(path);
        List<ItemStack> mails = new ArrayList<>();

        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof ItemStack) {
                    mails.add((ItemStack) obj);
                }
            }
        }
        return mails;
    }

    public void clearMails(UUID playerUUID) {
        dataConfig.set("players." + playerUUID.toString() + ".mails", null);
        saveData();
    }

    // --- 暂存箱 (Bag) 相关 ---
    public void setBagContents(UUID playerUUID, ItemStack[] contents) {
        String path = "players." + playerUUID.toString() + ".bag_contents";
        dataConfig.set(path, contents);
        saveData();
    }
//以玩家的UUD构建一个字符串
    public ItemStack[] getBagContents(UUID playerUUID) {
        String path = "players." + playerUUID.toString() + ".bag_contents";
        List<?> rawList = dataConfig.getList(path);
        if (rawList == null || rawList.isEmpty()) {
            // 返回一个26个槽位都为空的数组
            return new ItemStack[26];
        }
        ItemStack[] contents = new ItemStack[26];
        for (int i = 0; i < Math.min(rawList.size(), 26); i++) {
            Object obj = rawList.get(i);
            if (obj instanceof ItemStack) {
                contents[i] = (ItemStack) obj;
            }
        }
        return contents;
    }
}