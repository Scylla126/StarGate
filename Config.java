package gyurix.stargate;

import com.google.common.collect.Lists;
import gyurix.commands.Command;
import gyurix.configfile.PostLoadable;
import gyurix.spigotutils.BlockData;
import gyurix.spigotutils.DualMap;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by GyuriX on 2015.06.30..
 */
public class Config implements PostLoadable {
    public static String backup, signFirstLine="[sgs]";
    public static boolean protectGateLamps, protectGateBlocks;
    public static int cooldown, open, teleportDelay, lampUpdateVisibility = 30;
    public static GateDesign gateDesign;
    public static HashMap<String, Integer> gateLimits = new HashMap<>();
    public static ArrayList<String> defaultGateGroups= Lists.newArrayList("default");
    public static DualMap<String, Gate> gates=new DualMap<>();
    public static HashMap<BlockData, ItemStack> itemChanges = new HashMap<>();
    public static ArrayList<Command> gateCommands=new ArrayList<>();

    //public static String serverName;
    //public static long syncTime;
    //public static MySQLDatabase mysql;
    public Config() {
    }

    @Override
    public void postLoad() {
    }

    public static class GateDesign {
        public static BlockData bottom, top, sides, tophalf, bottomhalf, lampoff, lampon, open;
    }

}
