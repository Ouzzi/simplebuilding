package com.simplebuilding.version;

import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Minecraft 26.2 side of the version shim (server-safe part).
 *
 * <p>The shared tree common/src/shared/java is compiled against 26.2 AND 26.3. Where the two
 * Minecraft versions differ in a small API detail, shared code calls this class instead; there is
 * exactly one copy per Minecraft line: this one in common/src/mc26_2/java (compiled only by the
 * 26.2 modules) and its twin in mc26_3/overlay/java (compiled only by the 26.3 modules). Both must
 * keep the same public signatures - checkOverlays in the root build.gradle verifies that every
 * overlay class has a counterpart on the other line.
 */
public final class McVersion {
    /** Safe hub test-world spawn and explicit dev-only warning acknowledgment (26.3 first). */
    public static final boolean HUB_TEST_WORLD = false;
    /** Vanilla 26.3 guide examples: straw beds and the Dappled Forest wood set. */
    public static final boolean VANILLA_26_3_CONTENT = false;
    /** Redstone variants of the three chest tiers (26.3 first). */
    public static final boolean TRAPPED_TIERED_CHESTS = false;
    public static final boolean SILENT_DANDELION = false;
    /** Falling anvils crush diamond blocks (26.3 first). */
    public static final boolean ANVIL_DIAMOND_CRUSH = false;
    public static final boolean END_SYSTEMS = false;
    /** Overworld wave 2026-10-01: Sage Ore and the Sage Orb (main line first, ported later). */
    public static final boolean SAGE_ORE = false;
    /** Dimensional Scrap (2026-10-01): mysterious endgame block in every dimension, no use before v2. */
    public static final boolean DIMENSIONAL_SCRAP = false;
    /** Fletching table and crafted arrows (2026-10-01, main line first). */
    public static final boolean FLETCHING = false;
    /** Vanilla recipe book in the smithing table instead of the trim resonance button (2026-10-02). */
    public static final boolean SMITHING_RECIPE_BOOK = false;
    /** Auto Smither: the crafter of the smithing table (2026-10-02). */
    public static final boolean AUTO_SMITHER = false;
    /** Small items (pebbles, sticks, ingots, gems, bricks) placeable on blocks (2026-10-02). */
    public static final boolean SMALL_PLACEABLES = false;
    /** Bundles, backpacks and quivers dye like vanilla bundles: one fixed colour per dye, no mixing, no washing (2026-10-02). */
    public static final boolean VANILLA_DYEING = false;
    /** Iron Rod and the reworked gadget recipes (clock in the gauge, recovery compass in the detector, iron rods) (2026-10-02). */
    public static final boolean GADGET_REWORK = false;
    public static final boolean EXPENSIVE_TEMPLATES = false;
    /** Rare structure finds: better loot chests and tiered end city shulkers with their shells (2026-10-02). */
    public static final boolean RARE_STRUCTURE_FINDS = false;

    /** Trank des listigen Shulkers: Effekt, Traenke, Brau-Rezepte (2026-10-02, 26.3 braut datengetrieben). */
    public static final boolean CRAFTY_SHULKER = false;

    /**
     * Chorusfrucht-Teleport an eine vorgepruefte Stelle (LivingEntity#randomTeleport). 26.3 prueft zusaetzlich
     * die Bloecke in {@code avoid}; 26.2 kennt diese Variante nicht.
     */
    public static boolean randomTeleport(LivingEntity entity, double x, double y, double z, boolean particles,
            TagKey<net.minecraft.world.level.block.Block> avoid) {
        return entity.randomTeleport(x, y, z, particles);
    }

    /** Straw Armor Stand and Training Dummy, plus the archery station of the test centre (2026-10-02). */
    public static final boolean TRAINING_DUMMY = false;
    /** Hammock (2026-10-02): needs vanilla's AbstractBedBlock (26.3); resting by day speeds the clock up. */
    public static final boolean HAMMOCK = false;
    /** Standing rods (2026-10-04): main line 26.3 only until the port run. */
    public static final boolean STANDING_RODS = false;
    /** Dimension music discs with B-sides (sledgehammer flip) and the Astralit/Nihilit speakers (2026-10-03). */
    public static final boolean MUSIC_DISCS = false;
    /** Astral rail (boosts towards a raised top speed) and Nihil rail (brakes to a stop), fed by their End channel (2026-10-04). */
    public static final boolean END_RAILS = false;
    /** Crucible SB parts: 26.3 only (SimpleLib is not built for 26.2). */
    public static final boolean CRUCIBLE = false;
    /** Main-line transformations; older renderers/gameplay are ported after owner approval. */
    public static final boolean TRANSFORM_HINTS_AND_CORNERS = false;
    /** Building cores move in the hand when used (weighted pulse/spin/rise/boomerang, longer ore animation; Nachtrag 11). */
    public static final boolean CORE_MOTIONS = false;

