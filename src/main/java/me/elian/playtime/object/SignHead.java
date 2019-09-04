package me.elian.playtime.object;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class SignHead {

    private String location, headLocation;
    private int position;
    private TimeType type;

    public SignHead(String location, String headLocation, int position, TimeType type) {
        this.location = location;
        this.headLocation = headLocation;
        this.position = position;
        this.type = type;
    }

    public SignHead(Location location, Location headLocation, int position, TimeType type) {
        this(serializeLocation(location), serializeLocation(headLocation), position, type);
    }

    public String getLocation() {
        return location;
    }

    public Location getBukkitLocation() {
        return deserializeLocation(location);
    }

    public String getHeadLocation() {
        return headLocation;
    }

    public Location getBukkitHeadLocation() {
        return deserializeLocation(headLocation);
    }

    public int getPosition() {
        return position;
    }

    public TimeType getType() {
        return type;
    }

    private static String serializeLocation(Location loc) {
        if (loc == null)
            return null;

        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        String world = loc.getWorld().getName();

        return String.format("%d/%d/%d/%s", x, y, z, world);
    }

    private static Location deserializeLocation(String serialized) {
        if (serialized == null)
            return null;

        String[] split = serialized.split("/");

        int x = parseInt(split[0]), y = parseInt(split[1]), z = parseInt(split[2]);

        World world = Bukkit.getWorld(split[3]);

        return new Location(world, x, y, z);
    }

    private static int parseInt(String integer) {
        return Integer.parseInt(integer);
    }
}
