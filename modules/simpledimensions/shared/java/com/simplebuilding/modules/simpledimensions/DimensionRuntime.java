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
/** Per-server state: game ticks, exact persisted return addresses, bounded builds, no chunk tickets. */
public final class DimensionRuntime {
 private static final Map<MinecraftServer,DimensionRuntime> SERVERS=new WeakHashMap<>();
 public static final Path CONFIG_ROOT=Path.of("config/simpledimension");
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
 public interface Permission {boolean allow(ServerPlayer player,ServerLevel level,BlockPos pos);}
 public static Permission permission=null;
 public boolean permitted(ServerPlayer p,ServerLevel l,BlockPos pos){return l.mayInteract(p,pos)&&(!claimIntegrationRequired||permission!=null)&&(permission==null||permission.allow(p,l,pos));}
 public record ReturnAddress(String dimension,long pos){}
 private static final class Contact {String portal="";int warmup;long cooldown;boolean exit;}
 public static DimensionRuntime get(MinecraftServer s){return SERVERS.computeIfAbsent(s,DimensionRuntime::new);}
 public static void stop(MinecraftServer s){SERVERS.remove(s);}
 private DimensionRuntime(MinecraftServer s){
  server=s;configs=DimensionConfigStore.loadAndGenerate(CONFIG_ROOT,net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA).major());
  settings=DimensionSettings.load(CONFIG_ROOT);ledger=s.getWorldPath(LevelResource.ROOT).resolve("simpledimension-returns.json");
  if(Files.exists(ledger))try{
   if(Files.size(ledger)>1048576||Files.isSymbolicLink(ledger))throw new IllegalStateException("Unsafe ledger");
   var data=JsonParser.parseString(Files.readString(ledger)).getAsJsonObject();
   for(var e:data.getAsJsonObject("returns").entrySet()) {if(returns.size()>=4096)break;returns.put(UUID.fromString(e.getKey()),new Gson().fromJson(e.getValue(),ReturnAddress.class));}
   for(var e:data.getAsJsonArray("portals")){if(portals.size()>=ConfigLimits.MAX_PORTALS)break;portals.add(e.getAsString());}
  }catch(Exception e){throw new IllegalStateException("Cannot read return ledger",e);}
 }
 private void save(){
  try{var root=new JsonObject();root.add("returns",new Gson().toJsonTree(returns));root.add("portals",new Gson().toJsonTree(portals));
   var temp=ledger.resolveSibling("simpledimension-returns.tmp");Files.writeString(temp,new Gson().toJson(root));Files.move(temp,ledger,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }catch(Exception e){throw new IllegalStateException("Cannot persist safe returns",e);}
 }
 private static String key(ServerLevel l,BlockPos p){return l.dimension().identifier()+"@"+p.asLong();}
 public boolean reserve(ServerLevel l,BlockPos3i p){return portals.size()<ConfigLimits.MAX_PORTALS||portals.contains(key(l,pos(p)));}
 public void recordPortal(ServerLevel l,BlockPos3i p){portals.add(key(l,pos(p)));save();}
 public DimensionPortalConfig config(String id){return configs.stream().filter(c->c.id.equals(id)).findFirst().orElse(null);}
 public static BlockPos pos(BlockPos3i p){return new BlockPos(p.x(),p.y(),p.z());}
 public static void signal(ServerLevel l,BlockPos p,boolean success){
  l.playSound(null,p,success?SoundEvents.PORTAL_TRIGGER:SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.4f,success?1: .6f);
  l.sendParticles(success?ParticleTypes.PORTAL:ParticleTypes.SMOKE,p.getX()+.5,p.getY()+.5,p.getZ()+.5,8,.3,.3,.3,.01);
 }
 public void tick(){
  if(!portals.isEmpty()) {
   var anchors=new ArrayList<>(portals);
   for(int i=0;i<Math.min(8,anchors.size());i++){
    String k=anchors.get(Math.floorMod(decayCursor++,anchors.size()));int split=k.lastIndexOf('@');
    var l=level(k.substring(0,split));var p=BlockPos.of(Long.parseLong(k.substring(split+1)));
    if(l!=null&&l.hasChunkAt(p)&&l.getBlockEntity(p) instanceof SkyPortalBlockEntity b&&!b.generated){var def=config(b.definition);if(def!=null)validFrame(l,p,b,def);}
   }
  }
  ignitions.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
  contacts.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
  for(var p:server.getPlayerList().getPlayers()){
   var l=p.level();var touched=findTouched(l,p);var c=contacts.computeIfAbsent(p.getUUID(),id->new Contact());
   if(touched==null){c.portal="";c.warmup=0;c.exit=false;}
   else if(!c.exit&&server.getTickCount()>=c.cooldown){
    String current=key(l,touched);if(!current.equals(c.portal)){c.portal=current;c.warmup=0;}
    if(l.getBlockEntity(touched) instanceof SkyPortalBlockEntity be){
     var cfg=config(be.definition);
     int delay=Math.max(settings.portalDelayTicks,cfg==null?0:cfg.portalDelayTicks);
     if(++c.warmup>delay){c.warmup=0;if(travel(p,touched)) {c.cooldown=server.getTickCount()+Math.max(settings.teleportCooldownTicks,cfg==null?60:cfg.teleportCooldownTicks);c.exit=true;}}
    }
   }
   if(l.dimension().identifier().getNamespace().equals("simpledimension")&&(p.getY()<l.getMinY()+3||configs.stream().noneMatch(d->d.targetDimensionId.equals(l.dimension().identifier().toString())))) emergencyReturn(p);
  }
 }
 private BlockPos findTouched(ServerLevel l,ServerPlayer p){
  var box=p.getBoundingBox();for(var q:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(DimensionRegistry.portal(l.getBlockState(q)))return q.immutable();return null;
 }
 public static boolean safe(ServerLevel l,BlockPos p){
  if(p.getY()<=l.getMinY()||p.getY()+2>=l.getMaxY()||!l.getWorldBorder().isWithinBounds(p)||!l.hasChunkAt(p))return false;
  var ground=l.getBlockState(p.below());return ground.isFaceSturdy(l,p.below(),Direction.UP)&&!ground.is(Blocks.MAGMA_BLOCK)&&!ground.is(Blocks.CAMPFIRE)&&!ground.is(Blocks.SOUL_CAMPFIRE)&&
   clear(l,p)&&clear(l,p.above());
 }
 private static boolean clear(ServerLevel l,BlockPos p){var s=l.getBlockState(p);return (s.isAir()||DimensionRegistry.portal(s))&&s.getFluidState().isEmpty();}
 private static BlockPos safeNearby(ServerLevel l,BlockPos center){
  for(int r=0;r<=8;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++) {if(Math.abs(dx)!=r&&Math.abs(dz)!=r)continue;for(int dy=0;dy<=8;dy++){var p=center.offset(dx,dy,dz);if(safe(l,p))return p;}}
  return null;
 }
 private ServerLevel level(String id){var value=Identifier.tryParse(id);return value==null?null:server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,value));}
 public boolean travel(ServerPlayer player,BlockPos contact){
  var source=player.level();if(player.isPassenger()||player.isVehicle()||player.isSpectator()||!(source.getBlockEntity(contact) instanceof SkyPortalBlockEntity be))return false;
  var cfg=config(be.definition);
  if(be.linked){
   var target=level(be.linkDimension);if(target==null)return false;var landing=be.link;
   if(!target.getWorldBorder().isWithinBounds(landing))return false;target.getChunk(landing.getX()>>4,landing.getZ()>>4);
   if(!safe(target,landing)||!permitted(player,target,landing))return false;
   // A generated exit is independent of config, access and the original frame.
   if(!be.generated&&(cfg==null||!cfg.enabled||!settings.accessEnabled))return false;
   if(!be.generated&&!validFrame(source,contact,be,cfg))return false;
   if(!be.generated){if(returns.size()>=4096&&!returns.containsKey(player.getUUID()))return false;
    var back=safeNearby(source,player.blockPosition());if(back==null)return false;
    returns.put(player.getUUID(),new ReturnAddress(source.dimension().identifier().toString(),back.asLong()));save();}
   return move(player,target,landing,be.generated);
  }
  if(cfg!=null&&source.dimension().identifier().toString().equals(cfg.targetDimensionId)&&cfg.allowIgniteFromTarget)return emergencyReturn(player);
  if(cfg==null||!cfg.enabled||!settings.accessEnabled||!settings.automaticDestination||!cfg.generateReturnPortalOnArrival||!cfg.createDestinationPlatform||!validFrame(source,contact,be,cfg))return false;
  var target=level(be.getDestination());if(target==null||!target.dimension().identifier().toString().equals(cfg.targetDimensionId))return false;
  var origin=safeNearby(source,player.blockPosition());if(origin==null||!permitted(player,source,origin))return false;
  if(returns.size()>=4096&&!returns.containsKey(player.getUUID()))return false;
  if(lastBuildTick==server.getTickCount()||portals.size()>=ConfigLimits.MAX_PORTALS)return false;
  var shape=PortalActivationService.match(new PortalWorld(source,cfg),cfg,contact.getX(),contact.getY(),contact.getZ()).orElseThrow();
  var mapped=PortalTravelRules.toTarget(new BlockPos3i(be.anchor.getX(),be.anchor.getY(),be.anchor.getZ()),cfg.travelCoordinateScale);
  var center=pos(mapped);if(center.getY()<target.getMinY()+8||center.getY()>target.getMaxY()-24||!target.getWorldBorder().isWithinBounds(center))return false;
  // Terrain can be read after a bounded chunk generation; no persistent ticket is installed.
  target.getChunk(center.getX()>>4,center.getZ()>>4);
  int surface=target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,center.getX(),center.getZ());
  if(surface>center.getY())center=new BlockPos(center.getX(),surface+2,center.getZ());
  int width=(shape.axis()==PortalAxis.X?shape.interiorBounds().maxX()-shape.interiorBounds().minX():shape.interiorBounds().maxZ()-shape.interiorBounds().minZ())+3; // interior + two frame sides
  var plan=DestinationPlatform.plan(center,width+4);
  if(center.getY()+3>=target.getMaxY())return false;
  for(var p:plan.keySet()) {if(!target.getWorldBorder().isWithinBounds(p)||p.getY()<target.getMinY()||p.getY()>=target.getMaxY()||!target.hasChunkAt(p)||!target.getBlockState(p).isAir()||!permitted(player,target,p))return false;}
  // Check all headroom and portal cells before changing any block.
  for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<4;dy++) {var p=center.offset(dx,dy,dz);if(!target.hasChunkAt(p)||!target.getBlockState(p).isAir()||!permitted(player,target,p))return false;}
  lastBuildTick=server.getTickCount();
  for(var e:plan.entrySet())target.setBlock(e.getKey(),e.getValue(),3);
  for(int dy=0;dy<2;dy++){
   var p=center.above(dy);target.setBlock(p,DimensionRegistry.PORTAL.defaultBlockState().setValue(SkyPortalBlock.AXIS,Direction.Axis.X),3);
   if(target.getBlockEntity(p) instanceof SkyPortalBlockEntity exit){exit.configure(cfg.portalColorRgb(),source.dimension().identifier().toString());exit.define(cfg.id,center,true);exit.connect(source.dimension().identifier().toString(),origin);}
  }
  if(!safe(target,center))throw new IllegalStateException("Destination plan did not create safe ground");
  returns.put(player.getUUID(),new ReturnAddress(source.dimension().identifier().toString(),origin.asLong()));
  recordPortal(target,new BlockPos3i(center.getX(),center.getY(),center.getZ()));
  // Persist a bidirectional exact connection on every source portal cell.
  for(var cell:shape.interior())if(source.getBlockEntity(pos(cell)) instanceof SkyPortalBlockEntity b)b.connect(target.dimension().identifier().toString(),center);
  return move(player,target,center,false);
 }
 public boolean validFrame(ServerLevel l,BlockPos p,SkyPortalBlockEntity be,DimensionPortalConfig cfg){
  var result=PortalActivationService.match(new PortalWorld(l,cfg),cfg,p.getX(),p.getY(),p.getZ());
  if(result.isPresent())return true;
  // Only portal cells with the same anchor/definition may be removed.
  boolean x=l.getBlockState(p).getValue(SkyPortalBlock.AXIS)==Direction.Axis.X;
  for(int along=-1;along<=22;along++)for(int y=0;y<=22;y++){
   var q=be.anchor.offset(x?along:0,y,x?0:along);
   if(l.hasChunkAt(q)&&l.getBlockEntity(q) instanceof SkyPortalBlockEntity b&&!b.generated&&b.anchor.equals(be.anchor)&&b.definition.equals(be.definition))l.setBlock(q,Blocks.AIR.defaultBlockState(),3);
  }
  return false;
 }
 private boolean move(ServerPlayer player,ServerLevel target,BlockPos landing,boolean returning){
  boolean done=player.teleportTo(target,landing.getX()+.5,landing.getY(),landing.getZ()+.5,Set.<Relative>of(),player.getYRot(),player.getXRot(),false);
  if(done){player.setDeltaMovement(0,0,0);player.fallDistance=0;signal(target,landing,true);if(returning){returns.remove(player.getUUID());save();}}
  return done;
 }
 public boolean emergencyReturn(ServerPlayer player){
  var address=returns.get(player.getUUID());var target=address==null?server.overworld():level(address.dimension());if(target==null)target=server.overworld();
  var center=address==null?target.getRespawnData().pos():BlockPos.of(address.pos());
  if(!target.getWorldBorder().isWithinBounds(center))center=target.getRespawnData().pos();
  target.getChunk(center.getX()>>4,center.getZ()>>4);var safe=safeNearby(target,center);
  return safe!=null&&permitted(player,target,safe)&&move(player,target,safe,true);
 }
}
