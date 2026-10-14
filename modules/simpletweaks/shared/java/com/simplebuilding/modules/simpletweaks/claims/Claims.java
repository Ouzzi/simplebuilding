package com.simplebuilding.modules.simpletweaks.claims;

import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import com.simplebuilding.framework.api.Protection.Action;
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
            if (claims.config.enabled()) com.simplebuilding.framework.api.Protection.register(server,"simpletweaks",new com.simplebuilding.framework.api.Protection.Check() {
                @Override public boolean allows(com.simplebuilding.framework.api.Protection.Target target) {
                    return allows(target,Action.ANY);
                }
                @Override public boolean allows(com.simplebuilding.framework.api.Protection.Target target,Action action) {
                    return !on() || (claims.config.opBypass() && target.administrator()) || claims.allowed(target.dimension(),target.actor(),new ChunkPos(target.x()>>4,target.z()>>4).pack(),ClaimRights.need(action));
                }
                @Override public boolean allowsAutomation(com.simplebuilding.framework.api.Protection.Target source, com.simplebuilding.framework.api.Protection.Target target) {
                    return !on() || source.dimension().equals(target.dimension()) && claims.sameOwner(source.dimension(),ChunkPos.pack(source.x()>>4,source.z()>>4),ChunkPos.pack(target.x()>>4,target.z()>>4));
                }
            });
        }
    }
    /** Super-mod switch "Enable Simple Tweaks" (framework SubMods): off = claims stay registered but protect nothing. */
    public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"); }
    public static void stop(MinecraftServer server) { install(server,null); settings=null; }
    public static Claims get(MinecraftServer server) { return SERVERS.get(server); }
    public static boolean enabled(MinecraftServer server) { var c=get(server); return on() && c!=null && c.config.enabled(); }
    /** Old callers: no action, so strict (build and destroy). */
    public static boolean allow(ServerLevel level, UUID actor, BlockPos pos) { return allow(level,actor,pos,Action.ANY); }
    public static boolean allow(ServerLevel level, UUID actor, BlockPos pos, Action action) { return allowNeed(level,actor,pos,ClaimRights.need(action)); }
    private static boolean allowNeed(ServerLevel level, UUID actor, BlockPos pos, int need) {
        var c=get(level.getServer());
        return c==null || !on() || !c.config.enabled() || c.allowed(level.dimension().identifier().toString(),actor,ChunkPos.pack(pos),need);
    }
    public static boolean allow(ServerPlayer player, ServerLevel level, BlockPos pos) { return allow(player,level,pos,Action.ANY); }
    public static boolean allow(ServerPlayer player, ServerLevel level, BlockPos pos, Action action) { return allowNeed(player,level,pos,ClaimRights.need(action)); }
    private static boolean allowNeed(ServerPlayer player, ServerLevel level, BlockPos pos, int need) {
        var c=get(level.getServer());
        return c==null || !on() || !c.config.enabled() || (c.config.opBypass() && admin(player)) || allowNeed(level,player.getUUID(),pos,need);
    }
    /** Right an interaction with an entity needs: players any right, mobs and vehicles A, frames/stands/items C, the rest strict. */
    static int entityNeed(net.minecraft.world.entity.Entity target) {
        if (target instanceof net.minecraft.world.entity.player.Player) return ClaimRights.PRESENCE;
        if (target instanceof net.minecraft.world.entity.Mob || target instanceof net.minecraft.world.entity.vehicle.VehicleEntity) return ClaimRights.ANIMALS;
        if (target instanceof net.minecraft.world.entity.decoration.ItemFrame || target instanceof net.minecraft.world.entity.decoration.ArmorStand
                || target instanceof net.minecraft.world.entity.item.ItemEntity) return ClaimRights.CONTAINER;
        return ClaimRights.STRICT;
    }
    public static boolean allowEntity(ServerPlayer actor, net.minecraft.world.entity.Entity target) {
        if (!(target.level() instanceof ServerLevel level) || !enabled(level.getServer())) return true;
        int need=entityNeed(target);
        var box=target.getBoundingBox();
        int minX=net.minecraft.util.Mth.floor(box.minX)>>4, maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4;
        int minZ=net.minecraft.util.Mth.floor(box.minZ)>>4, maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;
        if ((long)(maxX-minX+1)*(maxZ-minZ+1)>256) return false;
        for (int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++)
            if (!allowNeed(actor,level,new BlockPos(x*16,target.blockPosition().getY(),z*16),need)) return false;
        return true;
    }
    public static boolean allowBlock(ServerPlayer actor, ServerLevel level, BlockPos pos) { return allowBlock(actor,level,pos,Action.ANY); }
    public static boolean allowBlock(ServerPlayer actor, ServerLevel level, BlockPos pos, Action action) {
        if (!enabled(level.getServer())) return true;
        if (!allow(actor,level,pos,action)) return false;
        var state=level.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.AbstractBedBlock) {
            var direction=state.getValue(net.minecraft.world.level.block.BedBlock.FACING);
            if (state.getValue(net.minecraft.world.level.block.BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD) direction=direction.getOpposite();
            if (!allow(actor,level,pos.relative(direction),action)) return false;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE)
            return allow(actor,level,net.minecraft.world.level.block.ChestBlock.getConnectedBlockPos(pos,state),action);
        return true;
    }
    static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> USE_TAG=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletweaks","claim_use"));
    static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> REDSTONE_TAG=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletweaks","claim_redstone"));
    /**
     * Right a right-click on a block needs. While the block would use the click (not sneaking with an item in hand):
     * redstone tag R, use tag U, container or menu block C. Otherwise a block item places (B), and everything else,
     * including every unknown block, is strict (B and D).
     */
    public static Action useAction(ServerPlayer player, BlockPos pos, net.minecraft.world.item.ItemStack stack) {
        var level=player.level();
        boolean placing=stack.getItem() instanceof net.minecraft.world.item.BlockItem;
        boolean suppress=player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty());
        if (!suppress) {
            var state=level.getBlockState(pos);
            if (state.is(REDSTONE_TAG)) return Action.REDSTONE;
            if (state.is(USE_TAG)) return Action.USE;
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container || state.getMenuProvider(level,pos)!=null) return Action.CONTAINER;
        }
        return placing ? Action.BUILD : Action.ANY;
    }
    public static boolean admin(ServerPlayer player) { return Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack()); }
    private boolean ready() {
        if (!config.enabled() || failed) return false;
        if (store==null) try { store=new ClaimStore(root.resolve("simpletweaks-claims.json"),legacy); }
        catch (Exception e) { failed=true; LOG.error("Claims are locked: invalid or unreadable data. Original files preserved.",e); }
        return !failed;
    }
    public boolean allowed(String dimension, UUID actor, long chunk) { return allowed(dimension,actor,chunk,ClaimRights.STRICT); }
    /** {@code need}: right bits that must all be held ({@link ClaimRights#PRESENCE}: at least one). Unclaimed land allows everything. */
    public boolean allowed(String dimension, UUID actor, long chunk, int need) {
        if (!config.enabled()) return true;
        if (!ready()) return false;
        var key=new ClaimStore.Key(dimension,chunk);
        if (!store.view().containsKey(key)) return true;
        if (actor==null) return false;
        int rights=store.rights(key,actor);
        return need==ClaimRights.PRESENCE ? rights!=0 : (rights&need)==need;
    }
    /** Rights of a player in a chunk (all rights on unclaimed land); 0 while locked. */
    public int rights(String dimension, UUID actor, long chunk) {
        return ready() ? store.rights(new ClaimStore.Key(dimension,chunk),actor) : 0;
    }
    public Map<ClaimStore.Key,ClaimStore.Claim> view() {
        return ready() ? store.view() : Map.of();
    }
    public Map<UUID,ClaimStore.Land> lands() {
        return ready() ? store.lands() : Map.of();
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
                || !mutationReady(owner,tick)) return false;
        var next=new HashMap<>(store.view()); next.put(key,new ClaimStore.Claim(owner));
        if (!commit(next)) return false;
        cooldowns.put(owner,tick); return true;
    }
    /** Successful ledger mutations share one bounded server-tick budget across dimensions. */
    private boolean mutationReady(UUID actor,long tick) {
        cooldowns.entrySet().removeIf(e->tick>=e.getValue() && tick-e.getValue()>=config.cooldownTicks());
        return !cooldowns.containsKey(actor) && cooldowns.size()<ClaimConfig.MAX_GLOBAL;
    }
    /** Per-chunk command path, old meaning: trusting grants {@code @member} (everything but managing). */
    public boolean trust(ServerPlayer actor,UUID target,boolean grant) { return trust(actor,target,grant?ClaimRights.LEGACY:0); }
    /**
     * Sets the rights of one person for the chunk the owner stands in (0 revokes). The chunk gets a section that starts
     * as a copy of the rules it had before; the code text is regenerated. Managing cannot be granted per chunk.
     */
    public boolean trust(ServerPlayer actor,UUID target,int rights) {
        if (!ready() || target==null || target.equals(new UUID(0,0)) || rights<0 || (rights&~ClaimRights.ALL)!=0 || (rights&ClaimRights.MANAGE)!=0) return false;
        var key=new ClaimStore.Key(actor.level().dimension().identifier().toString(),actor.chunkPosition().pack());
        var claim=store.view().get(key);
        // Trust never delegates administration, including when OP4 protection bypass is enabled.
        if (claim==null || !claim.owner().equals(actor.getUUID()) || target.equals(claim.owner())) return false;
        var land=store.lands().getOrDefault(claim.owner(),new ClaimStore.Land(claim.owner(),"",ClaimRights.Rules.EMPTY,Map.of()));
        var rules=land.rulesAt(key);
        int current=rules.members().getOrDefault(target,0);
        if (current==rights || (current==0 && rights!=0 && rules.members().size()>=config.maxTrustedPlayers())) return false;
        long tick=actor.level().getServer().overworld().getGameTime();
        if (!mutationReady(actor.getUUID(),tick)) return false;
        var sections=new HashMap<>(land.sections());
        var changed=rules.with(target,rights);
        if (changed.equals(land.top())) sections.remove(key); else sections.put(key,changed);
        var nextLands=new HashMap<>(store.lands());nextLands.put(claim.owner(),withCode(land.owner(),land.top(),sections));
        if (!commit(store.view(),nextLands)) return false;
        cooldowns.put(actor.getUUID(),tick);return true;
    }
    /** Land record with regenerated code (empty if the structure exceeds the editor limits; the structure stays authoritative). */
    private static ClaimStore.Land withCode(UUID owner,ClaimRights.Rules top,Map<ClaimStore.Key,ClaimRights.Rules> sections) {
        var at=new HashMap<ClaimCode.At,ClaimRights.Rules>();sections.forEach((k,r)->at.put(k.at(),r));
        return new ClaimStore.Land(owner,Objects.requireNonNullElse(ClaimCode.generate(top,at),""),top,sections);
    }
    public record ApplyResult(boolean ok,List<ClaimCode.Error> errors) {}
    private static ApplyResult fail(String key,String arg) { return new ApplyResult(false,List.of(new ClaimCode.Error(0,key,arg))); }
    /** The owner edits their own land. */
    public ApplyResult applyCode(ServerPlayer actor,String code) { return applyCode(actor,actor.getUUID(),code); }
    /**
     * Replaces the rules of {@code owner}'s land with a rights code. The server parses and decides: hard code limits,
     * names resolve only to online players, sections only for chunks the owner holds, one commit, all or nothing.
     * Managers (right M) may only change persons without M and only grant rights they hold themselves.
     */
    public ApplyResult applyCode(ServerPlayer actor,UUID owner,String code) {
        if (!ready()) return fail("locked","");
        if (store.view().values().stream().noneMatch(c->c.owner().equals(owner))) return fail("no_land","");
        var land=store.lands().getOrDefault(owner,new ClaimStore.Land(owner,"",ClaimRights.Rules.EMPTY,Map.of()));
        boolean isOwner=owner.equals(actor.getUUID());
        if (!isOwner && (land.top().of(actor.getUUID())&ClaimRights.MANAGE)==0) return fail("denied","");
        long tick=actor.level().getServer().overworld().getGameTime();
        if (!mutationReady(actor.getUUID(),tick)) return fail("cooldown","");
        var players=actor.level().getServer().getPlayerList();
        var parsed=ClaimCode.parse(code,owner,actor.level().dimension().identifier().toString(),name->{var p=players.getPlayerByName(name);return p==null?null:p.getUUID();},config.maxTrustedPlayers());
        if (!parsed.ok()) return new ApplyResult(false,parsed.errors());
        var errors=new ArrayList<ClaimCode.Error>();var sections=new HashMap<ClaimStore.Key,ClaimRights.Rules>();
        parsed.sections().forEach((at,rules)->{
            int line=parsed.sectionLines().getOrDefault(at,0);
            try {
                var key=ClaimStore.Key.of(at);var claim=store.view().get(key);
                if (claim==null || !claim.owner().equals(owner)) errors.add(new ClaimCode.Error(line,"foreign_chunk",at.x()+","+at.z()));
                else sections.put(key,rules);
            } catch (IllegalArgumentException e) { errors.add(new ClaimCode.Error(line,"bad_section",at.dimension())); }
        });
        if (!errors.isEmpty()) return new ApplyResult(false,errors.stream().limit(ClaimCode.MAX_ERRORS).toList());
        if (!isOwner && !managerMayChange(actor.getUUID(),land,parsed.top(),sections)) return fail("manager_limit","");
        var nextLands=new HashMap<>(store.lands());nextLands.put(owner,new ClaimStore.Land(owner,code,parsed.top(),sections));
        if (!commit(store.view(),nextLands)) return fail("locked","");
        cooldowns.put(actor.getUUID(),tick);return new ApplyResult(true,List.of());
    }
    /** Managers: no change to `*`, sections or anyone holding M, no M granted, only own rights handed out. */
    private static boolean managerMayChange(UUID manager,ClaimStore.Land old,ClaimRights.Rules top,Map<ClaimStore.Key,ClaimRights.Rules> sections) {
        if (old.top().everyone()!=top.everyone() || !old.sections().equals(sections)) return false;
        int own=old.top().of(manager);
        var people=new HashSet<>(old.top().members().keySet());people.addAll(top.members().keySet());
        for (var id:people) {
            int before=old.top().members().getOrDefault(id,0),after=top.members().getOrDefault(id,0);
            if (before==after) continue;
            if (id.equals(manager) || ((before|after)&ClaimRights.MANAGE)!=0 || ((before^after)&~own)!=0) return false;
        }
        return true;
    }
    public boolean unclaim(ServerPlayer actor,boolean administrative) {
        if (!ready()) return false;
        var key=new ClaimStore.Key(actor.level().dimension().identifier().toString(),actor.chunkPosition().pack());
        var claim=store.view().get(key);
        if (claim==null || (administrative ? !admin(actor) : !claim.owner().equals(actor.getUUID()))) return false;
        long tick=actor.level().getServer().overworld().getGameTime();
        if (!mutationReady(actor.getUUID(),tick)) return false;
        var next=new HashMap<>(store.view());next.remove(key);
        if (!commit(next,store.lands())) return false;
        cooldowns.put(actor.getUUID(),tick);return true;
    }
    private boolean commit(Map<ClaimStore.Key,ClaimStore.Claim> next) { return commit(next,store.lands()); }
    private boolean commit(Map<ClaimStore.Key,ClaimStore.Claim> next,Map<UUID,ClaimStore.Land> nextLands) {
        try { store.replace(next,nextLands); return true; }
        catch (Exception e) { failed=true; LOG.error("Claims are locked: atomic save failed; last saved data preserved.",e); return false; }
    }
    public static boolean anyEnabled(){return on() && SERVERS.values().stream().anyMatch(c->c.config.enabled());}
    public static boolean environment(ServerLevel level,BlockPos pos){return allow(level,null,pos);}
    /** Destruction can update another half even when only this cell is directly hit. */
    public static boolean environmentBlock(ServerLevel level,BlockPos pos){
        if(!enabled(level.getServer()))return true;
        for(var part:parts(level,pos))if(!environment(level,part))return false;
        return true;
    }
    private static java.util.List<BlockPos> parts(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);var positions=new ArrayList<BlockPos>();positions.add(pos);
        if(state.getBlock() instanceof net.minecraft.world.level.block.AbstractBedBlock){var direction=state.getValue(net.minecraft.world.level.block.BedBlock.FACING);if(state.getValue(net.minecraft.world.level.block.BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.HEAD)direction=direction.getOpposite();positions.add(pos.relative(direction));}
        if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE)positions.add(net.minecraft.world.level.block.ChestBlock.getConnectedBlockPos(pos,state));
        return positions;
    }
    public static boolean transfer(ServerLevel level,BlockPos source,BlockPos target){
        var c=get(level.getServer());if(c==null||!on()||!c.config.enabled())return true;if(!c.ready())return false;
        String dim=level.dimension().identifier().toString();
        var positions=new ArrayList<>(parts(level,source));positions.addAll(parts(level,target));
        for(var p:positions)if(!c.sameOwner(dim,ChunkPos.pack(source),ChunkPos.pack(p)))return false;
        return true;
    }
    private boolean sameOwner(String dimension,long source,long target){
        if(!config.enabled())return true;
        if(!ready())return false;
        var a=store.view().get(new ClaimStore.Key(dimension,source));
        var b=store.view().get(new ClaimStore.Key(dimension,target));
        return Objects.equals(a==null?null:a.owner(),b==null?null:b.owner());
    }
    /**
     * Claims protect land and claimed property from players. A responsible player needs rights at every
     * chunk of the target. Mobs and natural damage stay Vanilla, so a claim is never a mob-immune zone.
     * Unattributed explosions are environmental grief: they spare claimed non-player entities only.
     */
    public static boolean damage(net.minecraft.world.damagesource.DamageSource source,net.minecraft.world.entity.Entity target){
        if(!(target.level() instanceof ServerLevel level)||!enabled(level.getServer()))return true;
        var raw=source.getEntity()!=null?source.getEntity():source.getDirectEntity();
        var actor=responsible(raw);
        if(actor instanceof ServerPlayer player)return allowEntity(player,target);
        if(source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))return action(null,target);
        return true;
    }
    private static net.minecraft.world.entity.Entity responsible(net.minecraft.world.entity.Entity actor){
        for(int i=0;i<8;i++){
            if(actor instanceof net.minecraft.world.entity.projectile.Projectile p)actor=p.getOwner();
            else if(actor instanceof net.minecraft.world.entity.AreaEffectCloud cloud)actor=cloud.getOwner();
            else if(actor instanceof net.minecraft.world.entity.LightningBolt bolt)actor=bolt.getCause();
            else return actor;
        }return null;
    }
    public static boolean action(net.minecraft.world.entity.Entity actor,ServerLevel level,BlockPos pos){
        if(!enabled(level.getServer()))return true;actor=responsible(actor);
        return actor instanceof ServerPlayer player?allowBlock(player,level,pos):environmentBlock(level,pos);
    }
    public static boolean action(net.minecraft.world.entity.Entity actor,net.minecraft.world.entity.Entity target){
        if(!(target.level() instanceof ServerLevel level)||!enabled(level.getServer()))return true;
        actor=responsible(actor);if(actor instanceof ServerPlayer player)return allowEntity(player,target);
        // Mob attacks and players hit by the environment remain Vanilla; claimed property is spared.
        if(actor!=null||target instanceof net.minecraft.world.entity.player.Player)return true;
        var box=target.getBoundingBox();int minX=net.minecraft.util.Mth.floor(box.minX)>>4,maxX=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4,minZ=net.minecraft.util.Mth.floor(box.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;
        if((long)(maxX-minX+1)*(maxZ-minZ+1)>256)return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!environment(level,new BlockPos(x*16,target.blockPosition().getY(),z*16)))return false;return true;
    }
    /**
     * Block changes caused by an entity: players need rights; hostile or unattributed causes never change
     * claimed land; other mobs (villagers, sheep, boats) only act within the land they stand on.
     */
    public static boolean mob(net.minecraft.world.entity.Entity actor,ServerLevel level,BlockPos pos){
        if(!enabled(level.getServer()))return true;
        var who=responsible(actor);
        // Everything routed here is destruction (break, trample, egg, removal): a player needs D.
        if(who instanceof ServerPlayer player)return allowBlock(player,level,pos,Action.DESTROY);
        if(who==null||who instanceof net.minecraft.world.entity.monster.Enemy)return environmentBlock(level,pos);
        return transfer(level,who.blockPosition(),pos);
    }
    /** Land owner at a position: empty for wilderness or inactive claims, null while the ledger is locked. */
    public static Optional<UUID> owner(ServerLevel level,BlockPos pos){
        var c=get(level.getServer());if(c==null||!on()||!c.config.enabled())return Optional.empty();
        if(!c.ready())return null;
        var claim=c.store.view().get(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(pos)));
        return claim==null?Optional.empty():Optional.of(claim.owner());
    }

    private Claims() { throw new AssertionError(); }
}
