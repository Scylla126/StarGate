package gyurix.stargate;

import gyurix.protocol.Reflection;
import gyurix.spigotutils.ServerVersion;
import gyurix.stargate.Config.GateDesign;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Created by GyuriX on 2015.07.01..
 */
public class GateRunnables {
    public static class GateCloseRunnable implements Runnable {
        final Gate gate;

        public GateCloseRunnable(Gate g) {
            gate = g;
        }

        public void run() {
            gate.close();
        }
    }

    public static class GateLampOnRunnable implements Runnable {
        final Block lamp;

        public GateLampOnRunnable(Block lamp) {
            this.lamp = lamp;
        }

        public void run() {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().getName().equals(lamp.getWorld().getName()) && p.getLocation().distance(lamp.getLocation()) <= Config.lampUpdateVisibility) {
                    if (p.getWorld().getName().equals(lamp.getWorld().getName()))
                        GateDesign.lampon.sendChange(p, lamp.getLocation());
                }
            }
            if (Reflection.ver.isAbove(ServerVersion.v1_9)) {
                lamp.getWorld().playSound(lamp.getLocation(), Sound.ENTITY_GHAST_SHOOT, 2, 0);
            } else
                lamp.getWorld().playSound(lamp.getLocation(), Sound.valueOf("GHAST_FIREBALL"), 2, 0);
        }
    }

}
