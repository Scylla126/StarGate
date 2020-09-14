package gyurix.stargate;

import gyurix.api.VariableAPI;
import gyurix.spigotlib.SU;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * Created by GyuriX on 2016.04.06..
 */
public class GateVariable implements VariableAPI.VariableHandler {
    public static Gate gate;
    @Override
    public Object getValue(Player plr, ArrayList<Object> args, Object[] eArgs) {
        String sub= StringUtils.join(args,"");
        switch (sub){
            case "name":
                return gate.name;
            case "id":
                return gate.id;
            case "owner":
                return gate.owner;
            case "ownername":
                return SU.getName(gate.owner);
            case "face":
                return gate.face;
            case "cd":
                return gate.cd;
            case "loc":
                return gate.loc;
            case "closeduntil":
                return gate.closedUntil;
            case "off":
                return gate.off;
        }
        return "<sg:"+sub+">";
    }
}
