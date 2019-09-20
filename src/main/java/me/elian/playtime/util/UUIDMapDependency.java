package me.elian.playtime.util;

import me.Silverwolfg11.UUIDMap.UUIDMap;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UUIDMapDependency {

    private UUIDMap uuidMap;

    public UUIDMapDependency() {
        uuidMap = (UUIDMap) Bukkit.getPluginManager().getPlugin("UUIDMap");
    }

    public CompletableFuture<Map<String, String>> getNameFromUUID(Collection<UUID> ids) {
        return uuidMap.getName(ids);
    }

}
