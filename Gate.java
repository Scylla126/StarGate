package gyurix.stargate;

import gyurix.commands.Command;
import gyurix.configfile.ConfigSerialization.ConfigOptions;
import gyurix.configfile.PostLoadable;
import gyurix.spigotlib.SU;
import gyurix.spigotutils.BlockData;
import gyurix.spigotutils.LocationData;
import gyurix.stargate.Config.GateDesign;
import gyurix.stargate.GateRunnables.GateCloseRunnable;
import gyurix.stargate.GateRunnables.GateLampOnRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;

import static gyurix.stargate.Config.GateDesign.open;
import static gyurix.stargate.GateType.*;
import static gyurix.stargate.Main.*;

/**
 * Created by GyuriX on 2015.06.30..
 */
public class Gate implements Comparable<Gate>, PostLoadable {
  public long closedUntil;
  public ArrayList<Command> commands = Config.gateCommands;
  @ConfigOptions(serialize = false)
  public Gate destination;
  public ArrayList<Dialer> dialers = new ArrayList<>();
  //public String server;
  public int face, type, cd = -1;
  public TreeSet<String> groups = new TreeSet<>();
  public ItemStack item;
  public LocationData loc;
  public String name, id;
  public boolean off;
  public UUID owner;

  public Gate() {

  }

  public Gate(String id, Block bl, UUID owner, String name, int face) {
    this.id = id;
    groups.addAll(Config.defaultGateGroups);
    loc = new LocationData(bl);
    this.face = face;
    item = Config.itemChanges.get(new BlockData(bl));
    if (item == null)
      item = new ItemStack(bl.getType(), 1, (short) bl.getData());
    this.owner = owner;
    this.name = name;
  }

  public boolean canSee(Gate from, Player plr) {
    boolean basePerm = plr.hasPermission("sg.usegates");
    boolean gatePerm = from == null || plr.hasPermission("sg.gate." + from.id);
    boolean toWorldPerm = plr.hasPermission("sg.toworld." + loc.world);
    if (!basePerm || !gatePerm || !toWorldPerm)
      return false;
    if (!plr.hasPermission("sg.bypassgroups")) {
      boolean groupMatch = false;
      for (String g : groups) {
        if ((from == null || from.groups.contains(g)) && plr.hasPermission("sg.group." + g)) {
          groupMatch = true;
          break;
        }
      }
      if (!groupMatch)
        return false;
    }
    boolean privatePerm = plr.hasPermission("sg.private");
    boolean hiddenPerm = plr.hasPermission("sg.hide");
    return type == PUBLIC || type == PRIVATE && privatePerm || type == HIDDEN && hiddenPerm;
  }

  public boolean canUse(Gate from, Player plr) {
    boolean basePerm = plr.hasPermission("sg.usegates");
    boolean gatePerm = from == null || plr.hasPermission("sg.gate." + from.id);
    boolean toWorldPerm = plr.hasPermission("sg.toworld." + loc.world);
    if (!basePerm || !gatePerm || !toWorldPerm)
      return false;
    if (!plr.hasPermission("sg.bypassgroups")) {
      boolean groupMatch = false;
      for (String g : groups) {
        if ((from == null || from.groups.contains(g)) && plr.hasPermission("sg.group." + g)) {
          groupMatch = true;
          break;
        }
      }
      if (!groupMatch)
        return false;
    }
    boolean hiddenPerm = plr.hasPermission("sg.hide");
    return type == PUBLIC || type == PRIVATE || type == HIDDEN && hiddenPerm;
  }

