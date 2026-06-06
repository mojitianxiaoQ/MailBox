package org.system.changedMailBox;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChangedMailBoxPlugin extends JavaPlugin {

    private DataManager dataManager;
    private MailBoxCommandExecutor commandExecutor;
    private MailBoxListener listener;
    private final Set<UUID> pendingBinding = new HashSet<>();

    @Override
    public void onEnable() {
        dataManager = new DataManager(this);
        dataManager.reloadConfig();

        commandExecutor = new MailBoxCommandExecutor(this);
        listener = new MailBoxListener(this);

        registerCommand("setmailbox", commandExecutor);
        registerCommand("bag", commandExecutor);
        registerCommand("mail", commandExecutor);

        getServer().getPluginManager().registerEvents(listener, this);

        dataManager.startAutoSave();

        getLogger().info("changedMailBox 插件已启用！");
    }

    private void registerCommand(String name, CommandExecutor executor) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        } else {
            getLogger().severe("命令 '" + name + "' 未在 plugin.yml 中注册，跳过！");
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory openInv = player.getOpenInventory().getTopInventory();
            if (openInv != null) {
                String title = player.getOpenInventory().getTitle();
                if (title.startsWith("邮箱 #")) {
                    try {
                        int boxIndex = Integer.parseInt(title.substring(title.lastIndexOf('#') + 1));
                        dataManager.setMailBoxContents(player.getUniqueId(), boxIndex, openInv.getContents());
                    } catch (NumberFormatException ignored) {
                    }
                } else if ("物品暂存箱".equals(title)) {
                    ItemStack[] contents = new ItemStack[27];
                    for (int i = 0; i < 27; i++) {
                        contents[i] = openInv.getItem(i);
                    }
                    if (contents[26] != null && contents[26].getType() == Material.TOTEM_OF_UNDYING) {
                        contents[26] = null;
                    }
                    dataManager.setBagContents(player.getUniqueId(), contents);
                }
            }
        }
        if (dataManager != null) {
            dataManager.saveConfig();
        }
        getLogger().info("changedMailBox 插件已禁用！");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public boolean isPendingBinding(UUID uuid) {
        return pendingBinding.contains(uuid);
    }

    public void addPendingBinding(UUID uuid) {
        pendingBinding.add(uuid);
    }

    public void removePendingBinding(UUID uuid) {
        pendingBinding.remove(uuid);
    }
}