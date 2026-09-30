package com.simplebuilding.modules.simpletweaks.claims;

import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;

/** Server-thread authority. The disabled fast path never opens a claim data file. */
public final class Claims {
    static final Map<MinecraftServer,Claims> SERVERS = new IdentityHashMap<>();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simpletweaks-claims");
    private static ClaimConfig settings;
    /** Command construction and reloads share the same restart-bound server settings. */
    public static synchronized ClaimConfig settings() {
        if (settings==null) settings=ClaimConfig.load(Path.of("config/simpletweaks-claims.json"));
        return settings;
    }
    public final ClaimConfig config;
    private final Path root;
    private final Map<String,Path> legacy;
    private ClaimStore store;
    private boolean failed;
    private final Map<UUID,Long> cooldowns = new HashMap<>();
    public Claims(ClaimConfig config, Path root, Map<String,Path> legacy) {
        this.config = config; this.root = root; this.legacy = Map.copyOf(legacy);
    }
    public static void start(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        var legacy = new HashMap<String,Path>();
        for (var level : server.getAllLevels()) {
            var id = level.dimension().identifier();
            if (!ClaimConfig.validDimension(id.toString())) throw new IllegalArgumentException("Unsafe dimension path");
            Path folder = switch (id.toString()) {
                case "minecraft:overworld" -> root;
                case "minecraft:the_nether" -> root.resolve("DIM-1");
                case "minecraft:the_end" -> root.resolve("DIM1");
                default -> root.resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath());
            };
            legacy.put(id.toString(), folder.resolve("data/simpletweaks_claims.dat"));
        }
        SERVERS.put(server,new Claims(settings(), root, legacy));
    }
    public static void stop(MinecraftServer server) { SERVERS.remove(server); settings=null; }
    public static Claims get(MinecraftServer server) { return SERVERS.get(server); }
    public static boolean enabled(MinecraftServer server) { var c=get(server); return c!=null && c.config.enabled(); }
    public static boolean allow(ServerLevel level, UUID actor, BlockPos pos) {
        var c=get(level.getServer());
        return c==null || !c.config.enabled() || c.allowed(level.dimension().identifier().toString(),actor,ChunkPos.pack(pos));
    }
    public static boolean allow(ServerPlayer player, ServerLevel level, BlockPos pos) {
        var c=get(level.getServer());
        return c==null || !c.config.enabled() || (c.config.opBypass() && admin(player)) || allow(level,player.getUUID(),pos);
    }
    public static boolean admin(ServerPlayer player) { return Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack()); }
    private boolean ready() {
        if (!config.enabled() || failed) return false;
        if (store==null) try { store=new ClaimStore(root.resolve("simpletweaks-claims.json"),legacy); }
        catch (Exception e) { failed=true; LOG.error("Claims are locked: invalid or unreadable data. Original files preserved.",e); }
        return !failed;
    }
    public boolean allowed(String dimension, UUID actor, long chunk) {
        if (!config.enabled()) return true;
        if (!ready()) return false;
        var claim=store.view().get(new ClaimStore.Key(dimension,chunk));
        return claim==null || (actor!=null && claim.permits(actor));
    }
    public Map<ClaimStore.Key,ClaimStore.Claim> view() {
        return ready() ? store.view() : Map.of();
    }
    public boolean locked() { return config.enabled() && !ready(); }
    public boolean dataLoaded() { return store!=null; }
    public boolean claim(ServerPlayer player) {
        if (!config.enabled()) return false;
        var level=player.level(); var chunk=player.chunkPosition();
        if (player.isSpectator() || !player.mayBuild() || !config.dimensions().contains(level.dimension().identifier().toString())) return false;
        var min=new BlockPos(chunk.getMinBlockX(),player.blockPosition().getY(),chunk.getMinBlockZ());
        var max=min.offset(15,0,15);
        if (!level.getWorldBorder().isWithinBounds(min) || !level.getWorldBorder().isWithinBounds(max)) return false;
        // Claims may not intersect Vanilla-protected spawn cells, even for an operator.
        var spawn=level.getServer().getRespawnData();
        if (level.dimension().equals(spawn.dimension())) {
            int radius=Math.max(config.spawnBuffer(), level.getServer() instanceof net.minecraft.server.dedicated.DedicatedServer dedicated ? dedicated.spawnProtectionRadius() : 0);
            if (max.getX()>=spawn.pos().getX()-radius && min.getX()<=spawn.pos().getX()+radius
                    && max.getZ()>=spawn.pos().getZ()-radius && min.getZ()<=spawn.pos().getZ()+radius) return false;
        }
        return create(new ClaimStore.Key(level.dimension().identifier().toString(),chunk.pack()),player.getUUID(),level.getServer().overworld().getGameTime());
    }
    public boolean create(ClaimStore.Key key, UUID owner, long tick) {
        if (!ready() || !config.dimensions().contains(key.dimension())) return false;
        if (store.view().containsKey(key) || store.view().size()>=config.globalCap()
                || store.view().values().stream().filter(c->c.owner().equals(owner)).count()>=config.maxClaimsPerPlayer()
                || tick-cooldowns.getOrDefault(owner,Long.MIN_VALUE/2)<config.cooldownTicks()) return false;
        var next=new HashMap<>(store.view()); next.put(key,new ClaimStore.Claim(owner,Set.of()));
        if (!commit(next)) return false;
        cooldowns.put(owner,tick); return true;
    }
    private boolean commit(Map<ClaimStore.Key,ClaimStore.Claim> next) {
        try { store.replace(next); return true; }
        catch (Exception e) { failed=true; LOG.error("Claims are locked: atomic save failed; last saved data preserved.",e); return false; }
    }
    private Claims() { throw new AssertionError(); }
}
