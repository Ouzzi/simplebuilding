package com.simplebuilding.modules.simpledimensions;
import dev.simpledimension.common.portal.*;
import dev.simpledimension.common.config.DimensionConfigStore;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
import java.nio.file.*;
public final class DimensionTests {
 public static final Map<String,java.util.function.Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("claims_footprints",PortalProtectionTests::footprints);
  ALL.put("claims_linked_travel",PortalProtectionTests::linkedTravel);
  ALL.put("claims_unsupported_provider",PortalProtectionTests::unsupportedProvider);
  ALL.put("guide_book",com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide::gameTest);
  ALL.put("launch",DimensionTests::launch);ALL.put("world_generation",DimensionTests::worldGeneration);
  ALL.put("six_arches_both_axes",DimensionTests::sixArchesBothAxes);ALL.put("recipes_and_mutations",DimensionTests::recipesAndMutations);
  ALL.put("separate_light",DimensionTests::separateLight);ALL.put("ignition_costs",DimensionTests::ignitionCosts);
  ALL.put("portal_decay",DimensionTests::portalDecay);ALL.put("config_bounds",DimensionTests::configBounds);
  ALL.put("hostile_config",DimensionTests::hostileConfig);ALL.put("config_files",DimensionTests::configFiles);
  ALL.put("legacy_entity",DimensionTests::legacyEntity);ALL.put("coordinate_rules",DimensionTests::coordinateRules);
  ALL.put("platform_geometry",DimensionTests::platformGeometry);ALL.put("safe_ground",DimensionTests::safeGround);
  ALL.put("real_travel_and_return",DimensionTests::realTravelAndReturn);ALL.put("disabled_return",DimensionTests::disabledReturn);
  ALL.put("claims_and_costs",DimensionTests::claimsAndCosts);ALL.put("passengers",DimensionTests::passengers);
  ALL.put("unknown_target",DimensionTests::unknownTarget);ALL.put("cooldown_ticks",DimensionTests::cooldownTicks);
  ALL.put("cross_mod_storage",DimensionTests::crossModStorage);ALL.put("vanilla_portals",DimensionTests::vanillaPortals);
  ALL.put("restart_return",DimensionTests::restartReturn);ALL.put("border_and_height",DimensionTests::borderAndHeight);
  ALL.put("warmup_and_no_bounce",DimensionTests::warmupAndNoBounce);ALL.put("no_overwrite",DimensionTests::noOverwrite);
  ALL.put("out_of_range_ignition",DimensionTests::outOfRangeIgnition);ALL.put("removed_definition_return",DimensionTests::removedDefinitionReturn);
  ALL.put("player_inventory_roundtrip",DimensionTests::playerInventoryRoundtrip);ALL.put("config_and_lang",DimensionTests::configAndLang);ALL.put("mining_travel",DimensionTests::miningTravel);ALL.put("compressed_travel",DimensionTests::compressedTravel);ALL.put("exact_origin_links",DimensionTests::exactOriginLinks);ALL.put("destination_claim",DimensionTests::destinationClaim);
  ALL.put("settings_skyblock",h->DimensionSettingsTests.journey(h,"skyblock",130));
  ALL.put("settings_mining",h->DimensionSettingsTests.journey(h,"mining",220));
  ALL.put("settings_travel",h->DimensionSettingsTests.journey(h,"travel",310));
  ALL.put("settings_persistence",DimensionSettingsTests::persistence);

 }
 private static void yes(GameTestHelper h,boolean b,String s){h.assertTrue(b,s);}
 private static void rejects(Runnable r){try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Hostile input accepted");}
 private static BlockPos base(GameTestHelper h){return h.absolutePos(new BlockPos(12,8,12));}
 static BlockPos build(GameTestHelper h,DimensionPortalConfig c,int recipe,Direction.Axis axis){return buildAt(h,c,recipe,axis,base(h));}
 private static BlockPos buildAt(GameTestHelper h,DimensionPortalConfig c,int recipe,Direction.Axis axis,BlockPos bottom){
  var l=h.getLevel();var r=c.portalRecipes.get(recipe);int topY=bottom.getY()+r.rows.size()-1;
  for(int x=-3;x<=12;x++)for(int z=-3;z<=12;z++)for(int y=-1;y<=8;y++)l.setBlock(bottom.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
  for(int row=0;row<r.rows.size();row++)for(int col=0;col<r.rows.get(row).length();col++){
   String id=r.legend.get(""+r.rows.get(row).charAt(col));var p=new BlockPos(bottom.getX()+(axis==Direction.Axis.X?col:0),topY-row,bottom.getZ()+(axis==Direction.Axis.Z?col:0));
   if(id!=null)l.setBlock(p,BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState(),3);
  }
  for(int row=r.rows.size()-1;row>=0;row--)for(int col=0;col<r.rows.get(row).length();col++)if(r.rows.get(row).charAt(col)=='.')return new BlockPos(bottom.getX()+(axis==Direction.Axis.X?col:0),topY-row,bottom.getZ()+(axis==Direction.Axis.Z?col:0));
  throw new AssertionError("No interior");
 }
 static ServerPlayer player(GameTestHelper h,BlockPos at,Item igniter){var p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(at.getX()+.5,at.getY(),at.getZ()+1.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(igniter,2));return p;}
 static InteractionResult ignite(ServerPlayer p,BlockPos cell){return PortalActivation.ignite(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(cell.below()),Direction.UP,cell.below(),false));}
 static BlockPos activate(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();var p=build(h,c,1,Direction.Axis.X);h.getLevel().setBlock(p.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);var player=player(h,p,Items.FLINT_AND_STEEL);yes(h,ignite(player,p)==InteractionResult.SUCCESS,"Owner arch activates");return p;}
 public static void launch(GameTestHelper h){
  for(String id:List.of("sky_portal","light_blue_portal")){yes(h,BuiltInRegistries.BLOCK.containsKey(DimensionRegistry.id(id)),"Block "+id);yes(h,BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(DimensionRegistry.id(id)),"BE "+id);}
  yes(h,BuiltInRegistries.ITEM.containsKey(DimensionRegistry.id("light_blue_portal")),"Legacy item");
  h.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE,DimensionRegistry.id("skyblock")));
  yes(h,BuiltInRegistries.BLOCK.containsKey(Identifier.parse("simplebuilding:backpack")),"SimpleBuilding boots alongside module");
  yes(h,h.getLevel().getServer().getResourceManager().listPacks().anyMatch(p->p.packId().equals("simpledimension_generated")),"Config pack loaded at launch");h.succeed();
 }
 public static void worldGeneration(GameTestHelper h){
  var s=h.getLevel().getServer();for(String name:List.of("skyblock","mining","travel")){
   var l=s.getLevel(ResourceKey.create(Registries.DIMENSION,DimensionRegistry.id(name)));yes(h,l!=null,"Dimension loaded: "+name);l.getChunk(100,100);
   var p=new BlockPos(1605,l.getMinY(),1605);
   if(name.equals("skyblock"))yes(h,l.getBlockState(p).isAir(),"Void generated");
   if(name.equals("mining")){yes(h,l.getBlockState(p).is(Blocks.BEDROCK),"Mining bedrock");yes(h,l.getBlockState(p.atY(100)).is(Blocks.STONE),"Mining stone");yes(h,l.getBlockState(p.atY(191)).is(Blocks.GRASS_BLOCK),"Mining forest surface at 191");}
   if(name.equals("travel"))yes(h,l.getBlockState(p).is(Blocks.BEDROCK),"Travel bedrock plane");
  }h.succeed();
 }
 public static void sixArchesBothAxes(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();for(var axis:List.of(Direction.Axis.X,Direction.Axis.Z))for(int i=0;i<6;i++){
  var p=build(h,c,i,axis);var match=PortalActivationService.match(new PortalWorld(h.getLevel(),c),c,p.getX(),p.getY(),p.getZ());yes(h,match.isPresent(),"Arch "+i+" axis "+axis);
  var frame=match.orElseThrow().frame().stream().filter(f->!f.blockId().equals("minecraft:air")).findFirst().orElseThrow();h.getLevel().setBlock(DimensionRuntime.pos(frame.pos()),Blocks.DIRT.defaultBlockState(),3);
  yes(h,PortalActivationService.match(new PortalWorld(h.getLevel(),c),c,p.getX(),p.getY(),p.getZ()).isEmpty(),"Mutated frame refused");
 }h.succeed();}
 public static void recipesAndMutations(GameTestHelper h){for(var c:List.of(DimensionPortalConfig.defaultSkyblock(),DimensionPortalConfig.miningDimensionPreset(),DimensionPortalConfig.travelDimensionPreset()))for(var axis:List.of(Direction.Axis.X,Direction.Axis.Z)){
  var p=build(h,c,0,axis);yes(h,PortalActivationService.match(new PortalWorld(h.getLevel(),c),c,p.getX(),p.getY(),p.getZ()).isPresent(),"Source alternative recipe "+c.id);
 }var c=DimensionPortalConfig.defaultSkyblock();var p=build(h,c,0,Direction.Axis.X);h.getLevel().setBlock(base(h).above(2),Blocks.DIRT.defaultBlockState(),3);yes(h,PortalActivationService.match(new PortalWorld(h.getLevel(),c),c,p.getX(),p.getY(),p.getZ()).isEmpty(),"Empty outer corner checked");h.succeed();}
 public static void separateLight(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();var p=build(h,c,0,Direction.Axis.X);var w=new PortalWorld(h.getLevel(),c);var m=PortalActivationService.match(w,c,p.getX(),p.getY(),p.getZ()).orElseThrow();yes(h,!w.separateLight(m),"Glowstone alone does not count");
  for(var b:List.of(Blocks.GLOWSTONE,Blocks.FIRE,Blocks.SOUL_FIRE,Blocks.NETHER_PORTAL,Blocks.END_PORTAL,Blocks.END_GATEWAY,DimensionRegistry.PORTAL)){h.getLevel().setBlock(p.offset(0,1,2),b.defaultBlockState(),3);yes(h,!w.separateLight(m),"Excluded light "+b);}
  h.getLevel().setBlock(p.offset(0,1,2),Blocks.SEA_LANTERN.defaultBlockState(),3);yes(h,w.separateLight(m),"Separate light accepted");h.succeed();}
 public static void ignitionCosts(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();var cell=build(h,c,1,Direction.Axis.X);h.getLevel().setBlock(cell.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);var p=player(h,cell,Items.FIRE_CHARGE);yes(h,ignite(p,cell)==InteractionResult.SUCCESS,"Charge activates");yes(h,p.getMainHandItem().getCount()==1,"Exactly one charge consumed");
  cell=build(h,c,0,Direction.Axis.X);h.getLevel().setBlock(cell.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);p=player(h,cell,Items.FLINT_AND_STEEL);yes(h,ignite(p,cell)==InteractionResult.SUCCESS,"Flint activates");yes(h,p.getMainHandItem().getDamageValue()==1,"Exactly one damage consumed");h.succeed();}
 public static void portalDecay(GameTestHelper h){var p=activate(h);var b=(SkyPortalBlockEntity)h.getLevel().getBlockEntity(p);h.getLevel().setBlock(base(h),Blocks.DIRT.defaultBlockState(),3);yes(h,!DimensionRuntime.get(h.getLevel().getServer()).validFrame(h.getLevel(),p,b,DimensionPortalConfig.defaultSkyblock()),"Broken frame fails");yes(h,h.getLevel().getBlockState(p).isAir(),"Portal decays");h.succeed();}
 public static void configBounds(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();c.portalDelayTicks=999;c.teleportCooldownTicks=-1;c.travelCoordinateScale=999;c.maxPortalWidth=999;c.worldGeneration.height=Integer.MAX_VALUE;c.worldGeneration.minY=Integer.MIN_VALUE;ConfigLimits.validate(c);yes(h,c.portalDelayTicks==200&&c.teleportCooldownTicks==20&&c.travelCoordinateScale==10&&c.maxPortalWidth==21&&c.worldGeneration.height==384&&c.worldGeneration.minY==-64,"All numeric hard caps");h.succeed();}
 public static void hostileConfig(GameTestHelper h){for(double n:List.of(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)){var c=DimensionPortalConfig.defaultSkyblock();c.travelCoordinateScale=n;rejects(()->ConfigLimits.validate(c));}
  var badId=DimensionPortalConfig.defaultSkyblock();badId.id="../escape";rejects(()->ConfigLimits.validate(badId));var c=DimensionPortalConfig.defaultSkyblock();c.frameBlock="minecraft:obsidian";var obsidian=c;rejects(()->ConfigLimits.validate(obsidian));
  c=DimensionPortalConfig.defaultSkyblock();c.portalRecipes=new ArrayList<>(Collections.nCopies(9,c.portalRecipes.getFirst()));var tooMany=c;rejects(()->ConfigLimits.validate(tooMany));h.succeed();}
 public static void configFiles(GameTestHelper h){try{var root=Files.createTempDirectory("dimension-config-test");var loaded=DimensionConfigStore.loadAndGenerate(root,net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA).major());yes(h,loaded.size()==3,"Three default files");String original=Files.readString(root.resolve("dimensions/skyblock.json"));DimensionConfigStore.loadAndGenerate(root,100);yes(h,Files.readString(root.resolve("dimensions/skyblock.json")).equals(original),"Existing configs preserved");Files.writeString(root.resolve("dimensions/bad.json"),"{bad");yes(h,DimensionConfigStore.loadAndGenerate(root,100).size()==3,"Malformed config refused");}catch(Exception e){throw new AssertionError(e);}h.succeed();}
 public static void legacyEntity(GameTestHelper h){var pos=base(h);
  yes(h,DimensionRegistry.PORTAL_ENTITY.onlyOpCanSetNbt()&&DimensionRegistry.LEGACY_ENTITY.onlyOpCanSetNbt(),"Both portal types reject nonoperator item NBT");
  h.getLevel().setBlock(pos,DimensionRegistry.LEGACY.defaultBlockState(),3);var attacker=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
  yes(h,!attacker.canUseGameMasterBlocks(),"Attack comes from a nonoperator creative player");var forged=new ItemStack(DimensionRegistry.LEGACY.asItem());var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("Generated",true);tag.putBoolean("Linked",true);tag.putString("LinkDimension","minecraft:the_nether");tag.putLong("Link",pos.asLong());
  forged.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.<net.minecraft.world.level.block.entity.BlockEntityType<?>>of(DimensionRegistry.LEGACY_ENTITY,tag));
  yes(h,!BlockItem.updateCustomBlockEntityTag(h.getLevel(),attacker,pos,forged),"Vanilla item placement refuses spoofed return data");
  yes(h,!((SkyPortalBlockEntity)h.getLevel().getBlockEntity(pos)).linked,"No forged teleport authority installed");var b=new SkyPortalBlockEntity(pos,DimensionRegistry.LEGACY.defaultBlockState());var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());out.putInt("Color",0x123456);out.putString("Destination","simpledimension:skyblock");b.loadAdditional(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));yes(h,b.getColor()==0x123456&&b.getDestination().equals("simpledimension:skyblock"),"Legacy NBT fields load");b.define("skyblock",pos,true);b.connect("minecraft:overworld",pos.offset(3,0,4));var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());b.saveAdditional(saved);var restored=new SkyPortalBlockEntity(pos,DimensionRegistry.LEGACY.defaultBlockState());restored.loadAdditional(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved.buildResult()));yes(h,restored.linked&&restored.link.equals(b.link)&&restored.generated,"Persistent exact link roundtrip");h.succeed();}
 public static void coordinateRules(GameTestHelper h){var p=new BlockPos3i(-101,70,101);yes(h,PortalTravelRules.toTarget(p,10).equals(new BlockPos3i(-11,70,10)),"Negative rounding and 10:1");yes(h,PortalTravelRules.toTarget(p,.5).equals(new BlockPos3i(-202,70,202)),"Mining 0.5 ratio");rejects(()->PortalTravelRules.toTarget(p,Double.NaN));h.succeed();}
 public static void platformGeometry(GameTestHelper h){for(int size=7;size<=12;size++){var blocks=DestinationPlatform.plan(new BlockPos(0,100,0),size);int min=blocks.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow(),max=blocks.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow();yes(h,max-min+1==size,"Exact owner diameter "+size);yes(h,blocks.values().stream().allMatch(s->s.is(Blocks.BEDROCK)),"No ore/frame rewards");yes(h,blocks.keySet().stream().allMatch(p->p.getY()<100),"Flat safe top, rounded underside");}h.succeed();}
 public static void safeGround(GameTestHelper h){var p=base(h);var l=h.getLevel();l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);yes(h,DimensionRuntime.safe(l,p),"Solid floor");for(var b:List.of(Blocks.LAVA,Blocks.WATER,Blocks.MAGMA_BLOCK)){l.setBlock(p.below(),b.defaultBlockState(),3);yes(h,!DimensionRuntime.safe(l,p),"Hazard refused");}l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(p.above(),Blocks.STONE.defaultBlockState(),3);yes(h,!DimensionRuntime.safe(l,p),"Head collision refused");h.succeed();}
 private static void roundTrip(GameTestHelper h,boolean disable,boolean restart){h.runAfterDelay(restart?40:disable?25:10,()->{var cell=activate(h);var p=player(h,cell,Items.AIR);var source=p.level();var rt=DimensionRuntime.get(source.getServer());yes(h,rt.travel(p,cell),"Real outbound teleport and platform generation");yes(h,p.level()!=source,"Changed real ServerLevel");var exit=p.blockPosition();if(disable)rt.settings.accessEnabled=false;
  if(restart){DimensionRuntime.stop(source.getServer());rt=DimensionRuntime.get(source.getServer());yes(h,rt.emergencyReturn(p),"Persisted player return after restart");}
  else yes(h,rt.travel(p,exit),"Exact generated exit travels back");
  rt.settings.accessEnabled=true;yes(h,p.level()==source,"Exact source dimension");yes(h,DimensionRuntime.safe(source,p.blockPosition()),"Safe exact return");h.succeed();});}
 public static void realTravelAndReturn(GameTestHelper h){roundTrip(h,false,false);}public static void disabledReturn(GameTestHelper h){roundTrip(h,true,false);}public static void restartReturn(GameTestHelper h){roundTrip(h,false,true);}
 public static void claimsAndCosts(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();var cell=build(h,c,0,Direction.Axis.X);h.getLevel().setBlock(cell.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);var p=player(h,cell,Items.FIRE_CHARGE);var old=DimensionRuntime.permission;try{DimensionRuntime.permission=(player,l,pos)->false;yes(h,ignite(p,cell)==InteractionResult.FAIL,"Claim denial honored");yes(h,p.getMainHandItem().getCount()==2&&h.getLevel().getBlockState(cell).isAir(),"No cost or partial mutation");}finally{DimensionRuntime.permission=old;}h.succeed();}
 public static void passengers(GameTestHelper h){var cell=activate(h);var p=player(h,cell,Items.AIR);var vehicle=h.spawn(net.minecraft.world.entity.EntityTypes.PIG,new BlockPos(3,3,3));p.startRiding(vehicle,true,false);yes(h,!DimensionRuntime.get(h.getLevel().getServer()).travel(p,cell),"Mounted player cannot duplicate passenger inventory");p.stopRiding();h.succeed();}
 public static void unknownTarget(GameTestHelper h){var cell=activate(h);var b=(SkyPortalBlockEntity)h.getLevel().getBlockEntity(cell);b.configure(0,"simpledimension:missing");var p=player(h,cell,Items.AIR);yes(h,!DimensionRuntime.get(h.getLevel().getServer()).travel(p,cell)&&p.level()==h.getLevel(),"Unknown registry target leaves origin untouched");h.succeed();}
 public static void cooldownTicks(GameTestHelper h){var c=new TeleportCooldowns();var id=UUID.randomUUID();yes(h,c.tryConsume(id,100,60)&&!c.tryConsume(id,159,60)&&c.tryConsume(id,160,60),"Exact tick boundary");c.forget(id);yes(h,c.tryConsume(id,161,60),"Logout resets state");h.succeed();}
 public static void crossModStorage(GameTestHelper h){var id=Identifier.parse("simplebuilding:backpack");yes(h,BuiltInRegistries.ITEM.containsKey(id),"Backpack alongside dimensions");var hopper=new net.minecraft.world.level.block.entity.HopperBlockEntity(base(h),Blocks.HOPPER.defaultBlockState());var item=new ItemStack(BuiltInRegistries.ITEM.getValue(DimensionRegistry.id("light_blue_portal")),2);hopper.setItem(0,item.copy());yes(h,hopper.getItem(0).getCount()==2,"Foreign legacy item in real hopper without mutation");h.succeed();}
 public static void vanillaPortals(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();c.portalRecipes=List.of();c.frameBlock="minecraft:obsidian";rejects(()->ConfigLimits.validate(c));var cell=base(h);h.getLevel().setBlock(cell,Blocks.NETHER_PORTAL.defaultBlockState(),3);yes(h,!DimensionRegistry.portal(h.getLevel().getBlockState(cell)),"Vanilla nether portal is never custom portal");h.succeed();}
 public static void borderAndHeight(GameTestHelper h){var l=h.getLevel();yes(h,!DimensionRuntime.safe(l,new BlockPos(30000001,80,0)),"World border refused");yes(h,!DimensionRuntime.safe(l,new BlockPos(0,l.getMaxY(),0)),"Build height refused");h.succeed();}
 public static void warmupAndNoBounce(GameTestHelper h){h.runAfterDelay(80,()->{
  var cell=activate(h);var p=player(h,cell,Items.AIR);p.setPos(cell.getX()+.5,cell.getY(),cell.getZ()+.5);
  var rt=DimensionRuntime.get(h.getLevel().getServer());int old=rt.settings.portalDelayTicks;try{rt.settings.portalDelayTicks=2;
   rt.tickPlayer(p);rt.tickPlayer(p);yes(h,p.level()==h.getLevel(),"Warmup blocks early departure");
   rt.tickPlayer(p);yes(h,p.level()!=h.getLevel(),"Warmup reaches real travel");var target=p.level();rt.tickPlayer(p);yes(h,p.level()==target,"Arrival cannot bounce");
   p.setPos(p.getX(),p.getY(),p.getZ()+3);rt.tickPlayer(p);p.setPos(p.getX(),p.getY(),p.getZ()-3);rt.tickPlayer(p);yes(h,p.level()==target,"Leaving does not bypass tick cooldown");
  }finally{rt.settings.portalDelayTicks=old;}h.succeed();});
 }
 public static void noOverwrite(GameTestHelper h){h.runAfterDelay(82,()->{var cell=activate(h);var p=player(h,cell,Items.AIR);var be=(SkyPortalBlockEntity)h.getLevel().getBlockEntity(cell);var target=h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,DimensionRegistry.id("skyblock")));var dest=be.anchor.below().east();target.getChunk(dest.getX()>>4,dest.getZ()>>4);target.setBlock(dest,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
  yes(h,!DimensionRuntime.get(h.getLevel().getServer()).travel(p,cell),"Occupied destination rejected before build");yes(h,target.getBlockState(dest).is(Blocks.DIAMOND_BLOCK),"Existing valuable build preserved");h.succeed();});}
 public static void outOfRangeIgnition(GameTestHelper h){var c=DimensionPortalConfig.defaultSkyblock();var cell=build(h,c,0,Direction.Axis.X);h.getLevel().setBlock(cell.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);var p=player(h,cell,Items.FIRE_CHARGE);p.setPos(p.getX()+100,p.getY(),p.getZ());yes(h,ignite(p,cell)==InteractionResult.PASS,"Spoofed distant block hit refused");yes(h,h.getLevel().getBlockState(cell).isAir()&&p.getMainHandItem().getCount()==2,"No remote cost or mutation");h.succeed();}
 public static void removedDefinitionReturn(GameTestHelper h){h.runAfterDelay(84,()->{var cell=activate(h);var p=player(h,cell,Items.AIR);var rt=DimensionRuntime.get(h.getLevel().getServer());yes(h,rt.travel(p,cell),"Outbound");var exit=p.blockPosition();var be=(SkyPortalBlockEntity)p.level().getBlockEntity(exit);var exact=be.link;var removed=rt.configs.stream().filter(c->c.id.equals("skyblock")).findFirst().orElseThrow();
  rt.configs.remove(removed);try{rt.tickPlayer(p);yes(h,p.level()==h.getLevel()&&p.blockPosition().equals(exact),"Generated exact return survives removed config without a second spawn teleport");}finally{rt.configs.add(removed);}h.succeed();});}

 public static void playerInventoryRoundtrip(GameTestHelper h){h.runAfterDelay(86,()->{var cell=activate(h);var p=player(h,cell,Items.AIR);var backpack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:backpack")));backpack.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(DimensionRegistry.LEGACY.asItem(),3))));p.getInventory().setItem(0,backpack);var rt=DimensionRuntime.get(h.getLevel().getServer());yes(h,rt.travel(p,cell),"Travel with SimpleBuilding backpack");var exit=p.blockPosition();yes(h,rt.travel(p,exit),"Return with backpack");var contents=p.getInventory().getItem(0).get(net.minecraft.core.component.DataComponents.CONTAINER);yes(h,contents!=null&&contents.nonEmptyItemCopyStream().findFirst().orElseThrow().getCount()==3,"Foreign items preserved exactly without duplication");h.succeed();});}
 public static void configAndLang(GameTestHelper h){try{
  var resource=h.getLevel().getServer().getResourceManager();
  for(String locale:List.of("en_us","de_de")){
   // Lang is a client resource: inspect the shipped module resource, not server pack aliases.
   try(var input=DimensionTests.class.getResourceAsStream("/assets/simpledimension/lang/"+locale+".json")){
    yes(h,input!=null,"Locale shipped");var data=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    yes(h,data.has("simpledimension.config.dimensions"),"Dedicated dimensions tab translated");
    for(String key:List.of("accessEnabled","skyblockEnabled","miningEnabled","travelEnabled","automaticDestination","portalDelayTicks","teleportCooldownTicks","nonPlayerTravel")){
     yes(h,data.has("simpledimension.config."+key),"Option name "+key);String tip=data.get("simpledimension.config."+key+".tooltip").getAsString();yes(h,tip.contains(locale.equals("en_us")?"Default:":"Standard:"),"Default in tooltip "+key);
     if(List.of("skyblockEnabled","miningEnabled","travelEnabled").contains(key))yes(h,tip.contains(locale.equals("en_us")?"Default: On.":"Standard: Ein."),"Correct enabled default "+key);
    }
   }
  }
  var settings=new DimensionSettings();settings.portalDelayTicks=Integer.MAX_VALUE;settings.teleportCooldownTicks=Integer.MIN_VALUE;settings.nonPlayerTravel=true;settings.normalize();yes(h,settings.portalDelayTicks==200&&settings.teleportCooldownTicks==20&&!settings.nonPlayerTravel,"Client cannot enlarge limits or enable mobs");
 }catch(Exception e){throw new AssertionError(e);}h.succeed();}

 private static void presetTrip(GameTestHelper h,DimensionPortalConfig cfg,int tick){h.runAfterDelay(tick,()->{
  var cell=build(h,cfg,0,Direction.Axis.X);var p=player(h,cell,Items.FLINT_AND_STEEL);yes(h,ignite(p,cell)==InteractionResult.SUCCESS,"Source preset activates "+cfg.id);
  var anchor=((SkyPortalBlockEntity)h.getLevel().getBlockEntity(cell)).anchor;var rt=DimensionRuntime.get(h.getLevel().getServer());
  BlockPos vegetation=null;
  ServerLevel mining=null;
  if(cfg.id.equals("mining")){
   mining=h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(cfg.targetDimensionId)));
   var mapped=DimensionRuntime.pos(PortalTravelRules.toTarget(new BlockPos3i(anchor.getX(),anchor.getY(),anchor.getZ()),cfg.travelCoordinateScale));
   int highest=mining.getMinY();
   for(int dx=-4;dx<=3;dx++)for(int dz=-4;dz<=3;dz++){var column=mapped.offset(dx,0,dz);mining.getChunk(column.getX()>>4,column.getZ()>>4);highest=Math.max(highest,mining.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,column.getX(),column.getZ()));}
   // Reproduce a nonblocking plant above the solid surface, independent of the world seed.
   var soil=mapped.atY(highest+8);
   mining.setBlock(soil,Blocks.DIRT.defaultBlockState(),3);vegetation=soil.above();
   mining.setBlock(vegetation,Blocks.SHORT_GRASS.defaultBlockState(),3);
   yes(h,mining.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,mapped.getX(),mapped.getZ())==vegetation.getY(),"Motion height excludes arrival vegetation");
  }
  yes(h,rt.travel(p,cell),"Real "+cfg.id+" outbound");yes(h,p.level().dimension().identifier().toString().equals(cfg.targetDimensionId),"Correct registry destination");
  yes(h,p.blockPosition().getX()==(int)Math.floor(anchor.getX()/cfg.travelCoordinateScale)&&p.blockPosition().getZ()==(int)Math.floor(anchor.getZ()/cfg.travelCoordinateScale),"Real coordinate ratio");
  if(vegetation!=null){yes(h,mining.getBlockState(vegetation).is(Blocks.SHORT_GRASS),"Arrival preserves vegetation");yes(h,DimensionRuntime.safe(mining,p.blockPosition()),"Mining landing is safe above vegetation");}
  var exit=p.blockPosition();yes(h,rt.travel(p,exit)&&p.level()==h.getLevel(),"Exact return from "+cfg.id);h.succeed();
 });}
 public static void miningTravel(GameTestHelper h){presetTrip(h,DimensionPortalConfig.miningDimensionPreset(),100);}
 public static void compressedTravel(GameTestHelper h){presetTrip(h,DimensionPortalConfig.travelDimensionPreset(),103);}

 public static void exactOriginLinks(GameTestHelper h){h.runAfterDelay(120,()->{
  var cell=activate(h);var p=player(h,cell,Items.AIR);var rt=DimensionRuntime.get(h.getLevel().getServer());yes(h,rt.travel(p,cell),"First origin outbound");
  var firstLevel=p.level();var firstExit=p.blockPosition();var first=(SkyPortalBlockEntity)firstLevel.getBlockEntity(firstExit);var exactFirst=first.link;
  yes(h,rt.travel(p,firstExit)&&p.blockPosition().equals(exactFirst),"First exact address");
  h.runAfterDelay(2,()->{var other=buildAt(h,DimensionPortalConfig.defaultSkyblock(),1,Direction.Axis.X,base(h).offset(14,0,0));h.getLevel().setBlock(other.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);var second=player(h,other,Items.FLINT_AND_STEEL);
   yes(h,ignite(second,other)==InteractionResult.SUCCESS&&rt.travel(second,other),"Independent second origin outbound");var exit=second.blockPosition();var back=((SkyPortalBlockEntity)second.level().getBlockEntity(exit)).link;
   yes(h,!exit.equals(firstExit)&&!back.equals(exactFirst),"No foreign portal adoption");yes(h,rt.travel(second,exit)&&second.blockPosition().equals(back),"Second exact address");yes(h,first.link.equals(exactFirst),"First link remains unchanged");h.succeed();
  });
 });}
 public static void destinationClaim(GameTestHelper h){h.runAfterDelay(124,()->{var cell=activate(h);var p=player(h,cell,Items.AIR);var source=p.level();var be=(SkyPortalBlockEntity)source.getBlockEntity(cell);var target=source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,DimensionRegistry.id("skyblock")));var old=DimensionRuntime.permission;
  try{DimensionRuntime.permission=(player,level,pos)->level==source;yes(h,!DimensionRuntime.get(source.getServer()).travel(p,cell),"Destination claim prevents travel");yes(h,p.level()==source&&target.getBlockState(be.anchor).isAir()&&target.getBlockState(be.anchor.below()).isAir(),"No partial portal or island on claim refusal");}finally{DimensionRuntime.permission=old;}h.succeed();
 });}

}
