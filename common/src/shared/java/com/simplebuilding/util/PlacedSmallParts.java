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
 * <p>Lage: je Anzahl feste Zellen ({@link #CELLS}), in denen ein Teil mit kleiner fester Drehung sitzt (Queue N65: nie
 * ueberlappend, keine Hoehenversetzung noetig), alle zusammen nach der Blickrichtung
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
    /**
     * Plaetze je Anzahl (Queue N65, 2026-10-15): jedes Teil bekommt eine eigene Zelle des Blocks, die sich nie mit der Zelle
     * eines anderen Teils schneidet: {Zelle x0, z0, x1, z1 (Pixel), Wunschmitte x, z, Wunschdrehung in Grad}. Eins hat den
     * ganzen Block, zwei je eine Haelfte (16 x 8), drei zwei Viertel und eine Haelfte, vier je ein Viertel (8 x 8). Blickrichtung
     * Norden, Oberkante nach Norden. Wie das Teil darin sitzt, rechnet {@link #slot}; jedes Teil passt in jede Zelle seiner
     * Anzahl ({@link #footprint}), also ueberlappt nie etwas, egal in welcher Mischung.
     */
    private static final float[][][] CELLS = {
            {{0.0F, 0.0F, 16.0F, 16.0F, 8.0F, 8.0F, 0.0F}},
            {{0.0F, 0.0F, 16.0F, 8.0F, 5.5F, 4.0F, -12.0F}, {0.0F, 8.0F, 16.0F, 16.0F, 10.5F, 12.0F, 16.0F}},
            {{0.0F, 0.0F, 8.0F, 8.0F, 4.5F, 4.5F, -10.0F}, {8.0F, 0.0F, 16.0F, 8.0F, 11.5F, 4.5F, 14.0F},
                    {0.0F, 8.0F, 16.0F, 16.0F, 7.5F, 12.0F, -4.0F}},
            {{0.0F, 0.0F, 8.0F, 8.0F, 4.5F, 4.5F, -10.0F}, {8.0F, 0.0F, 16.0F, 8.0F, 11.5F, 4.75F, 12.0F},
                    {0.0F, 8.0F, 8.0F, 16.0F, 4.75F, 11.5F, 6.0F}, {8.0F, 8.0F, 16.0F, 16.0F, 11.25F, 11.25F, -14.0F}},
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

    /**
     * Plattenstapel (nur Platten auf dem Fleck, Besitzer D5): je Anzahl die Plaetze {Pixel x, Pixel z, Hoehe in Pixeln, Drehung in
     * Grad}. Jede Platte liegt genau {@link PlacedSolids#DISC_HEIGHT} ueber der vorigen, leicht versetzt und gedreht, damit es ein
     * Haufen bleibt; die Platten haben keine Unterseite, es gibt also keine Flaeche, die mit der der Platte darunter um dieselbe Ebene
     * kaempft. Radius 6 plus Versatz bleibt im Block.
     */
    private static final float[][][] DISC_STACK = {
            {{8.0F, 8.0F, 0.0F, 0.0F}},
            {{8.0F, 8.0F, 0.0F, 0.0F}, {8.6F, 7.6F, 1.0F, 11.0F}},
            {{8.0F, 8.0F, 0.0F, 0.0F}, {8.6F, 7.6F, 1.0F, 11.0F}, {7.5F, 8.5F, 2.0F, -8.0F}},
            {{8.0F, 8.0F, 0.0F, 0.0F}, {8.6F, 7.6F, 1.0F, 11.0F}, {7.5F, 8.5F, 2.0F, -8.0F}, {8.4F, 8.6F, 3.0F, 14.0F}},
    };

    /** Barren mit eigenem 3D-Modell ({@code simplebuilding:placed_<id>}); andere Barren liegen als flache Platte. */
    public static final java.util.Set<String> MODELLED_INGOTS = java.util.Set.of("minecraft:copper_ingot", "minecraft:iron_ingot",
            "minecraft:gold_ingot", "minecraft:netherite_ingot", "simplelib:enderite_ingot");

    /**
     * Der 3D-Ziegel (Queue N55, 2026-10-15): ein Quader 8 x 4 x 3 px, lang entlang x, in der Form {y0, y1, halbe Laenge in x,
     * halbe Breite in z} wie {@link #INGOT_BOXES}. Erzeugt von {@code tools/textures/placed_gems_bricks_2026_10_15.py}.
     */
    public static final float[][] BRICK_BOXES = {{0.0F, 3.0F, 4.0F, 2.0F}};

    /** Ziegel mit eigenem 3D-Modell ({@code simplebuilding:placed_<id>}); sie stapeln sich mit den Barren. */
    public static final java.util.Set<String> MODELLED_BRICKS = java.util.Set.of("minecraft:brick", "minecraft:nether_brick", "minecraft:resin_brick");

    /**
     * Sprite-Silhouetten der Edelsteine: je Pixelzeile der Item-Textur ab Zeile 2 die halbe Breite (Pixel). Daraus
     * entstehen abgestufte Quader im halben Massstab ({@link #gemBoxes}); das Modell {@code placed_<id>} und der Generator
     * {@code tools/textures/placed_gems_bricks_2026_10_15.py} muessen dazu passen (GameTest prueft es).
     */
    private static final java.util.Map<String, int[]> GEM_ROWS = java.util.Map.of(
            "minecraft:diamond", new int[]{2, 3, 4, 5, 5, 6, 6, 6, 6, 5, 5, 4, 3},
            "minecraft:emerald", new int[]{2, 3, 4, 5, 5, 5, 5, 5, 5, 4, 3, 2},
            // BS25-5: the cracked diamond stands like the diamond (sprite rows 1..13)
            "simplelib:cracked_diamond", new int[]{2, 3, 4, 4, 5, 5, 6, 6, 6, 5, 5, 4, 3});
    /** Edelsteine mit eigenem 3D-Modell ({@code simplebuilding:placed_<id>}). */
    public static final java.util.Set<String> MODELLED_GEMS = GEM_ROWS.keySet();

    /**
     * Die Quader eines Edelsteins: {y0, y1, halbe Breite} in Pixeln, um x = z = 8, aus den gleich breiten Zeilenfolgen der
     * Silhouette; ein Sprite-Pixel ist ein halber Modellpixel.
     */
    public static float[][] gemBoxes(String id) {
        int[] rows = GEM_ROWS.get(id);
        List<float[]> boxes = new ArrayList<>();
        int a = 0;
        while (a < rows.length) {
            int b = a;
            while (b + 1 < rows.length && rows[b + 1] == rows[a]) {
                b++;
            }
            // Zeile i liegt von (rows.length - 1 - i) * 0.5 bis (rows.length - i) * 0.5 ueber dem Boden.
            boxes.add(new float[]{(rows.length - 1 - b) * 0.5F, (rows.length - a) * 0.5F, rows[a] * 0.5F});
            a = b + 1;
        }
        return boxes.toArray(new float[0][]);
    }

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
        /** Ein 3D-Barren (Queue N24); nur Barren und Ziegel zusammen stapeln sich ({@link #INGOT_STACK}). */
        INGOT,
        /** Ein 3D-Ziegel (Queue N55): ein Quader, stapelt sich wie ein Barren. */
        BRICK,
        /** Ein aufrecht stehender 3D-Edelstein (Diamant, Smaragd; Queue N55). */
        GEM,
        /** Eine Vorlage oder Toepferscherbe als 2 px dicke Silhouette ({@link PlacedSolids.Shape#SLAB}), liegt wie eine Platte. */
        SLAB,
        /** Eine runde Schallplatte, 1 px dick ({@link PlacedSolids.Shape#DISC}); allein auf dem Fleck ein echter Stapel ({@link #DISC_STACK}). */
        DISC;

        /** Steht aufrecht auf dem Boden (Blockmodell, Mitte im Ursprung) statt flach zu liegen. */
        public boolean standing() {
            return this != PLATE && this != SLAB;
        }

        /** Liegt im Stapel ({@link #INGOT_STACK} bzw. {@link #DISC_STACK}), wenn nur solche der gleichen Familie auf dem Fleck sind. */
        public boolean stackable() {
            return this == INGOT || this == BRICK || this == DISC;
        }

        /** Barren und Ziegel stapeln sich miteinander, Platten nur mit Platten. */
        private int family() {
            return this == DISC ? 2 : 1;
        }

        /** Hat ein 3D-Modell, das der Vorschlaghammer auf die flache Platte umschalten kann (Queue N55). */
        public boolean switchable() {
            return this == INGOT || this == BRICK || this == GEM || this == SLAB || this == DISC;
        }

        /** Die Quader des stehenden Modells von {@code part} (Pixel, um x = z = 8). */
        public float[][] boxes(ItemStack part) {
            return switch (this) {
                case EGG -> EGG_BOXES;
                case CANDLE -> CANDLE_BOXES;
                case PICKLE -> PICKLE_BOXES;
                case INGOT -> INGOT_BOXES;
                case BRICK -> BRICK_BOXES;
                case GEM -> gemBoxes(BuiltInRegistries.ITEM.getKey(part.getItem()).toString());
                case DISC -> PlacedSolids.discBoxes();
                case PLATE, SLAB -> new float[0][];
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

    /** Ein Ziegel mit eigenem 3D-Modell ({@link #MODELLED_BRICKS}). */
    public static boolean isModelledBrick(ItemStack stack) {
        return !stack.isEmpty() && MODELLED_BRICKS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** Ein Edelstein mit eigenem 3D-Modell ({@link #MODELLED_GEMS}). */
    public static boolean isModelledGem(ItemStack stack) {
        return !stack.isEmpty() && MODELLED_GEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** Liegen nur Barren und Ziegel (oder nur Schallplatten) auf dem Fleck? Dann stapeln sie sich ({@link #INGOT_STACK}, {@link #DISC_STACK}). */
    public static boolean ingotStack(List<ItemStack> parts) {
        return ingotStack(parts, false);
    }

    /** Wie {@link #ingotStack(List)}; {@code flat}: die Teile liegen als Platten (Queue N55), dann stapelt nichts. */
    public static boolean ingotStack(List<ItemStack> parts, boolean flat) {
        if (parts.isEmpty() || flat) {
            return false;
        }
        int family = kind(parts.get(0)).family();
        for (ItemStack part : parts) {
            Kind kind = kind(part);
            if (!kind.stackable() || kind.family() != family) {
                return false;
            }
        }
        return true;
    }

    /** Ob ein Teil ein 3D-Modell hat, das sich auf flach umschalten laesst (Queue N55). */
    public static boolean isSwitchable(ItemStack stack) {
        return kind(stack).switchable();
    }

    /** Ob unter den Teilen eins mit umschaltbarem 3D-Modell ist. */
    public static boolean anySwitchable(List<ItemStack> parts) {
        for (ItemStack part : parts) {
            if (isSwitchable(part)) {
                return true;
            }
        }
        return false;
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
        if (isModelledBrick(stack)) {
            return Kind.BRICK;
        }
        if (isModelledGem(stack)) {
            return Kind.GEM;
        }
        PlacedSolids.Shape solid = PlacedSolids.shape(stack);
        if (solid != null) {
            return solid == PlacedSolids.Shape.DISC ? Kind.DISC : Kind.SLAB;
        }
        return stack.is(Items.SEA_PICKLE) ? Kind.PICKLE : Kind.PLATE;
    }

    /** Wie {@link #kind(ItemStack)}; {@code flat}: umschaltbare 3D-Teile liegen als Platte (Queue N55). */
    public static Kind kind(ItemStack stack, boolean flat) {
        Kind kind = kind(stack);
        return flat && kind.switchable() ? Kind.PLATE : kind;
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
        ItemStack lying = lyingTemplate(level, pos, state);
        return !lying.isEmpty() && joinsLying(lying, stack);
    }

    /** Simple Trims' placed trim template (26.3, N18), found by id: no class import, its block entity saves "Template". */
    private static final net.minecraft.resources.Identifier SIMPLE_TRIMS_PLACED =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletrims", "placed_template");

    /**
     * The template lying flat on the floor at {@code pos}: SimpleBuilding's own placed template or, on 26.3, Simple
     * Trims' (trim templates belong to it there; a second template on top still makes a pile, N24). Empty otherwise.
     */
    private static ItemStack lyingTemplate(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.PLACED_SMITHING_TEMPLATE)) {
            return state.getValue(PlacedTemplateBlock.FACE) == AttachFace.FLOOR
                    && level.getBlockEntity(pos) instanceof PlacedTemplateBlockEntity be ? be.getTemplate() : ItemStack.EMPTY;
        }
        if (com.simplebuilding.version.McVersion.TRIMS_IN_SB || !SIMPLE_TRIMS_PLACED.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
                || !state.hasProperty(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE)
                || state.getValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE) != AttachFace.FLOOR
                || level.getBlockEntity(pos) == null) {
            return ItemStack.EMPTY;
        }
        net.minecraft.nbt.CompoundTag saved = level.getBlockEntity(pos).saveCustomOnly(level.registryAccess());
        net.minecraft.nbt.Tag template = saved.get("Template");
        return template == null ? ItemStack.EMPTY : ItemStack.CODEC.parse(
                level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), template).result().orElse(ItemStack.EMPTY);
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
        } else if (!lyingTemplate(level, pos, state).isEmpty() && joinsLying(lyingTemplate(level, pos, state), stack)) {
            old.add(lyingTemplate(level, pos, state).copyWithCount(1));
            facing = state.getValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACING);
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
     * seiner Unterkante auf dem Boden, die Modellmitte (8, 8, 8) im Ursprung - dort sitzt auch die Kerzenflamme. Barren
     * und Ziegel liegen, wenn nur solche auf dem Fleck sind ({@code ingotStack}), als kleiner Stapel ({@link #INGOT_STACK}).
     * Die Plaetze sonst: {@link #slot}.
     */
    public static void place(Ops ops, Direction facing, int count, int index, Kind kind, boolean ingotStack) {
        if (kind.stackable() && ingotStack) {
            float[] spot = (kind == Kind.DISC ? DISC_STACK : INGOT_STACK)[Math.max(0, Math.min(MAX_PARTS, count) - 1)][Math.max(0, Math.min(index, count - 1))];
            ops.translate(0.5F, 0.0F, 0.5F);
            ops.rotateY(rad(180.0F - facing.toYRot()));
            ops.translate((spot[0] - 8.0F) / 16.0F, spot[2] / 16.0F, (spot[1] - 8.0F) / 16.0F);
            ops.rotateY(rad(spot[3]));
            ops.translate(0.0F, 0.5F, 0.0F);
            return;
        }
        boolean standing = kind.standing();
        float[] slot = slot(count, index, kind);
        ops.translate(0.5F, 0.0F, 0.5F);
        ops.rotateY(rad(180.0F - facing.toYRot()));
        ops.translate((slot[0] - 8.0F) / 16.0F, 0.0F, (slot[1] - 8.0F) / 16.0F);
        ops.rotateY(rad(slot[2]));
        if (standing) {
            // Stehende Teile stehen immer auf dem Boden, in ihrer eigenen Zelle (ueberlappen nie).
            ops.translate(0.0F, 0.5F, 0.0F);
            if (kind == Kind.DISC && count > 1) {
                // Eine Platte neben anderen Teilen ist kleiner (Radius 4 px = jede Zelle), die Dicke bleibt.
                ops.scale(PlacedSolids.DISC_PILE_SCALE, 1.0F, PlacedSolids.DISC_PILE_SCALE);
            }
            return;
        }
        float scale = count <= 1 ? SINGLE_SCALE : PILE_SCALE;
        if (kind == Kind.SLAB) {
            // 3D-Silhouette: das Modell traegt seine Dicke (2 px bei 14/16, ca. 1,1 px bei 8/16), nur gleichmaessig skalieren.
            ops.translate(0.0F, scale * PlacedSolids.MODEL_THICKNESS / 16.0F / 2.0F + PlacedPlate.GAP, 0.0F);
            ops.rotateX(rad(-90.0F));
            ops.scale(scale, scale, scale);
            return;
        }
        float thickness = scale * PlacedPlate.THICKNESS_SCALE / 16.0F;
        ops.translate(0.0F, thickness / 2.0F + PlacedPlate.GAP, 0.0F);
        ops.rotateX(rad(-90.0F));
        ops.scale(scale, scale, scale * PlacedPlate.THICKNESS_SCALE);
    }

    /**
     * Grundflaeche eines Teils der Art {@code kind} im Haeufchen mit {@code count} Teilen: {halbe Laenge in x, halbe Breite in z}
     * in Pixeln, vor der Drehung. Liegende Platten sind quadratisch (Kantenlaenge je Anzahl), alle anderen decken ihre Quader ab;
     * ein Edelstein nimmt die Breite des breitesten ({@link #GEM_HALF}), unabhaengig vom Stein.
     */
    public static float[] footprint(Kind kind, int count) {
        if (kind == Kind.PLATE || kind == Kind.SLAB) {
            float half = (count <= 1 ? SINGLE_SCALE : PILE_SCALE) * 8.0F;
            return new float[]{half, half};
        }
        if (kind == Kind.DISC) {
            float radius = count <= 1 ? PlacedSolids.DISC_RADIUS : PlacedSolids.DISC_RADIUS * PlacedSolids.DISC_PILE_SCALE;
            return new float[]{radius, radius};
        }
        if (kind == Kind.GEM) {
            return new float[]{GEM_HALF, GEM_HALF};
        }
        float[][] boxes = switch (kind) {
            case EGG -> EGG_BOXES;
            case CANDLE -> CANDLE_BOXES;
            case PICKLE -> PICKLE_BOXES;
            case INGOT -> INGOT_BOXES;
            default -> BRICK_BOXES;
        };
        float hx = 0.0F;
        float hz = 0.0F;
        for (float[] box : boxes) {
            hx = Math.max(hx, box[2]);
            hz = Math.max(hz, box.length > 3 ? box[3] : box[2]);
        }
        return new float[]{hx, hz};
    }

    /** Halbe Breite des breitesten Edelsteins (aus {@link #GEM_ROWS}, ein Sprite-Pixel ist ein halber Modellpixel). */
    private static final float GEM_HALF = GEM_ROWS.values().stream().flatMapToInt(java.util.Arrays::stream).max().getAsInt() * 0.5F;

    /**
     * Platz von Teil {@code index} (von {@code count}) der Art {@code kind}: {Mitte x, Mitte z (Pixel), Drehung in Grad}, Blickrichtung
     * Norden. Das Teil sitzt in seiner Zelle ({@link #CELLS}): die Wunschdrehung wird (in Vierteln) so weit verkleinert, dass die
     * gedrehte Grundflaeche in die Zelle passt, die Mitte wird in die Zelle geschoben.
     */
    public static float[] slot(int count, int index, Kind kind) {
        int n = Math.max(1, Math.min(MAX_PARTS, count));
        float[] cell = CELLS[n - 1][Math.max(0, Math.min(index, n - 1))];
        float[] fp = footprint(kind, n);
        float rot = 0.0F;
        float hx = fp[0];
        float hz = fp[1];
        // Eine runde Platte bleibt ungedreht: ihre Trefferform sind Quader, gedreht schoeben deren Ecken ueber die Zelle.
        for (float t = kind == Kind.DISC ? 0.0F : 1.0F; t > 0.0F; t -= 0.25F) {
            double a = Math.toRadians(cell[6] * t);
            float rx = (float) (fp[0] * Math.abs(Math.cos(a)) + fp[1] * Math.abs(Math.sin(a)));
            float rz = (float) (fp[0] * Math.abs(Math.sin(a)) + fp[1] * Math.abs(Math.cos(a)));
            if (rx * 2.0F <= cell[2] - cell[0] + 1.0E-4F && rz * 2.0F <= cell[3] - cell[1] + 1.0E-4F) {
                rot = cell[6] * t;
                hx = rx;
                hz = rz;
                break;
            }
        }
        float x = Math.max(cell[0] + hx, Math.min(cell[2] - hx, cell[4]));
        float z = Math.max(cell[1] + hz, Math.min(cell[3] - hz, cell[5]));
        return new float[]{x, z, rot};
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** Trefferform des ganzen Haeufchens: die Vereinigung der Teile, jedes pixelgenau wie gezeichnet. */
    public static VoxelShape shape(List<ItemStack> parts, Direction facing) {
        return shape(parts, facing, false);
    }

    /** Wie {@link #shape(List, Direction)}; {@code flat}: die umschaltbaren Teile liegen als Platten (Queue N55). */
    public static VoxelShape shape(List<ItemStack> parts, Direction facing, boolean flat) {
        if (parts.isEmpty()) {
            return net.minecraft.world.level.block.Block.box(4.0, 0.0, 4.0, 12.0, 1.0, 12.0);
        }
        VoxelShape shape = Shapes.empty();
        int count = Math.min(parts.size(), MAX_PARTS);
        boolean stack = ingotStack(parts, flat);
        for (int i = 0; i < count; i++) {
            shape = Shapes.or(shape, partShape(parts.get(i), facing, count, i, stack, flat));
        }
        return shape.optimize();
    }

    /**
     * N37 (Besitzer 2026-10-11, "das angeschaute Teil wird abgebaut"): das Teil, das der Strahl {@code from} -> {@code to}
     * zuerst trifft, je Teil mit seiner eigenen Trefferform ({@link #partShape}); -1, wenn er keins trifft.
     */
    public static int partAt(List<ItemStack> parts, Direction facing, BlockPos pos, net.minecraft.world.phys.Vec3 from,
                             net.minecraft.world.phys.Vec3 to) {
        return partAt(parts, facing, pos, from, to, false);
    }

    /** Wie {@link #partAt(List, Direction, BlockPos, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3)}; {@code flat} wie bei {@link #shape(List, Direction, boolean)}. */
    public static int partAt(List<ItemStack> parts, Direction facing, BlockPos pos, net.minecraft.world.phys.Vec3 from,
                             net.minecraft.world.phys.Vec3 to, boolean flat) {
        int count = Math.min(parts.size(), MAX_PARTS);
        boolean stack = ingotStack(parts, flat);
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            net.minecraft.world.phys.BlockHitResult hit = partShape(parts.get(i), facing, count, i, stack, flat).clip(from, to, pos);
            if (hit != null) {
                double distance = hit.getLocation().distanceToSqr(from);
                if (distance < bestDistance) {
                    best = i;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /**
     * Das Teil, auf das {@code player} schaut (Blickstrahl ueber seine Block-Reichweite, {@link #partAt}); trifft der
     * Strahl keins (etwa beim schnellen Wegdrehen), das zuletzt dazugelegte - abgebaut wird immer nur ein Teil.
     */
    public static int lookedAtPart(Player player, List<ItemStack> parts, Direction facing, BlockPos pos) {
        return lookedAtPart(player, parts, facing, pos, false);
    }

    /** Wie {@link #lookedAtPart(Player, List, Direction, BlockPos)}; {@code flat} wie bei {@link #shape(List, Direction, boolean)}. */
    public static int lookedAtPart(Player player, List<ItemStack> parts, Direction facing, BlockPos pos, boolean flat) {
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 reach = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        int index = partAt(parts, facing, pos, eye, reach, flat);
        return index >= 0 ? index : Math.min(parts.size(), MAX_PARTS) - 1;
    }

    /** Trefferform eines Teils: die Quader des stehenden Modells bzw. die deckenden Pixel der Item-Textur. */
    public static VoxelShape partShape(ItemStack part, Direction facing, int count, int index) {
        return partShape(part, facing, count, index, false);
    }

    /** Wie {@link #partShape(ItemStack, Direction, int, int)}; {@code ingotStack}: nur Barren und Ziegel auf dem Fleck. */
    public static VoxelShape partShape(ItemStack part, Direction facing, int count, int index, boolean ingotStack) {
        return partShape(part, facing, count, index, ingotStack, false);
    }

    /** Wie oben; {@code flat}: umschaltbare 3D-Teile liegen als Platte (Queue N55). */
    public static VoxelShape partShape(ItemStack part, Direction facing, int count, int index, boolean ingotStack, boolean flat) {
        Kind kind = kind(part, flat);
        Matrix4f m = new Matrix4f();
        place(new MatrixOps(m), facing, count, index, kind, ingotStack);
        m.translate(-0.5F, -0.5F, -0.5F);
        VoxelShape shape = Shapes.empty();
        if (kind.standing()) {
            for (float[] box : kind.boxes(part)) {
                float halfZ = box.length > 3 ? box[3] : box[2];
                float cx = box.length > 5 ? box[4] : 0.0F;
                float cz = box.length > 5 ? box[5] : 0.0F;
                shape = Shapes.or(shape, box(m, (8.0F + cx - box[2]) / 16.0F, box[0] / 16.0F, (8.0F + cz - halfZ) / 16.0F,
                        (8.0F + cx + box[2]) / 16.0F, box[1] / 16.0F, (8.0F + cz + halfZ) / 16.0F));
            }
            return shape;
        }
        short[] mask = PlacedPlate.mask(part.getItem());
        float z0 = kind == Kind.SLAB ? (8.0F - PlacedSolids.MODEL_THICKNESS / 2.0F) / 16.0F : 7.5F / 16.0F;
        float z1 = kind == Kind.SLAB ? (8.0F + PlacedSolids.MODEL_THICKNESS / 2.0F) / 16.0F : 8.5F / 16.0F;
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
                shape = Shapes.or(shape, box(m, u / 16.0F, (16 - end) / 16.0F, z0, u1 / 16.0F, (16 - v) / 16.0F, z1));
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
    public static Vector3f center(Direction facing, int count, int index, Kind kind) {
        Matrix4f m = new Matrix4f();
        place(new MatrixOps(m), facing, count, index, kind, false);
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
