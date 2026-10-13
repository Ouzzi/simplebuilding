package com.simplebuilding.modules.simplemobmanners;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Shared bodies (moved from Simple QoL); Fabric wraps them in ModuleGameTest, NeoForge/Forge register {@link #ALL}. */
public final class MobMannersTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("launch",MobMannersTests::launch);ALL.put("config",MobMannersTests::config);ALL.put("qol_migration",MobMannersTests::qolMigration);ALL.put("rename_migration",MobMannersTests::renameMigration);ALL.put("super_switch",MobMannersTests::superSwitch);
  ALL.put("muting",MobMannersTests::muting);
  ALL.put("baby",MobMannersTests::baby);
  ALL.put("piglins",MobMannersTests::piglins);
  ALL.put("gold_trim",MobMannersTests::goldTrim);
  // ALL-END
 }
 private MobMannersTests(){}
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath(SimpleMobManners.MOD_ID,s);}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static void configured(Runnable body){var old=SimpleMobManners.CONFIG;try{SimpleMobManners.CONFIG=new MobMannersConfig();body.run();}finally{SimpleMobManners.CONFIG=old;SubMods.set(SimpleMobManners.MOD_ID,true);}}
 public static void launch(GameTestHelper h){var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot();h.assertTrue(root.getChild(SimpleMobManners.MOD_ID)!=null,"/simplemobmanners registered");var alias=root.getChild(SimpleMobManners.LEGACY_ID);h.assertTrue(alias!=null&&alias.getChild("muteSuffixes")!=null&&alias.getChild("piglinsIgnoreGoldTools")!=null,"/simplecreatures kept as alias");h.succeed();}
 /** Rename Simple Creatures -> Simple Mob Manners: an old config/simplecreatures.json is taken over once, the old file stays, the own file wins afterwards. */
 public static void renameMigration(GameTestHelper h){try{var dir=Files.createTempDirectory("simplemobmanners-rename");Files.writeString(dir.resolve("simplecreatures.json"),"{\"mutedEntities\":[\"minecraft:pig\"],\"nametagMuteSuffixes\":[\"_quiet\"],\"piglinsIgnoreGoldTrims\":false}");Files.writeString(dir.resolve("simplequalityoflife.json"),"{\"qOL\":{\"mutedEntities\":[\"minecraft:cow\"]}}");var taken=SimpleMobManners.read(dir);h.assertTrue(taken.mutedEntities.equals(List.of("minecraft:pig"))&&taken.nametagMuteSuffixes.equals(List.of("_quiet"))&&!taken.piglinsIgnoreGoldTrims&&taken.piglinsIgnoreGoldTools,"old simplecreatures.json taken over before the QoL values");h.assertTrue(Files.isRegularFile(dir.resolve("simplemobmanners.json"))&&Files.isRegularFile(dir.resolve("simplecreatures.json")),"own file written, old file untouched");Files.writeString(dir.resolve("simplemobmanners.json"),"{\"piglinsIgnoreGoldTrims\":true}");h.assertTrue(SimpleMobManners.read(dir).piglinsIgnoreGoldTrims,"own file wins over the old one");h.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(i->i.getNamespace().equals(SimpleMobManners.LEGACY_ID)||i.getNamespace().equals(SimpleMobManners.MOD_ID))&&BuiltInRegistries.BLOCK.keySet().stream().noneMatch(i->i.getNamespace().equals(SimpleMobManners.LEGACY_ID)||i.getNamespace().equals(SimpleMobManners.MOD_ID)),"no registry ids under either namespace (nothing for IdMoves)");}catch(java.io.IOException e){h.fail("rename migration io: "+e);}h.succeed();}
 public static void config(GameTestHelper h){var c=new MobMannersConfig();h.assertTrue(c.mutedEntities.isEmpty()&&c.nametagMuteSuffixes.equals(List.of("_mute","_shhh"))&&c.nametagBabySuffixes.equals(List.of("_baby","_small"))&&c.piglinsIgnoreGoldTrims&&c.piglinsIgnoreGoldTools,"defaults like Simple QoL");c.nametagMuteSuffixes=new ArrayList<>(Collections.nCopies(100,"x"));c.mutedEntities=null;c.normalize();h.assertTrue(c.nametagMuteSuffixes.size()==1&&c.mutedEntities.isEmpty(),"list bounds");h.succeed();}
 /** First start: the old qOL values are taken over once; an existing own file is never overwritten. */
 public static void qolMigration(GameTestHelper h){try{var dir=Files.createTempDirectory("simplemobmanners-migration");Files.writeString(dir.resolve("simplequalityoflife.json"),"{\"qOL\":{\"mutedEntities\":[\"minecraft:cow\"],\"nametagBabySuffixes\":[\"_tiny\"],\"piglinsIgnoreGoldTools\":false,\"enableAutowalk\":true}}");var taken=SimpleMobManners.read(dir);h.assertTrue(taken.mutedEntities.equals(List.of("minecraft:cow"))&&taken.nametagBabySuffixes.equals(List.of("_tiny"))&&!taken.piglinsIgnoreGoldTools&&taken.piglinsIgnoreGoldTrims&&taken.nametagMuteSuffixes.equals(List.of("_mute","_shhh")),"old QoL values taken over, others default");h.assertTrue(Files.isRegularFile(dir.resolve("simplemobmanners.json")),"own file written as marker");Files.writeString(dir.resolve("simplemobmanners.json"),"{\"piglinsIgnoreGoldTrims\":false}");var own=SimpleMobManners.read(dir);h.assertTrue(!own.piglinsIgnoreGoldTrims&&own.piglinsIgnoreGoldTools,"own file wins over QoL");var empty=SimpleMobManners.read(Files.createTempDirectory("simplemobmanners-empty"));h.assertTrue(empty.piglinsIgnoreGoldTrims&&empty.mutedEntities.isEmpty(),"no file: defaults");}catch(java.io.IOException e){h.fail("migration io: "+e);}h.succeed();}
 /** "Enable Simple Mob Manners" off in the super-mod: name tags and piglins behave like Vanilla. */
 public static void superSwitch(GameTestHelper h){configured(()->{var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_mute"));h.assertTrue(cow.isSilent(),"suffix mutes while on");SubMods.set(SimpleMobManners.MOD_ID,false);h.assertTrue(!cow.isSilent(),"switched-off sub-mod leaves Vanilla sound");var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_HOE));h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"switched-off sub-mod: Vanilla piglins");});h.succeed();}
// MOVED-BEGIN
 public static void muting(GameTestHelper h){configured(()->{var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_mute"));h.assertTrue(cow.isSilent(),"Mute suffix");cow.setCustomName(null);h.assertTrue(!cow.isSilent(),"No suffix means normal sound");SimpleMobManners.config().mutedEntities.add("minecraft:cow");h.assertTrue(cow.isSilent(),"Type mute");});h.succeed();}
 public static void baby(GameTestHelper h){var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_baby"));cow.setAge(-1);cow.tickCount=99;h.runAfterDelay(3,()->{h.assertTrue(cow.isBaby()&&cow.getAge()<-23000,"Actual age tick keeps named baby young");cow.setCustomName(null);cow.setAge(-1);h.runAfterDelay(3,()->{h.assertTrue(!cow.isBaby(),"Removing suffix permits aging");h.succeed();});});}
 public static void piglins(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_HOE));h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Offhand gold is safe");SimpleMobManners.config().piglinsIgnoreGoldTools=false;h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Gold tool toggle");});h.succeed();}
 public static void goldTrim(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);var lookup=h.getLevel().registryAccess();armor.set(DataComponents.TRIM,new net.minecraft.world.item.equipment.trim.ArmorTrim(lookup.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.GOLD),lookup.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));p.setItemSlot(EquipmentSlot.CHEST,armor);h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Actual gold trim accepted");SimpleMobManners.config().piglinsIgnoreGoldTrims=false;h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Gold trim toggle enforced");});h.succeed();}
// MOVED-END
}
