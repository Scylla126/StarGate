package gyurix.stargate;

import gyurix.configfile.ConfigSerialization;
import gyurix.spigotutils.LocationData;

/**
 * Created by GyuriX on 2015.09.06..
 */
public class Dialer {
    public String number;
    public boolean locked;
    //public String server;
    public final LocationData loc;
    @ConfigSerialization.ConfigOptions(serialize = false)
    public Gate gate;

    public Dialer(LocationData ld) {
        loc = ld;
    }

    public Dialer(LocationData ld, Gate g) {
        loc = ld;
        gate = g;
        //server=Config.serverName;
    }

    @Override
    public int hashCode() {
        return loc.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dialer dialer = (Dialer) o;
        return loc.equals(dialer.loc);
    }
}