  public void close() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (!p.getWorld().getName().equals(loc.world) || p.getLocation().distance(loc.getLocation()) > Config.lampUpdateVisibility) {
        continue;
      }
      if (face == 0 || face == 2) {
        p.sendBlockChange(loc.clone().add(2, 5, 0).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(3, 3, 0).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(2, 1, 0).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(-2, 1, 0).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(-3, 3, 0).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(-2, 5, 0).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 6, 0).getLocation(), lampOff, (byte) 0);
      } else if (face == 1 || face == 3) {
        p.sendBlockChange(loc.clone().add(0, 5, 2).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 3, 3).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 1, 2).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 1, -2).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 3, -3).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 5, -2).getLocation(), lampOff, (byte) 0);
        p.sendBlockChange(loc.clone().add(0, 6, 0).getLocation(), lampOff, (byte) 0);
      }
    }
    destination = null;
    if (face == 0 || face == 2) {
      closeBlock(loc.clone().add(1, 1, 0).getBlock());
      closeBlock(loc.clone().add(0, 1, 0).getBlock());
      closeBlock(loc.clone().add(-1, 1, 0).getBlock());
      for (int y2 = 2; y2 < 5; y2++) {
        for (int x2 = -2; x2 < 3; x2++) {
          closeBlock(loc.clone().add(x2, y2, 0).getBlock());
        }
      }
      closeBlock(loc.clone().add(1, 5, 0).getBlock());
      closeBlock(loc.clone().add(0, 5, 0).getBlock());
      closeBlock(loc.clone().add(-1, 5, 0).getBlock());
    } else if (face == 1 || face == 3) {
      closeBlock(loc.clone().add(0, 1, 1).getBlock());
      closeBlock(loc.clone().add(0, 1, 0).getBlock());
      closeBlock(loc.clone().add(0, 1, -1).getBlock());
      for (int y2 = 2; y2 < 5; y2++) {
        for (int z2 = -2; z2 < 3; z2++) {
          closeBlock(loc.clone().add(0, y2, z2).getBlock());
        }
      }
      closeBlock(loc.clone().add(0, 5, 1).getBlock());
      closeBlock(loc.clone().add(0, 5, 0).getBlock());
      closeBlock(loc.clone().add(0, 5, -1).getBlock());
    }
    loc.clone().subtract(0, 1, 0).getBlock().setType(Material.AIR);
    if (cd != 0) {
      closedUntil = System.currentTimeMillis() + cd;
      Main.kf.save();
    }
  }

  public void closeBlock(Block b) {
    b.setType(Material.AIR, false);
    Main.openPortalBlocks.remove(b);
  }

  @Override
  public int compareTo(Gate g) {
    return name.compareTo(g.name);
  }

  public void createWater(Gate g) {
    TeleportData l = new TeleportData(this, g, commands/*,g.server*/);
    if (face == 0 || face == 2) {
      openBlock(loc.clone().add(1, 1, 0).getBlock(), l);
      openBlock(loc.clone().add(0, 1, 0).getBlock(), l);
      openBlock(loc.clone().add(-1, 1, 0).getBlock(), l);
      for (int y2 = 2; y2 < 5; y2++) {
        for (int x2 = -2; x2 < 3; x2++) {
          openBlock(loc.clone().add(x2, y2, 0).getBlock(), l);
        }
      }
      openBlock(loc.clone().add(1, 5, 0).getBlock(), l);
      openBlock(loc.clone().add(0, 5, 0).getBlock(), l);
      openBlock(loc.clone().add(-1, 5, 0).getBlock(), l);
    } else if (face == 1 || face == 3) {
      openBlock(loc.clone().add(0, 1, 1).getBlock(), l);
      openBlock(loc.clone().add(0, 1, 0).getBlock(), l);
      openBlock(loc.clone().add(0, 1, -1).getBlock(), l);
      for (int y2 = 2; y2 < 5; y2++) {
        for (int z2 = -2; z2 < 3; z2++) {
          openBlock(loc.clone().add(0, y2, z2).getBlock(), l);
        }
      }
      openBlock(loc.clone().add(0, 5, 1).getBlock(), l);
      openBlock(loc.clone().add(0, 5, 0).getBlock(), l);
      openBlock(loc.clone().add(0, 5, -1).getBlock(), l);
    }
  }

  public void destroy() {
    World w = loc.getWorld();
    int x = (int) loc.x, y = (int) loc.y, z = (int) loc.z;
    for (Dialer d : dialers) {
      Main.dialers.remove(new LocationData(d.loc));
    }
    dialers = null;
    if (face == 0 || face == 2) {
      if (Config.protectGateLamps) {
        noBreakLamps.remove(loc.clone().add(2, 5, 0));
        noBreakLamps.remove(loc.clone().add(3, 3, 0));
        noBreakLamps.remove(loc.clone().add(2, 1, 0));
        noBreakLamps.remove(loc.clone().add(-2, 1, 0));
        noBreakLamps.remove(loc.clone().add(-3, 3, 0));
        noBreakLamps.remove(loc.clone().add(-2, 5, 0));
        noBreakLamps.remove(loc.clone().add(0, 6, 0));
      }

      if (Config.protectGateBlocks) {
        for (int x2 = -3; x2 <= 3; ++x2) {
          for (int y2 = 0; y2 <= 6; ++y2) {
            noBreakBlocks.add(loc.clone().add(x2, y2, 0));
          }
        }
      }

      w.getBlockAt(x + 1, y, z).setType(Material.AIR, false);
      w.getBlockAt(x + 2, y, z).setType(Material.AIR, false);
      w.getBlockAt(x + 2, y + 1, z).setType(Material.AIR, false);
      w.getBlockAt(x + 3, y + 1, z).setType(Material.AIR, false);
      w.getBlockAt(x, y, z).setType(Material.AIR, false);
      w.getBlockAt(x - 1, y, z).setType(Material.AIR, false);
      w.getBlockAt(x - 2, y, z).setType(Material.AIR, false);
      w.getBlockAt(x - 2, y + 1, z).setType(Material.AIR, false);
      w.getBlockAt(x - 3, y + 1, z).setType(Material.AIR, false);

      w.getBlockAt(x + 3, y + 2, z).setType(Material.AIR, false);
      w.getBlockAt(x + 3, y + 3, z).setType(Material.AIR, false);
      w.getBlockAt(x + 3, y + 4, z).setType(Material.AIR, false);
      w.getBlockAt(x - 3, y + 2, z).setType(Material.AIR, false);
      w.getBlockAt(x - 3, y + 3, z).setType(Material.AIR, false);
      w.getBlockAt(x - 3, y + 4, z).setType(Material.AIR, false);

      w.getBlockAt(x + 1, y + 6, z).setType(Material.AIR, false);
      w.getBlockAt(x + 2, y + 6, z).setType(Material.AIR, false);
      w.getBlockAt(x + 2, y + 5, z).setType(Material.AIR, false);
      w.getBlockAt(x + 3, y + 5, z).setType(Material.AIR, false);
      w.getBlockAt(x, y + 6, z).setType(Material.AIR, false);
      w.getBlockAt(x - 1, y + 6, z).setType(Material.AIR, false);
      w.getBlockAt(x - 2, y + 6, z).setType(Material.AIR, false);
      w.getBlockAt(x - 2, y + 5, z).setType(Material.AIR, false);
      w.getBlockAt(x - 3, y + 5, z).setType(Material.AIR, false);
    } else if (face == 1 || face == 3) {
      if (Config.protectGateLamps) {
        noBreakLamps.remove(loc.clone().add(0, 5, 2));
        noBreakLamps.remove(loc.clone().add(0, 3, 3));
        noBreakLamps.remove(loc.clone().add(0, 1, 2));
        noBreakLamps.remove(loc.clone().add(0, 1, -2));
        noBreakLamps.remove(loc.clone().add(0, 3, -3));
        noBreakLamps.remove(loc.clone().add(0, 5, -2));
        noBreakLamps.remove(loc.clone().add(0, 6, 0));
      }
      if (Config.protectGateBlocks) {
        for (int z2 = -3; z2 <= 3; ++z2) {
          for (int y2 = 0; y2 <= 6; ++y2) {
            noBreakBlocks.add(loc.clone().add(0, y2, z2));
          }
        }
      }

      w.getBlockAt(x, y, z + 1).setType(Material.AIR, false);
      w.getBlockAt(x, y, z + 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 1, z + 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 1, z + 3).setType(Material.AIR, false);
      w.getBlockAt(x, y, z).setType(Material.AIR, false);
      w.getBlockAt(x, y, z - 1).setType(Material.AIR, false);
      w.getBlockAt(x, y, z - 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 1, z - 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 1, z - 3).setType(Material.AIR, false);

      w.getBlockAt(x, y + 2, z + 3).setType(Material.AIR, false);
      w.getBlockAt(x, y + 3, z + 3).setType(Material.AIR, false);
      w.getBlockAt(x, y + 4, z + 3).setType(Material.AIR, false);
      w.getBlockAt(x, y + 2, z - 3).setType(Material.AIR, false);
      w.getBlockAt(x, y + 3, z - 3).setType(Material.AIR, false);
      w.getBlockAt(x, y + 4, z - 3).setType(Material.AIR, false);

      w.getBlockAt(x, y + 6, z + 1).setType(Material.AIR, false);
      w.getBlockAt(x, y + 6, z + 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 5, z + 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 5, z + 3).setType(Material.AIR, false);
      w.getBlockAt(x, y + 6, z).setType(Material.AIR, false);
      w.getBlockAt(x, y + 6, z - 1).setType(Material.AIR, false);
      w.getBlockAt(x, y + 6, z - 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 5, z - 2).setType(Material.AIR, false);
      w.getBlockAt(x, y + 5, z - 3).setType(Material.AIR, false);
    }
  }

  public void generate() {
    World w = loc.getWorld();
    int x = (int) loc.x, y = (int) loc.y, z = (int) loc.z;

    if (face == 0 || face == 2) {
      GateDesign.bottom.setBlock(w.getBlockAt(x + 1, y, z));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x + 2, y, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x + 2, y + 1, z));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x + 3, y + 1, z));
      GateDesign.bottom.setBlock(w.getBlockAt(x - 1, y, z));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x - 2, y, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x - 2, y + 1, z));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x - 3, y + 1, z));

      GateDesign.sides.setBlock(w.getBlockAt(x + 3, y + 2, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x + 3, y + 3, z));
      GateDesign.sides.setBlock(w.getBlockAt(x + 3, y + 4, z));
      GateDesign.sides.setBlock(w.getBlockAt(x - 3, y + 2, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x - 3, y + 3, z));
      GateDesign.sides.setBlock(w.getBlockAt(x - 3, y + 4, z));

      GateDesign.top.setBlock(w.getBlockAt(x + 1, y + 6, z));
      GateDesign.tophalf.setBlock(w.getBlockAt(x + 2, y + 6, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x + 2, y + 5, z));
      GateDesign.tophalf.setBlock(w.getBlockAt(x + 3, y + 5, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 6, z));
      GateDesign.top.setBlock(w.getBlockAt(x - 1, y + 6, z));
      GateDesign.tophalf.setBlock(w.getBlockAt(x - 2, y + 6, z));
      GateDesign.lampoff.setBlock(w.getBlockAt(x - 2, y + 5, z));
      GateDesign.tophalf.setBlock(w.getBlockAt(x - 3, y + 5, z));


    } else if (face == 1 || face == 3) {
      GateDesign.bottom.setBlock(w.getBlockAt(x, y, z + 1));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x, y, z + 2));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 1, z + 2));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x, y + 1, z + 3));
      GateDesign.bottom.setBlock(w.getBlockAt(x, y, z - 1));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x, y, z - 2));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 1, z - 2));
      GateDesign.bottomhalf.setBlock(w.getBlockAt(x, y + 1, z - 3));

      GateDesign.sides.setBlock(w.getBlockAt(x, y + 2, z + 3));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 3, z + 3));
      GateDesign.sides.setBlock(w.getBlockAt(x, y + 4, z + 3));
      GateDesign.sides.setBlock(w.getBlockAt(x, y + 2, z - 3));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 3, z - 3));
      GateDesign.sides.setBlock(w.getBlockAt(x, y + 4, z - 3));

      GateDesign.top.setBlock(w.getBlockAt(x, y + 6, z + 1));
      GateDesign.tophalf.setBlock(w.getBlockAt(x, y + 6, z + 2));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 5, z + 2));
      GateDesign.tophalf.setBlock(w.getBlockAt(x, y + 5, z + 3));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 6, z));
      GateDesign.top.setBlock(w.getBlockAt(x, y + 6, z - 1));
      GateDesign.tophalf.setBlock(w.getBlockAt(x, y + 6, z - 2));
      GateDesign.lampoff.setBlock(w.getBlockAt(x, y + 5, z - 2));
      GateDesign.tophalf.setBlock(w.getBlockAt(x, y + 5, z - 3));
    }
    postLoad();
  }

  public LocationData getLocation() {
    LocationData l = loc.clone().add(0.5, 1, 0.5);
    if (face == 0) {
      l.yaw = 180;
      l.z = loc.z;
    } else if (face == 1) {
      l.yaw = 270;
      l.x = loc.x + 1;
    } else if (face == 2) {
      l.yaw = 0;
      l.z = loc.z + 1;
    } else if (face == 3) {
      l.yaw = 90;
      l.x = loc.x;
    }
    return l;
  }

  public void open(final Gate g) {
    final World w = loc.getWorld();
    final int x = (int) loc.x, y = (int) loc.y, z = (int) loc.z;
    destination = g;
    if (face == 0) {
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x - 2, y + 5, z)), 20);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x - 3, y + 3, z)), 40);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x - 2, y + 1, z)), 60);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x + 2, y + 1, z)), 80);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x + 3, y + 3, z)), 100);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x + 2, y + 5, z)), 120);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 6, z)), 140);
    } else if (face == 1) {
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 5, z - 2)), 20);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 3, z - 3)), 40);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 1, z - 2)), 60);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 1, z + 2)), 80);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 3, z + 3)), 100);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 5, z + 2)), 120);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 6, z)), 140);
    } else if (face == 2) {
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x + 2, y + 5, z)), 20);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x + 3, y + 3, z)), 40);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x + 2, y + 1, z)), 60);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x - 2, y + 1, z)), 80);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x - 3, y + 3, z)), 100);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x - 2, y + 5, z)), 120);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 6, z)), 140);
    } else if (face == 3) {
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 5, z + 2)), 20);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 3, z + 3)), 40);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 1, z + 2)), 60);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 1, z - 2)), 80);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 3, z - 3)), 100);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 5, z - 2)), 120);
      SU.sch.scheduleSyncDelayedTask(Main.pl, new GateLampOnRunnable(w.getBlockAt(x, y + 6, z)), 140);
    }
    SU.sch.scheduleSyncDelayedTask(Main.pl, new Runnable() {
      public void run() {
        Location l = null;
        switch (g.face) {
          case 0:
            l = loc.clone().add(0, 3, -3).getLocation();
            break;
          case 1:
            l = loc.clone().add(3, 3, 0).getLocation();
            break;
          case 2:
            l = loc.clone().add(0, 3, 3).getLocation();
            break;
          case 3:
            l = loc.clone().add(-3, 3, 0).getLocation();
            break;
        }
        cancelExplosion = true;
        w.createExplosion(l.getX(), l.getY(), l.getZ(), 15, false, false);
        cancelExplosion = false;
        createWater(g);
        w.getBlockAt(x, y - 1, z).setType(Material.REDSTONE_BLOCK);
      }
    }, 140);
    SU.sch.scheduleSyncDelayedTask(Main.pl, new GateCloseRunnable(this), Config.open * 20 + 141);
  }

  public void openBlock(Block b, TeleportData loc) {
    open.setBlock(b);
    openPortalBlocks.put(b, loc);
  }

  public void postLoad() {
    if (face == 0 || face == 2) {
      if (Config.protectGateLamps) {
        noBreakLamps.add(loc.clone().add(2, 5, 0));
        noBreakLamps.add(loc.clone().add(3, 3, 0));
        noBreakLamps.add(loc.clone().add(2, 1, 0));
        noBreakLamps.add(loc.clone().add(-2, 1, 0));
        noBreakLamps.add(loc.clone().add(-3, 3, 0));
        noBreakLamps.add(loc.clone().add(-2, 5, 0));
        noBreakLamps.add(loc.clone().add(0, 6, 0));
      }
      if (Config.protectGateBlocks) {
        for (int x = -3; x <= 3; ++x) {
          for (int y = 0; y <= 6; ++y) {
            noBreakBlocks.add(loc.clone().add(x, y, 0));
          }
        }
      }
    } else if (face == 1 || face == 3) {
      if (Config.protectGateLamps) {
        noBreakLamps.add(loc.clone().add(0, 5, 2));
        noBreakLamps.add(loc.clone().add(0, 3, 3));
        noBreakLamps.add(loc.clone().add(0, 1, 2));
        noBreakLamps.add(loc.clone().add(0, 1, -2));
        noBreakLamps.add(loc.clone().add(0, 3, -3));
        noBreakLamps.add(loc.clone().add(0, 5, -2));
        noBreakLamps.add(loc.clone().add(0, 6, 0));
      }
      if (Config.protectGateBlocks) {
        for (int z = -3; z <= 3; ++z) {
          for (int y = 0; y <= 6; ++y) {
            noBreakBlocks.add(loc.clone().add(0, y, z));
          }
        }
      }
    }
  }

  public boolean validate() {
    World w = loc.getWorld();
    int x = (int) loc.x, y = (int) loc.y, z = (int) loc.z;
    ArrayList<Block> blocks = new ArrayList<>();
    ArrayList<Block> lamps = new ArrayList<>();
    if (face == 0 || face == 2) {
      blocks.add(w.getBlockAt(x + 1, y, z));
      blocks.add(w.getBlockAt(x + 2, y, z));
      lamps.add(w.getBlockAt(x + 2, y + 1, z));
      blocks.add(w.getBlockAt(x + 3, y + 1, z));
      blocks.add(w.getBlockAt(x - 1, y, z));
      blocks.add(w.getBlockAt(x - 2, y, z));
      lamps.add(w.getBlockAt(x - 2, y + 1, z));
      blocks.add(w.getBlockAt(x - 3, y + 1, z));

      blocks.add(w.getBlockAt(x + 3, y + 2, z));
      lamps.add(w.getBlockAt(x + 3, y + 3, z));
      blocks.add(w.getBlockAt(x + 3, y + 4, z));
      blocks.add(w.getBlockAt(x - 3, y + 2, z));
      lamps.add(w.getBlockAt(x - 3, y + 3, z));
      blocks.add(w.getBlockAt(x - 3, y + 4, z));

      blocks.add(w.getBlockAt(x + 1, y + 6, z));
      blocks.add(w.getBlockAt(x + 2, y + 6, z));
      lamps.add(w.getBlockAt(x + 2, y + 5, z));
      blocks.add(w.getBlockAt(x + 3, y + 5, z));
      lamps.add(w.getBlockAt(x, y + 6, z));
      blocks.add(w.getBlockAt(x - 1, y + 6, z));
      blocks.add(w.getBlockAt(x - 2, y + 6, z));
      lamps.add(w.getBlockAt(x - 2, y + 5, z));
      blocks.add(w.getBlockAt(x - 3, y + 5, z));


    } else if (face == 1 || face == 3) {
      blocks.add(w.getBlockAt(x, y, z + 1));
      blocks.add(w.getBlockAt(x, y, z + 2));
      lamps.add(w.getBlockAt(x, y + 1, z + 2));
      blocks.add(w.getBlockAt(x, y + 1, z + 3));
      blocks.add(w.getBlockAt(x, y, z - 1));
      blocks.add(w.getBlockAt(x, y, z - 2));
      lamps.add(w.getBlockAt(x, y + 1, z - 2));
      blocks.add(w.getBlockAt(x, y + 1, z - 3));

      blocks.add(w.getBlockAt(x, y + 2, z + 3));
      lamps.add(w.getBlockAt(x, y + 3, z + 3));
      blocks.add(w.getBlockAt(x, y + 4, z + 3));
      blocks.add(w.getBlockAt(x, y + 2, z - 3));
      lamps.add(w.getBlockAt(x, y + 3, z - 3));
      blocks.add(w.getBlockAt(x, y + 4, z - 3));

      blocks.add(w.getBlockAt(x, y + 6, z + 1));
      blocks.add(w.getBlockAt(x, y + 6, z + 2));
      lamps.add(w.getBlockAt(x, y + 5, z + 2));
      blocks.add(w.getBlockAt(x, y + 5, z + 3));
      lamps.add(w.getBlockAt(x, y + 6, z));
      blocks.add(w.getBlockAt(x, y + 6, z - 1));
      blocks.add(w.getBlockAt(x, y + 6, z - 2));
      lamps.add(w.getBlockAt(x, y + 5, z - 2));
      blocks.add(w.getBlockAt(x, y + 5, z - 3));
    }
    for (Block b : blocks)
      if (!b.getType().isSolid())
        return false;
    for (Block b : lamps)
      if (!GateDesign.lampoff.isBlock(b))
        return false;
    return true;
  }
}
