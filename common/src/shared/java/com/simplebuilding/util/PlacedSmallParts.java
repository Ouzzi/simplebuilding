package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedEggBlock;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Gelegte Kleinteile auf einem Fleck (Besitzer 2026-10-02, "wie Seegurken"): Steinkiesel, Feuersteinsplitter, die
 * Vanilla-Kleinteile des Tags {@code simplebuilding:placeable_small} und Eier (normal, blau, braun) - bis zu
 * {@link #MAX_PARTS} in beliebiger Mischung auf einem Block ({@link PlacedSmallPartsBlock}).
 *
 * <p>Schleichen + Rechtsklick mit einem Kleinteil auf die Oberseite eines Blocks legt das erste ab; derselbe Klick
 * auf einen belegten Fleck (auf das Haeufchen selbst oder auf den Boden darunter) legt ein weiteres dazu, bis vier.
 * Ein fuenftes bleibt in der Hand (der Klick geht dann weiter an das Item: ein Ei fliegt). Die Obergrenze prueft
 * der Server. An Wand und Decke legen sich Kleinteile weiter einzeln wie eine Schmiedevorlage ab
 * ({@link PlacedTemplates}); Eier nur auf den Boden.
 *
 * <p>Lage: feste Plaetze je Anzahl ({@link #SLOTS}) mit kleinen festen Drehungen, alle zusammen nach der Blickrichtung
 * beim ersten Ablegen gedreht. Liegende Teile sind Platten aus der Item-Textur wie die abgelegte Vorlage
 * ({@link PlacedPlate}), allein in Vorlagen-Groesse, ab zwei Teilen halb so gross; Eier stehen als 3D-Ei
 * ({@code block/placed_egg_<farbe>}, Quader {@link #EGG_BOXES}). {@link #place} rechnet die Lage einmal fuer den
 * Renderer und fuer die Trefferform, beide treffen dieselben Pixel.
 *
 * <p>Abbauen: jedes liegende Teil faellt als es selbst heraus; jedes Ei kommt mit Behutsamkeit zurueck, sonst
 * zerbricht es und schluepft wie ein geworfenes ({@link PlacedEggBlock#hatch}). Kolben zerstoeren das Haeufchen wie
 * jedes Abbauen ohne Werkzeug.
 *
 * <p>Alte Welten: das einzelne gelegte Ei ({@code placed_egg}) und ein einzeln auf den Boden gelegtes Kleinteil
 * ({@code placed_smithing_template}) bleiben, wie sie sind; legt man etwas dazu, werden sie zum Haeufchen.
 *
 * <p>Kerzen und Seegurken (Besitzer 2026-10-03): Vanilla-Bloecke mit 1-4 Stueck. Allein bleiben sie Vanilla; erst ein
 * anderes Teil macht daraus ein Haeufchen (Schleichen + Rechtsklick mit dem Teil auf den Kerzen-/Seegurkenblock oder den
 * Boden darunter, {@link #isPileSpot}): die Kerzen bzw. Gurken werden einzelne Teile, Brennen und Wasser bleiben.
 * Auf ein Haeufchen legen sich Kerzen und Seegurken wie jedes andere Teil. Sie stehen als ihr Vanilla-Blockmodell
 * ({@link Kind#CANDLE}, {@link Kind#PICKLE}); das Licht rechnet {@link PlacedSmallPartsBlock#light} aus dem Blockzustand.
 */
public final class PlacedSmallParts {
    /** Hoechstens so viele Teile auf einem Block (feste Server-Obergrenze, wie Seegurken). */
    public static final int MAX_PARTS = 4;
    /** Kantenlaenge eines einzelnen liegenden Teils (wie die abgelegte Vorlage). */
    public static final float SINGLE_SCALE = PlacedPlate.SCALE;
    /** Kantenlaenge je liegendem Teil ab zwei Teilen. */
    public static final float PILE_SCALE = 8.0F / 16.0F;
    /** Jedes weitere Teil liegt so viel hoeher (gegen Z-Fighting, wo sich Teile ueberlappen). */
    public static final float LIFT = 0.2F / 16.0F;

    /** Plaetze (Pixel x, Pixel z, Drehung in Grad) je Anzahl; Blickrichtung Norden, Oberkante nach Norden. */
    private static final float[][][] SLOTS = {
            {{8.0F, 8.0F, 0.0F}},
            {{5.5F, 6.0F, -12.0F}, {10.5F, 10.5F, 16.0F}},
            {{5.0F, 5.5F, -10.0F}, {11.0F, 6.0F, 14.0F}, {7.5F, 11.0F, -4.0F}},
            {{4.5F, 4.5F, -10.0F}, {11.5F, 4.75F, 12.0F}, {4.75F, 11.5F, 6.0F}, {11.25F, 11.25F, -14.0F}},
    };

    /**
     * Die Quader des 3D-Eis (Pixel, von unten): {y0, y1, halbe Breite}; um x = z = 8 zentriert. Erzeugt aus der
     * Vanilla-Eiform von {@code tools/textures/placed_egg_textures.py} - das Modell muss dazu passen.
     */
    public static final float[][] EGG_BOXES = {
            {0.0F, 0.5F, 1.5F}, {0.5F, 1.0F, 2.0F}, {1.0F, 3.5F, 2.5F}, {3.5F, 4.5F, 2.0F}, {4.5F, 5.0F, 1.5F}, {5.0F, 5.5F, 1.0F},
    };

    /**
     * Die Quader des 3D-Barrens (Queue N24, 2026-10-09): {y0, y1, halbe Laenge in x, halbe Breite in z} in Pixeln, um
     * x = z = 8. Erzeugt von {@code tools/textures/placed_ingots_2026_10_09.py} - das Modell muss dazu passen.
     */
    public static final float[][] INGOT_BOXES = {{0.0F, 2.0F, 4.0F, 2.0F}, {2.0F, 3.0F, 3.0F, 1.0F}};

    /**
     * Barrenstapel (nur Barren auf dem Fleck): je Anzahl die Plaetze {Pixel x, Pixel z, Hoehe in Pixeln, Drehung in Grad}.
     * Zwei liegen unten nebeneinander, der dritte und vierte quer darueber - wie ein kleiner Barrenstapel.
     */
    private static final float[][][] INGOT_STACK = {
            {{8.0F, 8.0F, 0.0F, 0.0F}},
            {{8.0F, 5.0F, 0.0F, 0.0F}, {8.0F, 11.0F, 0.0F, 0.0F}},
            {{8.0F, 5.0F, 0.0F, 0.0F}, {8.0F, 11.0F, 0.0F, 0.0F}, {8.0F, 8.0F, 3.0F, 90.0F}},
            {{8.0F, 5.0F, 0.0F, 0.0F}, {8.0F, 11.0F, 0.0F, 0.0F}, {5.0F, 8.0F, 3.0F, 90.0F}, {11.0F, 8.0F, 3.0F, 90.0F}},
    };

    /** Barren mit eigenem 3D-Modell ({@code simplebuilding:placed_<id>}); andere Barren liegen als flache Platte. */
    public static final java.util.Set<String> MODELLED_INGOTS = java.util.Set.of("minecraft:copper_ingot", "minecraft:iron_ingot",
            "minecraft:gold_ingot", "minecraft:netherite_ingot", "simplebuilding:enderite_ingot");

    /** Quader einer Vanilla-Kerze ({@code block/template_candle}, ohne Docht): {y0, y1, halbe Breite}. */
    public static final float[][] CANDLE_BOXES = {{0.0F, 6.0F, 1.0F}};
    /** Quader einer Vanilla-Seegurke ({@code block/sea_pickle}, ohne Spross): {y0, y1, halbe Breite}. */
    public static final float[][] PICKLE_BOXES = {{0.0F, 6.0F, 2.0F}};

    /** Wie ein Teil auf dem Fleck liegt bzw. steht. */
    public enum Kind {
        /** Flache Platte aus der Item-Textur. */
        PLATE,
        /** Aufrechtes 3D-Ei. */
        EGG,
        /** Eine Vanilla-Kerze (Blockmodell, brennt mit dem Fleck). */
        CANDLE,
        /** Eine Vanilla-Seegurke (Blockmodell, leuchtet unter Wasser). */
        PICKLE,
        /** Ein 3D-Barren (Queue N24); nur Barren zusammen stapeln sich ({@link #INGOT_STACK}). */
        INGOT;

        /** Steht aufrecht auf dem Boden (Blockmodell, Mitte im Ursprung) statt flach zu liegen. */
        public boolean standing() {
            return this != PLATE;
        }

        /** Die Quader des stehenden Modells (Pixel, um x = z = 8). */
        public float[][] boxes() {
            return switch (this) {
                case EGG -> EGG_BOXES;
                case CANDLE -> CANDLE_BOXES;
                case PICKLE -> PICKLE_BOXES;
                case INGOT -> INGOT_BOXES;
                case PLATE -> new float[0][];
            };
        }
    }

    private PlacedSmallParts() {
    }

    // =====================================================================================
    // Was sich ablegen laesst
    // =====================================================================================

    /** Ein Kleinteil, ein Ei, eine Kerze oder eine Seegurke, das die Server-Optionen zulassen. */
    public static boolean isPart(ItemStack stack) {
        return PlacedTemplates.isPlaceableSmall(stack) || PlacedEggs.isPlaceableEgg(stack) || isBlockPart(stack) || isPileTemplate(stack);
    }

    /**
     * Eine Schmiedevorlage, die sich zu anderen legt (Queue N24 "Trims bis zu 4", 2026-10-09): jede
     * {@link net.minecraft.world.item.SmithingTemplateItem} und die zwei schlichten Aufwertungsvorlagen der Mod. Die erste
     * liegt weiter einzeln ({@link PlacedTemplates}, mit Hammer-Aufwertung); eine weitere Vorlage darauf macht ein Haeufchen.
     * Blaupause, Oktant, Attractor und Detector arbeiten im Block und bleiben einzeln.
     */
    public static boolean isPileTemplate(ItemStack stack) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES || stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.Item item = stack.getItem();
        return item instanceof net.minecraft.world.item.SmithingTemplateItem
                || item == com.simplebuilding.items.ModItems.ENDERITE_UPGRADE_TEMPLATE || item == com.simplebuilding.items.ModItems.BASIC_UPGRADE_TEMPLATE;
    }

    /** Ein Barren mit eigenem 3D-Modell ({@link #MODELLED_INGOTS}). */
    public static boolean isModelledIngot(ItemStack stack) {
        return !stack.isEmpty() && MODELLED_INGOTS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** Liegen nur Barren auf dem Fleck? Dann stapeln sie sich ({@link #INGOT_STACK}). */
    public static boolean ingotStack(List<ItemStack> parts) {
        if (parts.isEmpty()) {
            return false;
        }
        for (ItemStack part : parts) {
            if (kind(part) != Kind.INGOT) {
                return false;
            }
        }
        return true;
    }

    /**
     * Eine Vanilla-Kerze (alle 17) oder Seegurke, die die Server-Optionen zulassen ({@code placeVanillaItems},
     * {@code placeDisabledItems}). Sie wird nur gemischt zum Teil eines Haeufchens; allein bleibt sie Vanilla.
     */
    public static boolean isBlockPart(ItemStack stack) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES || !(isCandle(stack) || stack.is(Items.SEA_PICKLE))) {
            return false;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        return features.placeVanillaItems && !PlacedTemplates.itemListed(features.placeDisabledItems, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Eine Vanilla-Kerze (ungefaerbt oder eine der 16 Farben). */
    public static boolean isCandle(ItemStack stack) {
        return !stack.isEmpty() && "minecraft".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
                && Block.byItem(stack.getItem()) instanceof CandleBlock;
    }

    /** Wie das Teil auf dem Fleck liegt bzw. steht. */
    public static Kind kind(ItemStack stack) {
        if (isEgg(stack)) {
            return Kind.EGG;
        }
        if (isCandle(stack)) {
            return Kind.CANDLE;
        }
        if (isModelledIngot(stack)) {
            return Kind.INGOT;
        }
        return stack.is(Items.SEA_PICKLE) ? Kind.PICKLE : Kind.PLATE;
    }

    /** Ein Ei (normal, blau, braun) - steht aufrecht, zerbricht ohne Behutsamkeit. */
    public static boolean isEgg(ItemStack stack) {
        return PlacedEggBlock.Egg.of(stack) != null;
    }

    // =====================================================================================
    // Ablegen und Dazulegen
    // =====================================================================================

    /**
     * Schleichen + Rechtsklick mit einem Kleinteil oder Ei (aus {@code Item#useOn}): legt es zu einem belegten
     * Fleck dazu oder als erstes Teil auf die Oberseite eines Blocks. Null, wenn nichts abgelegt wird - dann laeuft
     * das gewohnte Verhalten weiter (Wand und Decke: {@link PlacedTemplates#tryPlace}; Ei: es wird geworfen).
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (ModBlocks.PLACED_SMALL_PARTS == null || player == null || !player.isSecondaryUseActive()
                || !isPart(stack) || !player.mayBuild()) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos pile = isPileSpot(level, clicked, stack) ? clicked
                : context.getClickedFace() == Direction.UP && isPileSpot(level, clicked.above(), stack) ? clicked.above() : null;
        if (pile != null) {
            // Voll (oder nicht erweiterbar): abgelehnt, das Teil bleibt in der Hand - und ein Ei fliegt nicht los.
            return add(level, pile, player, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        // Kerzen und Seegurken legen allein nie ein Haeufchen an: das bleibt der Vanilla-Block. Eine einzelne Vorlage liegt
        // als Vorlage (PlacedTemplates), erst die zweite macht ein Haeufchen.
        if (isBlockPart(stack) || isPileTemplate(stack) || context.getClickedFace() != Direction.UP) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return null;
        }
        BlockPos pos = place.getClickedPos();
        BlockState state = ModBlocks.PLACED_SMALL_PARTS.getStateForPlacement(place);
        if (state == null || !state.canSurvive(level, pos)) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, 11)) {
                return null;
            }
            if (!(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity be) || !be.add(stack)) {
                return null;
            }
            placed(level, pos, player, state, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Ein Fleck, auf den sich {@code stack} dazulegen laesst: ein Haeufchen, ein altes gelegtes Ei, ein liegendes
     * Kleinteil - oder ein Vanilla-Kerzen- bzw. Seegurkenblock, wenn {@code stack} etwas anderes ist (eine gleichfarbige
     * Kerze auf Kerzen, eine Seegurke auf Seegurken bleiben Vanilla).
     */
    public static boolean isPileSpot(Level level, BlockPos pos, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PlacedSmallPartsBlock) {
            return true;
        }
        if (ModBlocks.PLACED_EGG != null && state.is(ModBlocks.PLACED_EGG)) {
            return true;
        }
        if (isCandleBlock(state)) {
            return !stack.is(state.getBlock().asItem());
        }
        if (state.is(Blocks.SEA_PICKLE)) {
            return !stack.is(Items.SEA_PICKLE);
        }
        return state.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && state.getValue(PlacedTemplateBlock.FACE) == AttachFace.FLOOR
                && level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity be && joinsLying(be.getTemplate(), stack);
    }

    /**
     * Ob sich {@code stack} zu diesem einzeln liegenden Stapel legt: zu einem Kleinteil jedes Teil, zu einer
     * Schmiedevorlage nur eine weitere Vorlage (bis zu vier Vorlagen, Queue N24).
     */
    private static boolean joinsLying(ItemStack lying, ItemStack stack) {
        return isSmallPart(lying) || isPileTemplate(lying) && isPileTemplate(stack);
    }

    /** Ein Vanilla-Kerzenblock (1-4 Kerzen einer Farbe). */
    private static boolean isCandleBlock(BlockState state) {
        return state.getBlock() instanceof CandleBlock && isCandle(new ItemStack(state.getBlock().asItem()));
    }

    /** Ein Kleinteil des Tags, unabhaengig von den Server-Optionen (was schon liegt, darf liegen bleiben). */
    private static boolean isSmallPart(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Items.PLACEABLE_SMALL);
    }

    /**
     * Legt ein Teil aus {@code stack} zum Fleck bei {@code pos} dazu (Server prueft und verbraucht; der Client sagt nur
     * voraus). Ein altes Ei oder liegendes Kleinteil wird dabei zum Haeufchen. Liefert false, wenn der Fleck voll
     * ist ({@link #MAX_PARTS}) oder dort nichts dazugelegt werden kann.
     */
    public static boolean add(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PlacedSmallPartsBlock) {
            if (!(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity be) || be.parts().size() >= MAX_PARTS) {
                return false;
            }
            if (!level.isClientSide()) {
                if (!be.add(stack)) {
                    return false;
                }
                placed(level, pos, player, state, stack);
            }
            return true;
        }
        // Alte Einzelteile und Vanilla-Kerzen/-Seegurken: erst zum Haeufchen machen, dann dazulegen.
        List<ItemStack> old = new ArrayList<>();
        Direction facing = player.getDirection();
        boolean lit = false;
        boolean vanillaBlock = false;
        if (ModBlocks.PLACED_EGG != null && state.is(ModBlocks.PLACED_EGG)) {
            old.add(new ItemStack(state.getValue(PlacedEggBlock.EGG).item()));
        } else if (state.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity template
                && joinsLying(template.getTemplate(), stack)) {
            old.add(template.getTemplate().copyWithCount(1));
            facing = state.getValue(PlacedTemplateBlock.FACING);
        } else if (isCandleBlock(state) && !stack.is(state.getBlock().asItem())) {
            for (int i = 0; i < state.getValue(CandleBlock.CANDLES); i++) {
                old.add(new ItemStack(state.getBlock().asItem()));
            }
            lit = state.getValue(CandleBlock.LIT);
            vanillaBlock = true;
        } else if (state.is(Blocks.SEA_PICKLE) && !stack.is(Items.SEA_PICKLE)) {
            for (int i = 0; i < state.getValue(SeaPickleBlock.PICKLES); i++) {
                old.add(new ItemStack(Items.SEA_PICKLE));
            }
            vanillaBlock = true;
        } else {
            return false;
        }
        if (old.size() >= MAX_PARTS) {
            return false;
        }
        BlockState pile = ModBlocks.PLACED_SMALL_PARTS.defaultBlockState().setValue(PlacedSmallPartsBlock.FACING, facing)
                .setValue(PlacedSmallPartsBlock.WATERLOGGED, state.getFluidState().is(Fluids.WATER))
                .setValue(PlacedSmallPartsBlock.LIT, lit);
        if (vanillaBlock && !pile.canSurvive(level, pos)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        // Ohne Drops austauschen: die alten Teile wandern in das Haeufchen.
        if (!level.setBlock(pos, pile, 3)) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity be)) {
            return false;
        }
        be.setParts(old);
        if (!be.add(stack)) {
            return false;
        }
        placed(level, pos, player, pile, stack);
        return true;
    }

    /** Klang, Spielereignis, ein Teil weniger in der Hand (nicht im Kreativmodus). */
    private static void placed(Level level, BlockPos pos, Player player, BlockState state, ItemStack stack) {
        if (isEgg(stack)) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.5F, 1.6F);
        } else if (isCandle(stack) || stack.is(Items.SEA_PICKLE)) {
            // Der Klang des Vanilla-Blocks (Kerze bzw. Seegurke).
            SoundType sound = Block.byItem(stack.getItem()).defaultBlockState().getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        } else {
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        stack.consume(1, player);
    }

    // =====================================================================================
    // Abbauen
    // =====================================================================================

    /** Hat das Werkzeug Behutsamkeit? */
    public static boolean silkTouch(ServerLevel level, @Nullable ItemStack tool) {
        return tool != null && !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
    }

    /** Was herausfaellt: jedes liegende Teil, Eier nur mit Behutsamkeit. */
    public static List<ItemStack> drops(List<ItemStack> parts, boolean silk) {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack part : parts) {
            if (!part.isEmpty() && (silk || !isEgg(part))) {
                drops.add(part.copy());
            }
        }
        return drops;
    }

    /**
     * Ohne Behutsamkeit zerbricht jedes Ei des Haeufchens: ein Knacken, und jedes schluepft fuer sich wie ein
     * geworfenes Ei. Liefert die Zahl der Kueken.
     */
    public static int breakEggs(ServerLevel level, BlockPos pos, List<ItemStack> parts, RandomSource random) {
        int chicks = 0;
        boolean any = false;
        for (ItemStack part : parts) {
            if (isEgg(part)) {
                any = true;
                chicks += PlacedEggBlock.hatch(level, pos, part, random);
            }
        }
        if (any) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return chicks;
    }

    // =====================================================================================
    // Lage (Renderer und Trefferform)
    // =====================================================================================

    /** Die vier Grundschritte, die sowohl ein {@code PoseStack} als auch eine {@link Matrix4f} kennen. */
    public interface Ops {
        void translate(float x, float y, float z);

        void rotateY(float radians);

        void rotateX(float radians);

        void scale(float x, float y, float z);
    }

    /** {@link Ops} auf eine Matrix (rechts multipliziert, wie der PoseStack). */
    public record MatrixOps(Matrix4f m) implements Ops {
        @Override
        public void translate(float x, float y, float z) {
            m.translate(x, y, z);
        }

        @Override
        public void rotateY(float radians) {
            m.rotateY(radians);
        }

        @Override
        public void rotateX(float radians) {
            m.rotateX(radians);
        }

        @Override
        public void scale(float x, float y, float z) {
            m.scale(x, y, z);
        }
    }

    /**
     * Vom Blockursprung zum Modellraum von Teil {@code index} (von {@code count}), so wie ein Item-Modell ohne
     * Anzeige-Transformation gezeichnet wird (Mitte des Modells im Ursprung). Liegende Teile: die Platte flach, die
     * Oberkante nach {@code facing}; stehende Teile (Eier, Kerzen, Seegurken, {@link Kind#standing}): das Blockmodell mit
     * seiner Unterkante auf dem Boden, die Modellmitte (8, 8, 8) im Ursprung - dort sitzt auch die Kerzenflamme.
     */
    public static void place(Ops ops, Direction facing, int count, int index, boolean standing) {
        place(ops, facing, count, index, standing ? Kind.EGG : Kind.PLATE, false);
    }

    /**
     * Wie {@link #place(Ops, Direction, int, int, boolean)}, mit der Art des Teils: Barren stehen als 3D-Barren und liegen,
     * wenn nur Barren auf dem Fleck sind ({@code ingotStack}), als kleiner Stapel ({@link #INGOT_STACK}).
     */
    public static void place(Ops ops, Direction facing, int count, int index, Kind kind, boolean ingotStack) {
        if (kind == Kind.INGOT && ingotStack) {
            float[] spot = INGOT_STACK[Math.max(0, Math.min(MAX_PARTS, count) - 1)][Math.max(0, Math.min(index, count - 1))];
            ops.translate(0.5F, 0.0F, 0.5F);
            ops.rotateY(rad(180.0F - facing.toYRot()));
            ops.translate((spot[0] - 8.0F) / 16.0F, spot[2] / 16.0F, (spot[1] - 8.0F) / 16.0F);
            ops.rotateY(rad(spot[3]));
            ops.translate(0.0F, 0.5F, 0.0F);
            return;
        }
        boolean standing = kind.standing();
        float[] slot = SLOTS[Math.max(0, Math.min(MAX_PARTS, count) - 1)][Math.max(0, Math.min(index, count - 1))];
        float lift = index * LIFT;
        ops.translate(0.5F, 0.0F, 0.5F);
        ops.rotateY(rad(180.0F - facing.toYRot()));
        ops.translate((slot[0] - 8.0F) / 16.0F, 0.0F, (slot[1] - 8.0F) / 16.0F);
        ops.rotateY(rad(slot[2]));
        if (standing) {
            // Stehende Teile stehen immer auf dem Boden (sie koennen eine liegende Platte streifen, das stoert nicht).
            ops.translate(0.0F, 0.5F, 0.0F);
            return;
        }
        float scale = count <= 1 ? SINGLE_SCALE : PILE_SCALE;
        float thickness = scale * PlacedPlate.THICKNESS_SCALE / 16.0F;
        ops.translate(0.0F, lift + thickness / 2.0F + PlacedPlate.GAP, 0.0F);
        ops.rotateX(rad(-90.0F));
        ops.scale(scale, scale, scale * PlacedPlate.THICKNESS_SCALE);
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** Trefferform des ganzen Haeufchens: die Vereinigung der Teile, jedes pixelgenau wie gezeichnet. */
    public static VoxelShape shape(List<ItemStack> parts, Direction facing) {
        if (parts.isEmpty()) {
            return net.minecraft.world.level.block.Block.box(4.0, 0.0, 4.0, 12.0, 1.0, 12.0);
        }
        VoxelShape shape = Shapes.empty();
        int count = Math.min(parts.size(), MAX_PARTS);
        boolean stack = ingotStack(parts);
        for (int i = 0; i < count; i++) {
            shape = Shapes.or(shape, partShape(parts.get(i), facing, count, i, stack));
        }
        return shape.optimize();
    }

    /** Trefferform eines Teils: die Quader des stehenden Modells bzw. die deckenden Pixel der Item-Textur. */
    public static VoxelShape partShape(ItemStack part, Direction facing, int count, int index) {
        return partShape(part, facing, count, index, false);
    }

    /** Wie {@link #partShape(ItemStack, Direction, int, int)}; {@code ingotStack}: nur Barren auf dem Fleck. */
    public static VoxelShape partShape(ItemStack part, Direction facing, int count, int index, boolean ingotStack) {
        Kind kind = kind(part);
        Matrix4f m = new Matrix4f();
        place(new MatrixOps(m), facing, count, index, kind, ingotStack);
        m.translate(-0.5F, -0.5F, -0.5F);
        VoxelShape shape = Shapes.empty();
        if (kind.standing()) {
            for (float[] box : kind.boxes()) {
                float halfZ = box.length > 3 ? box[3] : box[2];
                shape = Shapes.or(shape, box(m, (8.0F - box[2]) / 16.0F, box[0] / 16.0F, (8.0F - halfZ) / 16.0F,
                        (8.0F + box[2]) / 16.0F, box[1] / 16.0F, (8.0F + halfZ) / 16.0F));
            }
            return shape;
        }
        short[] mask = PlacedPlate.mask(part.getItem());
        int v = 0;
        while (v < 16) {
            int row = mask[v] & 0xFFFF;
            int end = v + 1;
            while (end < 16 && (mask[end] & 0xFFFF) == row) {
                end++;
            }
            int u = 0;
            while (u < 16) {
                if ((row & (1 << (15 - u))) == 0) {
                    u++;
                    continue;
                }
                int u1 = u;
                while (u1 < 16 && (row & (1 << (15 - u1))) != 0) {
                    u1++;
                }
                shape = Shapes.or(shape, box(m, u / 16.0F, (16 - end) / 16.0F, 7.5F / 16.0F, u1 / 16.0F, (16 - v) / 16.0F, 8.5F / 16.0F));
                u = u1;
            }
            v = end;
        }
        return shape;
    }

    /**
     * Die Mitte von Teil {@code index} (von {@code count}) relativ zum Blockursprung: bei stehenden Teilen die
     * Modellmitte, wo die Flamme einer Kerze sitzt (wie Vanilla 8 Pixel ueber dem Boden), bei liegenden die Platte.
     */
    public static Vector3f center(Direction facing, int count, int index, boolean standing) {
        Matrix4f m = new Matrix4f();
        place(new MatrixOps(m), facing, count, index, standing);
        return m.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
    }

    /** Wie viele Kerzen unter den Teilen sind. */
    public static int candles(List<ItemStack> parts) {
        int n = 0;
        for (ItemStack part : parts) {
            if (isCandle(part)) {
                n++;
            }
        }
        return n;
    }

    /** Wie viele Seegurken unter den Teilen sind. */
    public static int pickles(List<ItemStack> parts) {
        int n = 0;
        for (ItemStack part : parts) {
            if (part.is(Items.SEA_PICKLE)) {
                n++;
            }
        }
        return n;
    }

    private static VoxelShape box(Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        Vector3f corner = new Vector3f();
        for (int i = 0; i < 8; i++) {
            m.transformPosition((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, corner);
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return Shapes.box(clamp(minX), clamp(minY), clamp(minZ), clamp(maxX), clamp(maxY), clamp(maxZ));
    }

    /** Auf 1/10000 gerundet und in den Block geklemmt. */
    private static double clamp(float value) {
        return Math.max(0.0, Math.min(1.0, Math.round(value * 10000.0) / 10000.0));
    }
}
