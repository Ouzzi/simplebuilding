package com.simplebuilding.modules.simplecreatures;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Shared bodies (moved from Simple QoL); Fabric wraps them in ModuleGameTest, NeoForge/Forge register {@link #ALL}. */
public final class CreaturesTests {
 public static final Map<String,Consumer<GameTestHelper>> ALL=new LinkedHashMap<>();
 static {
  ALL.put("launch",CreaturesTests::launch);ALL.put("config",CreaturesTests::config);ALL.put("qol_migration",CreaturesTests::qolMigration);ALL.put("super_switch",CreaturesTests::superSwitch);
  ALL.put("muting",CreaturesTests::muting);
  ALL.put("baby",CreaturesTests::baby);
  ALL.put("piglins",CreaturesTests::piglins);
  ALL.put("gold_trim",CreaturesTests::goldTrim);
  // ALL-END
 }
 private CreaturesTests(){}
 public static Identifier id(String s){return Identifier.fromNamespaceAndPath(SimpleCreatures.MOD_ID,s);}
 private static Player player(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(h.absolutePos(pos)));return p;}
 private static void configured(Runnable body){var old=SimpleCreatures.CONFIG;try{SimpleCreatures.CONFIG=new CreaturesConfig();body.run();}finally{SimpleCreatures.CONFIG=old;SubMods.set(SimpleCreatures.MOD_ID,true);}}
 public static void launch(GameTestHelper h){h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(SimpleCreatures.MOD_ID)!=null,"/simplecreatures registered");h.succeed();}
 public static void config(GameTestHelper h){var c=new CreaturesConfig();h.assertTrue(c.mutedEntities.isEmpty()&&c.nametagMuteSuffixes.equals(List.of("_mute","_shhh"))&&c.nametagBabySuffixes.equals(List.of("_baby","_small"))&&c.piglinsIgnoreGoldTrims&&c.piglinsIgnoreGoldTools,"defaults like Simple QoL");c.nametagMuteSuffixes=new ArrayList<>(Collections.nCopies(100,"x"));c.mutedEntities=null;c.normalize();h.assertTrue(c.nametagMuteSuffixes.size()==1&&c.mutedEntities.isEmpty(),"list bounds");h.succeed();}
 /** First start: the old qOL values are taken over once; an existing own file is never overwritten. */
 public static void qolMigration(GameTestHelper h){try{var dir=Files.createTempDirectory("simplecreatures-migration");Files.writeString(dir.resolve("simplequalityoflife.json"),"{\"qOL\":{\"mutedEntities\":[\"minecraft:cow\"],\"nametagBabySuffixes\":[\"_tiny\"],\"piglinsIgnoreGoldTools\":false,\"enableAutowalk\":true}}");var taken=SimpleCreatures.read(dir);h.assertTrue(taken.mutedEntities.equals(List.of("minecraft:cow"))&&taken.nametagBabySuffixes.equals(List.of("_tiny"))&&!taken.piglinsIgnoreGoldTools&&taken.piglinsIgnoreGoldTrims&&taken.nametagMuteSuffixes.equals(List.of("_mute","_shhh")),"old QoL values taken over, others default");h.assertTrue(Files.isRegularFile(dir.resolve("simplecreatures.json")),"own file written as marker");Files.writeString(dir.resolve("simplecreatures.json"),"{\"piglinsIgnoreGoldTrims\":false}");var own=SimpleCreatures.read(dir);h.assertTrue(!own.piglinsIgnoreGoldTrims&&own.piglinsIgnoreGoldTools,"own file wins over QoL");var empty=SimpleCreatures.read(Files.createTempDirectory("simplecreatures-empty"));h.assertTrue(empty.piglinsIgnoreGoldTrims&&empty.mutedEntities.isEmpty(),"no file: defaults");}catch(java.io.IOException e){h.fail("migration io: "+e);}h.succeed();}
 /** "Enable Simple Creatures" off in the super-mod: name tags and piglins behave like Vanilla. */
 public static void superSwitch(GameTestHelper h){configured(()->{var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_mute"));h.assertTrue(cow.isSilent(),"suffix mutes while on");SubMods.set(SimpleCreatures.MOD_ID,false);h.assertTrue(!cow.isSilent(),"switched-off sub-mod leaves Vanilla sound");var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_HOE));h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"switched-off sub-mod: Vanilla piglins");});h.succeed();}
// MOVED-BEGIN
 public static void muting(GameTestHelper h){configured(()->{var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_mute"));h.assertTrue(cow.isSilent(),"Mute suffix");cow.setCustomName(null);h.assertTrue(!cow.isSilent(),"No suffix means normal sound");SimpleCreatures.config().mutedEntities.add("minecraft:cow");h.assertTrue(cow.isSilent(),"Type mute");});h.succeed();}
 public static void baby(GameTestHelper h){var cow=h.spawn(EntityTypes.COW,2,2,2);cow.setCustomName(net.minecraft.network.chat.Component.literal("Cow_baby"));cow.setAge(-1);cow.tickCount=99;h.runAfterDelay(3,()->{h.assertTrue(cow.isBaby()&&cow.getAge()<-23000,"Actual age tick keeps named baby young");cow.setCustomName(null);cow.setAge(-1);h.runAfterDelay(3,()->{h.assertTrue(!cow.isBaby(),"Removing suffix permits aging");h.succeed();});});}
 public static void piglins(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_HOE));h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Offhand gold is safe");SimpleCreatures.config().piglinsIgnoreGoldTools=false;h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Gold tool toggle");});h.succeed();}
 public static void goldTrim(GameTestHelper h){configured(()->{var p=player(h,new BlockPos(2,2,2));var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);var lookup=h.getLevel().registryAccess();armor.set(DataComponents.TRIM,new net.minecraft.world.item.equipment.trim.ArmorTrim(lookup.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.GOLD),lookup.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));p.setItemSlot(EquipmentSlot.CHEST,armor);h.assertTrue(net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Actual gold trim accepted");SimpleCreatures.config().piglinsIgnoreGoldTrims=false;h.assertTrue(!net.minecraft.world.entity.monster.piglin.PiglinAi.isWearingSafeArmor(p),"Gold trim toggle enforced");});h.succeed();}
// MOVED-END
}
