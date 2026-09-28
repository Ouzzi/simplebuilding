package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.PlacedTemplates;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests der abgelegten Schmiedevorlagen (Besitzer 2026-09-28, {@link PlacedTemplates}):
 * Ablegen mit Schleichen + Rechtsklick auf Boden, Wand und Decke (ohne Schleichen nicht), Wasser
 * zerstoert sie nicht, sie fallen mit allen Komponenten als sie selbst heraus, die Besatz-Aufwertung
 * braucht an der abgelegten Vorlage drei Schlaege, und die Hinweis-Funken kommen nur fuer Spieler mit
 * Leuchttinte oder Glowstonestaub in der Hand.
 */
public final class PlacedTemplateTests {

    /** Zeit fuer das Wasser (fliesst alle 5 Ticks einen Block weiter) und die Drops. */
    public static final int WATER_MAX_TICKS = 60;

    private PlacedTemplateTests() {
    }

    // =====================================================================================
    // Ablegen
    // =====================================================================================

    /**
     * Schleichen + Rechtsklick legt jede Art Vorlage ab - Netherit-Aufwertung, Besatzvorlage, die
     * Enderit-Aufwertung der Mod (ein schlichtes Item) und die leuchtende Vorlage: flach auf den
     * Boden (Oberkante in Blickrichtung), an die Wand (Schauseite zur geklickten Seite) und unter die
     * Decke. Die Block-Entity haelt denselben Stapel samt Namen.
     */
    public static void sneakUsePlacesTemplatesOnTheFloorAgainstTheWallAndUnderTheCeiling(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setShiftKeyDown(true);

        // Boden
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        ItemStack named = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Anvil note"));
        InteractionResult floor = use(helper, player, named, new BlockPos(1, 1, 1), Direction.UP);
        helper.assertTrue(floor.consumesAction(), "sneak + use on the floor did not place the template: " + floor);
        BlockState floorState = helper.getBlockState(new BlockPos(1, 2, 1));
        helper.assertTrue(floorState.is(ModBlocks.PLACED_SMITHING_TEMPLATE), "no placed template on the floor but " + floorState);
        helper.assertTrue(floorState.getValue(PlacedTemplateBlock.FACE) == AttachFace.FLOOR
                        && floorState.getValue(PlacedTemplateBlock.FACING) == player.getDirection(),
                "the floor template lies " + floorState + " instead of flat with its top edge towards " + player.getDirection());
        ItemStack stored = template(helper, new BlockPos(1, 2, 1));
        helper.assertTrue(ItemStack.isSameItemSameComponents(stored, named) && stored.getCount() == 1,
                "the floor template holds " + stored + " instead of the named netherite upgrade");

        // Wand: die Nordseite eines Steins, die Vorlage zeigt nach Norden
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        use(helper, player, new ItemStack(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(3, 2, 2), Direction.NORTH);
        BlockState wall = helper.getBlockState(new BlockPos(3, 2, 1));
        helper.assertTrue(wall.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && wall.getValue(PlacedTemplateBlock.FACE) == AttachFace.WALL
                        && wall.getValue(PlacedTemplateBlock.FACING) == Direction.NORTH,
                "the wall template is " + wall + " instead of hanging on the wall facing north");
        helper.assertTrue(template(helper, new BlockPos(3, 2, 1)).is(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE),
                "the wall template lost its item");

        // Decke
        helper.setBlock(new BlockPos(5, 4, 1), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE), new BlockPos(5, 4, 1), Direction.DOWN);
        BlockState ceiling = helper.getBlockState(new BlockPos(5, 3, 1));
        helper.assertTrue(ceiling.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && ceiling.getValue(PlacedTemplateBlock.FACE) == AttachFace.CEILING,
                "the enderite upgrade template under the ceiling is " + ceiling);

        // Die leuchtende Vorlage der Mod geht auch.
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.GLOWING_TRIM_TEMPLATE), new BlockPos(1, 1, 4), Direction.UP);
        helper.assertTrue(template(helper, new BlockPos(1, 2, 4)).is(ModItems.GLOWING_TRIM_TEMPLATE),
                "the glowing trim template was not placed");
        TestCleanup.succeed(helper);
    }

    /**
     * Ohne Schleichen bleibt eine Vorlage ein gewoehnliches Item: der Rechtsklick legt nichts ab und
     * gibt PASS zurueck. Andere Items legen auch mit Schleichen nichts ab.
     */
    public static void withoutSneakingTheTemplateKeepsItsNormalBehaviour(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(false);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        ItemStack template = new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        InteractionResult plain = use(helper, player, template, new BlockPos(1, 1, 1), Direction.UP);
        helper.assertTrue(plain == InteractionResult.PASS, "a right click without sneaking returned " + plain + " instead of PASS");
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 1)).isAir(), "a right click without sneaking placed "
                + helper.getBlockState(new BlockPos(1, 2, 1)));
        helper.assertTrue(template.getCount() == 1, "the template stack changed to " + template);

        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        InteractionResult paper = use(helper, player, new ItemStack(Items.PAPER), new BlockPos(3, 1, 1), Direction.UP);
        helper.assertTrue(paper == InteractionResult.PASS && helper.getBlockState(new BlockPos(3, 2, 1)).isAir(),
                "sneaking with paper placed something: " + paper + ", " + helper.getBlockState(new BlockPos(3, 2, 1)));
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Wasser und Drops
    // =====================================================================================

    /**
     * Fliessendes Wasser laeuft um eine abgelegte Vorlage herum statt sie wegzuspuelen, in eine
     * Wasserquelle abgelegt wird sie wassergefuellt. Abbauen und ein weggenommener Halt geben genau
     * den gespeicherten Stapel zurueck, samt Namen.
     */
    public static void placedTemplatesSurviveWaterAndDropThemselvesWithTheirData(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        player.setShiftKeyDown(true);
        // Becken aus Stein: innen nur die Vorlage (1,2,1) und die Wasserquelle daneben (2,2,1) - das
        // Wasser kann nirgends hin als in die Vorlage.
        basin(helper, 0, 0, 3, 2);
        use(helper, player, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(1, 1, 1), Direction.UP);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.WATER);

        // In eine Wasserquelle abgelegt (eigenes Becken): wassergefuellt.
        basin(helper, 4, 0, 6, 2);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.WATER);
        use(helper, player, new ItemStack(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(5, 1, 1), Direction.UP);
        BlockState soaked = helper.getBlockState(new BlockPos(5, 2, 1));
        helper.assertTrue(soaked.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && soaked.getValue(PlacedTemplateBlock.WATERLOGGED),
                "a template placed into a water source is " + soaked + " instead of waterlogged");

        // Abbauen: derselbe Stapel mit Namen.
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
        ItemStack named = new ItemStack(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
        use(helper, player, named, new BlockPos(1, 1, 4), Direction.UP);
        helper.assertTrue(template(helper, new BlockPos(1, 2, 4)).is(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE), "the named template was not placed");
        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(1, 2, 4)), true);
        ItemEntity broken = dropped(helper, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        helper.assertTrue(broken != null && ItemStack.isSameItemSameComponents(broken.getItem(), named) && broken.getItem().getCount() == 1,
                "breaking the placed template dropped " + (broken == null ? "nothing (items: " + items(helper) + ")" : broken.getItem())
                        + " instead of the named template");
        // Halt weg: die Vorlage an der Wand faellt mit heraus.
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.STONE);
        use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(4, 2, 5), Direction.NORTH);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 4)).is(ModBlocks.PLACED_SMITHING_TEMPLATE), "wall template missing");
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 4)).isAir(), "the wall template stayed without its wall");
        ItemEntity fallen = dropped(helper, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        helper.assertTrue(fallen != null, "the wall template dropped nothing when its wall went away (items: " + items(helper) + ")");

        helper.runAfterDelay(30, () -> {
            BlockState state = helper.getBlockState(new BlockPos(1, 2, 1));
            helper.assertTrue(state.is(ModBlocks.PLACED_SMITHING_TEMPLATE), "flowing water washed the template away: " + state);
            helper.assertTrue(template(helper, new BlockPos(1, 2, 1)).is(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
                    "the template under flowing water lost its item");
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 1)).is(Blocks.WATER),
                    "the water next to the template is gone, the test proves nothing");
            helper.assertTrue(!state.getValue(PlacedTemplateBlock.WATERLOGGED),
                    "flowing water filled the template like a source would");
            succeed(helper);
        });
    }

    // =====================================================================================
    // Aufwertung mit dem Vorschlaghammer
    // =====================================================================================

    /**
     * Die Besatz-Aufwertung an der abgelegten Vorlage braucht {@link PlacedTemplates#PLACED_HITS}
     * Schlaege: der echte Linksklick-Weg ({@code handleBlockBreakAction}), der Ueberlebens-Haken
     * ({@code attack}) und der Kreativ-Haken ({@code canDestroyBlock}) zaehlen je einen Schlag, erst
     * der dritte wertet auf, der Block bricht dabei nie. Ein Wechsel des Materials faengt von vorn
     * an. Eine Aufwertungsvorlage (keine Besatzvorlage) zaehlt keinen Schlag, und die Hammer-Neigung
     * gilt nur fuer aufwertbare Vorlagen.
     */
    public static void placedTrimTemplatesNeedThreeHammerHits(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.STONE);
        use(helper, player, new ItemStack(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(1, 1, 1), Direction.UP);
        use(helper, player, new ItemStack(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(3, 1, 1), Direction.UP);
        use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(5, 1, 1), Direction.UP);
        player.setShiftKeyDown(false);

        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = helper.absolutePos(new BlockPos(3, 2, 1));
        BlockPos upgrade = helper.absolutePos(new BlockPos(5, 2, 1));
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOWSTONE_DUST, 8));

        helper.assertTrue(PlacedTemplates.isHammerTarget(level, first, player), "a placed trim template is no hammer target");
        helper.assertTrue(!PlacedTemplates.isHammerTarget(level, upgrade, player), "a placed upgrade template counts as hammer target");
        BlockState state = level.getBlockState(first);
        helper.assertTrue(state.getDestroyProgress(player, level, first) == 0.0F,
                "hammer + glowstone still break the placed template (progress " + state.getDestroyProgress(player, level, first) + ")");

        // Der echte Linksklick, dreimal.
        for (int hit = 1; hit <= PlacedTemplates.PLACED_HITS; hit++) {
            player.gameMode.handleBlockBreakAction(first, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP,
                    level.getMaxY(), hit);
            BlockState now = level.getBlockState(first);
            helper.assertTrue(now.is(ModBlocks.PLACED_SMITHING_TEMPLATE), "hit " + hit + " broke the placed template: " + now);
            Item expected = hit < PlacedTemplates.PLACED_HITS ? Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE : ModItems.EMITTING_TRIM_TEMPLATE;
            ItemStack held = helper.getBlockEntity(new BlockPos(1, 2, 1), PlacedTemplateBlockEntity.class).getTemplate();
            helper.assertTrue(held.is(expected), "after hit " + hit + " the template is " + held + " instead of " + expected);
        }

        // Beide Haken einzeln, mit Materialwechsel dazwischen.
        PlacedTemplateBlockEntity be = helper.getBlockEntity(new BlockPos(3, 2, 1), PlacedTemplateBlockEntity.class);
        level.getBlockState(second).attack(level, second, player);
        helper.assertTrue(be.hits() == 1, "the survival hook counted " + be.hits() + " hits instead of 1");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC, 8));
        level.getBlockState(second).attack(level, second, player);
        helper.assertTrue(be.hits() == 1, "switching to glow ink did not restart the count: " + be.hits());
        boolean destroys = hammer.getItem().canDestroyBlock(hammer, level.getBlockState(second), level, second, player);
        helper.assertTrue(!destroys, "the sledgehammer may destroy a placed template while holding glow ink");
        helper.assertTrue(be.hits() == (player.getAbilities().instabuild ? 2 : 1), "the creative hook counted " + be.hits());
        for (int i = 0; i < PlacedTemplates.PLACED_HITS && be.getTemplate().is(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE); i++) {
            level.getBlockState(second).attack(level, second, player);
        }
        helper.assertTrue(be.getTemplate().is(ModItems.GLOWING_TRIM_TEMPLATE), "glow ink made " + be.getTemplate());

        // Aufwertungsvorlage: kein Schlag, nichts aendert sich.
        level.getBlockState(upgrade).attack(level, upgrade, player);
        PlacedTemplateBlockEntity upgradeBe = helper.getBlockEntity(new BlockPos(5, 2, 1), PlacedTemplateBlockEntity.class);
        helper.assertTrue(upgradeBe.hits() == 0 && upgradeBe.getTemplate().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                "a hammer hit counted on an upgrade template: " + upgradeBe.hits() + ", " + upgradeBe.getTemplate());

        // Ohne Material in der Nebenhand ist es ein gewoehnlicher Block.
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(!PlacedTemplates.isHammerStance(player) && level.getBlockState(upgrade).getDestroyProgress(player, level, upgrade) > 0.0F,
                "without a catalyst the placed template cannot be mined");
        succeed(helper);
    }

    // =====================================================================================
    // Hinweis
    // =====================================================================================

    /**
     * Eine aufwertbare abgelegte Vorlage zeigt ihre Funken nur einem Spieler in der Naehe, der
     * Glowstonestaub oder Leuchttinte in einer Hand haelt - nicht mit leeren Haenden, nicht aus der
     * Ferne, und eine Aufwertungsvorlage nie.
     */
    public static void hintSparksOnlyShowNearPlayersHoldingGlowstoneOrGlowInk(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        use(helper, player, new ItemStack(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(2, 1, 2), Direction.UP);
        use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(4, 1, 2), Direction.UP);
        player.setShiftKeyDown(false);
        ServerLevel level = helper.getLevel();
        BlockPos trim = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos upgrade = helper.absolutePos(new BlockPos(4, 2, 2));
        PlacedTemplateBlockEntity be = helper.getBlockEntity(new BlockPos(2, 2, 2), PlacedTemplateBlockEntity.class);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) != player, "empty hands got a hint");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE_DUST));
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) == player, "glowstone dust in the main hand got no hint");
        helper.assertTrue(PlacedTemplates.tryHint(level, trim, be) == player, "tryHint did not fire for the glowstone holder");
        helper.assertTrue(PlacedTemplates.hintViewer(level, upgrade) == null, "an upgrade template gave a hint");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC));
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) == player, "glow ink in the off hand got no hint");

        // Aus der Ferne nicht.
        Vec3 far = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5 + PlacedTemplates.HINT_RANGE + 3.0));
        player.snapTo(far.x, far.y, far.z, 0.0F, 0.0F);
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) != player, "a player " + (PlacedTemplates.HINT_RANGE + 3.0)
                + " blocks away got a hint");

        // Der Ton ist gedrosselt.
        long now = level.getGameTime();
        helper.assertTrue(be.takeHintSound(now + 1000, PlacedTemplates.HINT_SOUND_INTERVAL)
                        && !be.takeHintSound(now + 1001, PlacedTemplates.HINT_SOUND_INTERVAL)
                        && be.takeHintSound(now + 1000 + PlacedTemplates.HINT_SOUND_INTERVAL, PlacedTemplates.HINT_SOUND_INTERVAL),
                "the hint sound is not rate limited to once per " + PlacedTemplates.HINT_SOUND_INTERVAL + " ticks");
        succeed(helper);
    }

    // =====================================================================================

    /** Rechtsklick mit {@code stack} auf die Seite {@code face} des Blocks {@code on} (relativ). */
    private static InteractionResult use(GameTestHelper helper, Player player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    /** Steinbecken: Boden auf y 1 von (x0,z0) bis (x1,z1), Rand auf y 2, innen frei. */
    private static void basin(GameTestHelper helper, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
                helper.setBlock(new BlockPos(x, 2, z), rim ? Blocks.STONE : Blocks.AIR);
            }
        }
    }

    private static ItemStack template(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, PlacedTemplateBlockEntity.class).getTemplate();
    }

    /** Das erste Item dieser Art, das in der Teststruktur liegt, oder null. */
    private static ItemEntity dropped(GameTestHelper helper, Item item) {
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (entity.getItem().is(item)) {
                return entity;
            }
        }
        return null;
    }

    private static List<String> items(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
                .map(entity -> entity.getItem() + "@" + entity.blockPosition().toShortString()).toList();
    }

    private static void succeed(GameTestHelper helper) {
        TestCleanup.succeed(helper);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
