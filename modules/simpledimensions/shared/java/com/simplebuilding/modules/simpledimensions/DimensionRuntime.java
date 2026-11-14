package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.portal.*;
import dev.simpledimension.common.config.DimensionConfigStore;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.simplebuilding.framework.api.Protection;
/** Per-server state: game ticks, exact persisted return addresses, bounded builds, no chunk tickets. */
public final class DimensionRuntime {
 private static final Map<MinecraftServer,DimensionRuntime> SERVERS=new WeakHashMap<>();
 /** Config folder {@code <loader config dir>/simpledimensions}, set by {@link #configure} (REV2-A-5b: never the working directory). */
 public static volatile Path CONFIG_ROOT=Path.of("config","simpledimensions");
 /** Called first by every loader entry: config folder from the loader, old {@code simpledimension} folder taken over once, id moves. */
 public static void configure(Path loaderConfigDir){
  CONFIG_ROOT=loaderConfigDir.resolve(LegacyIds.NEW);LegacyIds.adoptConfig(loaderConfigDir,CONFIG_ROOT);LegacyIds.registerIdMoves();
 }
 public final List<DimensionPortalConfig> configs;
 public final DimensionSettings settings;
 private final MinecraftServer server;
 private final Map<UUID,Contact> contacts=new HashMap<>();
 private final Map<UUID,ReturnAddress> returns=new HashMap<>();
 private final Set<String> portals=new HashSet<>();
 private long lastBuildTick=-1;
 private int decayCursor;
 private final Map<UUID,Long> ignitions=new HashMap<>();
 public boolean canIgnite(UUID id){long now=server.getTickCount();long last=ignitions.getOrDefault(id,-100L);if(now-last<10)return false;ignitions.put(id,now);return true;}
 private final Path ledger;
 public static boolean claimIntegrationRequired=false;
 /** Claim mods without a protection adapter here: while one is loaded, lighting portals and entering mod dimensions stay locked. */
 public static final List<String> FOREIGN_CLAIM_MODS=List.of("flan","ftbchunks","openpartiesandclaims","griefdefender","claimchunk");
 /**
  * Loader entry (owner AUDSB31E2 F3=B): the loaded {@link #FOREIGN_CLAIM_MODS}. They lock lighting and entering, never the way out
  * (generated exits, emergency return, void rescue); the lock shows like every refusal (dry click + smoke, no screen text) and one
  * log line at start names the mods.
  */
 public static void foreignClaimMods(List<String> loaded){
  claimIntegrationRequired=!loaded.isEmpty();
  if(claimIntegrationRequired)org.slf4j.LoggerFactory.getLogger("simpledimensions").warn("Claim mod(s) {} loaded without a Simple Dimensions protection adapter: lighting portals and entering mod dimensions stay locked; exits and the emergency return stay open.",loaded);
 }
 /** True while a player is on the way out (generated exit, emergency return): the foreign-claim-mod lock does not apply (F3=B). */
 private boolean leaving;
 public interface Permission {boolean allow(ServerPlayer player,ServerLevel level,BlockPos pos);}
 public static Permission permission=null;
 public boolean permitted(ServerPlayer p,ServerLevel l,BlockPos pos){
  // A framework provider never satisfies the separate unsupported-foreign-mod gate.
  return l.mayInteract(p,pos)&&(leaving||!claimIntegrationRequired||permission!=null)&&(permission==null||permission.allow(p,l,pos))
   &&(!Protection.active(server)||Protection.allows(server,new Protection.Target(p.getUUID(),l.dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_OWNERS).test(p.createCommandSourceStack()))));
 }
 private boolean protectedAccess(){return Protection.active(server)||claimIntegrationRequired||permission!=null;}
 boolean permittedShape(ServerPlayer p,ServerLevel l,MatchedPortal shape){
  if(!protectedAccess())return true;
  for(var cell:shape.frame())if(!permitted(p,l,pos(cell.pos())))return false;
  for(var cell:shape.interior())if(!permitted(p,l,pos(cell)))return false;
  return true;
 }
 private boolean permittedLanding(ServerPlayer p,ServerLevel l,BlockPos at){
  if(!permitted(p,l,at))return false;
  if(!protectedAccess())return true;
  var box=p.getBoundingBox().move(at.getX()+.5-p.getX(),at.getY()-p.getY(),at.getZ()+.5-p.getZ());
  return permittedBox(p,l,box);
 }
 private boolean permittedBox(ServerPlayer p,ServerLevel l,net.minecraft.world.phys.AABB box){
  // Same envelope as portal contact scanning; include every occupied column and its supporting floor.
  if(box.maxX-box.minX>3||box.maxY-box.minY>5||box.maxZ-box.minZ>3)return false;
  for(var cell:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-1,box.minZ),BlockPos.containing(Math.nextDown(box.maxX),Math.nextDown(box.maxY),Math.nextDown(box.maxZ))))
   if(!permitted(p,l,cell))return false;
  return true;
 }
 private boolean permittedPortal(ServerPlayer p,ServerLevel l,BlockPos contact,SkyPortalBlockEntity be,DimensionPortalConfig cfg){
  if(!protectedAccess())return true;
  if(!permittedLanding(p,l,contact))return false;
  if(be.generated)return permittedLanding(p,l,be.anchor)&&permitted(p,l,be.anchor.above());
  if(cfg==null)return false;
  var shape=PortalActivationService.match(new PortalWorld(l,cfg),cfg,contact.getX(),contact.getY(),contact.getZ());
  return shape.isPresent()&&permittedShape(p,l,shape.get());
 }
 private boolean permittedArrival(ServerPlayer p,ServerLevel l,BlockPos at){
  return permittedLanding(p,l,at)&&(!protectedAccess()||!(l.getBlockEntity(at) instanceof SkyPortalBlockEntity be)
   ||permittedPortal(p,l,at,be,config(be.definition)));
 }
 public record ReturnAddress(String dimension,long pos){}
 private static final class Contact {String portal="";int warmup;long cooldown;boolean exit;long rescueAt;boolean preloaded;}
 public static DimensionRuntime get(MinecraftServer s){return SERVERS.computeIfAbsent(s,DimensionRuntime::new);}
 public static void stop(MinecraftServer s){SERVERS.remove(s);}
 /** CMD24: a new settings object (from {@code /simpledimensions config}) is saved and taken over by every running server at once. */
 public static void applySettings(DimensionSettings n){n.normalize();n.save(CONFIG_ROOT);SERVERS.values().forEach(r->r.settings.copyFrom(n));}
 private DimensionRuntime(MinecraftServer s){
  server=s;configs=DimensionConfigStore.loadAndGenerate(CONFIG_ROOT,net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA).major());
  settings=DimensionSettings.load(CONFIG_ROOT);ledger=s.getWorldPath(LevelResource.ROOT).resolve("simpledimensions-returns.json");
  // WD25-14: a world of the old mod id keeps its ledger under the old name; read once, the next save writes the new file.
  var source=Files.exists(ledger)?ledger:ledger.resolveSibling(LegacyIds.OLD+"-returns.json");
  if(Files.exists(source))readLedger(source,returns,portals);
 }
 /**
  * Reads the return ledger into the two collections. Owner AUDSB31E2 F4=A: a broken, too big or linked file no longer stops the server at
  * every start - it is kept as {@code <name>.broken-<time>} (framework {@code ConfigFiles}), a warning names it and the server starts with an
  * empty ledger (every portal keeps its own way back in its block entity, else the emergency return to the spawn applies). True if read.
  */
 static boolean readLedger(Path source,Map<UUID,ReturnAddress> returns,Set<String> portals){
  try{
   if(Files.size(source)>1048576||Files.isSymbolicLink(source))throw new IllegalStateException("Unsafe ledger");
   var data=JsonParser.parseString(Files.readString(source)).getAsJsonObject();
   for(var e:data.getAsJsonObject("returns").entrySet()) {if(returns.size()>=ConfigLimits.MAX_RETURN_RECORDS)break;var r=new Gson().fromJson(e.getValue(),ReturnAddress.class);returns.put(UUID.fromString(e.getKey()),new ReturnAddress(LegacyIds.migrate(r.dimension()),r.pos()));}
   for(var e:data.getAsJsonArray("portals")){if(portals.size()>=ConfigLimits.MAX_PORTALS)break;portals.add(LegacyIds.migrate(e.getAsString()));}
   return true;
  }catch(Exception e){
   returns.clear();portals.clear();
   org.slf4j.LoggerFactory.getLogger("simpledimensions").warn("Unreadable return ledger {}, starting with an empty one ({}); old file kept as {}",source.getFileName(),e.toString(),com.simplebuilding.framework.api.ConfigFiles.quarantine(source));
   return false;
  }
 }
 private void save(){
  try{var root=new JsonObject();root.add("returns",new Gson().toJsonTree(returns));root.add("portals",new Gson().toJsonTree(portals));
   var temp=ledger.resolveSibling("simpledimensions-returns.tmp");Files.writeString(temp,new Gson().toJson(root));Files.move(temp,ledger,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }catch(Exception e){org.slf4j.LoggerFactory.getLogger("simpledimensions").error("Cannot persist safe returns (will retry on the next change)",e);}
 }
 static String key(ServerLevel l,BlockPos p){return l.dimension().identifier()+"@"+p.asLong();}
 public boolean reserve(ServerLevel l,BlockPos3i p){return portals.size()<ConfigLimits.MAX_PORTALS||portals.contains(key(l,pos(p)));}
 public void recordPortal(ServerLevel l,BlockPos3i p){portals.add(key(l,pos(p)));save();}
 public DimensionPortalConfig config(String id){return configs.stream().filter(c->c.id.equals(id)).findFirst().orElse(null);}
 public static BlockPos pos(BlockPos3i p){return new BlockPos(p.x(),p.y(),p.z());}
 public static void signal(ServerLevel l,BlockPos p,boolean success){
  // Same cue family as SimpleBuilding and the other modules: success its own sound, failure the dry dispenser click.
  if(success)l.playSound(null,p,SoundEvents.PORTAL_TRIGGER,SoundSource.BLOCKS,.4f,1f);
  else l.playSound(null,p,SoundEvents.DISPENSER_FAIL,SoundSource.BLOCKS,.5f,1.2f);
  l.sendParticles(success?ParticleTypes.PORTAL:ParticleTypes.SMOKE,p.getX()+.5,p.getY()+.5,p.getZ()+.5,success?6:4,.3,.3,.3,.01);
 }
 /**
  * One ledger entry per visit (audit simpledimensions-P2-1): a lit source portal is checked for its frame (decay). An entry whose
  * anchor chunk is loaded but whose anchor cell is no portal any more (frame broken, portal gone), or whose key cannot be read, is
  * forgotten: {@link ConfigLimits#MAX_PORTALS} then counts portals that exist, not every portal ever lit (before, a long-running
  * server ran out of records and no portal could be lit or built any more). True if the record was forgotten.
  */
 boolean decay(String k){
  ServerLevel l;BlockPos p;
  try{int split=k.lastIndexOf('@');l=level(k.substring(0,split));p=BlockPos.of(Long.parseLong(k.substring(split+1)));}catch(RuntimeException e){return portals.remove(k);}
  if(l==null||!l.hasChunkAt(p))return false;
  if(!(l.getBlockEntity(p) instanceof SkyPortalBlockEntity b))return portals.remove(k);
  if(!b.generated){var def=config(b.definition);if(def!=null&&!validFrame(l,p,b,def))return portals.remove(k);}
  return false;
 }
 boolean recorded(String key){return portals.contains(key);}
 public void tick(){
  if(!portals.isEmpty()) {
   var anchors=new ArrayList<>(portals);boolean forgot=false;
   for(int i=0;i<Math.min(ConfigLimits.MAX_DECAY_SCANS_PER_TICK,anchors.size());i++)forgot|=decay(anchors.get(Math.floorMod(decayCursor++,anchors.size())));
   if(forgot)save();
  }
  ignitions.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
  contacts.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
  boolean dropped=false;
  for(var p:server.getPlayerList().getPlayers()){
   // PLAN40 simpledimensions-4: a dead player no longer needs the way back; the entry would only fill the 4096-record ledger.
   if(p.isDeadOrDying())dropped|=returns.remove(p.getUUID())!=null;
   tickPlayer(p);
  }
  if(dropped)save();
 }
 // Test entries: all game tests share one server, so a parallel test's build in the same tick must not fail an unrelated travel on the one-build-per-tick throttle.
 boolean travelTest(ServerPlayer player,BlockPos contact){lastBuildTick=-1;return travel(player,contact);}
 void tickPlayerTest(ServerPlayer p){lastBuildTick=-1;tickPlayer(p);}
 public void tickPlayer(ServerPlayer p){
   var l=p.level();var touched=DimensionRegistry.on()?findTouched(l,p):null;var c=contacts.computeIfAbsent(p.getUUID(),id->new Contact());
   if(touched==null){c.portal="";c.warmup=0;c.exit=false;}
   else if(!c.exit&&server.getTickCount()>=c.cooldown){
    String current=key(l,touched);if(!current.equals(c.portal)){c.portal=current;c.warmup=0;}
    if(l.getBlockEntity(touched) instanceof SkyPortalBlockEntity be){
     var cfg=config(be.definition);
     int delay=Math.max(settings.portalDelayTicks,cfg==null?0:cfg.portalDelayTicks);
     if(++c.warmup>delay){c.warmup=0;if(travel(p,touched)) {c.cooldown=server.getTickCount()+Math.max(settings.teleportCooldownTicks,cfg==null?60:cfg.teleportCooldownTicks);c.exit=true;}else{c.cooldown=server.getTickCount()+20;signal(l,touched,false);}}
    }
   }
   if(p.level()==l&&l.dimension().identifier().getNamespace().equals("simpledimensions"))rescue(p,l,c);
 }
 /** Rows above the rescue line in which a falling player's destination chunks are loaded ahead (F5). */
 private static final int VOID_PRELOAD_ROWS=24;
 DimensionPortalConfig definitionOf(ServerLevel l){var id=l.dimension().identifier().toString();return configs.stream().filter(d->d.targetDimensionId.equals(id)).findFirst().orElse(null);}
 /**
  * Mod dimensions: a player below {@code minY + 3} - or anywhere in a dimension without definition - is rescued as the definition's
  * {@code voidFall} says (owner AUDSB31E2 F5: "home" = the emergency return as before and the default, "spawn", "sky", "vanilla" = no
  * rescue). A falling player within {@link #VOID_PRELOAD_ROWS} above that line gets the destination chunks loaded ahead.
  */
 private void rescue(ServerPlayer p,ServerLevel l,Contact c){
  var def=definitionOf(l);var mode=def==null?"home":ConfigLimits.voidFall(def.voidFall);
  if(def!=null&&p.getY()>=l.getMinY()+3){
   boolean falling=!p.onGround()&&p.fallDistance>1;
   if(!falling)c.preloaded=false;
   else if(!c.preloaded&&p.getY()<l.getMinY()+3+VOID_PRELOAD_ROWS&&!mode.equals("vanilla")){c.preloaded=true;var spot=voidSpot(p,def,mode);spot.level().getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL,net.minecraft.world.level.ChunkPos.containing(spot.pos()),2);}
   return;
  }
  if(mode.equals("vanilla")||server.getTickCount()<c.rescueAt)return;
  // REV2-A: a failed rescue (chunk loads, ~3000 checks) retries once a second, not every tick
  if(voidFall(p,def,mode))c.preloaded=false;else c.rescueAt=server.getTickCount()+20;
 }
 /** Where a void fall of {@code mode} leads (the chunk that is loaded ahead; "home" without address and "spawn": the overworld spawn). */
 record VoidSpot(ServerLevel level,BlockPos pos){}
 VoidSpot voidSpot(ServerPlayer p,DimensionPortalConfig def,String mode){
  var overworld=server.overworld();
  if(mode.equals("sky"))return new VoidSpot(overworld,skyTarget(p.blockPosition(),def));
  var address=mode.equals("home")?returns.get(p.getUUID()):null;var home=address==null?null:level(address.dimension());
  return home==null?new VoidSpot(overworld,overworld.getRespawnData().pos()):new VoidSpot(home,BlockPos.of(address.pos()));
 }
 /**
  * F5 "sky": the overworld column above the player's position taken back through the portal travel - the definition's
  * {@code travelCoordinateScale}, then Vanilla's scale from its origin dimension to the overworld - inside the world border, at
  * {@code voidFallHeight} kept below the build limit.
  */
 BlockPos skyTarget(BlockPos at,DimensionPortalConfig def){
  var overworld=server.overworld();var origin=level(def.sourceDimensionId);if(origin==null)origin=overworld;
  var mapped=PortalTravelRules.toSource(new BlockPos3i(at.getX(),at.getY(),at.getZ()),def.travelCoordinateScale);
  double factor=net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(origin.dimensionType(),overworld.dimensionType());
  int y=Math.max(overworld.getMinY()+8,Math.min(def.voidFallHeight,overworld.getMaxY()-2));
  return overworld.getWorldBorder().clampToBounds(mapped.x()*factor,y,mapped.z()*factor).atY(y);
 }
 boolean voidFall(ServerPlayer p,DimensionPortalConfig def,String mode){
  return switch(mode){
   case "spawn" -> emergencyReturn(p,false);
   case "sky" -> skyFall(p,def);
   default -> emergencyReturn(p,true);
  };
 }
 /** F5 "sky": into the overworld sky, still falling (no safe ground needed, nothing is built); the travel back counts as done. */
 private boolean skyFall(ServerPlayer p,DimensionPortalConfig def){
  var target=server.overworld();var at=skyTarget(p.blockPosition(),def);target.getChunk(at.getX()>>4,at.getZ()>>4);
  boolean done=p.teleport(new net.minecraft.world.level.portal.TeleportTransition(target,new net.minecraft.world.phys.Vec3(at.getX()+.5,at.getY(),at.getZ()+.5),
   new net.minecraft.world.phys.Vec3(0,Math.min(0,p.getDeltaMovement().y),0),p.getYRot(),p.getXRot(),net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING))!=null;
  if(done){p.fallDistance=0;signal(target,at,true);if(returns.remove(p.getUUID())!=null)save();}
  return done;
 }
 private BlockPos findTouched(ServerLevel l,ServerPlayer p){
  var box=p.getBoundingBox();if(box.maxX-box.minX>3||box.maxY-box.minY>5||box.maxZ-box.minZ>3)return null;for(var q:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(DimensionRegistry.portal(l.getBlockState(q)))return q.immutable();return null;
 }
 public static boolean safe(ServerLevel l,BlockPos p){
  if(p.getY()<=l.getMinY()||p.getY()+2>=l.getMaxY()||!l.getWorldBorder().isWithinBounds(p)||!l.hasChunkAt(p))return false;
  var ground=l.getBlockState(p.below());return ground.isFaceSturdy(l,p.below(),Direction.UP)&&!ground.is(Blocks.MAGMA_BLOCK)&&!ground.is(Blocks.CAMPFIRE)&&!ground.is(Blocks.SOUL_CAMPFIRE)&&
   clear(l,p)&&clear(l,p.above());
 }
 private static boolean clear(ServerLevel l,BlockPos p){var s=l.getBlockState(p);return (s.isAir()||DimensionRegistry.portal(s))&&s.getFluidState().isEmpty();}
 private BlockPos safeNearby(ServerPlayer player,ServerLevel l,BlockPos center){
  for(int r=0;r<=ConfigLimits.SAFE_SEARCH_RADIUS;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++) {if(Math.abs(dx)!=r&&Math.abs(dz)!=r)continue;for(int dy=0;dy<=8;dy++){var p=center.offset(dx,dy,dz);if(safe(l,p)&&permittedArrival(player,l,p))return p;}}
  return null;
 }
 private ServerLevel level(String id){var value=Identifier.tryParse(id);return value==null?null:server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,value));}
 public boolean travel(ServerPlayer player,BlockPos contact){
  // Owner AUDSB31E2 F3=B: a generated exit always lets the player out, also while a foreign claim mod locks lighting and entering.
  boolean exit=player.level().getBlockEntity(contact) instanceof SkyPortalBlockEntity b&&b.generated;boolean was=leaving;leaving=was||exit;
  try{return travelOnce(player,contact);}finally{leaving=was;}
 }
 private boolean travelOnce(ServerPlayer player,BlockPos contact){
  var source=player.level();if(!DimensionRegistry.on()||player.isPassenger()||player.isVehicle()||player.isSpectator()||!(source.getBlockEntity(contact) instanceof SkyPortalBlockEntity be))return false;
  var cfg=config(be.definition);
  if(cfg==null&&be.definition.isEmpty()) {
   if(source.dimension().identifier().getNamespace().equals("simpledimensions"))return emergencyReturn(player);
   for(var candidate:configs){
    if(!candidate.sourceDimensionId.equals(source.dimension().identifier().toString())&&!candidate.openFromDimensions.contains(source.dimension().identifier().toString()))continue;
    boolean legacy=source.getBlockState(contact).is(DimensionRegistry.LEGACY);
    if(!candidate.targetDimensionId.equals(be.getDestination())&&!(legacy&&candidate.id.equals("skyblock")&&be.getDestination().equals("minecraft:overworld")))continue;
    var matched=PortalActivationService.match(new PortalWorld(source,candidate),candidate,contact.getX(),contact.getY(),contact.getZ());
    if(matched.isPresent()){var shape=matched.get();if(!permittedShape(player,source,shape))return false;for(var cell:shape.interior())if(source.getBlockEntity(pos(cell)) instanceof SkyPortalBlockEntity old){old.define(candidate.id,pos(shape.anchor()),false);old.configure(old.getColor(),candidate.targetDimensionId);}cfg=candidate;break;}
   }
  }
  // Generated exits allow escape from a newly denied source, never entry into a denied destination.
  if(!permittedPortal(player,source,contact,be,cfg)|| (protectedAccess()&&!permittedBox(player,source,player.getBoundingBox())))return be.generated&&emergencyReturn(player);
  if(be.linked){
   // Refuse outbound access before loading the linked destination chunk. Return links stay open.
   if(!be.generated&&!settings.allowsAccess(cfg))return false;
   var target=level(be.linkDimension);if(target==null)return be.generated&&emergencyReturn(player);var landing=be.link;
   if(!target.getWorldBorder().isWithinBounds(landing))return be.generated&&emergencyReturn(player);target.getChunk(landing.getX()>>4,landing.getZ()>>4);
   boolean allowed=safe(target,landing)&&permittedArrival(player,target,landing);
   if(!allowed)return be.generated&&emergencyReturn(player);
   // A generated exit is independent of config, access and the original frame.
   if(!be.generated&&!validFrame(source,contact,be,cfg))return false;
   if(!be.generated){if(returns.size()>=ConfigLimits.MAX_RETURN_RECORDS&&!returns.containsKey(player.getUUID()))return false;
    var back=safeNearby(player,source,player.blockPosition());if(back==null)return false;
    returns.put(player.getUUID(),new ReturnAddress(source.dimension().identifier().toString(),back.asLong()));save();}
   return move(player,target,landing,be.generated);
  }
  if(cfg!=null&&source.dimension().identifier().toString().equals(cfg.targetDimensionId)&&cfg.allowIgniteFromTarget)return emergencyReturn(player);
  if(!settings.allowsAccess(cfg)||!settings.automaticDestination||!cfg.generateReturnPortalOnArrival||!cfg.createDestinationPlatform||!validFrame(source,contact,be,cfg))return false;
  var target=level(be.getDestination());if(target==null||!target.dimension().identifier().toString().equals(cfg.targetDimensionId))return false;
  var origin=safeNearby(player,source,player.blockPosition());if(origin==null)return false;
  if(returns.size()>=ConfigLimits.MAX_RETURN_RECORDS&&!returns.containsKey(player.getUUID()))return false;
  if(lastBuildTick==server.getTickCount()||portals.size()>=ConfigLimits.MAX_PORTALS)return false;
  var shape=PortalActivationService.match(new PortalWorld(source,cfg),cfg,contact.getX(),contact.getY(),contact.getZ()).orElseThrow();
  var mapped=PortalTravelRules.toTarget(new BlockPos3i(be.anchor.getX(),be.anchor.getY(),be.anchor.getZ()),cfg.travelCoordinateScale);
  var center=pos(mapped);if(center.getY()<target.getMinY()+8||center.getY()>target.getMaxY()-24||!target.getWorldBorder().isWithinBounds(center))return false;
  // Terrain can be read after a bounded chunk generation; no persistent ticket is installed.
  target.getChunk(center.getX()>>4,center.getZ()>>4);
  int width=(shape.axis()==PortalAxis.X?shape.interiorBounds().maxX()-shape.interiorBounds().minX():shape.interiorBounds().maxZ()-shape.interiorBounds().minZ())+3; // interior + two frame sides
  var plan=DestinationPlatform.plan(center,width+4);
  var chunks=new HashSet<Long>();for(var p:plan.keySet()){
   if(!target.getWorldBorder().isWithinBounds(p))return false;
   chunks.add(net.minecraft.world.level.ChunkPos.pack(p.getX()>>4,p.getZ()>>4));
  }
  if(chunks.size()>ConfigLimits.MAX_CHUNKS_PER_BUILD)return false;
  for(long chunk:chunks)target.getChunk(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk));
  boolean terrain="noise".equals(cfg.worldGeneration.generatorType)||!cfg.worldGeneration.flatLayers.isEmpty();
  if(terrain){
   int highest=target.getMinY();
   // Include nonblocking vegetation: every planned block must remain strictly above all existing blocks.
   for(var p:plan.keySet())highest=Math.max(highest,target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,p.getX(),p.getZ()));
   int depth=plan.keySet().stream().mapToInt(p->p.getY()).max().orElseThrow()-plan.keySet().stream().mapToInt(p->p.getY()).min().orElseThrow()+1;
   if(highest+depth>center.getY()){center=center.atY(highest+depth);plan=DestinationPlatform.plan(center,width+4);}
  }
  if(center.getY()+3>=target.getMaxY())return false;
  for(var p:plan.keySet()) {if(!target.getWorldBorder().isWithinBounds(p)||p.getY()<target.getMinY()||p.getY()>=target.getMaxY()||!target.hasChunkAt(p)||!target.getBlockState(p).isAir()||!permitted(player,target,p))return false;}
  // Check all headroom and portal cells before changing any block.
  for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<4;dy++) {var p=center.offset(dx,dy,dz);if(!target.hasChunkAt(p)||!target.getBlockState(p).isAir()||!permitted(player,target,p))return false;}
  if(!permittedLanding(player,target,center))return false;
  var previous=returns.put(player.getUUID(),new ReturnAddress(source.dimension().identifier().toString(),origin.asLong()));save();
  lastBuildTick=server.getTickCount();
  for(var e:plan.entrySet())target.setBlock(e.getKey(),e.getValue(),3);
  for(int dy=0;dy<2;dy++){
   var p=center.above(dy);target.setBlock(p,DimensionRegistry.PORTAL.defaultBlockState().setValue(SkyPortalBlock.AXIS,Direction.Axis.X),3);
   if(target.getBlockEntity(p) instanceof SkyPortalBlockEntity exit){exit.configure(cfg.portalColorRgb(),source.dimension().identifier().toString());exit.define(cfg.id,center,true);exit.connect(source.dimension().identifier().toString(),origin);}
  }
  // PLAN40 simpledimensions-4: a foreign mod that changes block placement must not stop the server - build back instead.
  if(!safe(target,center)||!move(player,target,center,false)){
   for(int dy=0;dy<2;dy++)target.setBlock(center.above(dy),Blocks.AIR.defaultBlockState(),3);
   for(var p:plan.keySet())target.setBlock(p,Blocks.AIR.defaultBlockState(),3);
   if(previous==null)returns.remove(player.getUUID());else returns.put(player.getUUID(),previous);save();return false;
  }
  recordPortal(target,new BlockPos3i(center.getX(),center.getY(),center.getZ()));
  // Persist a bidirectional exact connection on every source portal cell.
  for(var cell:shape.interior())if(source.getBlockEntity(pos(cell)) instanceof SkyPortalBlockEntity b)b.connect(target.dimension().identifier().toString(),center);
  return true;
 }
 public boolean validFrame(ServerLevel l,BlockPos p,SkyPortalBlockEntity be,DimensionPortalConfig cfg){
  var result=PortalActivationService.match(new PortalWorld(l,cfg),cfg,p.getX(),p.getY(),p.getZ());
  if(result.isPresent()&&(!cfg.requireSeparateLight||new PortalWorld(l,cfg).separateLight(result.get())))return true;
  // Only portal cells with the same anchor/definition may be removed.
  boolean x=l.getBlockState(p).getValue(SkyPortalBlock.AXIS)==Direction.Axis.X;
  for(int along=-1;along<=22;along++)for(int y=0;y<=22;y++){
   var q=be.anchor.offset(x?along:0,y,x?0:along);
   if(l.hasChunkAt(q)&&l.getBlockEntity(q) instanceof SkyPortalBlockEntity b&&!b.generated&&b.anchor.equals(be.anchor)&&b.definition.equals(be.definition))l.setBlock(q,Blocks.AIR.defaultBlockState(),3);
  }
  return false;
 }
 private boolean move(ServerPlayer player,ServerLevel target,BlockPos landing,boolean returning){
  if(!permittedArrival(player,target,landing))return false;
  boolean done=player.teleport(new net.minecraft.world.level.portal.TeleportTransition(target,
   new net.minecraft.world.phys.Vec3(landing.getX()+.5,landing.getY(),landing.getZ()+.5),net.minecraft.world.phys.Vec3.ZERO,
   player.getYRot(),player.getXRot(),net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING))!=null;
  if(done){player.setDeltaMovement(0,0,0);player.fallDistance=0;signal(target,landing,true);if(returning){returns.remove(player.getUUID());save();}else if(settings.giveGuideOnFirstTravel)com.simplebuilding.modules.simpledimensions.guide.DimensionsGuideAccess.welcome(player);}
  return done;
 }
 public boolean emergencyReturn(ServerPlayer player){return emergencyReturn(player,true);}
 /** The way out is always open (F3=B, no foreign-claim-mod lock); {@code home} false = straight to the overworld spawn (F5 "spawn"). */
 boolean emergencyReturn(ServerPlayer player,boolean home){boolean was=leaving;leaving=true;try{return returnOnce(player,home);}finally{leaving=was;}}
 private boolean returnOnce(ServerPlayer player,boolean home){
  var address=home?returns.get(player.getUUID()):null;var target=address==null?server.overworld():level(address.dimension());if(target==null)target=server.overworld();
  var center=address==null?target.getRespawnData().pos():BlockPos.of(address.pos());
  if(!target.getWorldBorder().isWithinBounds(center))center=target.getRespawnData().pos();
  target.getChunk(center.getX()>>4,center.getZ()>>4);var safe=safeNearby(player,target,center);
  if(safe!=null&&move(player,target,safe,true))return true;
  // A destroyed origin must not strand a traveler: try checked overworld surface near spawn.
  target=server.overworld();center=target.getRespawnData().pos();
  for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
   var column=center.offset(dx,0,dz);if(!target.getWorldBorder().isWithinBounds(column))continue;
   target.getChunk(column.getX()>>4,column.getZ()>>4);
   int y=target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
   var p=column.atY(y);if(safe(target,p)&&permittedArrival(player,target,p))return move(player,target,p,true);
  }
  return false;

 }
}
