package com.simplebuilding.gametest;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.dummy.PartialArmorStand;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ruestungsstaender (Besitzer 2026-10-08, docs/ai/PLAN-STAENDER-2026-10-09.md): Ruestung tauschen, Arme fuer neu
 * aufgestellte Staender, mittlerer und kleiner Staender. Alle Wege wie im Spiel ({@code interact}, {@code useOn},
 * {@code Player.attack}). Loader-neutral; ohne {@link McVersion#TRAINING_DUMMY} gelingen die Tests sofort.
 */
public final class ArmorStandTests {
    private ArmorStandTests() {
    }

    private static final Vec3 CHEST = new Vec3(0.0, 1.0, 0.0);

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static <T extends ArmorStand> T stand(GameTestHelper helper, EntityType<T> type, int x, int z) {
        T stand = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 1.0, z + 0.5));
        stand.setPos(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(stand);
        return stand;
    }

    private static void wear(net.minecraft.world.entity.LivingEntity entity, Item head, Item chest, Item legs, Item feet) {
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
        entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
    }

    private static void wears(GameTestHelper helper, net.minecraft.world.entity.LivingEntity entity, String who, Item head, Item chest, Item legs,
            Item feet) {
        Item[] items = {head, chest, legs, feet};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack worn = entity.getItemBySlot(slots[i]);
            boolean ok = items[i] == Items.AIR ? worn.isEmpty() : worn.is(items[i]);
            helper.assertTrue(ok, who + " " + slots[i].getName() + ": expected " + items[i] + ", got " + worn);
        }
    }

    /** Schleich-Rechtsklick mit leerer Hand tauscht die ganze Ruestung - am Vanilla-Staender und an der Puppe. */
    public static void sneakingEmptyHandSwapsTheWholeArmour(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        for (EntityType<? extends ArmorStand> type : List.<EntityType<? extends ArmorStand>>of(EntityTypes.ARMOR_STAND, ModEntities.TRAINING_DUMMY)) {
            ArmorStand stand = stand(helper, type, 1, 1);
            ServerPlayer player = player(helper);
            wear(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
            wear(stand, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
            player.setShiftKeyDown(true);
            stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
            wears(helper, player, "player", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
            wears(helper, stand, type.toShortString(), Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
            // ein halb leerer Staender: die Teile wandern einzeln, Leeres wird leer
            stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
            wears(helper, player, "player", Items.AIR, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
            wears(helper, stand, type.toShortString(), Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
            stand.discard();
        }
        helper.succeed();
    }

    /** Ohne Schleichen bleibt Vanilla (ein Teil), mit Item in der Hand tauscht auch Schleichen nicht. */
    public static void plainRightClicksKeepTheVanillaSingleSlot(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ArmorStand stand = stand(helper, EntityTypes.ARMOR_STAND, 1, 1);
        ServerPlayer player = player(helper);
        wear(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        wear(stand, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND_CHESTPLATE), "a plain empty-hand click did not take the chestplate");
        wears(helper, player, "player", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        player.setShiftKeyDown(true);
        stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
        helper.assertTrue(stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "sneaking with an item swapped instead of putting it on");
        wears(helper, player, "player", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        helper.succeed();
    }

    /** Bestromter Staender (Redstone-Block darunter): jeder Rechtsklick tauscht, das Item in der Hand bleibt. */
    public static void aPoweredStandSwapsOnEveryRightClick(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.REDSTONE_BLOCK);
        ArmorStand stand = stand(helper, EntityTypes.ARMOR_STAND, 1, 1);
        ServerPlayer player = player(helper);
        wear(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        wear(stand, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
        wears(helper, player, "player", Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        wears(helper, stand, "stand", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        helper.assertTrue(player.getMainHandItem().is(Items.STICK), "the held stick went somewhere");
        helper.succeed();
    }

    /** Fluch der Bindung bleibt am Spieler, ausgeschaltet tauscht nichts. */
    public static void bindingCurseAndTheSwitchStopTheSwap(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ArmorStand stand = stand(helper, EntityTypes.ARMOR_STAND, 1, 1);
        ServerPlayer player = player(helper);
        wear(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        player.getItemBySlot(EquipmentSlot.HEAD).enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.BINDING_CURSE), 1);
        wear(stand, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        player.setShiftKeyDown(true);
        stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
        wears(helper, player, "player", Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        ServerTuningConfig.Features features = ServerTuning.get().features;
        boolean before = features.armorStandSwap;
        try {
            features.armorStandSwap = false;
            stand.interact(player, InteractionHand.MAIN_HAND, CHEST);
            wears(helper, player, "player (switched off)", Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        } finally {
            features.armorStandSwap = before;
        }
        helper.succeed();
    }

    private static Entity place(GameTestHelper helper, ServerPlayer player, Item item, int x, int z) {
        BlockPos floor = helper.absolutePos(new BlockPos(x, 0, z));
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        List<ArmorStand> found = helper.getLevel().getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(floor.above()));
        helper.assertTrue(found.size() == 1, "placing " + item + " made " + found.size() + " stands");
        return found.get(0);
    }

    /** Neu aufgestellte Staender (Vanilla-, Stroh-Item) haben Arme; per Befehl/Code erzeugte und ausgeschaltet wie Vanilla ohne; mittel/klein nie. */
    public static void placedStandsHaveArms(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        helper.assertTrue(((ArmorStand) place(helper, player, Items.ARMOR_STAND, 1, 1)).showArms(), "a placed armor stand has no arms");
        helper.assertTrue(((ArmorStand) place(helper, player, ModItems.STRAW_ARMOR_STAND, 3, 1)).showArms(), "a placed straw stand has no arms");
        helper.assertFalse(((ArmorStand) place(helper, player, ModItems.MEDIUM_ARMOR_STAND, 5, 1)).showArms(), "the medium stand has arms");
        helper.assertFalse(EntityTypes.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.COMMAND).showArms(),
                "a stand made by a command (or code) got arms");
        ServerTuningConfig.Features features = ServerTuning.get().features;
        boolean before = features.armorStandArms;
        try {
            features.armorStandArms = false;
            helper.assertFalse(((ArmorStand) place(helper, player, Items.ARMOR_STAND, 1, 3)).showArms(), "switched off, a placed stand has arms");
        } finally {
            features.armorStandArms = before;
        }
        helper.succeed();
    }

    /** Mittel: Hose und Stiefel, sonst nichts; klein: nur Stiefel. Rechtsklick wie Vanilla, Fremdes bleibt in der Hand. */
    public static void partialStandsHoldOnlyTheirSlots(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        PartialArmorStand medium = (PartialArmorStand) place(helper, player, ModItems.MEDIUM_ARMOR_STAND, 1, 1);
        PartialArmorStand small = (PartialArmorStand) place(helper, player, ModItems.SMALL_ARMOR_STAND, 4, 1);
        helper.assertTrue(medium.getType() == ModEntities.MEDIUM_ARMOR_STAND && small.getType() == ModEntities.SMALL_ARMOR_STAND, "wrong stand types");
        helper.assertTrue(medium.getBbHeight() < 1.1F && small.getBbHeight() < 0.6F, "partial stands are as tall as a full stand");
        for (Item item : new Item[]{Items.IRON_LEGGINGS, Items.IRON_BOOTS}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            medium.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.3, 0.0));
            helper.assertTrue(player.getMainHandItem().isEmpty(), "the medium stand did not take " + item);
        }
        for (Item item : new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_SWORD}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            medium.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.3, 0.0));
            helper.assertTrue(player.getMainHandItem().is(item), "the medium stand took " + item);
        }
        wears(helper, medium, "medium", Items.AIR, Items.AIR, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        for (Item item : new Item[]{Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            small.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.2, 0.0));
            helper.assertTrue(player.getMainHandItem().is(item), "the small stand took " + item);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_BOOTS));
        small.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.2, 0.0));
        wears(helper, small, "small", Items.AIR, Items.AIR, Items.AIR, Items.GOLDEN_BOOTS);
        // leere Hand nimmt die Stiefel wieder ab
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        small.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.2, 0.0));
        helper.assertTrue(player.getMainHandItem().is(Items.GOLDEN_BOOTS) && small.getItemBySlot(EquipmentSlot.FEET).isEmpty(),
                "the small stand kept its boots");
        // Tausch: nur Hose und Stiefel wandern, Helm und Brust bleiben am Spieler
        wear(player, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        medium.interact(player, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.3, 0.0));
        wears(helper, player, "player", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        wears(helper, medium, "medium", Items.AIR, Items.AIR, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        helper.assertTrue(medium.getPickResult().is(ModItems.MEDIUM_ARMOR_STAND) && small.getPickResult().is(ModItems.SMALL_ARMOR_STAND),
                "pick block gives the wrong item");
        helper.succeed();
    }

    /** Zwei schnelle Schlaege (echter Weg) bauen die Staender ab: das eigene Item faellt, nicht der Vanilla-Staender. */
    public static void partialStandsDropTheirOwnItem(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        for (EntityType<PartialArmorStand> type : List.of(ModEntities.MEDIUM_ARMOR_STAND, ModEntities.SMALL_ARMOR_STAND)) {
            PartialArmorStand stand = stand(helper, type, 3, 3);
            stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            player.snapTo(stand.getX() - 1.5, stand.getY(), stand.getZ());
            player.lookAt(EntityAnchorArgument.Anchor.EYES, stand.position());
            player.attack(stand);
            player.attack(stand);
            helper.assertTrue(stand.isRemoved(), "two fast hits do not break the " + type.toShortString());
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, stand.getBoundingBox().inflate(2.0));
            Item own = stand.item();
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(own)), "the " + type.toShortString() + " did not drop itself");
            helper.assertFalse(drops.stream().anyMatch(e -> e.getItem().is(Items.ARMOR_STAND)), "the " + type.toShortString() + " dropped a vanilla stand");
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.IRON_BOOTS)), "the " + type.toShortString() + " lost its boots");
            drops.forEach(Entity::discard);
        }
        helper.succeed();
    }

    /** Die Testzentrale fuehrt die neuen Staender. */
    public static void theTestCentreStocksThePartialStands(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        var plan = com.simplebuilding.dev.testcentre.TestCentreLayout.plan(helper.getLevel().registryAccess(), new BlockPos(0, 64, 0));
        helper.assertTrue(plan.coveredItems().contains(ModItems.MEDIUM_ARMOR_STAND) && plan.coveredItems().contains(ModItems.SMALL_ARMOR_STAND),
                "the partial armor stands have no place in the test centre");
        helper.succeed();
    }
}
