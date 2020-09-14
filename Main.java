package gyurix.stargate;

import com.google.common.collect.Lists;
import gyurix.api.VariableAPI;
import gyurix.configfile.ConfigFile;
import gyurix.protocol.Reflection;
import gyurix.spigotlib.GlobalLangFile;
import gyurix.spigotlib.GlobalLangFile.PluginLang;
import gyurix.spigotlib.SU;
import gyurix.spigotutils.LocationData;
import gyurix.spigotutils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import static gyurix.stargate.Config.gates;

public class Main extends JavaPlugin implements Listener {
  public static final String nonce = "%%__NONCE__%% ";
  public static final String resource = "%%__RESOURCE__%%";
  public static final String user = "%%__USER__%%";
  public static final String version = "10.0";
  public static boolean cancelExplosion;
  public static ArrayList<String> commands = Lists.newArrayList("list", "link", "set", "setdhd", "remdhd",
          "remalldhd", "del", "info", "stat", "tp", "menu", "cd", "toggle", "setcdtime", "settime");
  public static HashMap<LocationData, Dialer> dialers = new HashMap<>();
  public static ConfigFile kf;
  public static Material lampOn, lampOff;
  public static PluginLang lang;
  public static HashSet<LocationData> noBreakBlocks = new HashSet<>();
  public static HashSet<LocationData> noBreakLamps = new HashSet<>();
  public static HashMap<Block, TeleportData> openPortalBlocks = new HashMap<>();
  public static Plugin pl;
  public static HashMap<UUID, Long> portalUse = new HashMap<>();

  public static void openInv(Player plr, GateSelector.Action action) {
    openInv(plr, null, action);
  }

  public static void openInv(Player plr, Gate from, GateSelector.Action action) {
    GateSelector im = new GateSelector(from, plr.getUniqueId(), action);
    UUID uid = plr.getUniqueId();
    im.inv = Bukkit.createInventory(im, (gates.size() + 8) / 9 * 9, SU.setLength(lang.get(plr, action.name().toLowerCase()), 32));
    boolean showOthers = true;
    switch (action) {
      case Delete:
        showOthers = plr.hasPermission("sg.del.others");
        break;
      case Toggle:
        showOthers = plr.hasPermission("sg.toggle.others");
    }
    TreeSet<Gate> gateList = new TreeSet<>();
    for (Gate g : gates.values()) {
      boolean owned = g.owner.equals(uid) || plr.hasPermission("sg.own." + g.id);
      if (!owned && !showOthers || g == from)
        continue;
      if (g.canSee(from, plr))
        gateList.add(g);
    }
    im.addGates(gateList, plr);
    plr.openInventory(im.inv);
  }

  public boolean create(Player plr, String[] args, Block center, boolean generate) {
    if (args.length < 3) {
      lang.msg(plr, "set.usage");
      return false;
    }
    if (center == null) {
      center = plr.getTargetBlock(null, 10);
      if (center == null || center.getType() == null || center.getType() == Material.AIR) {
        lang.msg(plr, "set.noblock");
        return false;
      }
    }
    int count = getCreatedGateNumber(plr.getUniqueId());
    if (count >= getAllowedGateNumber(plr)) {
      lang.msg(plr, "set.limit");
      return false;
    }
    int id = -1;
    try {
      id = Integer.parseInt(args[1]);
    } catch (Throwable ignored) {
    }
    if (id < 0) {
      lang.msg(plr, "wrongid");
      return false;
    }
    if (gates.containsKey(args[1])) {
      lang.msg(plr, "set.exists", "id", args[1]);
      return false;
    }
    for (Gate g : gates.values()) {
      if (g.name.equals(args[2])) {
        lang.msg(plr, "set.existsName", "name", args[2]);
        return false;
      }
    }
    Location loc = plr.getLocation();
    float yaw = loc.getYaw();
    if (yaw < 0)
      yaw += 360;
    int face = (yaw > 45 && yaw <= 135) ? 1 : (yaw > 135 && yaw <= 225) ? 2 : (yaw > 225 && yaw <= 315) ? 3 : 0;
    Gate gate = new Gate(args[1], center, plr.getUniqueId(), args[2], face);
    if (args.length > 3) {
      args[3] = ' ' + args[3] + ' ';
      if ((' ' + lang.get(plr, "set.private") + ' ').contains(args[3])) {
        gate.type = 1;
      } else if ((' ' + lang.get(plr, "set.hidden") + ' ').contains(args[3])) {
        gate.type = 2;
      }
    }
    if (generate)
      gate.generate();
    else if (!gate.validate()) {
      lang.msg(plr, "invalid");
      return false;
    }
    gates.put(args[1], gate);
    gate.postLoad();
    kf.save();
    lang.msg(plr, "set", "type", lang.get(plr, "type." + (gate.type == 0 ? "public" : gate.type == 1 ? "private" : "hidden")));
    return true;
  }