    public static boolean canVanillaTransform(net.minecraft.world.level.Level level,
            net.minecraft.world.phys.BlockHitResult hit, Player player, InteractionHand hand) {
        return false;
    }

    public static void setSignTextFacingPlayer(net.minecraft.world.level.block.entity.SignBlockEntity sign,
            Player player, Component text, boolean glowing) {
        sign.setText(new net.minecraft.world.level.block.entity.SignText().setMessage(0, text).setHasGlowingText(glowing),
                sign.isFacingFrontText(player));
    }
    public static final boolean PIECEWISE_HAMMER_TIME = false;
    public static final boolean MEGA_GUIDES = false;
    /** Schachfiguren, Achtelbloecke, Checker-Stufen/-Treppen: nur Hauptlinie 26.3. */
    public static final boolean CHESS = false;

    private McVersion() {
    }

    /** Piston reactions under their 26.2 names (26.3: IMMOVEABLE / POPPED / PUSH). */
    public static final PushReaction PUSH_BLOCKED = PushReaction.BLOCK;
    public static final PushReaction PUSH_DESTROYS = PushReaction.DESTROY;
    public static final PushReaction PUSH_ONLY = PushReaction.PUSH_ONLY;
    /** Entities with this reaction stay where they are when a piston or a shulker lid moves (26.3: IGNORE_ENTITY). */
    public static final PushReaction PUSH_IGNORED = PushReaction.IGNORE;

    /**
     * A block's piston push reaction as the 26.2 rules see it. 26.3 folded vanilla's hard-coded
     * "never push" list (obsidian, crying obsidian, respawn anchor, reinforced deepslate) into the
     * IMMOVEABLE reaction; the mod's pistons treat that list separately, so they ask this instead of
     * {@code getPistonPushReaction()}.
     */
    public static PushReaction pushReaction(net.minecraft.world.level.block.state.BlockState state) {
        return state.getPistonPushReaction();
    }

    /** Explorer-map structure tags (26.3 renamed them after the structure). */
    public static final TagKey<Structure> MANSION_MAP_STRUCTURES = StructureTags.ON_WOODLAND_EXPLORER_MAPS;
    public static final TagKey<Structure> MONUMENT_MAP_STRUCTURES = StructureTags.ON_OCEAN_EXPLORER_MAPS;
    public static final TagKey<Structure> TRIAL_CHAMBERS_MAP_STRUCTURES = StructureTags.ON_TRIAL_CHAMBERS_MAPS;

    /**
     * Drops a stack from a player. {@code predictedByClient} only matters on 26.3, where a drop
     * also swings the arm and the flag decides whether the dropping player is told about it.
     */
    public static net.minecraft.world.entity.item.@Nullable ItemEntity drop(Player player, ItemStack stack,
                                                                            boolean thrownFromHand,
                                                                            boolean predictedByClient) {
        return player.drop(stack, thrownFromHand);
    }

    /** Swings the arm; {@code sendToSwingingEntity} as in vanilla. */
    public static void swing(LivingEntity entity, InteractionHand hand, boolean sendToSwingingEntity) {
        entity.swing(hand, sendToSwingingEntity);
    }

    /** The per-entity "Invulnerable" flag (26.3: setPermanentlyInvulnerable). */
    public static void setInvulnerable(Entity entity, boolean invulnerable) {
        entity.setInvulnerable(invulnerable);
    }

    /** Clears the post-damage invulnerability window (26.3 made the field private). */
    public static void resetInvulnerableTime(Entity entity) {
        entity.invulnerableTime = 0;
    }

    /** {@code LivingEntity#getVisibilityPercent} (26.3 added a ServerLevel parameter). */
    public static double visibilityPercent(LivingEntity entity, @Nullable Entity targeting) {
        return entity.getVisibilityPercent(targeting);
    }

    /** Copies of the stacks in a bundle (26.3: itemCopies). */
    public static Stream<ItemStack> bundleItemCopies(BundleContents contents) {
        return contents.itemCopyStream();
    }

