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
        install(server,new Claims(settings(), root, legacy));
    }
    static void install(MinecraftServer server, Claims claims) {
        com.simplebuilding.framework.api.Protection.unregister(server,"simpletweaks");
        if (claims==null) SERVERS.remove(server);
        else {
            SERVERS.put(server,claims);
            if (claims.config.enabled()) com.simplebuilding.framework.api.Protection.register(server,"simpletweaks",target -> (claims.config.opBypass() && target.administrator()) || claims.allowed(target.dimension(),target.actor(),new ChunkPos(target.x()>>4,target.z()>>4).pack()));
        }
    }
    public static void stop(MinecraftServer server) { install(server,null); settings=null; }
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
    public static boolean allowEntity(ServerPlayer actor, net.minecraft.world.entity.Entity target) {
        if (!(target.level() instanceof ServerLevel level) || !enabled(level.getServer())) return true;
        var box=target.getBoundingBox();
        int minX=net.minecraft.util.Mth.floor(box.minX)>>4, maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4;
        int minZ=net.minecraft.util.Mth.floor(box.minZ)>>4, maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;
        if ((long)(maxX-minX+1)*(maxZ-minZ+1)>256) return false;
        for (int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++)
            if (!allow(actor,level,new BlockPos(x*16,target.blockPosition().getY(),z*16))) return false;
        return true;
    }
    public static boolean allowBlock(ServerPlayer actor, ServerLevel level, BlockPos pos) {
        if (!enabled(level.getServer())) return true;
        if (!allow(actor,level,pos)) return false;
        var state=level.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.AbstractBedBlock) {
            var direction=state.getValue(net.minecraft.world.level.block.BedBlock.FACING);
            if (state.getValue(net.minecraft.world.level.block.BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD) direction=direction.getOpposite();
            if (!allow(actor,level,pos.relative(direction))) return false;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE)
            return allow(actor,level,net.minecraft.world.level.block.ChestBlock.getConnectedBlockPos(pos,state));
        return true;
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
