package org.system.mailBox; // 请确保与你的文件夹名和其它文件一致

import java.lang.System;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.lang.System.*;

public class MailBoxListener implements Listener {

    //定义名字
    public static final String BAG_GUI_TITLE = "物品暂存箱";
    public static final String MAILBOX_GUI_TITLE = "§6你的邮箱";
    //我靠这真是神秘小代码
    public static final String test ="TextComponentImpl{content=\"物品暂存箱\", style=StyleImpl{obfuscated=not_set, bold=not_set, strikethrough=not_set, underlined=not_set, italic=not_set, color=null, clickEvent=null, hoverEvent=null, insertion=null, font=null}, children=[]}";
    private static final NamespacedKey CONFIRM_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(MailBoxPlugin.class), "confirm_send");

    private final MailBoxPlugin plugin;

    //新定义MailBoxListener
    public MailBoxListener(MailBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        var inv = e.getInventory();
        var view = e.getView();
        String title = String.valueOf(view.title());

        if (!title.equals(test)) {
            //这边需要添加一个非其他类型的点击将会使得整个背包被保存
            return;
        }

        // 如果点击的是发送按钮 (槽位26),666还检测不到。检测到了现在
        if (e.getSlot() == 26) {
            System.out.println("检测点击成功");
            e.setCancelled(true); // 取消默认的物品交换行为

            var player = (Player) e.getWhoClicked();
            var clickedItem = e.getCurrentItem();

            //防止空指针问题
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null) return;

            // 如果点击的是初始的复活图腾
            if (clickedItem.getType() == Material.TOTEM_OF_UNDYING && "§a发送到邮箱".equals(meta.getDisplayName())) {
                // 将复活图腾替换成“再点一次确认”的下界之星
                ItemStack confirmStar = new ItemStack(Material.NETHER_STAR);
                ItemMeta confirmMeta = confirmStar.getItemMeta();
                confirmMeta.setDisplayName("§c再点一次确认");
                confirmMeta.getPersistentDataContainer().set(CONFIRM_KEY, PersistentDataType.INTEGER, 1);
                confirmStar.setItemMeta(confirmMeta);
                inv.setItem(26, confirmStar);
                player.updateInventory();
            }
            // 如果点击的是带有确认标记的下界之星
            else if (clickedItem.getType() == Material.NETHER_STAR &&
                    meta.getPersistentDataContainer().has(CONFIRM_KEY, PersistentDataType.INTEGER)) {
                // 执行发送邮件的操作
                sendItemsToMailbox(player, inv);
                // 发送完成后，将按钮状态重置为初始的复活图腾
                resetConfirmButton(inv);
            }
        }
    }

    /**
     * 重置发送按钮为初始状态
     */

    private void resetConfirmButton(Inventory inv) {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta totemMeta = totem.getItemMeta();
        totemMeta.setDisplayName("§a发送到邮箱");
        totem.setItemMeta(totemMeta);
        inv.setItem(26, totem);
    }

    //现在核心问题就在发送这里，我猜测是因为某个命名出错了导致无法发送
    //这里开始是保存的核心代码，保存的原理是先遍历格子再
    private void saveItemsInGroup(Player player,title,Inventory inv){
        System.out.println("保存");
        var saved = new ArrayList<ItemStack>();
        for (int i = 0; i < 26; i++) {
            var item = inv.getItem(i);
            System.out.println(item);
            if (item != null && item.getType() != Material.AIR) {
                itemsToSend.add(item.clone()); // 使用clone()避免引用问题
                System.out.println("添加一个物品");
                System.out.println(itemsToSend);
            }

    }

    private void sendItemsToMailbox(Player player, Inventory inv) {
        // 从GUI的前26个槽位收集物品
        System.out.println("开始发送物品");

        }
//以上已经成功将物品存储进数组了
        //常规检测
        if (itemsToSend.isEmpty()) {
            player.sendMessage("§e没有物品可以发送。");
            resetConfirmButton(inv);
            return;
        }


        // 创建潜影盒
        var shulkerBox = new ItemStack(Material.SHULKER_BOX, 1);
        System.out.println("创建了潜影盒1");
        var meta = (BlockStateMeta) shulkerBox.getItemMeta();
        var shulker = (ShulkerBox) meta.getBlockState();
        shulker.getInventory().setContents(itemsToSend.toArray(new ItemStack[0]));
        meta.setDisplayName("§f" + new SimpleDateFormat("MM月dd日HH时").format(new Date()));
        shulker.update();
        meta.setBlockState(shulker);
        shulkerBox.setItemMeta(meta);
        System.out.println("这是盒子：" + shulker);
//这边直接删掉
        // 获取邮箱位置并发送
        var mailboxLoc = plugin.getDataManager().getMailBoxLocation(player.getUniqueId());
        System.out.println("获取邮箱位置2");
        System.out.println(mailboxLoc);
        //下面是对邮箱的检测
        if (mailboxLoc == null) {
            player.sendMessage("§c你还没有绑定信箱。");
            resetConfirmButton(inv); // 发送失败也应重置按钮
            return;
        }

        if (mailboxLoc.getBlock().getType() != Material.CHEST) {
            player.sendMessage("§c你的信箱箱子已被破坏。");
            plugin.getDataManager().removeMailBox(player.getUniqueId());
            resetConfirmButton(inv); // 发送失败也应重置按钮
            return;
        }

        if (mailboxLoc.getBlock().getState() instanceof Chest chest) {
            var leftover = chest.getInventory().addItem(shulkerBox);
            if (!leftover.isEmpty()) {
                player.sendMessage("§c发送失败，邮箱空间不足。");
                resetConfirmButton(inv); // 发送失败也应重置按钮
                return;
            }
        }

        player.sendMessage("§a发送成功！");

        // 发送成功后，清空GUI的前26个槽位
        for (int i = 0; i < 26; i++) {
            inv.setItem(i, null);
        }
        // 重置发送按钮
        resetConfirmButton(inv);
        // 更新玩家界面以反映变化
        player.updateInventory();
    }


    // 监听GUI关闭事件，保存暂存箱内容
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        var view = e.getView();
        String title = view.getTitle();
        if (!title.equals(BAG_GUI_TITLE)) {
            return;
        }

        // GUI关闭时，将前26个槽位的内容保存回去
        Player player = (Player) e.getPlayer();
        Inventory inventory = e.getInventory();
        ItemStack[] contents = new ItemStack[26];
        for (int i = 0; i < 26; i++) {
            contents[i] = inventory.getItem(i);
        }
        // 保存当前GUI关闭时的实际状态
        plugin.getDataManager().setBagContents(player.getUniqueId(), contents);
    }

    //这里时单纯打开箱子执行的指令，基本不需要修改
    @EventHandler
    public void onPlayerOpenChest(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var block = e.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;
        var chestLoc = block.getLocation();
        var player = e.getPlayer();
        UUID ownerUUID = getMailBoxOwnerUUID(chestLoc);
        if (ownerUUID == null || !ownerUUID.equals(player.getUniqueId())) {
            return; // 不是自己的邮箱，允许正常打开
        }
        e.setCancelled(true);
        var mails = plugin.getDataManager().getMails(ownerUUID);
        var mailInv = Bukkit.createInventory(null, 27, MAILBOX_GUI_TITLE);
        int i = 0;
        for (ItemStack mail : mails) {
            if (i >= 26) break;
            mailInv.setItem(i++, mail);
        }
        player.openInventory(mailInv);
    }

    //打开箱子的指令到此结束

    //获取信箱所有者ID
    private UUID getMailBoxOwnerUUID(Location chestLoc) {
        var config = plugin.getDataManager().dataConfig;
        var playersSection = config.getConfigurationSection("players");
        if (playersSection == null) return null;
        for (String playerIdStr : playersSection.getKeys(false)) {
            var playerId = UUID.fromString(playerIdStr);
            var loc = plugin.getDataManager().getMailBoxLocation(playerId);
            if (loc != null && loc.equals(chestLoc)) {
                return playerId;
            }
        }
        return null;
    }

    //获取ID的指令到此结束

    //破坏箱子事件
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        var block = e.getBlock();
        if (block.getType() != Material.CHEST) return;
        var brokenLoc = block.getLocation();
        var config = plugin.getDataManager().dataConfig;
        var playersSection = config.getConfigurationSection("players");
        if (playersSection == null) return;
        for (String playerIdStr : playersSection.getKeys(false)) {
            var playerId = UUID.fromString(playerIdStr);
            var loc = plugin.getDataManager().getMailBoxLocation(playerId);
            if (loc != null && loc.equals(brokenLoc)) {
                plugin.getDataManager().removeMailBox(playerId);
                var onlinePlayer = Bukkit.getPlayer(playerId);
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage("§e你的信箱已被破坏。");
                }
            }
        }
    }
}//到此结束