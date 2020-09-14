package gyurix.stargate;

import gyurix.protocol.Reflection;
import gyurix.spigotlib.SU;
import gyurix.spigotutils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created by GyuriX on 2015.07.01..
 */
public class DialerMenu extends InventoryManager {
    final Dialer d;
    final Gate from;
    final UUID plId;
    String number = "";

    public DialerMenu(Gate g, Dialer dialer, Player plr) {
        d = dialer;
        d.locked = false;
        from = g;
        plId = plr.getUniqueId();
        inv = Bukkit.createInventory(this, 45, SU.setLength(Main.lang.get(plr, "dialer"), 32));
        setItem(11, Material.REDSTONE_BLOCK, Reflection.ver.isAbove(ServerVersion.v1_11) ? 1 : 0, "§b0");
        setItem(29, Material.REDSTONE_BLOCK, "§b1");
        setItem(39, Material.REDSTONE_BLOCK, 2, "§b2");
        setItem(40, Material.REDSTONE_BLOCK, 3, "§b3");
        setItem(41, Material.REDSTONE_BLOCK, 4, "§b4");
        setItem(33, Material.REDSTONE_BLOCK, 5, "§b5");
        setItem(15, Material.REDSTONE_BLOCK, 6, "§b6");
        setItem(5, Material.REDSTONE_BLOCK, 7, "§b7");
        setItem(4, Material.REDSTONE_BLOCK, 8, "§b8");
        setItem(3, Material.REDSTONE_BLOCK, 9, "§b9");
        setItem(22, Material.STONE, Main.lang.get(plr, "dialer.dial"));
        if (plr.hasPermission("sg.lockdialer"))
            setItem(43, Material.BARRIER, "§cLock", "If you lock a dialer, players, will need to", "just click on it to dial the currently set number");
        setItem(44, Material.MAP, Main.lang.get(plr, "dialer.info", "id", "" + Config.gates.getKey(g), "name", g.name),
                Main.lang.get(plr, "dialer.info.lore").split("\\|"));
        plr.openInventory(inv);
    }

    @Override
    public void handleClick(int slot) {
        Player plr = Bukkit.getPlayer(plId);
        plr.playSound(plr.getLocation(), Sound.UI_BUTTON_CLICK, 1, 0);
        if (from.destination != null)
            plr.closeInventory();
        if (slot == 11)
            number += "0";
        else if (slot == 29)
            number += "1";
        else if (slot == 39)
            number += "2";
        else if (slot == 40)
            number += "3";
        else if (slot == 41)
            number += "4";
        else if (slot == 33)
            number += "5";
        else if (slot == 15)
            number += "6";
        else if (slot == 5)
            number += "7";
        else if (slot == 4)
            number += "8";
        else if (slot == 3)
            number += "9";
        else if (slot == 22) {
            plr.closeInventory();
            Gate g = Config.gates.get(number);
            long time = System.currentTimeMillis();
            if (g == null) {
                Main.lang.msg(plr, "dialermenu.error", "id", "" + number);
                return;
            }
            boolean canUse = g.canUse(from, plr);
            boolean cdok = (plr.hasPermission("sg.nocd")) || (from.closedUntil < time && g.closedUntil < time);
            boolean notopen = from.destination == null && g.destination == null && g != from;
            if (canUse && cdok && notopen) {
                plr.playSound(plr.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0);
                Main.lang.msg(plr, "dialermenu.dial", "id", "" + number, "name", g.name);
                from.open(g);
                g.open(from);
                SU.sch.scheduleSyncDelayedTask(Main.pl, new GateRunnables.GateCloseRunnable(from), Config.open * 20 + 141);
            } else {
                Main.lang.msg(plr, "dialermenu.error", "id", "" + number);
            }
        } else if (slot == 43) {
            if (plr.hasPermission("sg.lockdialer")) {
                plr.closeInventory();
                d.number = number;
                d.locked = true;
                Main.lang.msg(plr, "dialermenu.locked", "id", "" + number);
            }

        } else if (slot == 44) {
            plr.closeInventory();
            Main.openInv(plr, from, GateSelector.Action.Dialermenu);
        }
    }
}