  public void dial(Player plr, Gate from, String number) {
    Gate g = gates.get(number);
    long time = System.currentTimeMillis();
    if (g == null) {
      lang.msg(plr, "dialermenu.error", "id", "" + number);
      return;
    }
    boolean canUse = g.canUse(from, plr);
    boolean cdok = (plr.hasPermission("sg.nocd")) || (from.closedUntil < time && g.closedUntil < time);
    boolean notopen = from.destination == null && g.destination == null && g != from;
    if (canUse && cdok && notopen) {
      plr.playSound(plr.getLocation(), "mob.enderdragon.wings", 1, 0);
      lang.msg(plr, "dialermenu.dial", "id", "" + number, "name", g.name);
      from.open(g);
      g.open(from);
    } else {
      lang.msg(plr, "dialermenu.error", "id", "" + number);
    }
  }

  public int getAllowedGateNumber(Player plr) {
    int limit = 0;
    for (Entry<String, Integer> e : Config.gateLimits.entrySet()) {
      if (plr.hasPermission("sg.limit." + e.getKey())) {
        int l = e.getValue();
        if (l < 0) {
          return Integer.MAX_VALUE;
        } else if (l > limit)
          limit = l;
      }
    }
    return limit;
  }

  public int getCreatedGateNumber(UUID id) {
    int out = 0;
    for (Gate g : gates.values()) {
      if (g.owner.equals(id)) {
        out++;
      }
    }
    return out;
  }

  @EventHandler
  public void invClick(InventoryClickEvent e) {
    if (e.getInventory() == null)
      return;
    InventoryHolder holder = e.getInventory().getHolder();
    if (!(holder instanceof InventoryManager))
      return;
    e.setCancelled(true);
    ((InventoryManager) holder).handleClick(e.getSlot());
  }

  public void loadGates() {
    HashMap<LocationData, Dialer> tempDialers = new HashMap<>();
    for (Gate g : gates.values()) {
      for (Dialer d : g.dialers) {
        tempDialers.put(d.loc, d);
        d.gate = g;
      }
    }
    dialers = tempDialers;
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Block b = e.getBlock();
    LocationData loc = new LocationData(b);
    if (noBreakLamps.contains(loc)) {
      e.setCancelled(true);
      lang.msg(e.getPlayer(), "nolampbreak");
    } else if (noBreakBlocks.contains(loc)) {
      e.setCancelled(true);
      lang.msg(e.getPlayer(), "noblockbreak");
    } else if (dialers.containsKey(loc)) {
      e.setCancelled(true);
      lang.msg(e.getPlayer(), "nodhdbreak");
    }
  }

  @EventHandler
  public void onBlockBreak(BlockExplodeEvent e) {
    e.blockList().removeIf(block -> {
      LocationData ld = new LocationData(block);
      return noBreakBlocks.contains(ld) || noBreakLamps.contains(ld) || dialers.containsKey(ld);
    });
  }

  @EventHandler
  public void onBlockBreak(EntityExplodeEvent e) {
    e.blockList().removeIf(block -> {
      LocationData ld = new LocationData(block);
      return noBreakBlocks.contains(ld) || noBreakLamps.contains(ld) || dialers.containsKey(ld);
    });
  }

