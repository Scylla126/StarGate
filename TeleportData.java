package gyurix.stargate;

import gyurix.commands.Command;
import gyurix.spigotutils.LocationData;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * Created by GyuriX on 2016.02.13..
 */
public class TeleportData {
    public final Gate from, to;
    public final LocationData location;
    public final ArrayList<Command> commands;

    //public final String server;
    public TeleportData(Gate from, Gate to, ArrayList<Command> commands /*, String server*/) {
        this.from = from;
        this.to = to;
        this.location = to.getLocation();
        this.commands = commands;
        /*this.server=server;*/
    }

    public void teleport(Player plr) {
        location.pitch = plr.getLocation().getPitch();
        /*if (!server.equals(Config.serverName)){
            BungeeAPI.send(server,plr);
            BungeeAPI.executeServerCommands(new Command[]{new Command("{10}SGTP:"+plr.getName()+" "+location)},server);
        }
        else {*/
        Main.portalUse.put(plr.getUniqueId(), System.currentTimeMillis() + Config.teleportDelay);
        plr.teleport(location.getLocation());
        if (commands != null) {
            GateVariable.gate = to;
            for (Command c : commands)
                c.execute(plr);
        }
    }
}
