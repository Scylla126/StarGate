package gyurix.stargate;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Created by GyuriX on 2015.07.01..
 */
public abstract class InventoryManager implements InventoryHolder {
    public Inventory inv;

    public Inventory getInventory() {
        return inv;
    }

    public abstract void handleClick(int slot);

    public void setItem(int slot, Material material, String name, String... lore) {
        setItem(slot, material, 1, name, lore);
    }

    public void setItem(int slot, Material material, int amount, String name, String... lore) {
        ItemStack it = new ItemStack(material, amount);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        it.setItemMeta(meta);
        inv.setItem(slot, it);
    }
}
