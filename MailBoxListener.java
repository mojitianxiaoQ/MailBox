package org.system.changedMailBox;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailBoxListener implements Listener {

    private final ChangedMailBoxPlugin plugin;
    private final NamespacedKey confirmKey;

    public MailBoxListener(ChangedMailBoxPlugin plugin) {
        this.plugin = plugin;
        this.confirmKey = new NamespacedKey(plugin, "confirm_send");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = e.getView().getTitle();

        // 处理暂存箱GUI
        if ("物品暂存箱".equals(title)) {
            // 阻止点击发送按钮（索引26）时的物品移动
            if (e.getSlot() == 26) {
                e.setCancelled(true);

                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null) return;

                if (clickedItem.getType() == Material.TOTEM_OF_UNDYING && "发送到邮箱".equals(meta.getDisplayName())) {
                    // 替换为确认星
                    ItemStack star = new ItemStack(Material.NETHER_STAR);
                    ItemMeta starMeta = star.getItemMeta();
                    starMeta.setDisplayName("再点一次确认");
                    starMeta.getPersistentDataContainer().set(confirmKey, PersistentDataType.INTEGER, 1);
                    star.setItemMeta(starMeta);
                    e.getInventory().setItem(26, star);
                    player.updateInventory();
                }
                else if (clickedItem.getType() == Material.NETHER_STAR &&
                        meta.getPersistentDataContainer().has(confirmKey, PersistentDataType.INTEGER)) {
                    // 执行发送操作
                    sendItemsToMailBox(player, e.getInventory());
                }
            }
        }
        // 处理邮箱选择器GUI
        else if ("邮箱选择".equals(title)) {
            e.setCancelled(true); // 阻止所有移动
            if (e.getSlot() >= 0 && e.getSlot() <= 8) { // 点击了1-9号箱子
                int boxIndex = e.getSlot() + 1;
                if (plugin.getDataManager().isMailBoxUnlocked(player.getUniqueId(), boxIndex)) {
                    openMailBoxInnerGUI(player, boxIndex);
                } else {
                    player.sendMessage(ChatColor.RED + "该邮箱尚未解锁！使用 /mail unlock 解锁。");
                    player.closeInventory();
                }
            }
        }
        // 处理具体邮箱GUI (1-9号箱)
        else if (title.startsWith("邮箱 #")) {
            // 允许玩家自由操作，不需要取消事件
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }

        String title = e.getView().getTitle();
        Inventory inv = e.getInventory();

        if ("物品暂存箱".equals(title)) {
            // 保存暂存箱物品
            ItemStack[] contents = new ItemStack[27];
            for (int i = 0; i < 27; i++) {
                contents[i] = inv.getItem(i);
            }
            // 保存时排除发送按钮
            if(contents[26] != null && contents[26].getType() == Material.TOTEM_OF_UNDYING){
                contents[26] = null; // 清空按钮位置
            }
            plugin.getDataManager().setBagContents(player.getUniqueId(), contents);
        }
        else if (title.startsWith("邮箱 #")) {
            try {
                int boxIndex = Integer.parseInt(title.substring(title.lastIndexOf('#') + 1));
                if (boxIndex < 1 || boxIndex > 9) {
                    plugin.getLogger().warning("无效的邮箱编号: " + title);
                    return;
                }
                ItemStack[] contents = inv.getContents();
                plugin.getDataManager().setMailBoxContents(player.getUniqueId(), boxIndex, contents);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("无法解析邮箱编号: " + title);
            }
        }
    }