  @EventHandler
  public void onBlockClick(PlayerInteractEvent e) {
    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
      Dialer d = dialers.get(new LocationData(e.getClickedBlock()));
      if (d == null)
        return;
      Gate g = d.gate;
      Player plr = e.getPlayer();
      e.setCancelled(true);
      if (d.locked && !(plr.hasPermission("sg.lockdialer") && plr.isSneaking())) {
        dial(plr, g, d.number);
        return;
      }
      if (g.off || g.destination != null) {
        openClosedDialer(plr);
        return;
      }
      new DialerMenu(g, d, plr);
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    try {
      if (command.getName().equals("dhd"))
        args = new String[]{"menu"};
      Player plr = sender instanceof Player ? (Player) sender : null;
      if (args.length == 0) {
        lang.msg("§9§l--> §bStarGate§9§l <-••-> §bv:§e" + version + "§9§l <-••-> §bby:§egyuriX§9§l <--\n", sender, "main");
        return true;
      }
      if (!sender.hasPermission("sg." + args[0])) {
        lang.msg(sender, "noperm");
        return true;
      }
      switch (args[0].toLowerCase()) {
        case "set":
        case "create": {
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          create(plr, args, null, true);
          return true;
        }
        case "license":
          sender.sendMessage("§6§lPlugin license info:\n" +
                  "§eLicensed to: §fSpigot User #" + user + "\n" +
                  "§eLicense key:§f " + nonce + "\n" +
                  "§eResource id:§f " + resource);
          return true;
        case "list": {
          String publ = lang.get(plr, "type.public.list");
          String priv = lang.get(plr, "type.private.list");
          String hid = lang.get(plr, "type.hidden.list");
          String sep = lang.get(plr, "list.separator");
          StringBuilder out = new StringBuilder();
          boolean showhidden = sender.hasPermission("sg.hide");
          boolean showpriv = sender.hasPermission("sg.private");
          for (Entry<String, Gate> e : gates.entrySet()) {
            String id = e.getKey();
            Gate g = e.getValue();
            if (g.type == 0) {
              out.append(sep);
              out.append(publ.replace("<id>", "" + id).replace("<name>", g.name));
            } else if (g.type == 1) {
              if (showpriv || plr.hasPermission("sg.gate." + g.id)) {
                out.append(sep);
                out.append(priv.replace("<id>", "" + id).replace("<name>", g.name));
              }
            } else if (g.type == 2) {
              if (showhidden || plr.hasPermission("sg.gate." + g.id)) {
                out.append(sep);
                out.append(hid.replace("<id>", "" + id).replace("<name>", g.name));
              }
            }
          }
          lang.msg(sender, "list", "list", out.length() == 0 ? "" : out.substring(sep.length()));
          return true;
        }
        case "del":
        case "remove":
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          openInv(plr, GateSelector.Action.Delete);
          return true;
        case "info":
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          openInv(plr, GateSelector.Action.Info);
          return true;
        case "toggle":
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          openInv(plr, GateSelector.Action.Toggle);
          return true;
        case "cd":
        case "cooldown":
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          openInv(plr, GateSelector.Action.Cooldown);
          return true;
        case "tp":
        case "teleport":
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          openInv(plr, GateSelector.Action.Teleport);
          return true;
        case "menu":
        case "menü":
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          openInv(plr, GateSelector.Action.Dialermenu);
          return true;
        case "dhdset":
        case "setdhd": {
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          if (args.length == 1) {
            lang.msg(sender, "noid");
            return true;
          }
          Gate g = gates.get(args[1]);
          if (g == null) {
            lang.msg(plr, "notfound", "id", "" + args[1]);
            return true;
          }
          Block b = plr.getTargetBlock(null, 10);
          if (b == null || !b.getType().isSolid()) {
            lang.msg(plr, "setdhd.notlooking");
            return true;
          }
          LocationData ld = new LocationData(b);
          Dialer d = new Dialer(ld, g);
          if (dialers.containsKey(ld)) {
            lang.msg(plr, "setdhd.already");
            return true;
          }
          dialers.put(ld, d);
          g.dialers.add(d);
          kf.save();
          lang.msg(plr, "setdhd", "id", "" + args[1], "name", g.name);
          return true;
        }
        case "link": {
          if (args.length == 1) {
            lang.msg(sender, "noid");
            return true;
          }
          if (args.length == 2) {
            lang.msg(sender, "nogroup");
            return true;
          }
          Gate g = gates.get(args[1]);
          if (g == null) {
            lang.msg(plr, "notfound", "id", "" + args[1]);
            return true;
          }
          if (g.groups.remove(args[2])) {
            lang.msg(plr, "link.unlink", "id", g.id, "name", g.name, "group", args[2]);
            kf.save();
            return true;
          }
          g.groups.add(args[2]);
          lang.msg(plr, "link.done", "id", g.id, "name", g.name, "group", args[2]);
          kf.save();
          return true;
        }
        case "remdhd": {
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          Block b = plr.getTargetBlock(null, 10);
          if (b == null || !b.getType().isSolid()) {
            lang.msg(plr, "setdhd.notlooking");
            return true;
          }
          LocationData ld = new LocationData(b);
          Dialer d = dialers.remove(ld);
          if (d == null) {
            lang.msg(plr, "remdhd.not");
            return true;
          }
          d.gate.dialers.remove(d);
          kf.save();
          lang.msg(plr, "remdhd", "id", d.gate.id, "name", d.gate.name);
          return true;
        }
        case "remalldhd": {
          if (plr == null) {
            lang.msg(sender, "noconsole");
            return true;
          }
          if (args.length == 1) {
            lang.msg(sender, "noid");
            return true;
          }
          Gate g = gates.get(args[1]);
          if (g == null) {
            lang.msg(plr, "notfound", "id", args[1]);
            return true;
          }
          g.dialers.forEach(d -> dialers.remove(d.loc));
          g.dialers.clear();
          kf.save();
          lang.msg(plr, "remalldhd", "id", g.id, "name", g.name);
          return true;
        }
        case "stat":
        case "stats":
        case "statistic":
        case "statistics": {
          int open = 0;
          for (Gate g : gates.values()) {
            if (g.destination != null)
              ++open;
          }
          lang.msg(sender, "stat", "count", "" + gates.size(), "cooldown", "" + Config.cooldown, "open", "" + open, "opentime", "" + Config.open);
          return true;
        }
        case "setcd":
        case "setcdtime": {
          if (args.length == 1) {
            lang.msg(sender, "notime");
            return true;
          }
          int time = -1;
          try {
            time = Integer.parseInt(args[1]);
          } catch (Throwable ignored) {
          }
          if (time < 1) {
            lang.msg(sender, "wrongtime");
            return true;
          }
          Config.cooldown = time;
          kf.save();
          lang.msg(sender, "cooldown.set", "cooldown", "" + Config.cooldown);
          return true;
        }
        case "settime": {
          if (args.length == 1) {
            lang.msg(sender, "notime");
            return true;
          }
          int time = -1;
          try {
            time = Integer.parseInt(args[1]);
          } catch (Throwable ignored) {
          }
          if (time < 1) {
            lang.msg(sender, "wrongtime");
            return true;
          }
          Config.open = Integer.parseInt(args[1]);
          kf.save();
          lang.msg(sender, "time", "time", "" + Config.open);
          return true;
        }
        default:
          lang.msg(sender, "wrongsub");
      }
      return true;
    } catch (Throwable e) {
      SU.error(sender, e, "StarGate", "gyurix");
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    TreeSet<String> out = new TreeSet<>();
    if (args.length == 1) {
      args[0] = args[0].toLowerCase();
      for (String cmd : commands) {
        if (cmd.startsWith(args[0]) && sender.hasPermission("sg." + cmd))
          out.add(cmd);
      }
    }
    return new ArrayList<>(out);
  }

  @Override
  public void onDisable() {
    for (Gate g : gates.values()) {
      if (g.destination != null)
        g.close();
    }
    gyurix.commands.Command.customCommands.remove("SGTP");
    gyurix.commands.Command.customCommands.remove("SGOG");
    VariableAPI.handlers.remove("gate");
  }

  @Override
  public void onEnable() {
    SU.saveResources(this, "lang.yml", "config.yml");
    lampOff = Material.valueOf(Reflection.ver.isAbove(ServerVersion.v1_13) ? "LEGACY_REDSTONE_LAMP_OFF" : "REDSTONE_LAMP_OFF");
    lampOn = Material.valueOf(Reflection.ver.isAbove(ServerVersion.v1_13) ? "LEGACY_REDSTONE_LAMP_ON" : "REDSTONE_LAMP_ON");
    pl = this;
    kf = new ConfigFile(new File(getDataFolder() + File.separator + "config.yml"));
    kf.data.deserialize(Config.class);
    gyurix.commands.Command.customCommands.put("SGOG", (cs, text, args) -> {
      String[] d = text.split(" ");
      gates.get(d[0]).open(gates.get(d[1]));
      return true;
    });
    gyurix.commands.Command.customCommands.put("SGTP", (sender, text, args) -> {
      String[] d = text.split(" ");
      Player plr = Bukkit.getPlayer(d[0]);
      plr.teleport(new Location(Bukkit.getWorld(d[1]), Double.parseDouble(d[2]), Double.parseDouble(d[3]), Double.parseDouble(d[4]), Float.parseFloat(d[5]), Float.parseFloat(d[6])));
      portalUse.put(plr.getUniqueId(), System.currentTimeMillis() + Config.teleportDelay);
      return true;
    });
    VariableAPI.handlers.put("gate", new GateVariable());
    SU.sch.scheduleSyncRepeatingTask(this, () -> {
      for (Player p : Bukkit.getOnlinePlayers()) {
        TeleportData tp = openPortalBlocks.get(p.getLocation().getBlock());
        if (tp != null) {
          Long old = portalUse.get(p.getUniqueId());
          boolean hasperm = tp.to.canUse(tp.from, p);
          if ((old == null || old < System.currentTimeMillis()) && hasperm) {
            tp.teleport(p);
          }
        }
      }
    }, 5, 5);

    //Config.mysql.command("CREATE TABLE IF NOT EXISTS `"+Config.mysql.table+"`( `server` text, `gate` text, `data` text )");
    loadGates();
        /*sch.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                loadGates();
            }
        },Config.syncTime,Config.syncTime,TimeUnit.MILLISECONDS);*/
    SU.pm.registerEvents(this, this);
    lang = GlobalLangFile.loadLF("stargate", getResource("lang.yml"), getDataFolder() + File.separator + "lang.yml");
  }

  @EventHandler
  public void onDamage(EntityDamageEvent e) {
    if (cancelExplosion)
      e.setCancelled(true);
  }

  @EventHandler
  public void onDamage(HangingBreakEvent e) {
    if (cancelExplosion)
      e.setCancelled(true);
  }

  @EventHandler
  public void onSignEdit(SignChangeEvent e) {
    String[] lines = e.getLines();
    if (lines[0].equalsIgnoreCase(Config.signFirstLine)) {
      if (!e.getPlayer().hasPermission("sg.createsign"))
        return;
      create(e.getPlayer(), lines, e.getBlock().getRelative(BlockFace.DOWN), false);
      e.getBlock().getWorld().dropItem(e.getBlock().getLocation().add(0.5, 0, 0.5),
              new ItemStack(Material.valueOf(Reflection.ver.isAbove(ServerVersion.v1_14) ? "OAK_SIGN" : "SIGN")));
      e.getBlock().setType(Material.AIR);
    }
  }

  @EventHandler
  public void onWaterFlow(BlockFromToEvent e) {
    TeleportData loc = openPortalBlocks.get(e.getBlock());
    if (loc != null)
      e.setCancelled(true);
  }

  public void openClosedDialer(Player plr) {
    Inventory inv = Bukkit.createInventory(new ClosedDialer(), 45, lang.get(plr, "dialer"));
    ItemStack is = new ItemStack(Material.BEDROCK);
    ItemMeta meta = is.getItemMeta();
    meta.setDisplayName(lang.get(plr, "dialer.off"));
    is.setItemMeta(meta);
    for (int slot : new int[]{2, 6, 12, 14, 22, 30, 32, 38, 42}) {
      inv.setItem(slot, is);
    }
    plr.openInventory(inv);
  }
}
