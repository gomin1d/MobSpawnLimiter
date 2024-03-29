//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ua.lokha.mobspawnlimiter;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main extends JavaPlugin implements Listener {
    private Map<String, WorldData> worldDataMap = new HashMap<>();
    private int startTick;
    private int radiusChunks;
    private int limit;
    private int ticks;
    private EnumSet<EntityType> ignoreEntityTypes;

    public Main() {
        this.startTick = this.getTick();
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        this.loadConfigParams();
        this.getCommand("mobspawnlimiter").setExecutor(this);
    }

    public void loadConfigParams() {
        this.limit = Math.max(0, this.getConfig().getInt("limit"));
        this.radiusChunks = Math.max(0, this.getConfig().getInt("radius-chunks"));
        this.ticks = Math.max(1, this.getConfig().getInt("ticks"));
        this.ignoreEntityTypes = EnumSet.noneOf(EntityType.class);
        this.getConfig().getStringList("ignore-entity-types").stream()
                .map(name -> {
                    try {
                        return EntityType.valueOf(name);
                    } catch (Exception e) {
                        this.getLogger().severe("Моб " + name + " не найден.");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(ignoreEntityTypes::add);

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.reloadConfig();
        this.loadConfigParams();
        sender.sendMessage("§aКонфиг перезагружен.");
        return true;
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void on(EntityAddToWorldEvent event) {
        if (event.getEntityType().equals(EntityType.PLAYER)) {
            return;
        }

        if (this.isIgnore(event.getEntity())) { // лимит не распространяется на скот
            return;
        }

        String worldName = event.getEntity().getWorld().getName();
        WorldData worldData = worldDataMap.computeIfAbsent(worldName, WorldData::new);

        int tick = this.getTick();
        if (tick - this.startTick >= this.ticks) {
            this.startTick = tick;

            try {
                this.printTopChunks();
            } catch (Exception var13) {
                this.getLogger().info("Ошибка при выводе списка загруженных чанков.");
                var13.printStackTrace();
            }

            for (WorldData it : worldDataMap.values()) {
                it.counter.clear();
            }
        }

        Location loc = event.getEntity().getLocation();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        long key = key(chunkX, chunkZ);
        int count = worldData.counter.get(key);
        worldData.counter.put(key, count + 1);
        int toX = chunkX + this.radiusChunks;
        int toZ = chunkZ + this.radiusChunks;
        int sumCount = count;

        for(int x = chunkX - this.radiusChunks; x <= toX; ++x) {
            for(int z = chunkZ - this.radiusChunks; z <= toZ; ++z) {
                if (x != chunkX || z != chunkZ) {
                    sumCount += worldData.counter.get(key(x, z));
                }
            }
        }

        if (sumCount >= this.limit) {
            event.getEntity().remove();
            worldData.toLog.put(key, sumCount);
        }

    }

    private boolean isIgnore(Entity entity) {
        return ignoreEntityTypes.contains(entity.getType());
    }

    private void printTopChunks() {
        for (WorldData worldData : worldDataMap.values()) {
            try {
                if (!worldData.toLog.isEmpty()) {
                    StringBuilder builder = new StringBuilder("[Мир " + worldData.getWorldName() + "] Список чанков, которые превысили лимит спавна мобов за последние " + this.ticks + " тиков:");
                    ObjectIterator iterator = worldData.toLog.long2IntEntrySet().fastIterator();

                    while(iterator.hasNext()) {
                        Entry next = (Entry)iterator.next();
                        long key = next.getLongKey();
                        long x = getX(key);
                        long z = getZ(key);
                        builder.append("\nChunk X: ").append(x).append(", Chunk Z: ").append(z).append(", Count: ").append(next.getIntValue());
                    }

                    this.getLogger().info(builder.toString());
                }
            } catch (Exception e) {
                this.getLogger().severe("[printTopChunks] Ошибка при обработке мира " + worldData.getWorldName());
                e.printStackTrace();
            } finally {
                worldData.toLog.clear();
            }
        }
    }

    public static long key(int x, int z) {
        return (long)x << 32 | (long)z & 4294967295L;
    }

    public static long getX(long key) {
        return (long)((int)(key >> 32));
    }

    public static long getZ(long key) {
        return (long)((int)key);
    }

    public int getTick() {
        return ((CraftServer)Bukkit.getServer()).getServer().aq();
    }
}
