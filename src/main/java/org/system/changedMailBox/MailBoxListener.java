package org.system.changedMailBox;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailBoxListener implements Listener {

    private final ChangedMailBoxPlugin plugin;
    private static final org.bukkit.NamespacedKey CONFIRM_KEY = new org.bukkit.NamespacedKey(
            JavaPlugin.getProvidingPlugin(ChangedMailBoxPlugin.class), "confirm_send");

    public MailBoxListener(ChangedMailBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        Player player = (Player) e.getWhoClicked();

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
                    starMeta.getPersistentDataContainer().set(CONFIRM_KEY, PersistentDataType.INTEGER, 1);
                    star.setItemMeta(starMeta);
                    e.getInventory().setItem(26, star);
                    player.updateInventory();
                }
                else if (clickedItem.getType() == Material.NETHER_STAR &&
                        meta.getPersistentDataContainer().has(CONFIRM_KEY, PersistentDataType.INTEGER)) {
                    // 执行发送操作
                    sendItemsToMailBox(player, e.getInventory());
                }
            }
            // 阻止移动发送图腾
            else if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.TOTEM_OF_UNDYING) {
                if(e.getCurrentItem().getItemMeta() != null && "发送到邮箱".equals(e.getCurrentItem().getItemMeta().getDisplayName())){
                    e.setCancelled(true);
                }
            }
        }
        // 处理邮箱选择器GUI
        else if ("邮箱选择".equals(title)) {
            e.setCancelled(true); // 阻止所有移动
            if (e.getSlot() >= 0 && e.getSlot() <= 8) { // 点击了1-9号箱子
                int boxIndex = e.getSlot() + 1;
                openMailBoxInnerGUI(player, boxIndex);
            }
        }
        // 处理具体邮箱GUI (1-9号箱)
        else if (title.startsWith("邮箱 #")) {
            // 允许玩家自由操作，不需要取消事件
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        Player player = (Player) e.getPlayer();
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
            // 保存具体邮箱的物品
            int boxIndex = Integer.parseInt(title.substring(4)); // 提取数字
            ItemStack[] contents = inv.getContents();
            plugin.getDataManager().setMailBoxContents(player.getUniqueId(), boxIndex, contents);
        }
    }
//监听玩家打开邮箱的方法
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if (block != null && block.getType() == Material.CHEST) {
                Player player = e.getPlayer();
                Location clickedLoc = block.getLocation();
                Location mailboxLoc = plugin.getDataManager().getMailBoxLocation(player.getUniqueId());

                if (mailboxLoc != null && mailboxLoc.equals(clickedLoc)) {
                    e.setCancelled(true);
                    openMailBoxSelectorGUI(player);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!(block.getType() == Material.CHEST)) return;

        Location brokenLoc = block.getLocation();
        // 遍历所有玩家，检查是否是他们的邮箱
        for (String playerUUID : plugin.getDataManager().getConfig().getConfigurationSection("players").getKeys(false)) {
            Location mailboxLoc = plugin.getDataManager().getMailBoxLocation(UUID.fromString(playerUUID));
            if (mailboxLoc != null && mailboxLoc.equals(brokenLoc)) {
                plugin.getDataManager().removeMailBox(UUID.fromString(playerUUID));
                Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
                if (player != null) {
                    player.sendMessage(ChatColor.YELLOW + "你的邮箱箱子已被破坏，绑定已解除。");
                }
                return;
            }
        }
    }

    private void sendItemsToMailBox(Player player, Inventory bagInv) {
        List<ItemStack> itemsToSend = new ArrayList<>();
        for (int i = 0; i < 26; i++) { // 不包括发送按钮所在的26号槽
            ItemStack item = bagInv.getItem(i);
            if (item != null) {
                itemsToSend.add(item);
            }
        }

        if (itemsToSend.isEmpty()) {
            player.sendMessage(ChatColor.RED + "没有物品可以发送。");
            return;
        }

        // 寻找邮箱中第一个有空位的27格区域
        int targetStartIndex = -1;
        UUID playerUUID = player.getUniqueId();
        for (int boxIndex = 1; boxIndex <= 9; boxIndex++) {
            ItemStack[] existingContents = plugin.getDataManager().getMailBoxContents(playerUUID, boxIndex);
            for (ItemStack content : existingContents) {
                if (content == null) {
                    targetStartIndex = (boxIndex - 1) * 27 + 1;
                    break; // 找到空位
                }
            }
            if (targetStartIndex != -1) break; // 找到可插入的区域
        }

        if (targetStartIndex == -1) {
            player.sendMessage(ChatColor.RED + "发送失败，邮箱空间已满。");
            return; // 9个箱子都满了
        }

        // 将物品写入找到的区域
        int currentIndex = targetStartIndex;
        for (ItemStack item : itemsToSend) {
            plugin.getDataManager().getConfig().set("data." + playerUUID + ".type.mailbox." + currentIndex, item);
            currentIndex++;
        }
        plugin.getDataManager().saveConfig();

        // 清空暂存箱中的物品
        for (int i = 0; i < 26; i++) {
            bagInv.setItem(i, null);
        }
        // 重置发送按钮
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        meta.setDisplayName("发送到邮箱");
        totem.setItemMeta(meta);
        bagInv.setItem(26, totem);

        player.sendMessage(ChatColor.GREEN + "发送成功！");
        player.updateInventory();
    }

    private void openMailBoxSelectorGUI(Player player) {
        Inventory selectorInv = Bukkit.createInventory(null, 9, "邮箱选择");
        for (int i = 0; i < 9; i++) {
            ItemStack chestItem = new ItemStack(Material.CHEST);
            ItemMeta meta = chestItem.getItemMeta();
            meta.setDisplayName(String.valueOf(i + 1));
            chestItem.setItemMeta(meta);
            selectorInv.setItem(i, chestItem);
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