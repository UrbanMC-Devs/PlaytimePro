package me.elian.playtime.util;

import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.Messages;
import me.elian.playtime.manager.TopListManager;
import me.elian.playtime.object.SignHead;
import me.elian.playtime.object.TopListItem;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;

import java.util.List;

public class HeadUtil {

    private static final DataManager data = DataManager.getInstance();
    private static final TopListManager topList = TopListManager.getInstance();

    public static List<SignHead> getHeads() {
        return data.getHeads();
    }

    public static boolean signAt(Location location) {
        return getSignHeadAt(location) != null;
    }

    public static boolean headAt(Location location) {
        for (SignHead head : getHeads()) {
            if (location.equals(head.getBukkitHeadLocation()))
                return true;
        }

        return false;
    }

    public static SignHead getSignHeadAt(Location location) {
        for (SignHead head : getHeads()) {
            if (head.getBukkitLocation().equals(location))
                return head;
        }

        return null;
    }

    public static Location findHead(Location loc) {
        for (int x = loc.getBlockX() - 1; x < loc.getBlockX() + 2; x++) {
            for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + 2; y++) {
                for (int z = loc.getBlockZ() - 1; z < loc.getBlockZ() + 2; z++) {
                    Block tblock = loc.getWorld().getBlockAt(x, y, z);

                    if (tblock.getState() instanceof Skull && !headAt(tblock.getLocation()))
                        return tblock.getLocation();
                }
            }
        }

        Block tblock = loc.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP);

        if (tblock.getState() instanceof Skull && !headAt(tblock.getLocation()))
            return tblock.getLocation();

        return null;
    }

    public static void updateHead(SignHead head) {
        Block block = head.getBukkitLocation().getBlock();

        if (!(block.getState() instanceof Sign)) {
            data.removeHead(head);
            return;
        }

        Sign sign = (Sign) block.getState();

        int position = head.getPosition();

        List<TopListItem> list = null;

        switch (head.getType()) {
            case ALL_TIME:
                list = topList.getTimesConverted();
                break;
            case MONTHLY:
                list = topList.getMonthlyTimesConverted();
                break;
            case WEEKLY:
                list = topList.getWeeklyTimesConverted();
                break;
            case SEASON:
                list = topList.getSeasonTopConverted();
                break;
        }

        if (list == null)
            return;

        int index = position - 1;

        String name;
        int hours;

        if (index >= list.size()) {
            name = Messages.getString("top_list_unknown");
            hours = 0;
        } else {
            TopListItem listItem = list.get(index);

            name = listItem.getName();
            hours = listItem.getTime();
        }

        String type = head.getType().toString().toLowerCase();

        String line1 = Messages.getString("heads_" + type + "_line1", name, position, hours);
        String line2 = Messages.getString("heads_" + type + "_line2", name, position, hours);
        String line3 = Messages.getString("heads_" + type + "_line3", name, position, hours);
        String line4 = Messages.getString("heads_" + type + "_line4", name, position, hours);

        sign.setLine(0, line1);
        sign.setLine(1, line2);
        sign.setLine(2, line3);
        sign.setLine(3, line4);

        sign.update();

        if (head.getHeadLocation() == null)
            return;

        Block headBlock = head.getBukkitHeadLocation().getBlock();

        if (!(headBlock.getState() instanceof Skull))
            return;

        Skull skull = (Skull) headBlock.getState();

        skull.setOwner(name);
        skull.update();
    }
}
