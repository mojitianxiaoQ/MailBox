package org.system.changedMailBox;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
            plugin.addPendingBinding(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "请右键点击一个箱子来绑定为你的邮箱。");
            return true;
        }

        else if (command.getName().equalsIgnoreCase("bag")) {
            openBagGUI(player);
            return true;
        }

        else if (command.getName().equalsIgnoreCase("mail")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "用法: /mail unlock");
                return true;
            }

            if (args[0].equalsIgnoreCase("unlock")) {
                handleUnlock(player);
            } else {
                player.sendMessage(ChatColor.RED + "未知子命令。用法: /mail unlock");
            }
            return true;
        }

        return false;
    }

    private void handleUnlock(Player player) {
        int unlockedCount = plugin.getDataManager().getUnlockedMailboxCount(player.getUniqueId());

        if (unlockedCount >= 9) {
            player.sendMessage(ChatColor.GREEN + "所有邮箱已解锁！");
            return;
        }

        int targetBox = unlockedCount + 1;
        int cost = getUnlockCost(targetBox);

        if (!hasNetherStars(player, cost)) {
            player.sendMessage(ChatColor.RED + "下界之星不足！解锁 " + targetBox + " 号邮箱需要 " + cost + " 个下界之星。");
            return;
        }

        consumeNetherStars(player, cost);
        plugin.getDataManager().setUnlockedMailboxCount(player.getUniqueId(), targetBox);
        plugin.getDataManager().saveConfig();
        player.sendMessage(ChatColor.GREEN + "成功解锁 " + targetBox + " 号邮箱！消耗了 " + cost + " 个下界之星。");
    }

    private int getUnlockCost(int boxIndex) {
        if (boxIndex == 2) return 1;
        if (boxIndex == 3) return 2;
        if (boxIndex == 4) return 3;
        return 4;
    }

    private boolean hasNetherStars(Player player, int count) {
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHER_STAR) {
                found += item.getAmount();
                if (found >= count) return true;
            }
        }
        return false;
    }

    private void consumeNetherStars(Player player, int count) {
        int remaining = count;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHER_STAR) {
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        player.updateInventory();
    }

    private void openBagGUI(Player player) {
        Inventory bagInv = plugin.getServer().createInventory(player, 27, "物品暂存箱");

        ItemStack[] savedItems = plugin.getDataManager().getBagContents(player.getUniqueId());
        for (int i = 0; i < 27; i++) {
            if (i < savedItems.length && savedItems[i] != null) {
                bagInv.setItem(i, savedItems[i]);
            }
        }

        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        var meta = totem.getItemMeta();
        meta.setDisplayName("发送到邮箱");
        totem.setItemMeta(meta);
        bagInv.setItem(26, totem);

        player.openInventory(bagInv);
    }
}
