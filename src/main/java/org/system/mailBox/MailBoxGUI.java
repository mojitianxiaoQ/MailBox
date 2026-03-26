package org.system.mailBox; // 请确保与你的文件夹名和其它文件一致

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MailBoxGUI {

    public static final String BAG_GUI_TITLE = "物品暂存箱";
    public static final String MAILBOX_GUI_TITLE = "§6你的邮箱";
//这是GUI初始创建
    public static Inventory createBagGUI(Player player, ItemStack[] contents) {
        Inventory inv = Bukkit.createInventory(null, 27, BAG_GUI_TITLE);

        // 将加载的内容填充到前26个槽位
        for (int i = 0; i < 26; i++) {
            inv.setItem(i, contents[i]);
        }

        // 创建图腾
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        meta.setDisplayName("§a发送到邮箱");
        totem.setItemMeta(meta);
        inv.setItem(26, totem);

        return inv;
    }
}