    /** An empty, mutable bundle content (26.3 dropped the copy constructor). */
    public static BundleContents.Mutable emptyBundleMutable() {
        return new BundleContents.Mutable(BundleContents.EMPTY);
    }

    /** A mutable copy of existing bundle contents (26.3: {@code BundleContents#asMutable}). */
    public static BundleContents.Mutable bundleMutable(BundleContents contents) {
        return new BundleContents.Mutable(contents);
    }

    /** The trades a trade set draws from (26.3 turned TradeSet into a record). */
    public static HolderSet<VillagerTrade> tradeSetTrades(TradeSet tradeSet) {
        return tradeSet.getTrades();
    }

    /** A tag predicate built during datagen/bootstrap (26.3 resolves the tag through a lookup). */
    public static <T> TagPredicate<T> tagPredicate(BootstrapContext<?> context,
                                                   ResourceKey<? extends Registry<T>> registry, TagKey<T> tag) {
        return TagPredicate.is(tag);
    }

    /** A trim material for a mod palette (26.2: asset group; 26.3: palette id). */
    public static TrimMaterial trimMaterial(String paletteName, Component description) {
        return new TrimMaterial(MaterialAssetGroup.create(paletteName), description);
    }

    /**
     * A trim material whose palette turns "_darker" on armour of its own equipment asset, the way
     * vanilla iron trim is darker on iron armour. 26.2: an override in the material's asset group.
     */
    public static TrimMaterial trimMaterial(String paletteName, Component description,
                                            ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> darkerOn) {
        return new TrimMaterial(MaterialAssetGroup.create(paletteName, java.util.Map.of(darkerOn, paletteName + "_darker")),
                description);
    }

    /**
     * The palette suffix a trim material shows on armour of the given equipment asset, e.g. "iron"
     * or "iron_darker" (iron trim on iron armour). 26.2: the material's asset group decides.
     */
    public static String trimColourSuffix(TrimMaterial material,
                                          ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset) {
        return material.assets().assetId(asset).suffix();
    }

    /** Gametest metadata (26.3 added the test dimension; the 26.2 tests run in the overworld). */
    public static <E> net.minecraft.gametest.framework.TestData<E> testData(
            E environment, net.minecraft.resources.Identifier structure, int maxTicks, int setupTicks, boolean required,
            net.minecraft.world.level.block.Rotation rotation, boolean manualOnly, int maxAttempts, int requiredSuccesses,
            boolean skyAccess, int padding) {
        return new net.minecraft.gametest.framework.TestData<>(environment, structure, maxTicks, setupTicks, required,
                rotation, manualOnly, maxAttempts, requiredSuccesses, skyAccess, padding);
    }

    /** Block properties that never block the view (26.3 added a near-plane box parameter). */
    public static BlockBehaviour.Properties neverViewBlocking(BlockBehaviour.Properties properties) {
        return properties.isViewBlocking((state, level, pos) -> false);
    }

    /** Front text of a sign, up to four lines (26.3: list constructor and SignTextSlot instead of setMessage/boolean). */
    public static void setSignFrontText(net.minecraft.world.level.block.entity.SignBlockEntity sign,
            java.util.List<Component> lines) {
        net.minecraft.world.level.block.entity.SignText text = new net.minecraft.world.level.block.entity.SignText();
        for (int i = 0; i < lines.size() && i < 4; i++) {
            text = text.setMessage(i, lines.get(i));
        }
        sign.setText(text, true);
    }

    /** One line of a sign's front text (26.3: getText(SignTextSlot).getMessages). */
    public static Component signFrontLine(net.minecraft.world.level.block.entity.SignBlockEntity sign, int index) {
        return sign.getFrontText().getMessage(index, false);
    }

    /** Puts a stack into the inventory or drops it at the player (26.3 added a Prediction argument). */
    public static void placeItemBackInInventory(Player player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack);
    }

    /** Vanilla wax-on particles and sound (26.3: level event 3003 is particles only, the sound is separate). */
    public static void waxOnEffects(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        level.levelEvent(null, net.minecraft.world.level.block.LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0);
    }

    /** Pistons can neither push nor pull the block (26.3 renamed BLOCK to IMMOVEABLE). */
    public static net.minecraft.world.level.material.PushReaction immovable() {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }

    /** Furnace fuel of an item: 26.2 has no cooking_fuel component (crucible items are 26.3 only). */
    public static net.minecraft.world.item.Item.Properties cookingFuel(net.minecraft.world.item.Item.Properties properties, int ticks) {
        return properties;
    }
}
