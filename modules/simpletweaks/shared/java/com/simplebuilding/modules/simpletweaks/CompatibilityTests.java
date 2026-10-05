package com.simplebuilding.modules.simpletweaks;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
/** Exercises actual registry/codec/container entrypoints, on both loaders with SimpleBuilding. */
public final class CompatibilityTests {
 public static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
  Map.entry("guide_book", com.simplebuilding.modules.simpletweaks.guide.TweaksGuide::gameTest),
  Map.entry("boot_and_no_duplicate_registrations", CompatibilityTests::boot),
  Map.entry("every_legacy_registry_lookup", CompatibilityTests::lookups),
  Map.entry("every_old_item_decodes_and_saves_canonical_id", CompatibilityTests::items),
  Map.entry("old_stacked_pads_keep_their_count", CompatibilityTests::stackCounts),
  Map.entry("every_old_block_state_decodes", CompatibilityTests::blocks),
  Map.entry("every_old_block_entity_decodes_and_preserves_owner", CompatibilityTests::blockEntities),
  Map.entry("old_launchpad_preserves_stored_charges", CompatibilityTests::charges),
  Map.entry("old_elytra_components_decode_and_save_canonical_ids", CompatibilityTests::components),
  Map.entry("deed_custom_data_survives_without_claim_authority", CompatibilityTests::deed),
  Map.entry("legacy_items_work_in_simplebuilding_storage", CompatibilityTests::storage),
  Map.entry("unknown_names_and_wrong_registries_are_refused", CompatibilityTests::refusal),
  Map.entry("claims_and_old_commands_are_not_registered", CompatibilityTests::commands));
 static Identifier id(String namespace, String path) { return Identifier.fromNamespaceAndPath(namespace, path); }
 static Identifier old(String path) { return id("simpletweaks", path); }
 static Identifier now(String path) { return id("simplebuilding", path); }
 static RegistryOps<Tag> ops(GameTestHelper h) { return RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess()); }
 static ItemStack stack(GameTestHelper h, String path) {
  CompoundTag tag=new CompoundTag();tag.putString("id",old(path).toString());tag.putInt("count",1);
  return ItemStack.CODEC.parse(ops(h),tag).getOrThrow();
 }
 static void require(GameTestHelper h, boolean condition, String message) { h.assertTrue(condition,message); }
 static void boot(GameTestHelper h) {
  require(h,BuiltInRegistries.ITEM.containsKey(old("claim_deed")),"Legacy deed registered");
  require(h,BuiltInRegistries.ITEM.containsKey(now("amethyst_lens")),"SimpleBuilding loaded");
  require(h,BuiltInRegistries.ITEM.keySet().stream().filter(x->x.getNamespace().equals("simpletweaks")).count()==2,"Only the deed and the guide are real module items");
  for (Registry<?> r : List.of(BuiltInRegistries.BLOCK,BuiltInRegistries.BLOCK_ENTITY_TYPE,BuiltInRegistries.DATA_COMPONENT_TYPE,BuiltInRegistries.ENTITY_TYPE,BuiltInRegistries.CREATIVE_MODE_TAB))
   require(h,r.keySet().stream().noneMatch(x->x.getNamespace().equals("simpletweaks")),"No duplicate gameplay registry: "+r.key());
  try {
   java.nio.file.Path root=java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
   while(root!=null && !java.nio.file.Files.isRegularFile(root.resolve("modules/modules.json")))root=root.getParent();
   if(root==null)throw new java.io.IOException("Cannot locate module root");
   var output=root.resolve("modules/simpletweaks/generated/resources/wiki/items.json");java.nio.file.Files.createDirectories(output.getParent());
   String json="{\"items\":[{\"id\":\""+BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.getValue(old("claim_deed")))+"\",\"kind\":\"item\"}]}\n";
   java.nio.file.Files.writeString(output,json);
  } catch(java.io.IOException e){throw new AssertionError(e);}
  h.succeed();
 }
 static <T> void lookup(GameTestHelper h, Registry<T> r, String path) {
  Identifier target=LegacyAliases.target(r,old(path));
  var expected=r.get(target).orElseThrow();
  require(h,r.get(old(path)).orElseThrow()==expected,"Holder by ID "+path);
  require(h,r.get(ResourceKey.create(r.key(),old(path))).orElseThrow()==expected,"Holder by key "+path);
  require(h,r.getValue(old(path))==expected.value(),"Value by ID "+path);
  require(h,r.getValue(ResourceKey.create(r.key(),old(path)))==expected.value(),"Value by key "+path);
  require(h,r.containsKey(old(path)),"Contains "+path);
  require(h,r.getKey(expected.value()).equals(target),"Canonical save ID "+path);
 }
 static void lookups(GameTestHelper h) {
  for(String x:LegacyAliases.BLOCKS){lookup(h,BuiltInRegistries.ITEM,x);lookup(h,BuiltInRegistries.BLOCK,x);}
  for(String x:LegacyAliases.BLOCK_ENTITIES)lookup(h,BuiltInRegistries.BLOCK_ENTITY_TYPE,x);
  for(String x:LegacyAliases.COMPONENTS)lookup(h,BuiltInRegistries.DATA_COMPONENT_TYPE,x);
  lookup(h,BuiltInRegistries.ITEM,"laser_pointer");lookup(h,BuiltInRegistries.ITEM,"spawn_elytra");h.succeed();
 }
 static void items(GameTestHelper h) {
  for(String x:java.util.stream.Stream.concat(LegacyAliases.BLOCKS.stream(),java.util.stream.Stream.of("laser_pointer","spawn_elytra")).toList()) {
   ItemStack s=stack(h,x); require(h,!s.isEmpty(),"Decoded "+x);
   CompoundTag saved=(CompoundTag)ItemStack.CODEC.encodeStart(ops(h),s).getOrThrow();
   require(h,saved.getStringOr("id","").equals(LegacyAliases.target(BuiltInRegistries.ITEM,old(x)).toString()),"Canonical stack "+x);
  }h.succeed();
 }
 static void stackCounts(GameTestHelper h) {
  CompoundTag tag=new CompoundTag();tag.putString("id",old("elytra_pad").toString());tag.putInt("count",64);
  ItemStack s=ItemStack.CODEC.parse(ops(h),tag).getOrThrow();
  require(h,s.getCount()==64,"Legacy count retained by save codec");h.succeed();
 }
 static void blocks(GameTestHelper h) {
  for(String x:LegacyAliases.BLOCKS) {
   CompoundTag t=new CompoundTag();t.putString("id",old(x).toString());
   BlockState state=BlockState.CODEC.parse(ops(h),t).getOrThrow();
   require(h,state.getBlock()==BuiltInRegistries.BLOCK.getValue(now(x)),"Palette "+x);
  }h.succeed();
 }
 static final Map<String,String> BE_BLOCKS=Map.of("spawn_teleporter_be","spawn_teleporter","launchpad_be","launchpad","elytra_pad_be","elytra_pad","flypad_be","flypad","chunk_loader_be","chunk_loader","copper_pressure_plate_be","copper_pressure_plate","netherite_pressure_plate_be","netherite_pressure_plate");
 static BlockEntity loadBE(GameTestHelper h,String type,CompoundTag tag) {
  tag.putString("id",old(type).toString());
  var state=BuiltInRegistries.BLOCK.getValue(old(BE_BLOCKS.get(type))).defaultBlockState();
  BlockEntity be=BlockEntity.loadStatic(BlockPos.ZERO,state,tag,h.getLevel().registryAccess());
  require(h,be!=null,"Legacy block entity loads "+type);return be;
 }
 static void blockEntities(GameTestHelper h) {
  for(String type:LegacyAliases.BLOCK_ENTITIES) {
   CompoundTag tag=new CompoundTag();tag.putIntArray("Owner",new int[]{1,2,3,4});
   BlockEntity be=loadBE(h,type,tag);CompoundTag saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
   require(h,java.util.Arrays.equals(saved.getIntArray("Owner").orElseThrow(),new int[]{1,2,3,4}),"Owner survives "+type);
   require(h,BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).equals(now(type)),"Canonical BE "+type);
  }h.succeed();
 }
 static void charges(GameTestHelper h) {
  CompoundTag tag=new CompoundTag();tag.putInt("Charges",16);
  BlockEntity be=loadBE(h,"launchpad_be",tag);
  require(h,be.saveWithoutMetadata(h.getLevel().registryAccess()).getIntOr("Charges",0)==16,"No charge loss during decode; normal tier upgrade handles surplus");h.succeed();
 }
 static void components(GameTestHelper h) {
  CompoundTag tag=new CompoundTag();tag.putString("id",old("spawn_elytra").toString());tag.putInt("count",1);
  CompoundTag c=new CompoundTag();c.putInt("simpletweaks:flight_time",6000);c.putFloat("simpletweaks:boost_level",0.5f);c.putLong("simpletweaks:last_pad_tick",17);c.putBoolean("simpletweaks:is_safe_elytra",true);tag.put("components",c);
  ItemStack s=ItemStack.CODEC.parse(ops(h),tag).getOrThrow();
  CompoundTag saved=((CompoundTag)ItemStack.CODEC.encodeStart(ops(h),s).getOrThrow()).getCompoundOrEmpty("components");
  for(String x:LegacyAliases.COMPONENTS){require(h,saved.contains("simplebuilding:"+x),"Component retained "+x);require(h,!saved.contains("simpletweaks:"+x),"Component canonical "+x);}
  require(h,saved.getIntOr("simplebuilding:flight_time",0)==6000 && saved.getFloatOr("simplebuilding:boost_level",0)==0.5f && saved.getLongOr("simplebuilding:last_pad_tick",0)==17 && saved.getBooleanOr("simplebuilding:is_safe_elytra",false),"All values unchanged");h.succeed();
 }
 static void deed(GameTestHelper h) {
  ItemStack s=stack(h,"claim_deed");CompoundTag data=new CompoundTag();data.putLong("ClaimPos",123456);data.putString("OwnerName","Someone else");data.putString("UnknownFutureField","preserve");s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
  ItemStack loaded=ItemStack.CODEC.parse(ops(h),ItemStack.CODEC.encodeStart(ops(h),s).getOrThrow()).getOrThrow();
  require(h,loaded.get(DataComponents.CUSTOM_DATA).copyTag().equals(data),"Opaque deed data retained");
  require(h,loaded.getMaxStackSize()==16,"Source stack size");
  var player=h.makeMockServerPlayerInLevel();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,loaded);
  require(h,loaded.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.PASS,"Forged deed has no use authority");
  require(h,loaded.get(DataComponents.CUSTOM_DATA).copyTag().equals(data),"Use preserves data");h.succeed();
 }
 static void storage(GameTestHelper h) {
  var pos=new BlockPos(1,2,1);h.setBlock(pos,BuiltInRegistries.BLOCK.getValue(now("reinforced_hopper")));
  require(h,h.getBlockEntity(pos,BlockEntity.class) instanceof Container,"Public hopper Container");
  Container container=(Container)h.getBlockEntity(pos,BlockEntity.class);ItemStack s=stack(h,"claim_deed");Item expected=s.getItem();container.setItem(0,s);require(h,container.removeItem(0,1).is(expected),"Module artifact in SimpleBuilding storage");
  s=stack(h,"elytra_pad");expected=s.getItem();container.setItem(0,s);require(h,container.removeItem(0,1).is(expected),"Migrated pad in SimpleBuilding storage");h.succeed();
 }
 static void refusal(GameTestHelper h) {
  for(String x:List.of("claim_deed","token","admin","arbitrary","amethyst_lens"))require(h,LegacyAliases.target(BuiltInRegistries.ITEM,old(x))==null,"No invented alias "+x);
  require(h,LegacyAliases.target(BuiltInRegistries.ITEM,id("othermod","laser_pointer"))==null,"No foreign namespace");
  require(h,LegacyAliases.target(BuiltInRegistries.ITEM,null)==null,"Null safe");
  require(h,LegacyAliases.target(BuiltInRegistries.ENTITY_TYPE,old("elytra_pad"))==null,"Wrong registry refused");
  require(h,LegacyAliases.target(BuiltInRegistries.BLOCK,old("spawn_elytra"))==null,"Item not block");
  require(h,LegacyAliases.target(BuiltInRegistries.ITEM,now("laser_pointer"))==null,"No alias chain recursion");h.succeed();
 }
 static void commands(GameTestHelper h) {
  var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();
  require(h,(root.getChild("claim")==null || !root.getChild("claim").canUse(h.getLevel().getServer().createCommandSourceStack())) && root.getChild("simpletweaks")==null,"No usable disabled claim commands or duplicate old commands");
  require(h,root.getChild("simplebuilding")!=null && root.getChild("killboats")!=null && root.getChild("killcarts")!=null,"Existing commands present");h.succeed();
 }
 private CompatibilityTests() {}
}
