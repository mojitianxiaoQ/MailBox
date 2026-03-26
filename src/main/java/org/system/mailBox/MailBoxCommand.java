package org.system.mailBox; // 请确保与你的文件夹名和其它文件一致

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class MailBoxCommand implements CommandExecutor {

    private final MailBoxPlugin plugin;

    public MailBoxCommand(MailBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令。");
            return true;
        }

        Player player = (Player) sender;
        DataManager dataManager = plugin.getDataManager();

        if (label.equalsIgnoreCase("setmailbox")) {
            if (args.length != 3) {
                player.sendMessage("§c使用方法: /setmailbox <x> <y> <z>");
                return true;
            }

            int x, y, z;
            try {
                x = Integer.parseInt(args[0]);
                y = Integer.parseInt(args[1]);
                z = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c坐标必须是数字！");
                return true;
            }

            var loc = player.getLocation().clone();
            loc.setX(x);
            loc.setY(y);
            loc.setZ(z);

            if (loc.getBlock().getType() != Material.CHEST) {
                player.sendMessage("§c该坐标处没有箱子！");
                return true;
            }

            if (dataManager.hasMailBox(player.getUniqueId())) {
                player.sendMessage("§c你已经绑定过一个信箱了。");
                return true;
            }

            dataManager.setMailBoxLocation(player.getUniqueId(), loc);
            player.sendMessage("§a成功绑定信箱。");
            return true;
        }

        if (label.equalsIgnoreCase("bag")) {
            // 打开GUI时，从数据管理器加载内容
            Inventory gui = MailBoxGUI.createBagGUI(player, dataManager.getBagContents(player.getUniqueId()));
            player.openInventory(gui);
            return true;
        }

        return false;
    }
}