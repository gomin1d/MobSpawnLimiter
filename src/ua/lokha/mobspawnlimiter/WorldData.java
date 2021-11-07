package ua.lokha.mobspawnlimiter;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class WorldData {
    private String worldName;
    public Long2IntOpenHashMap counter = new Long2IntOpenHashMap();
    public Long2IntOpenHashMap toLog = new Long2IntOpenHashMap();

    public WorldData(String worldName) {
        this.worldName = worldName;

        this.counter.defaultReturnValue(0);
        this.toLog.defaultReturnValue(0);
    }

    public String getWorldName() {
        return worldName;
    }
}
