package org.system.mailBox;

import org.bukkit.plugin.java.JavaPlugin;

public class MailBoxPlugin extends JavaPlugin {

    private DataManager dataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataManager = new DataManager(this);

        getCommand("setmailbox").setExecutor(new MailBoxCommand(this));
        getCommand("bag").setExecutor(new MailBoxCommand(this));
        // 注册 MailBoxListener
        getServer().getPluginManager().registerEvents(new MailBoxListener(this), this);

        getLogger().info("MailBoxPlugin 已启用");
    }

    @Override
    public void onDisable() {
        dataManager.saveData();
        getLogger().info("MailBoxPlugin 已关闭");
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}