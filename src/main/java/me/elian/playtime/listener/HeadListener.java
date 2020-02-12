package me.elian.playtime.listener;

import me.elian.playtime.manager.DataManager;
import me.elian.playtime.object.SignHead;
import me.elian.playtime.object.TimeType;
import me.elian.playtime.util.HeadUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;

public class HeadListener implements Listener {

    private final DataManager data = DataManager.getInstance();

    @EventHandler (priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();

        if (!p.hasPermission("playtime.heads.create"))
            return;

        String top = e.getLine(0);

        if (top.length() == 0 || top.charAt(0) != '['  || top.charAt(top.length() - 1) != ']')
            return;

        top = removeOuterChars(top);

        if (top.length() < 2)
            return;

        int place;

        try {
            int firstInt = getFirstInt(top);

            if (firstInt == -1)
                return;

            place = Integer.parseInt(top.substring(firstInt));

            top = top.substring(0, firstInt);
        } catch (NumberFormatException ex) {
            return;
        }

        top = top.toUpperCase();

        TimeType type;

        try {
            type = TimeType.valueOf(top);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Location signLoc = block.getLocation();
        Location headLoc = HeadUtil.findHead(signLoc);

        if (place < 1)
            place = 1;

        SignHead head = new SignHead(signLoc, headLoc, place, type);

        e.setCancelled(true);

        data.addHead(head);
        HeadUtil.updateHead(head);
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        BlockState state = e.getBlock().getState();

        if (state instanceof Sign) {
            if (!HeadUtil.signAt(state.getLocation()))
                return;

            if (!p.hasPermission("playtime.heads.destroy")) {
                e.setCancelled(true);
                return;
            }

            SignHead head = HeadUtil.getSignHeadAt(state.getLocation());

            data.removeHead(head);
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockPhysics(BlockPhysicsEvent e) {
        Block block = e.getBlock();

        if (!(block.getState() instanceof Sign))
            return;

        if (!HeadUtil.signAt(block.getLocation()))
            return;

        MaterialData data = block.getState().getData();

        if (!(data instanceof Attachable))
            return;

        Block attached = block.getRelative(((Attachable) data).getAttachedFace());

        if (attached.getType().equals(Material.AIR)) {
            this.data.removeHead(HeadUtil.getSignHeadAt(block.getLocation()));
        }
    }

    private String removeOuterChars(String str) {
        return str.substring(1, str.length() - 2);
    }

    private int getFirstInt(String str) {
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if ((c >= '0') && (c <= '9'))
                return i;
        }

        return -1;
    }
}
