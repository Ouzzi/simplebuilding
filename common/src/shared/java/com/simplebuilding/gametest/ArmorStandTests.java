package com.simplebuilding.gametest;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.dummy.SmallArmorStand;
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
 * aufgestellte Staender, kleiner Staender (Nachtrag 29: genau ein Ruestungsteil oder eine Tier-Ruestung, Spender, alte Welten). Alle Wege wie im Spiel ({@code interact}, {@code useOn},
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

    /** Neu aufgestellte Staender (Vanilla-, Stroh-Item) haben Arme; per Befehl/Code erzeugte und ausgeschaltet wie Vanilla ohne; der kleine nie. */
    public static void placedStandsHaveArms(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        helper.assertTrue(((ArmorStand) place(helper, player, Items.ARMOR_STAND, 1, 1)).showArms(), "a placed armor stand has no arms");
        helper.assertTrue(((ArmorStand) place(helper, player, ModItems.STRAW_ARMOR_STAND, 3, 1)).showArms(), "a placed straw stand has no arms");
        helper.assertFalse(((ArmorStand) place(helper, player, ModItems.SMALL_ARMOR_STAND, 5, 1)).showArms(), "the small stand has arms");
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

    /** Jedes Vanilla-Ruestungsteil mit seinem Slot. */
    private static final Object[][] PIECES = {
            {EquipmentSlot.HEAD, new Item[]{Items.LEATHER_HELMET, Items.COPPER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET,
                    Items.DIAMOND_HELMET, Items.NETHERITE_HELMET, Items.TURTLE_HELMET}},
            {EquipmentSlot.CHEST, new Item[]{Items.LEATHER_CHESTPLATE, Items.COPPER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE,
                    Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE}},
            {EquipmentSlot.LEGS, new Item[]{Items.LEATHER_LEGGINGS, Items.COPPER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS,
                    Items.GOLDEN_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS}},
            {EquipmentSlot.FEET, new Item[]{Items.LEATHER_BOOTS, Items.COPPER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS,
                    Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}},
            {EquipmentSlot.BODY, new Item[]{Items.LEATHER_HORSE_ARMOR, Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR,
                    Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR, Items.WOLF_ARMOR, Items.COPPER_NAUTILUS_ARMOR, Items.IRON_NAUTILUS_ARMOR,
                    Items.GOLDEN_NAUTILUS_ARMOR, Items.DIAMOND_NAUTILUS_ARMOR, Items.NETHERITE_NAUTILUS_ARMOR}},
    };

    /** Was der kleine Staender ablehnt: Waffen, Elytren, Koepfe/Kuerbis, Lama-Teppich, Geschirr, Sattel, Schild, Stock. */
    private static final Item[] FOREIGN = {Items.IRON_SWORD, Items.ELYTRA, Items.CARVED_PUMPKIN, Items.SKELETON_SKULL, Items.CARPET.white(),
            Items.HARNESS.white(), Items.SADDLE, Items.SHIELD, Items.STICK, Items.ARMOR_STAND};

    private static final Vec3 POST = new Vec3(0.0, 0.6, 0.0);

    /**
     * Jedes Ruestungsteil und jede Tier-Ruestung: Rechtsklick legt es in den passenden Slot (Tier-Ruestung: Koerper, mit
     * der richtigen Tierform), leere Hand nimmt es wieder ab. Fremdes bleibt in der Hand.
     */
    public static void theSmallStandTakesEveryArmorPieceAndAnimalArmor(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        SmallArmorStand stand = (SmallArmorStand) place(helper, player, ModItems.SMALL_ARMOR_STAND, 1, 1);
        helper.assertTrue(stand.getType() == ModEntities.SMALL_ARMOR_STAND && stand.getBbHeight() < 1.1F && stand.getBbHeight() > 0.9F,
                "the small stand is not one block tall: " + stand.getBbHeight());
        int count = 0;
        for (Object[] row : PIECES) {
            EquipmentSlot slot = (EquipmentSlot) row[0];
            for (Item item : (Item[]) row[1]) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                stand.interact(player, InteractionHand.MAIN_HAND, POST);
                helper.assertTrue(player.getMainHandItem().isEmpty(), "the small stand did not take " + item);
                helper.assertTrue(stand.getItemBySlot(slot).is(item) && stand.shown().is(item) && stand.shownSlot() == slot,
                        item + " is not shown in " + slot.getName() + ": " + stand.getItemBySlot(slot));
                stand.interact(player, InteractionHand.MAIN_HAND, POST);
                helper.assertTrue(player.getMainHandItem().is(item) && stand.shownSlot() == null, "an empty hand did not take " + item + " back");
                count++;
            }
        }
        helper.assertTrue(count == 41, "expected 41 pieces, tried " + count);
        helper.assertTrue(SmallArmorStand.animal(new ItemStack(Items.DIAMOND_HORSE_ARMOR)) == SmallArmorStand.Animal.HORSE
                        && SmallArmorStand.animal(new ItemStack(Items.WOLF_ARMOR)) == SmallArmorStand.Animal.WOLF
                        && SmallArmorStand.animal(new ItemStack(Items.IRON_NAUTILUS_ARMOR)) == SmallArmorStand.Animal.NAUTILUS
                        && SmallArmorStand.animal(new ItemStack(Items.CARPET.white())) == null,
                "animal armor is shown in the wrong animal shape");
        for (Item item : FOREIGN) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            stand.interact(player, InteractionHand.MAIN_HAND, POST);
            helper.assertTrue(player.getMainHandItem().is(item) && player.getMainHandItem().getCount() == 1 && stand.shownSlot() == null,
                    "the small stand took " + item);
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                helper.assertTrue(stand.getItemBySlot(slot).isEmpty(), "the small stand holds " + stand.getItemBySlot(slot) + " after " + item);
            }
        }
        helper.succeed();
    }

    /**
     * Genau ein Item: ein zweites passendes Teil tauscht mit dem gezeigten (das alte kommt in die Hand), ein Stapel aus mehreren
     * nicht; Fremdes neben dem Item bleibt abgelehnt. Schleichen + leere Hand tauscht nur das eine Teil mit dem Spieler.
     */
    public static void theSmallStandHoldsExactlyOneItem(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        SmallArmorStand stand = (SmallArmorStand) place(helper, player, ModItems.SMALL_ARMOR_STAND, 1, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_CHESTPLATE));
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_CHESTPLATE) && stand.getItemBySlot(EquipmentSlot.BODY).is(Items.DIAMOND_HORSE_ARMOR)
                && stand.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "a second piece did not swap with the shown one");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_BOOTS, 2));
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && stand.shown().is(Items.DIAMOND_HORSE_ARMOR),
                "a stack of two swapped onto a full stand");
        // Schleich-Tausch: nur das eine Teil wandert, die uebrige Ruestung bleibt am Spieler
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_CHESTPLATE));
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        wear(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        player.setShiftKeyDown(true);
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        wears(helper, player, "player", Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        wears(helper, stand, "small stand", Items.AIR, Items.IRON_CHESTPLATE, Items.AIR, Items.AIR);
        // leerer Staender nimmt beim Tausch das erste getragene Teil (Brust)
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_CHESTPLATE) && stand.shownSlot() == null, "the chestplate did not come off");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        stand.interact(player, InteractionHand.MAIN_HAND, POST);
        wears(helper, player, "player (empty stand)", Items.IRON_HELMET, Items.AIR, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        wears(helper, stand, "small stand (empty)", Items.AIR, Items.DIAMOND_CHESTPLATE, Items.AIR, Items.AIR);
        helper.assertTrue(stand.getPickResult().is(ModItems.SMALL_ARMOR_STAND), "pick block gives the wrong item");
        helper.succeed();
    }

    /** Zwei schnelle Schlaege (echter Weg) bauen den kleinen Staender ab: er selbst und sein Item fallen, kein Vanilla-Staender. */
    public static void theSmallStandDropsItselfAndItsItem(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper);
        int x = 1;
        for (Item item : new Item[]{Items.IRON_BOOTS, Items.GOLDEN_HORSE_ARMOR, Items.WOLF_ARMOR}) {
            SmallArmorStand stand = stand(helper, ModEntities.SMALL_ARMOR_STAND, x, 3);
            x += 2;
            helper.assertTrue(stand.put(new ItemStack(item)), "put " + item);
            player.snapTo(stand.getX() - 1.5, stand.getY(), stand.getZ());
            player.lookAt(EntityAnchorArgument.Anchor.EYES, stand.position());
            player.attack(stand);
            player.attack(stand);
            helper.assertTrue(stand.isRemoved(), "two fast hits do not break the small stand");
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, stand.getBoundingBox().inflate(1.5));
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.SMALL_ARMOR_STAND)), "the small stand did not drop itself");
            helper.assertFalse(drops.stream().anyMatch(e -> e.getItem().is(Items.ARMOR_STAND)), "the small stand dropped a vanilla stand");
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(item)), "the small stand lost its " + item);
            drops.forEach(Entity::discard);
        }
        helper.succeed();
    }

    /**
     * Spender (echter Weg, Redstone): ein Helm, eine Pferde- und eine Wolfs-Ruestung landen auf je einem leeren kleinen
     * Staender; ein voller Staender nimmt nichts (der Spender wirft das Item wie Vanilla aus), ein Schwert auch nicht.
     */
    public static void aDispenserPutsArmorOnTheSmallStand(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        Item[] items = {Items.IRON_HELMET, Items.DIAMOND_HORSE_ARMOR, Items.WOLF_ARMOR, Items.GOLDEN_BOOTS, Items.IRON_SWORD};
        SmallArmorStand[] stands = new SmallArmorStand[items.length];
        net.minecraft.world.level.block.entity.DispenserBlockEntity[] dispensers = new net.minecraft.world.level.block.entity.DispenserBlockEntity[items.length];
        for (int i = 0; i < items.length; i++) {
            BlockPos dispenser = new BlockPos(0, 1, i);
            helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.EAST));
            stands[i] = stand(helper, ModEntities.SMALL_ARMOR_STAND, 1, i);
            stands[i].setNoGravity(true);
            dispensers[i] = (net.minecraft.world.level.block.entity.DispenserBlockEntity) level.getBlockEntity(helper.absolutePos(dispenser));
            dispensers[i].setItem(0, new ItemStack(items[i]));
        }
        stands[3].put(new ItemStack(Items.IRON_LEGGINGS));
        for (int i = 0; i < items.length; i++) {
            helper.setBlock(new BlockPos(0, 2, i), Blocks.REDSTONE_BLOCK);
        }
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(stands[i].shown().is(items[i]) && dispensers[i].getItem(0).isEmpty(),
                        "the dispenser did not put " + items[i] + " on the small stand: " + stands[i].shown());
            }
            helper.assertTrue(stands[3].shown().is(Items.IRON_LEGGINGS) && stands[3].getItemBySlot(EquipmentSlot.FEET).isEmpty(),
                    "a full stand took boots from a dispenser");
            helper.assertTrue(stands[4].shownSlot() == null && stands[4].getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
                    "the small stand took a sword from a dispenser");
            helper.succeed();
        });
    }

    /**
     * Alte Welten: ein gespeicherter mittlerer Staender (Hose + Stiefel) laedt als kleiner Staender, behaelt die Hose und
     * laesst die Stiefel fallen; ein altes Item {@code medium_armor_stand} wird ein kleiner Staender.
     */
    public static void anOldMediumStandTurnsIntoTheSmallStand(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        net.minecraft.resources.Identifier old = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "medium_armor_stand");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(old) == ModItems.SMALL_ARMOR_STAND,
                "an old medium stand item does not load as a small stand");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(old) == ModEntities.SMALL_ARMOR_STAND,
                "the old medium stand entity id does not load as a small stand");
        SmallArmorStand template = stand(helper, ModEntities.SMALL_ARMOR_STAND, 2, 2);
        template.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        template.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        template.saveAsPassenger(output);
        template.discard();
        net.minecraft.nbt.CompoundTag tag = output.buildResult();
        tag.putString("id", old.toString());
        Entity loaded = EntityType.loadEntityRecursive(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag), level, EntitySpawnReason.LOAD,
                net.minecraft.world.entity.EntityProcessor.NOP);
        helper.assertTrue(loaded instanceof SmallArmorStand && loaded.getType() == ModEntities.SMALL_ARMOR_STAND,
                "the old medium stand loaded as " + (loaded == null ? "nothing" : loaded.getType().toShortString()));
        SmallArmorStand stand = (SmallArmorStand) loaded;
        level.addFreshEntity(stand);
        stand.tick();
        wears(helper, stand, "converted stand", Items.AIR, Items.AIR, Items.DIAMOND_LEGGINGS, Items.AIR);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, stand.getBoundingBox().inflate(1.5));
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND_BOOTS)), "the converted stand lost its boots");
        drops.forEach(Entity::discard);
        helper.succeed();
    }

    /** Die Testzentrale fuehrt den kleinen Staender. */
    public static void theTestCentreStocksTheSmallStand(GameTestHelper helper) {
        if (!McVersion.TRAINING_DUMMY) {
            helper.succeed();
            return;
        }
        var plan = com.simplebuilding.dev.testcentre.TestCentreLayout.plan(helper.getLevel().registryAccess(), new BlockPos(0, 64, 0));
        helper.assertTrue(plan.coveredItems().contains(ModItems.SMALL_ARMOR_STAND), "the small armor stand has no place in the test centre");
        helper.succeed();
    }
}
