package org.system.changedMailBox;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MailBoxCommandExecutor implements CommandExecutor {

    private final ChangedMailBoxPlugin plugin;

    public MailBoxCommandExecutor(ChangedMailBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行。");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("setmailbox")) {
            if (args.length != 3) {
                player.sendMessage(ChatColor.RED + "用法: /setmailbox <x> <y> <z>");
                return true;
            }

            double x, y, z;
            try {
                x = Double.parseDouble(args[0]);
                y = Double.parseDouble(args[1]);
                z = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "坐标必须是数字。");
                return true;
            }

            // 构建一个在世界0,0,0处的位置，需要玩家手动右键点击以验证
            // 实际上，我们应获取玩家所在的世界
            Location loc = new Location(player.getWorld(), x, y, z);

            if (loc.getBlock().getType() == Material.CHEST) {
                System.out.println(loc.getBlock().getType());
                plugin.getDataManager().setMailBoxLocation(player.getUniqueId(), loc);
                player.sendMessage(ChatColor.GREEN + "邮箱绑定成功！");
            } else {
                player.sendMessage(ChatColor.RED + "该位置不是一个箱子。");
            }
            return true;
        }

        else if (command.getName().equalsIgnoreCase("bag")) {
            // 打开暂存箱GUI
            openBagGUI(player);
            return true;
        }

        return false;
    }

    private void openBagGUI(Player player) {
        Inventory bagInv = Bukkit.createInventory(player, 27, "物品暂存箱");

        // 加载玩家上次保存的物品
        ItemStack[] savedItems = plugin.getDataManager().getBagContents(player.getUniqueId());
        for (int i = 0; i < 27; i++) {
            if (i < savedItems.length && savedItems[i] != null) {
                bagInv.setItem(i, savedItems[i]);
            }
        }

        // 在右下角（索引26）放置发送图腾
        org.bukkit.inventory.ItemStack totem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING);
        var meta = totem.getItemMeta();
        meta.setDisplayName("发送到邮箱");
        // 防止被取出的技巧：使用NBT标签或设置为“不可交互”，这里简单设置为无耐久度的头盔作为占位符
        // 更好的方式是在监听器中处理，不让它被移动
        totem.setItemMeta(meta);
        bagInv.setItem(26, totem);

        player.openInventory(bagInv);
    }
}