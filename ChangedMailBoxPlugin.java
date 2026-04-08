package org.system.changedMailBox;

import org.bukkit.plugin.java.JavaPlugin;

public class ChangedMailBoxPlugin extends JavaPlugin {

    private DataManager dataManager;
    private MailBoxCommandExecutor commandExecutor;
    private MailBoxListener listener;

    @Override
    public void onEnable() {
        // 初始化数据管理器
        dataManager = new DataManager(this);
        dataManager.reloadConfig(); // 加载现有数据

        // 初始化命令执行器和监听器
        commandExecutor = new MailBoxCommandExecutor(this);
        listener = new MailBoxListener(this);

        // 注册命令
        getCommand("setmailbox").setExecutor(commandExecutor);
        getCommand("bag").setExecutor(commandExecutor);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(listener, this);

        getLogger().info("changedMailBox 插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("changedMailBox 插件已禁用！");
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}