//监听玩家打开邮箱的方法
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if (block != null) {
                Player player = e.getPlayer();
                if (player == null) return;

                if (plugin.isPendingBinding(player.getUniqueId())) {
                    e.setCancelled(true);
                    if (block.getType() == Material.CHEST) {
                        plugin.getDataManager().setMailBoxLocation(player.getUniqueId(), block.getLocation());
                        plugin.getDataManager().saveConfig();
                        plugin.removePendingBinding(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "邮箱绑定成功！");
                    } else {
                        player.sendMessage(ChatColor.RED + "该方块不是箱子，请右键点击一个箱子。");
                    }
                    return;
                }

                if (block.getType() == Material.CHEST) {
                    Location clickedLoc = block.getLocation();
                    Location mailboxLoc = plugin.getDataManager().getMailBoxLocation(player.getUniqueId());

                    if (mailboxLoc != null && mailboxLoc.equals(clickedLoc)) {
                        e.setCancelled(true);
                        openMailBoxSelectorGUI(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.CHEST) return;

        Location brokenLoc = block.getLocation();
        if (!plugin.getDataManager().getConfig().isConfigurationSection("players")) {
            return;
        }
        for (String playerUUID : plugin.getDataManager().getConfig().getConfigurationSection("players").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(playerUUID);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("无效的UUID格式: " + playerUUID);
                continue;
            }
            Location mailboxLoc = plugin.getDataManager().getMailBoxLocation(uuid);
            if (mailboxLoc != null && mailboxLoc.equals(brokenLoc)) {
                plugin.getDataManager().removeMailBox(uuid);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(ChatColor.YELLOW + "你的邮箱箱子已被破坏，绑定已解除。");
                }
                return;
            }
        }
    }

    private void sendItemsToMailBox(Player player, Inventory bagInv) {
        List<ItemStack> itemsToSend = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            ItemStack item = bagInv.getItem(i);
            if (item != null) {
                itemsToSend.add(item.clone());
            }
        }

        if (itemsToSend.isEmpty()) {
            player.sendMessage(ChatColor.RED + "没有物品可以发送。");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        int unlockedCount = plugin.getDataManager().getUnlockedMailboxCount(playerUUID);
        ItemStack[][] snapshots = new ItemStack[unlockedCount][];
        int totalEmptySlots = 0;

        for (int boxIndex = 1; boxIndex <= unlockedCount; boxIndex++) {
            ItemStack[] contents = plugin.getDataManager().getMailBoxContents(playerUUID, boxIndex);
            snapshots[boxIndex - 1] = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    snapshots[boxIndex - 1][i] = contents[i].clone();
                }
            }
            for (ItemStack content : contents) {
                if (content == null) {
                    totalEmptySlots++;
                }
            }
        }

        if (totalEmptySlots < itemsToSend.size()) {
            player.sendMessage(ChatColor.RED + "发送失败，邮箱空间不足。当前空格数：" + totalEmptySlots + "，需要空格数：" + itemsToSend.size());
            return;
        }

        int itemsPlaced = 0;
        for (int boxIndex = 0; boxIndex < unlockedCount && itemsPlaced < itemsToSend.size(); boxIndex++) {
            ItemStack[] currentBoxContents = snapshots[boxIndex];
            for (int slotIndex = 0; slotIndex < 27 && itemsPlaced < itemsToSend.size(); slotIndex++) {
                if (currentBoxContents[slotIndex] == null) {
                    currentBoxContents[slotIndex] = itemsToSend.get(itemsPlaced).clone();
                    itemsPlaced++;
                }
            }
        }

        for (int boxIndex = 0; boxIndex < unlockedCount; boxIndex++) {
            plugin.getDataManager().setMailBoxContents(playerUUID, boxIndex + 1, snapshots[boxIndex]);
        }
        plugin.getDataManager().saveConfig();

        for (int i = 0; i < 26; i++) {
            bagInv.setItem(i, null);
        }
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        meta.setDisplayName("发送到邮箱");
        totem.setItemMeta(meta);
        bagInv.setItem(26, totem);

        player.sendMessage(ChatColor.GREEN + "发送成功！共发送了 " + itemsPlaced + " 个物品。");
        player.updateInventory();
    }

    private void openMailBoxSelectorGUI(Player player) {
        Inventory selectorInv = Bukkit.createInventory(player, 9, "邮箱选择");
        int unlockedCount = plugin.getDataManager().getUnlockedMailboxCount(player.getUniqueId());
        for (int i = 0; i < 9; i++) {
            int boxNumber = i + 1;
            ItemStack item;
            ItemMeta meta;
            if (boxNumber <= unlockedCount) {
                item = new ItemStack(Material.CHEST);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + String.valueOf(boxNumber));
            } else {
                item = new ItemStack(Material.BARRIER);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + String.valueOf(boxNumber) + " - 未解锁");
            }
            item.setItemMeta(meta);
            selectorInv.setItem(i, item);
        }
        player.openInventory(selectorInv);
    }

    private void openMailBoxInnerGUI(Player player, int boxIndex) {
        Inventory innerInv = Bukkit.createInventory(player, 27, "邮箱 #" + boxIndex);
        ItemStack[] contents = plugin.getDataManager().getMailBoxContents(player.getUniqueId(), boxIndex);
        innerInv.setContents(contents);
        player.openInventory(innerInv);
    }
}