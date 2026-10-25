package com.simplegadgets.test;

import com.simplegadgets.GadgetComponents;
import com.simplegadgets.GadgetEnchantments;
import com.simplegadgets.GadgetProtection;
import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.config.GadgetsConfig;
import com.simplegadgets.network.LaserPayload;
import com.simplegadgets.network.RodNetwork;
import com.simplegadgets.rod.LensMeasurement;
import com.simplegadgets.rod.ResonanceRodItem;
import com.simplegadgets.rod.RodBeam;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Resonance Rod (OW24-2 G2): the beam and its effects, charge and recharge, measuring, the dot relay, protection - ported
 * from SimpleBuilding's {@code TweaksTests}, {@code CombatTests}, {@code ServerTuningTests} and {@code ModpackTests}. The Fabric adapter
 * registers them through {@link GadgetsTests#ALL} as module_game_test_rod_<name>. Standalone and with SimpleBuilding (Constructor's
 * Touch is SimpleBuilding's enchantment: the measuring test only goes on with it).
 */
public final class RodTests {
    private RodTests() {
    }

    /**
     * #17: Das Laser-Relay ersetzt die UUID im Paket durch die des Absenders, schickt nichts ohne
     * Laserpointer in Benutzung oder mit abgeschaltetem Laser, nur an Spieler in der Naehe und
     * hoechstens {@link RodNetwork#LASER_PACKETS_PER_SECOND} Pakete je Sekunde.
     */
    public static void theLaserRelayChecksTheSenderAndOnlyReachesNearbyPlayers(GameTestHelper helper) {
        ServerPlayer sender = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ServerPlayer near = mockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        ServerPlayer far = mockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        far.snapTo(far.getX() + 600, far.getY(), far.getZ());
        Vec3 dotPos = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        UUID spoofed = UUID.randomUUID();
        LaserPayload payload = new LaserPayload(spoofed, (float) dotPos.x, (float) dotPos.y, (float) dotPos.z, true);
        List<ServerPlayer> receivers = new ArrayList<>();
        List<LaserPayload> relayed = new ArrayList<>();
        RodNetwork.PlayerSender capture = (player, sent) -> {
            receivers.add(player);
            relayed.add((LaserPayload) sent);
        };

        helper.assertValueEqual(RodNetwork.relayLaser(payload, sender, capture), 0, "players reached without a laser pointer in use");
        sender.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GadgetsRegistry.AMETHYST_LENS));
        sender.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(sender.isUsingItem(), "the probe player does not use the laser pointer");

        boolean enabled = GadgetsConfig.rodEnabled;
        try {
            GadgetsConfig.rodEnabled = false;
            helper.assertValueEqual(RodNetwork.relayLaser(payload, sender, capture), 0, "players reached with the laser switched off on the server");
        } finally {
            GadgetsConfig.rodEnabled = enabled;
        }
        helper.assertTrue(relayed.isEmpty(), "something was relayed before the checks passed");

        RodNetwork.relayLaser(payload, sender, capture);
        helper.assertTrue(receivers.contains(near), "the nearby player did not get the laser dot");
        helper.assertFalse(receivers.contains(far), "a player 600 blocks away got the laser dot");
        helper.assertFalse(receivers.contains(sender), "the sender got its own dot back");
        helper.assertValueEqual(relayed.get(0).player(), sender.getUUID(), "player id in the relayed packet");

        Vec3 outOfRange = sender.getEyePosition().add(0, 0, GadgetsConfig.rodRange + 50);
        relayed.clear();
        RodNetwork.relayLaser(new LaserPayload(sender.getUUID(), (float) outOfRange.x, (float) outOfRange.y, (float) outOfRange.z, true), sender, capture);
        helper.assertTrue(relayed.isEmpty(), "a dot beyond the laser range was relayed");

        int accepted = 0;
        for (int i = 0; i < 40; i++) {
            if (RodNetwork.relayLaser(payload, sender, capture) > 0) {
                accepted++;
            }
        }
        helper.assertTrue(accepted <= RodNetwork.LASER_PACKETS_PER_SECOND, accepted + " laser packets relayed within one tick");
        sender.stopUsingItem();
        helper.succeed();
    }

    /**
     * Amethystlinse (Id amethyst_lens, frueher laser_pointer): Redstone/Amethyst/Redstone, Eisen/Eisen-Baukern/Eisen,
     * drei Eisen - kein Glas mehr. Die Haltbarkeit ist die Ladung.
     */
    public static void theAmethystLensIsCraftedAroundAnIronCore(GameTestHelper helper) {
        // With SimpleBuilding: its Iron Core and Iron Rod; alone: iron ingots take their place (recipe ..._without_simplebuilding).
        Item core0 = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(SimpleGadgets.OLD_NAMESPACE, "iron_core"));
        Item pole0 = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(SimpleGadgets.OLD_NAMESPACE, "iron_pole"));
        boolean sb = core0 != Items.AIR && pole0 != Items.AIR;
        Item core = sb ? core0 : Items.IRON_INGOT;
        Item pole = sb ? pole0 : Items.IRON_INGOT;
        Item i = Items.IRON_INGOT;
        Item r = Items.REDSTONE;
        Item nug = Items.IRON_NUGGET;
        String recipeId = sb ? "simplegadgets:amethyst_lens" : "simplegadgets:amethyst_lens_without_simplebuilding";
        CraftingInput grid = grid(null, nug, Items.AMETHYST_SHARD, r, core, null, pole, null, null);
        expectCrafting(helper, grid, GadgetsRegistry.AMETHYST_LENS, recipeId);
        helper.assertTrue(craftingResult(helper, grid(null, r, Items.AMETHYST_SHARD, nug, core, r, pole, nug, null)).isEmpty(), "the previous rod recipe still works");
        helper.assertTrue(craftingResult(helper, grid(null, nug, Items.AMETHYST_SHARD, r, core, nug, pole, r, null)).isEmpty(), "the rod accepts the removed redstone/nugget pair");
        helper.assertTrue(craftingResult(helper, grid(null, null, Items.AMETHYST_SHARD, r, core, null, pole, null, null)).isEmpty(), "the rod crafts without its nugget");
        helper.assertTrue(craftingResult(helper, grid(null, nug, Items.AMETHYST_SHARD, null, core, null, pole, null, null)).isEmpty(), "the rod crafts without its redstone");
        helper.assertTrue(craftingResult(helper, grid(i, i, r, i, core, Items.AMETHYST_SHARD, i, i, r)).isEmpty(), "the 26.2 ingot recipe still works on 26.3");
        helper.assertTrue(craftingResult(helper, grid(r, Items.AMETHYST_SHARD, r, i, core, i, i, i, i)).isEmpty(), "the unrotated rod recipe still works");
        Optional<ItemStack> oldPattern = craftingResult(helper, grid(null, Items.AMETHYST_SHARD, null, i, Items.GLASS, i, i, r, i));
        helper.assertTrue(oldPattern.isEmpty() || !oldPattern.get().is(GadgetsRegistry.AMETHYST_LENS), "the old glass pattern still makes the lens");
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        helper.assertTrue(lens.isDamageableItem() && lens.getMaxDamage() == ResonanceRodItem.MAX_CHARGE,
                "the lens has no charge of " + ResonanceRodItem.MAX_CHARGE + " (max damage " + lens.getMaxDamage() + ")");
        helper.assertTrue(ResonanceRodItem.CHARGE_PER_SHARD * 16 == ResonanceRodItem.MAX_CHARGE, "16 amethyst shards are not exactly one full charge");
        helper.succeed();
    }

    /** Eis wird Wasser, Packeis Eis, Blaueis Packeis; Schneeschicht und Schneeblock schmelzen weg - erst nach der Verweildauer. */
    public static void theLensBeamMeltsIceAndSnow(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        BlockPos target = new BlockPos(2, 2, 2);
        helper.setBlock(target.below(), Blocks.STONE);
        Block[][] steps = {
                {Blocks.ICE, Blocks.WATER}, {Blocks.PACKED_ICE, Blocks.ICE}, {Blocks.BLUE_ICE, Blocks.PACKED_ICE},
                {Blocks.SNOW_BLOCK, Blocks.AIR}};
        for (Block[] step : steps) {
            helper.setBlock(target, step[0]);
            beam(helper, player, lens, target, Direction.UP, RodBeam.MELT_TICKS - 1);
            helper.assertTrue(helper.getBlockState(target).is(step[0]), step[0] + " melted before the dwell time");
            beam(helper, player, lens, target, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(target).is(step[1]), step[0] + " became " + helper.getBlockState(target) + " instead of " + step[1]);
            helper.setBlock(target, Blocks.AIR);
        }
        helper.setBlock(target, Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 3));
        beam(helper, player, lens, target, Direction.UP, RodBeam.MELT_TICKS);
        helper.assertTrue(helper.getBlockState(target).isAir(), "a snow layer did not melt away");
        helper.succeed();
    }

    /** Brennbares (Bretter oben, Stamm seitlich) faengt erst nach der Verweildauer Feuer, auf der angestrahlten Seite; Stein nie. */
    public static void theLensBeamIgnitesFlammableBlocksOnlyAfterDwelling(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        try {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, helper.getLevel().getServer());
            BlockPos planks = new BlockPos(1, 2, 2);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            beam(helper, player, lens, planks, Direction.UP, RodBeam.IGNITE_TICKS - 1);
            helper.assertTrue(helper.getBlockState(planks.above()).isAir(), "the planks caught fire before the dwell time");
            beam(helper, player, lens, planks, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(planks.above()).is(Blocks.FIRE), "the planks did not catch fire on top after the dwell time");
            helper.setBlock(planks.above(), Blocks.AIR);

            BlockPos wool = new BlockPos(3, 2, 2);
            helper.setBlock(wool, Blocks.OAK_LOG);
            beam(helper, player, lens, wool, Direction.SOUTH, RodBeam.IGNITE_TICKS);
            helper.assertTrue(helper.getBlockState(wool.south()).is(Blocks.FIRE), "the log did not catch fire on the beamed side");
            helper.setBlock(wool.south(), Blocks.AIR);

            BlockPos stone = new BlockPos(2, 2, 4);
            helper.setBlock(stone, Blocks.STONE);
            beam(helper, player, lens, stone, Direction.UP, RodBeam.IGNITE_TICKS * 3);
            helper.assertTrue(helper.getBlockState(stone.above()).isAir(), "stone caught fire from the beam");
        } finally {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /** Seelensand bekommt oben Seelenfeuer; Lagerfeuer, Seelenlagerfeuer und Kerzen gehen an. */
    public static void theLensBeamLightsSoulFireCampfiresAndCandles(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        BlockPos soul = new BlockPos(1, 2, 1);
        helper.setBlock(soul, Blocks.SOUL_SAND);
        beam(helper, player, lens, soul, Direction.UP, RodBeam.SOUL_FIRE_TICKS - 1);
        helper.assertTrue(helper.getBlockState(soul.above()).isAir(), "soul fire appeared before the dwell time");
        beam(helper, player, lens, soul, Direction.UP, 1);
        helper.assertTrue(helper.getBlockState(soul.above()).is(Blocks.SOUL_FIRE), "the soul sand got " + helper.getBlockState(soul.above()) + " instead of soul fire");
        helper.setBlock(soul.above(), Blocks.AIR);

        BlockPos floor = new BlockPos(3, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        BlockState[] unlit = {
                Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false),
                Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false),
                Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, false)};
        for (BlockState state : unlit) {
            helper.setBlock(floor.above(), state);
            beam(helper, player, lens, floor.above(), Direction.UP, RodBeam.LIGHT_TICKS);
            helper.assertTrue(helper.getBlockState(floor.above()).getValue(BlockStateProperties.LIT), state.getBlock() + " was not lit by the beam");
        }
        helper.setBlock(floor.above(), Blocks.AIR);
        helper.succeed();
    }

    /**
     * Anders als ein Feuerzeug fuellt der Strahl nie einen leeren Portalrahmen (Bretter hinter dem
     * Rahmen, Feuerplatz im Rahmen) - auch nicht, seit er TNT zuenden darf.
     */
    public static void theLensBeamNeverLightsNetherPortals(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(5.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        try {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, helper.getLevel().getServer());
            // Rahmen in der X-Ebene z = 3: innen x 1..2, y 2..4.
            int z = 3;
            for (int x = 1; x <= 2; x++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.OBSIDIAN);
                helper.setBlock(new BlockPos(x, 5, z), Blocks.OBSIDIAN);
            }
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(0, y, z), Blocks.OBSIDIAN);
                helper.setBlock(new BlockPos(3, y, z), Blocks.OBSIDIAN);
            }
            BlockPos planks = new BlockPos(1, 2, z - 1);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            BlockPos inside = new BlockPos(1, 2, z);
            helper.assertTrue(net.minecraft.world.level.portal.PortalShape.findEmptyPortalShape(helper.getLevel(), helper.absolutePos(inside), Direction.Axis.X).isPresent(),
                    "the test frame is no valid portal frame, so this test proves nothing");
            beam(helper, player, lens, planks, Direction.SOUTH, RodBeam.IGNITE_TICKS * 2);
            helper.assertTrue(helper.getBlockState(inside).isAir(), "the beam put " + helper.getBlockState(inside) + " into an empty portal frame");
        } finally {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /**
     * TNT (Besitzer 2026-09-27): nach derselben Verweildauer wie beim Anzuenden wird es gezuendet -
     * der Block wird zu gezuendetem TNT mit dem Spieler als Zuender, die Ladung sinkt um die
     * Wirkungskosten. Nicht im Abenteuermodus, nicht mit der Spielregel tnt_explodes aus (dann kostet
     * es auch nichts).
     */
    public static void theLensBeamPrimesTntAfterDwellingButRespectsTheRules(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(5.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        var rules = helper.getLevel().getGameRules();
        boolean tntExplodes = rules.get(GameRules.TNT_EXPLODES);
        try {
            rules.set(GameRules.TNT_EXPLODES, true, helper.getLevel().getServer());
            BlockPos tnt = new BlockPos(3, 2, 2);
            helper.setBlock(tnt.below(), Blocks.STONE);
            helper.setBlock(tnt, Blocks.TNT);
            beam(helper, player, lens, tnt, Direction.UP, RodBeam.IGNITE_TICKS - 1);
            helper.assertTrue(helper.getBlockState(tnt).is(Blocks.TNT), "the TNT was primed before the dwell time");
            beam(helper, player, lens, tnt, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(tnt).isAir(), "the beam did not prime the TNT after the dwell time: " + helper.getBlockState(tnt));
            List<PrimedTnt> primed = helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(tnt)).inflate(1.0));
            helper.assertValueEqual(primed.size(), 1, "primed TNT entities after the beam");
            PrimedTnt fuse = primed.get(0);
            helper.assertTrue(fuse.getOwner() == player, "the primed TNT does not name the beaming player as its igniter: " + fuse.getOwner());
            fuse.discard();
            helper.assertValueEqual(lens.getDamageValue(), ResonanceRodItem.EFFECT_COST, "charge spent on priming the TNT");

            helper.setBlock(tnt, Blocks.TNT);
            player.getAbilities().mayBuild = false;
            beam(helper, player, lens, tnt, Direction.UP, RodBeam.IGNITE_TICKS * 2);
            player.getAbilities().mayBuild = true;
            helper.assertTrue(helper.getBlockState(tnt).is(Blocks.TNT), "the beam primed TNT for a player who may not build");

            rules.set(GameRules.TNT_EXPLODES, false, helper.getLevel().getServer());
            beam(helper, player, lens, tnt, Direction.UP, RodBeam.IGNITE_TICKS * 2);
            helper.assertTrue(helper.getBlockState(tnt).is(Blocks.TNT), "the beam primed TNT with tnt_explodes switched off");
            helper.assertValueEqual(lens.getDamageValue(), ResonanceRodItem.EFFECT_COST, "charge spent while TNT was not allowed to explode");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(tnt)).inflate(1.0)).isEmpty(),
                    "primed TNT appeared although the block stayed");
            helper.setBlock(tnt, Blocks.AIR);
        } finally {
            rules.set(GameRules.TNT_EXPLODES, tntExplodes, helper.getLevel().getServer());
            for (PrimedTnt left : helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(new BlockPos(3, 2, 2))).inflate(2.0))) {
                left.discard();
            }
        }
        helper.succeed();
    }

    /**
     * Jede Nutzung kostet Ladung (Besitzer 2026-09-27): schon der erste Tick zieht
     * {@link ResonanceRodItem#BEAM_COST} ab - frueher erst nach 20 Ticks, kurzes Antippen war gratis -,
     * danach je angefangene Sekunde, auch wenn der Strahl ins Leere zeigt oder auf einem Block ohne
     * Wirkung ruht. Die Reichweite wird dafuer auf {@value #SHORT_LASER_RANGE} Bloecke gesenkt, damit
     * "ins Leere" nicht von dem abhaengt, was ueber der Teststruktur steht.
     */
    public static void theLensDrainsChargeEvenWhenItPointsIntoTheAir(GameTestHelper helper) {
        int range = GadgetsConfig.rodRange;
        try {
            GadgetsConfig.rodRange = SHORT_LASER_RANGE;
            lensDrainsChargeEvenWhenItPointsIntoTheAir(helper);
        } finally {
            GadgetsConfig.rodRange = range;
        }
    }

    /**
     * Messen mit der Amethystlinse (Besitzer 2026-09-28): ohne Verzauberung schreibt die Linse nie
     * eine Messung; mit Berührung des Konstrukteurs steht die letzte Messung
     * ({@link LensMeasurement}: Entfernung Auge -> Trefferpunkt auf
     * 0,1 gerundet, Hoehenunterschied, Zielblock) schon nach dem ersten Tick in der Linse und wird
     * beim Loslassen auf das neue Ziel aktualisiert.
     */
    public static void theLensRecordsTheLastMeasurementOnlyWithConstructorsTouch(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        faceNorth(player);
        BlockPos stone = new BlockPos(2, 3, 3);
        BlockPos nearer = new BlockPos(2, 3, 4);
        helper.setBlock(stone, Blocks.STONE);
        helper.assertTrue(player.pick(8.0, 1.0f, false) instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(helper.absolutePos(stone)),
                "the player does not look at the stone block");
        double stoneDistance = player.pick(8.0, 1.0f, false).getLocation().distanceTo(player.getEyePosition());
        var component = GadgetComponents.LENS_MEASUREMENT;

        ItemStack plain = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        helper.assertTrue(plain.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the plain lens could not be used");
        tickUse(player, 21);
        player.releaseUsingItem();
        helper.assertFalse(ResonanceRodItem.measures(plain, helper.getLevel()), "a plain lens counts as measuring");
        helper.assertTrue(!plain.has(component), "a lens without Constructor's Touch stored a measurement: " + plain.get(component));

        // Constructor's Touch is SimpleBuilding's enchantment: without it nothing measures, which the plain rod above showed.
        var touch = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(GadgetEnchantments.CONSTRUCTORS_TOUCH);
        if (touch.isEmpty()) {
            helper.succeed();
            return;
        }
        ItemStack touched = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        touched.enchant(touch.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, touched);
        helper.assertTrue(ResonanceRodItem.measures(touched, helper.getLevel()), "a lens with Constructor's Touch does not count as measuring");
        helper.assertTrue(touched.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the enchanted lens could not be used");
        tickUse(player, 1);
        LensMeasurement first = touched.get(component);
        helper.assertTrue(first != null, "the enchanted lens stored no measurement after the first tick");
        helper.assertTrue(Math.abs(first.distance() - LensMeasurement.round(stoneDistance)) < 0.001f,
                "measured " + first.distance() + " blocks to the stone instead of " + stoneDistance);
        helper.assertValueEqual(first.heightDifference(), 1, "height difference to the stone one block above the feet");
        helper.assertValueEqual(first.target(), Blocks.STONE.getDescriptionId(), "measured target");

        // Neues Ziel einen Block naeher: das Loslassen misst noch einmal.
        helper.setBlock(nearer, Blocks.COBBLESTONE);
        player.releaseUsingItem();
        LensMeasurement last = touched.get(component);
        helper.assertTrue(last != null && last.target().equals(Blocks.COBBLESTONE.getDescriptionId()),
                "releasing the lens did not record the new target: " + last);
        helper.assertTrue(Math.abs(last.distance() - LensMeasurement.round(stoneDistance - 1.0)) < 0.001f,
                "measured " + last.distance() + " blocks to the nearer block instead of " + (stoneDistance - 1.0));
        helper.setBlock(nearer, Blocks.AIR);
        helper.setBlock(stone, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Verweildauer und Abstand (Besitzer 2026-09-27): bis 5 Bloecke die Basis, bei 10 Bloecken etwa
     * eine Sekunde mehr, bei 200 Bloecken rund 20 s (Anzuenden, Basis 3 s), stetig wachsend. In der
     * Welt schmilzt Eis aus 40 und aus 200 Bloecken erst nach der verlaengerten Verweildauer - auch
     * jenseits der alten Grenze von 24 Bloecken.
     */
    public static void theLensDwellTimeGrowsModeratelyWithDistance(GameTestHelper helper) {
        int base = RodBeam.IGNITE_TICKS;
        helper.assertValueEqual(RodBeam.dwellTicks(base, 1.0), base, "ignite dwell ticks right in front");
        helper.assertValueEqual(RodBeam.dwellTicks(base, 5.0), base, "ignite dwell ticks at 5 blocks");
        int ten = RodBeam.dwellTicks(base, 10.0);
        helper.assertTrue(ten >= base + 14 && ten <= base + 26, "10 blocks should add about one second to the 3 s ignite dwell, got " + ten + " ticks");
        int far = RodBeam.dwellTicks(base, 200.0);
        helper.assertTrue(far >= 380 && far <= 420, "200 blocks should take about 20 s to ignite, got " + far + " ticks");
        int last = base;
        for (int d = 0; d <= 512; d += 4) {
            int ticks = RodBeam.dwellTicks(base, d);
            helper.assertTrue(ticks >= last, "the dwell time shrinks between " + (d - 4) + " and " + d + " blocks");
            last = ticks;
        }

        BlockPos ice = new BlockPos(2, 2, 2);
        helper.setBlock(ice.below(), Blocks.STONE);
        for (double away : new double[]{40.0, 200.0}) {
            helper.setBlock(ice, Blocks.ICE);
            ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5 + away));
            ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
            int needed = RodBeam.dwellTicks(RodBeam.MELT_TICKS, player.getEyePosition().distanceTo(beamPoint(helper, ice, Direction.UP)));
            helper.assertTrue(needed > RodBeam.MELT_TICKS, "no longer dwell at " + away + " blocks: " + needed);
            beam(helper, player, lens, ice, Direction.UP, needed - 1);
            helper.assertTrue(helper.getBlockState(ice).is(Blocks.ICE), "the ice melted before the " + needed + " tick dwell at " + away + " blocks");
            beam(helper, player, lens, ice, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(ice).is(Blocks.WATER), "the ice did not melt after the " + needed + " tick dwell at " + away + " blocks");
        }
        helper.setBlock(ice, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Lebewesen fangen Feuer (Besitzer 2026-09-27), brauchen dafuer aber doppelt so lange wie ein
     * brennbarer Block im selben Abstand; das kostet die Wirkungsladung. Im Wasser nicht.
     */
    public static void theLensSetsLivingEntitiesOnFireTakingTwiceAsLong(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        Mob pig = helper.spawn(PIG_TYPE, new BlockPos(2, 2, 2));
        pig.setNoAi(true);
        try {
            EntityHitResult hit = new EntityHitResult(pig, pig.position().add(0.0, 0.4, 0.0));
            int block = RodBeam.dwellTicks(RodBeam.IGNITE_TICKS, player.getEyePosition().distanceTo(hit.getLocation()));
            beamEntity(player, lens, hit, block - 1);
            helper.assertFalse(pig.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "the pig was scanned before the dwell time of a block");
            beamEntity(player, lens, hit, 1);
            helper.assertFalse(pig.getRemainingFireTicks() > 0, "the pig caught fire after the dwell time of a block, not twice that");
            // 2026-10-02: nach der einfachen Verweildauer wird es gescannt (leuchtet), das kostet eine Wirkungsladung.
            helper.assertTrue(pig.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "the pig does not glow after the dwell time of a block");
            helper.assertValueEqual(lens.getDamageValue(), ResonanceRodItem.EFFECT_COST, "charge spent on scanning the pig");
            beamEntity(player, lens, hit, block - 1);
            helper.assertFalse(pig.getRemainingFireTicks() > 0, "the pig caught fire a tick before twice the block dwell time");
            beamEntity(player, lens, hit, 1);
            helper.assertTrue(pig.getRemainingFireTicks() > 0, "the pig did not catch fire after twice the block dwell time (" + (2 * block) + " ticks)");
            helper.assertValueEqual(lens.getDamageValue(), 2 * ResonanceRodItem.EFFECT_COST, "charge spent on scanning and setting the pig on fire");

            pig.clearFire();
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
            beamEntity(player, lens, hit, 4 * block);
            helper.assertFalse(pig.getRemainingFireTicks() > 0, "a pig standing in water caught fire");
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        } finally {
            pig.discard();
        }
        helper.succeed();
    }

    /**
     * Spieler nur mit PvP (Besitzer 2026-09-27): mit der Spielregel pvp an brennt ein anderer
     * Spieler nach der doppelten Verweildauer, mit pvp aus nie; ein Spieler im Kreativmodus
     * (unverwundbar) nie.
     */
    public static void theLensOnlyIgnitesPlayersWhenPvpAllowsIt(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ServerPlayer target = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        target.getAbilities().invulnerable = false;
        target.getAbilities().instabuild = false;
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        var rules = helper.getLevel().getGameRules();
        boolean pvp = rules.get(GameRules.PVP);
        try {
            EntityHitResult hit = new EntityHitResult(target, target.position().add(0.0, 1.0, 0.0));
            int needed = RodBeam.ENTITY_DWELL_FACTOR * RodBeam.dwellTicks(RodBeam.IGNITE_TICKS, player.getEyePosition().distanceTo(hit.getLocation()));
            rules.set(GameRules.PVP, true, helper.getLevel().getServer());
            beamEntity(player, lens, hit, needed);
            helper.assertTrue(target.getRemainingFireTicks() > 0, "with pvp on the other player did not catch fire, so the checks below prove nothing");
            target.clearFire();

            rules.set(GameRules.PVP, false, helper.getLevel().getServer());
            beamEntity(player, lens, hit, needed * 2);
            helper.assertFalse(target.getRemainingFireTicks() > 0, "the beam set a player on fire with pvp switched off");

            rules.set(GameRules.PVP, true, helper.getLevel().getServer());
            target.getAbilities().invulnerable = true;
            beamEntity(player, lens, hit, needed * 2);
            helper.assertFalse(target.getRemainingFireTicks() > 0, "the beam set an invulnerable (creative) player on fire");
        } finally {
            rules.set(GameRules.PVP, pvp, helper.getLevel().getServer());
            target.clearFire();
        }
        helper.succeed();
    }

    /**
     * Klaenge am Trefferpunkt (Besitzer 2026-09-27): ein leises Summen, sobald der Strahl irgendetwas
     * trifft, hoechstens einmal je {@link RodBeam#HUM_PERIOD} Ticks, nicht ins Leere; waehrend Eis
     * schmilzt ein Zischen, waehrend Brennbares heiss wird ein Knistern, je hoechstens einmal je
     * {@link RodBeam#HEAT_SOUND_PERIOD} Ticks. Gezaehlt ueber den Test-Haken der Linse.
     */
    public static void theLensHumsOnAnySurfaceAndSizzlesOrCracklesWhileHeating(GameTestHelper helper) {
        java.util.Map<RodBeam.Sound, Integer> heard = new java.util.EnumMap<>(RodBeam.Sound.class);
        int range = GadgetsConfig.rodRange;
        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        RodBeam.setSoundHook((sound, at) -> heard.merge(sound, 1, Integer::sum));
        try {
            GadgetsConfig.rodRange = SHORT_LASER_RANGE;
            ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
            ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
            player.setItemInHand(InteractionHand.MAIN_HAND, lens);
            BlockPos stone = new BlockPos(2, 3, 3);
            helper.setBlock(stone, Blocks.STONE);
            faceNorth(player);
            helper.assertTrue(player.pick(SHORT_LASER_RANGE, 1.0f, false) instanceof BlockHitResult hit
                            && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(helper.absolutePos(stone)),
                    "the player does not look at the stone block");
            helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction() && player.isUsingItem(), "the lens could not be used");
            tickUse(player, 1);
            helper.assertValueEqual(lens.getDamageValue(), ResonanceRodItem.BEAM_COST, "charge after the first tick (did the use tick run?)");
            helper.assertValueEqual(heard.getOrDefault(RodBeam.Sound.HUM, 0), 1, "hums after the first tick on stone");
            tickUse(player, RodBeam.HUM_PERIOD - 1);
            helper.assertValueEqual(heard.getOrDefault(RodBeam.Sound.HUM, 0), 1, "hums within the first hum period (rate limit)");
            tickUse(player, 1);
            helper.assertValueEqual(heard.getOrDefault(RodBeam.Sound.HUM, 0), 2, "hums at the start of the second hum period");
            helper.assertTrue(!heard.containsKey(RodBeam.Sound.SIZZLE) && !heard.containsKey(RodBeam.Sound.CRACKLE),
                    "stone, which the beam cannot change, sizzled or crackled: " + heard);
            player.releaseUsingItem();
            player.setXRot(-90.0f);
            helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the lens could not be used again");
            tickUse(player, RodBeam.HUM_PERIOD * 2);
            player.releaseUsingItem();
            helper.assertValueEqual(heard.getOrDefault(RodBeam.Sound.HUM, 0), 2, "hums while pointing into the air");
            helper.setBlock(stone, Blocks.AIR);

            heard.clear();
            BlockPos ice = new BlockPos(1, 2, 2);
            helper.setBlock(ice, Blocks.ICE);
            int meltTicks = RodBeam.MELT_TICKS - 1;
            beam(helper, player, lens, ice, Direction.UP, meltTicks);
            helper.assertValueEqual(heard.getOrDefault(RodBeam.Sound.SIZZLE, 0), (meltTicks - 1) / RodBeam.HEAT_SOUND_PERIOD + 1, "sizzles while the ice heats up");
            helper.assertFalse(heard.containsKey(RodBeam.Sound.CRACKLE), "melting ice crackled like fire");
            helper.setBlock(ice, Blocks.AIR);

            heard.clear();
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, helper.getLevel().getServer());
            BlockPos planks = new BlockPos(3, 2, 2);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            int igniteTicks = RodBeam.IGNITE_TICKS - 1;
            beam(helper, player, lens, planks, Direction.UP, igniteTicks);
            helper.assertValueEqual(heard.getOrDefault(RodBeam.Sound.CRACKLE, 0), (igniteTicks - 1) / RodBeam.HEAT_SOUND_PERIOD + 1, "crackles while the planks heat up");
            helper.assertFalse(heard.containsKey(RodBeam.Sound.SIZZLE), "heating planks sizzled like ice");
            helper.setBlock(planks, Blocks.AIR);
        } finally {
            RodBeam.setSoundHook(null);
            GadgetsConfig.rodRange = range;
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /** Die Zusatzwirkung: ein nasser Schwamm trocknet nach laengerem Strahlen. */
    public static void theLensBeamDriesWetSponges(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        BlockPos sponge = new BlockPos(2, 2, 2);
        helper.setBlock(sponge, Blocks.WET_SPONGE);
        beam(helper, player, lens, sponge, Direction.NORTH, RodBeam.DRY_TICKS - 1);
        helper.assertTrue(helper.getBlockState(sponge).is(Blocks.WET_SPONGE), "the sponge dried before the dwell time");
        beam(helper, player, lens, sponge, Direction.NORTH, 1);
        helper.assertTrue(helper.getBlockState(sponge).is(Blocks.SPONGE), "the wet sponge did not dry");
        helper.succeed();
    }

    /** Im Abenteuermodus wirkt der Strahl nicht; ohne Feuerausbreitung (Spielregel 0) zuendet er kein Brennbares. */
    public static void theLensBeamRespectsAdventureModeAndTheFireSpreadRule(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        BlockPos ice = new BlockPos(1, 2, 2);
        helper.setBlock(ice, Blocks.ICE);
        player.getAbilities().mayBuild = false;
        beam(helper, player, lens, ice, Direction.UP, RodBeam.MELT_TICKS * 2);
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.ICE), "the beam melted ice for a player who may not build");
        player.getAbilities().mayBuild = true;

        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        try {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, helper.getLevel().getServer());
            BlockPos planks = new BlockPos(3, 2, 2);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            beam(helper, player, lens, planks, Direction.UP, RodBeam.IGNITE_TICKS * 2);
            helper.assertTrue(helper.getBlockState(planks.above()).isAir(), "the beam lit planks with fire spread switched off");
        } finally {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /**
     * Die Ladung sinkt (Strahlen je Sekunde, jede Wirkung mehr), aber die Linse zerbricht nie: leer
     * bleibt sie im Inventar, laesst sich nicht mehr benutzen und wirkt nicht mehr. Kreativ kostet nichts.
     */
    public static void theLensChargeRunsDownButTheLensNeverBreaks(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ResonanceRodItem item = (ResonanceRodItem) GadgetsRegistry.AMETHYST_LENS;
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        item.onUseTick(helper.getLevel(), player, lens, item.getUseDuration(lens, player) - 20);
        helper.assertTrue(lens.getDamageValue() == ResonanceRodItem.BEAM_COST, "a second of beaming cost " + lens.getDamageValue() + " charge");

        lens.setDamageValue(ResonanceRodItem.MAX_CHARGE - 2);
        BlockPos ice = new BlockPos(2, 2, 2);
        helper.setBlock(ice, Blocks.ICE);
        beam(helper, player, lens, ice, Direction.UP, RodBeam.MELT_TICKS);
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.WATER), "the last bit of charge did not melt the ice");
        helper.assertTrue(!lens.isEmpty() && lens.is(GadgetsRegistry.AMETHYST_LENS) && lens.getDamageValue() == ResonanceRodItem.MAX_CHARGE,
                "the lens broke or overshot instead of ending empty: " + lens + " damage " + lens.getDamageValue());
        helper.assertTrue(ResonanceRodItem.isEmpty(lens), "a fully used lens does not count as empty");

        helper.setBlock(ice, Blocks.ICE);
        beam(helper, player, lens, ice, Direction.UP, RodBeam.MELT_TICKS * 2);
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.ICE), "an empty lens still melted ice");
        player.setItemInHand(InteractionHand.MAIN_HAND, lens);
        InteractionResult result = lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(player.isUsingItem(), "an empty lens could still be used (" + result + ")");
        ResonanceRodItem.drain(player, lens, 50);
        helper.assertTrue(lens.getDamageValue() == ResonanceRodItem.MAX_CHARGE && !lens.isEmpty(), "draining an empty lens broke it");

        ServerPlayer creative = mockPlayer(helper, new Vec3(4.5, 2.0, 5.5));
        ItemStack free = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        ResonanceRodItem.drain(creative, free, 50);
        helper.assertTrue(free.getDamageValue() == 0, "beaming cost charge in creative");
        helper.setBlock(ice, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Aufladen im Amboss mit dem Material des Stabs, Amethystscherben (Besitzer 2026-09-29, vorher
     * Redstone): 0 Stufen, auch bei Stufe 0 abholbar, 16 Scherben = voll, nur der noetige Teil eines
     * Stapels wird verbraucht. Redstone laedt nicht mehr.
     */
    public static void anvilRechargeWithAmethystShardsCostsNoLevels(GameTestHelper helper) {
        ItemStack rod = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        helper.assertTrue(new ItemStack(Items.AMETHYST_SHARD).is(ResonanceRodItem.REPAIRS),
                "amethyst shards are missing from the resonance rod repair tag");
        helper.assertTrue(rod.isValidRepairItem(new ItemStack(Items.AMETHYST_SHARD)), "rod repair component rejects amethyst");
        helper.assertTrue(!rod.isValidRepairItem(new ItemStack(Items.REDSTONE)), "rod repair component still accepts redstone");
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        player.experienceLevel = 0;

        ItemStack empty = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        empty.setDamageValue(ResonanceRodItem.MAX_CHARGE);
        net.minecraft.world.inventory.AnvilMenu full = recharge(helper, player, empty, Items.AMETHYST_SHARD, 16);
        ItemStack out = full.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(out.is(GadgetsRegistry.AMETHYST_LENS) && out.getDamageValue() == 0, "16 amethyst shards did not fully charge an empty rod: " + out + " damage " + out.getDamageValue());
        helper.assertTrue(full.getCost() == 0, "recharging costs " + full.getCost() + " levels");
        takeResult(helper, player, full);
        helper.assertTrue(full.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "a full recharge left shards behind");
        helper.assertTrue(player.experienceLevel == 0, "recharging changed the player's level");

        ItemStack partial = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        partial.setDamageValue(25);
        net.minecraft.world.inventory.AnvilMenu topUp = recharge(helper, player, partial, Items.AMETHYST_SHARD, 64);
        helper.assertTrue(topUp.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem().getDamageValue() == 0, "a top-up did not fill the rod");
        helper.assertTrue(topUp.getCost() == 0, "a top-up costs " + topUp.getCost() + " levels");
        takeResult(helper, player, topUp);
        int left = topUp.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).getItem().getCount();
        helper.assertTrue(left == 63, "topping up 25 charge used " + (64 - left) + " shards instead of 1");

        ItemStack drained = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        drained.setDamageValue(ResonanceRodItem.MAX_CHARGE);
        net.minecraft.world.inventory.AnvilMenu some = recharge(helper, player, drained, Items.AMETHYST_SHARD, 10);
        int damage = some.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem().getDamageValue();
        helper.assertTrue(damage == ResonanceRodItem.MAX_CHARGE - 10 * ResonanceRodItem.CHARGE_PER_SHARD, "10 shards charged to damage " + damage);

        ItemStack redstoneTry = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        redstoneTry.setDamageValue(ResonanceRodItem.MAX_CHARGE);
        net.minecraft.world.inventory.AnvilMenu withRedstone = recharge(helper, player, redstoneTry, Items.REDSTONE, 64);
        ItemStack redstoneOut = withRedstone.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(redstoneOut.isEmpty() || redstoneOut.getDamageValue() == ResonanceRodItem.MAX_CHARGE,
                "redstone still recharges the rod: " + redstoneOut + " damage " + redstoneOut.getDamageValue());

        ItemStack alreadyFull = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        net.minecraft.world.inventory.AnvilMenu none = recharge(helper, player, alreadyFull, Items.AMETHYST_SHARD, 5);
        helper.assertTrue(none.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem().isEmpty(), "the anvil offers to recharge a full rod");
        helper.succeed();
    }

    /**
     * Der Stab leert sich zuegig (Besitzer 2026-09-29): 4 Ladung je angefangener Sekunde Strahl
     * (Standard der Config {@code chargePerSecond}), eine volle Ladung reicht fuer 160 s statt fast
     * 11 min. Gefahren ueber den echten Benutzungs-Tick: nach 20 Ticks Zeigen ins Leere 4, nach 40
     * Ticks 8, und die letzte Sekunde Ladung leert den Stab.
     */
    public static void theRodDrainsFourChargePerSecondOfBeaming(GameTestHelper helper) {
        helper.assertTrue(ResonanceRodItem.BEAM_COST == 4, "BEAM_COST is " + ResonanceRodItem.BEAM_COST + ", not 4");
        helper.assertTrue(GadgetsConfig.ROD_BEAM_COST_DEFAULT == 4,
                "the default config charge per second is not 4");
        helper.assertTrue(ResonanceRodItem.beamCost() == GadgetsConfig.rodChargePerSecond,
                "beamCost() does not read rodChargePerSecond");
        helper.assertTrue(ResonanceRodItem.MAX_CHARGE / ResonanceRodItem.BEAM_COST <= 160,
                "a full rod lasts " + (ResonanceRodItem.MAX_CHARGE / ResonanceRodItem.BEAM_COST) + " s of beaming, more than 160 s");

        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        player.setXRot(-90.0f); // straight up into the air: no block effect, only the beam cost
        ItemStack rod = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        int duration = rod.getItem().getUseDuration(rod, player);
        int cost = ResonanceRodItem.beamCost();
        for (int used = 0; used < 40; used++) {
            rod.getItem().onUseTick(helper.getLevel(), player, rod, duration - used);
            if (used == 19) {
                helper.assertValueEqual(rod.getDamageValue(), cost, "charge used after the first second of beaming");
            }
        }
        helper.assertValueEqual(rod.getDamageValue(), 2 * cost, "charge used after two seconds of beaming");
        RodBeam.reset(player);

        ItemStack almost = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        almost.setDamageValue(ResonanceRodItem.MAX_CHARGE - cost);
        player.setItemInHand(InteractionHand.MAIN_HAND, almost);
        almost.getItem().onUseTick(helper.getLevel(), player, almost, duration);
        helper.assertTrue(ResonanceRodItem.isEmpty(almost), "the last second of charge did not empty the rod");
        RodBeam.reset(player);
        helper.succeed();
    }

    /**
     * Scannen (2026-10-02): ein feuerfester Mob leuchtet nach der einfachen Verweildauer (brennt aber nicht), ein
     * anderer Spieler ohne {@code scanPlayers} nie; mit Schalter aus leuchtet nichts.
     */
    public static void theResonanceRodScansCreaturesButNotPlayers(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Vec3 eye = helper.absoluteVec(new Vec3(2.5, 2.0, 6.5));
        player.snapTo(eye.x, eye.y, eye.z, 180.0F, 0.0F);
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        Mob blaze = helper.spawnWithNoFreeWill(EntityTypes.BLAZE, new BlockPos(2, 2, 2));
        boolean scan = GadgetsConfig.rodScanEntities;
        int scanInterval = GadgetsConfig.rodScanIntervalTicks;
        try {
            GadgetsConfig.rodScanIntervalTicks = 40;
            EntityHitResult hit = new EntityHitResult(blaze, blaze.position().add(0.0, 0.8, 0.0));
            int scanAt = RodBeam.dwellTicks(RodBeam.IGNITE_TICKS, player.getEyePosition().distanceTo(hit.getLocation()));
            for (int i = 0; i < scanAt - 1; i++) {
                RodBeam.beamAtEntity(player, lens, hit);
            }
            helper.assertFalse(blaze.hasEffect(MobEffects.GLOWING), "the blaze glows before the scan dwell time");
            RodBeam.beamAtEntity(player, lens, hit);
            helper.assertTrue(blaze.hasEffect(MobEffects.GLOWING), "the blaze does not glow after the scan dwell time");
            helper.assertValueEqual(blaze.getEffect(MobEffects.GLOWING).getDuration(), GadgetsConfig.rodScanIntervalTicks + 10,
                    "configured scan glow duration");
            helper.assertFalse(blaze.getRemainingFireTicks() > 0, "a fire-immune blaze caught fire");

            ServerPlayer other = shooter(helper);
            other.snapTo(blaze.getX() + 1.0, blaze.getY(), blaze.getZ(), 0.0F, 0.0F);
            helper.assertFalse(RodBeam.canScan(player, other), "another player is scanned with rodScanPlayers off");

            GadgetsConfig.rodScanEntities = false;
            helper.assertFalse(RodBeam.canScan(player, blaze), "creatures are scanned with rodScanEntities off");
        } finally {
            GadgetsConfig.rodScanEntities = scan;
            GadgetsConfig.rodScanIntervalTicks = scanInterval;
            blaze.discard();
        }
        helper.succeed();
    }

    /**
     * The laser switch for creatures: off, the beam may not set a living target on fire, on it may.
     */
    public static void theLaserSwitchesStopWhatTheBeamIgnites(GameTestHelper helper) {
        boolean entities = GadgetsConfig.rodIgniteEntities;
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        LivingEntity pig = spawnPig(helper, new BlockPos(4, 1, 4));
        try {
            GadgetsConfig.rodIgniteEntities = true;
            helper.assertTrue(RodBeam.canIgnite(player, pig), "the beam may not ignite a dry pig with the switch on");
            GadgetsConfig.rodIgniteEntities = false;
            helper.assertTrue(!RodBeam.canIgnite(player, pig), "the beam may ignite a pig with the creature switch off");
        } finally {
            GadgetsConfig.rodIgniteEntities = entities;
            pig.discard();
            removePlayer(helper, player);
        }
        helper.succeed();
    }

    /**
     * The amethyst lens beam melts ice only where the loader's break event lets the player change
     * the block; the refusal costs nothing and the same beam melts the ice once the claim is gone.
     *
     * <p>What breaks it: dropping {@code claimAllows} from {@code RodBeam#beamAt}.
     */
    public static void lensBeamLeavesBlocksTheLoaderEventsProtect(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        BlockPos target = new BlockPos(2, 2, 2);
        helper.setBlock(target.below(), Blocks.STONE);
        helper.setBlock(target, Blocks.ICE);
        BlockPos refused = helper.absolutePos(target);
        GadgetProtection.BuildGuard open = GadgetProtection.buildGuard();
        GadgetProtection.setBuildGuard(new GadgetProtection.BuildGuard() {
            @Override
            public boolean mayBreak(ServerLevel level, ServerPlayer p, BlockPos pos, BlockState state) {
                return !pos.equals(refused);
            }

            @Override
            public boolean mayPlace(ServerLevel level, ServerPlayer p, BlockPos pos, BlockState state) {
                return !pos.equals(refused);
            }
        });
        Runnable release = () -> GadgetProtection.setBuildGuard(open);
        try {
            beam(helper, player, lens, target, RodBeam.MELT_TICKS);
            helper.assertTrue(helper.getBlockState(target).is(Blocks.ICE),
                    "the beam melted ice the loader's break event protects: " + helper.getBlockState(target));
            helper.assertTrue(lens.getDamageValue() == 0, "a refused beam still used up charge");
        } finally {
            release.run();
        }
        beam(helper, player, lens, target, RodBeam.MELT_TICKS);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.WATER),
                "without the claim the beam no longer melts the ice (positive control): " + helper.getBlockState(target));
        helper.setBlock(target, Blocks.AIR);
        helper.succeed();
    }

    private static final int SHORT_LASER_RANGE = 2;

    private static net.minecraft.world.inventory.AnvilMenu recharge(GameTestHelper helper, ServerPlayer player, ItemStack lens, Item material, int count) {
        net.minecraft.world.inventory.AnvilMenu menu = new net.minecraft.world.inventory.AnvilMenu(1, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
        menu.getSlot(net.minecraft.world.inventory.AnvilMenu.INPUT_SLOT).set(lens);
        menu.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(material, count));
        return menu;
    }

    private static void takeResult(GameTestHelper helper, ServerPlayer player, net.minecraft.world.inventory.AnvilMenu menu) {
        Slot result = menu.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT);
        helper.assertTrue(result.mayPickup(player), "a level-0 survival player may not take the free recharge");
        result.onTake(player, result.getItem());
    }

    /** Like the 6-argument beam on the top face. */
    private static void beam(GameTestHelper helper, ServerPlayer player, ItemStack lens, BlockPos relative, int ticks) {
        beam(helper, player, lens, relative, Direction.UP, ticks);
    }

    /** Laesst den Strahl {@code ticks} Mal auf die Mitte der Seite {@code face} von {@code relative} fallen. */
    private static void beam(GameTestHelper helper, ServerPlayer player, ItemStack lens, BlockPos relative, Direction face, int ticks) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(beamPoint(helper, relative, face), face, pos, false);
        for (int i = 0; i < ticks; i++) {
            RodBeam.beamAt(player, lens, hit);
        }
    }

    /** Blick waagerecht nach Norden (-Z); Lebewesen blicken ueber den Kopf, darum auch yHeadRot. */
    private static void faceNorth(ServerPlayer player) {
        player.setXRot(0.0f);
        player.setYRot(180.0f);
        player.setYHeadRot(180.0f);
    }

    /** Wo der Strahl eine Blockseite trifft (Mitte der Seite, absolut). */
    private static Vec3 beamPoint(GameTestHelper helper, BlockPos relative, Direction face) {
        return Vec3.atCenterOf(helper.absolutePos(relative)).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
    }

    private static void beamEntity(ServerPlayer player, ItemStack lens, EntityHitResult hit, int ticks) {
        for (int i = 0; i < ticks; i++) {
            RodBeam.beamAtEntity(player, lens, hit);
        }
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * {@code ticks} Benutz-Ticks wie Vanillas LivingEntity#updatingUsingItem: onUseTick, Zaehler
     * herunter, bei 0 completeUsingItem. Per Reflexion, weil der Mock-Spieler hier nicht getickt wird.
     */
    private static void tickUse(ServerPlayer player, int ticks) {
        try {
            java.lang.reflect.Method update = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("updateUsingItem", ItemStack.class);
            update.setAccessible(true);
            for (int i = 0; i < ticks && player.isUsingItem(); i++) {
                update.invoke(player, player.getUseItem());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("LivingEntity#updateUsingItem not reachable", e);
        }
    }

    private static CraftingInput grid(Item... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) {
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static Optional<ItemStack> craftingResult(GameTestHelper helper, CraftingInput grid) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(holder -> holder.value().assemble(grid));
    }

    private static void expectCrafting(GameTestHelper helper, CraftingInput grid, Item result, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the documented pattern for " + result + " matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched for " + result);
        helper.assertTrue(match.get().value().assemble(grid).is(result), "the recipe for " + result + " made something else");
    }

    private static ServerPlayer shooter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static LivingEntity spawnPig(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(EntityTypes.PIG, pos);
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static final net.minecraft.world.entity.EntityType<? extends Mob> PIG_TYPE = EntityTypes.PIG;

    private static void lensDrainsChargeEvenWhenItPointsIntoTheAir(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        player.setXRot(-90.0f);
        ItemStack lens = new ItemStack(GadgetsRegistry.AMETHYST_LENS);
        player.setItemInHand(InteractionHand.MAIN_HAND, lens);
        helper.assertTrue(player.pick(SHORT_LASER_RANGE, 1.0f, false).getType() == HitResult.Type.MISS,
                "something blocks the view straight up within " + SHORT_LASER_RANGE + " blocks, so the beam does not point into the air");
        helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction() && player.isUsingItem(), "the lens could not be used");
        tickUse(player, 1);
        helper.assertValueEqual(lens.getDamageValue(), ResonanceRodItem.BEAM_COST, "charge after one tick of pointing into the air");
        tickUse(player, 19);
        helper.assertValueEqual(lens.getDamageValue(), ResonanceRodItem.BEAM_COST, "charge after the first second of pointing into the air");
        tickUse(player, 1);
        helper.assertValueEqual(lens.getDamageValue(), 2 * ResonanceRodItem.BEAM_COST, "charge at the start of the second second");
        player.releaseUsingItem();

        // Kurzes Antippen: jede neue Nutzung kostet sofort.
        helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the lens could not be used again");
        tickUse(player, 3);
        player.releaseUsingItem();
        helper.assertValueEqual(lens.getDamageValue(), 3 * ResonanceRodItem.BEAM_COST, "charge after a three tick tap");

        // Auf einem Block ohne Wirkung (Stein) kostet es genauso.
        BlockPos stone = new BlockPos(2, 3, 3);
        helper.setBlock(stone, Blocks.STONE);
        faceNorth(player);
        helper.assertTrue(player.pick(SHORT_LASER_RANGE, 1.0f, false) instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(helper.absolutePos(stone)),
                "the player does not look at the stone block");
        helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the lens could not be used a third time");
        tickUse(player, 1);
        player.releaseUsingItem();
        helper.assertValueEqual(lens.getDamageValue(), 4 * ResonanceRodItem.BEAM_COST, "charge after pointing at a stone block");
        helper.setBlock(stone, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Ein Spieler, der wie im Ueberlebensmodus baut (instabuild aus). Der In-Level-Mock bleibt fuer
     * isCreative() kreativ, darum fragt der Flypad-Code instabuild; eine Verbindung hat er, anders als
     * ein losgeloester Mock, sodass onUpdateAbilities nicht ins Leere sendet.
     */
    private static ServerPlayer survivalLikePlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = mockPlayer(helper, relative);
        player.getAbilities().instabuild = false;
        return player;
    }
}
