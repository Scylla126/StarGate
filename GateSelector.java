package gyurix.stargate;

import gyurix.spigotlib.SU;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;

import static gyurix.stargate.GateType.PRIVATE;
import static gyurix.stargate.GateType.PUBLIC;
import static gyurix.stargate.Main.lang;

/**
 * Created by GyuriX on 2015.06.30..
 */
public class GateSelector extends InventoryManager {

  public final UUID plId;
  public final Action action;
  private final ArrayList<Gate> gates = new ArrayList<Gate>();
  public Gate from;

  public GateSelector(Gate from, UUID plId, Action action) {
    this.from = from;
    this.plId = plId;
    this.action = action;
  }

  public void addGates(TreeSet<Gate> gs, Player plr) {
    gates.addAll(gs);
    String publ = lang.get(plr, "type.public.gui");
    String priv = lang.get(plr, "type.private.gui");
    String hide = lang.get(plr, "type.hidden.gui");
    String off = lang.get(plr, "type.off");
    String nocd = lang.get(plr, "type.nocdsuffix");
    int slot = 0;
    for (Gate g : gates) {
      ItemStack is = g.item.clone();
      ItemMeta meta = is.getItemMeta();
      if (meta != null) {
        meta.setDisplayName(
                SU.fillVariables(
                        g.off ? off : g.type == PUBLIC ? publ : g.type == PRIVATE ? priv : hide,
                        "name", g.name,
                        "id", g.id,
                        "group", StringUtils.join(g.groups, ", ")) + (g.cd == 0 ? nocd : ""));
        is.setItemMeta(meta);
      }
      inv.setItem(slot++, is);
    }
  }

  public void handleClick(int slot) {
    if (slot < 0 || slot >= gates.size())
      return;
    Player plr = Bukkit.getPlayer(plId);
    Gate g = gates.get(slot);
    String id = g.id;
    if (action == Action.Cooldown) {
      g.cd = g.cd == 0 ? Config.cooldown : 0;
      Main.kf.save();
    } else if (action == Action.Toggle) {
      g.off = !g.off;
      //g.updateMySQL();
      Main.kf.save();
    } else if (action == Action.Teleport) {
      lang.msg(plr, "teleport.text", "id", id + "", "name", g.name);
      plr.closeInventory();
      new TeleportData(from, g, g.commands/*,g.server*/).teleport(plr);
      return;
    } else if (action == Action.Delete) {
      if (g.destination == null) {
        plr.closeInventory();
        Config.gates.remove(id);
        g.destroy();
        lang.msg(plr, "delete.deleted", "id", id + "", "name", g.name);
        //Config.mysql.command("DELETE FROM "+Config.mysql.table+" WHERE server=\""+Config.serverName+"\" AND gate=\""+ MySQLDatabase.escape(g.id)+"\"");
        Main.kf.save();
      }
      return;
    } else if (action == Action.Dialermenu) {
      if (from == null)
        from = g;
      else {
        long time = System.currentTimeMillis();
        boolean cdok = (plr.hasPermission("sg.nocd")) || (from.closedUntil < time && g.closedUntil < time);
        boolean notopen = from.destination == null && g.destination == null && g != from;
        if (cdok && notopen) {
          g.open(from);
          from.open(g);
          SU.sch.scheduleSyncDelayedTask(Main.pl, new GateRunnables.GateCloseRunnable(from), Config.open * 20 + 141);
        }
        plr.closeInventory();
      }
      return;
    } else if (action == Action.Info) {
      plr.closeInventory();
      Player owner = SU.loadPlayer(g.owner);
      lang.msg("", plr, "info.text", "id", "" + id, "name", g.name, "world", g.loc.world, "owner", owner == null ? "?(" + g.owner + ")" : owner.getName(),
              "x", "" + g.loc.x, "y", "" + g.loc.y, "z", "" + g.loc.z, "face", "" + g.face, "cooldown", g.cd == 0 ? ".no" : ".yes", "dialable", g.off ? ".no" : ".yes");
      return;
    }

    ItemStack is = inv.getItem(slot);
    ItemMeta im = is.getItemMeta();
    im.setDisplayName(lang.get(plr, "type." + (g.off ? "off" : g.type == 0 ? "public.gui" : g.type == 1 ? "private.gui" :
            "hidden.gui"), "name", g.name, "id", "" + id) + (g.cd == 0 ? lang.get(plr, "type.nocdsuffix") : ""));
    is.setItemMeta(im);
  }

  public enum Action {Delete, Info, Dialermenu, Teleport, Toggle, Cooldown}
}
