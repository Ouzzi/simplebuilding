package com.simplebuilding.util;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedBundleBlock;
import com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Abgestellte Buendel (Besitzer 2026-09-28): Schleichen + Rechtsklick mit einem Buendel auf die
 * <em>Oberseite</em> eines Blocks stellt es als 3D-Buendel ab ({@link PlacedBundleBlock}); an Waenden,
 * Decken und ohne Schleichen bleibt alles beim Alten. Abstellbar sind die drei Buendel der Mod
 * (verstaerkt, Netherit, Enderit, auch gefaerbt) und Vanillas Buendel in allen 17 Farben - die
 * farbigen Vanilla-Buendel nehmen das Vanilla-Modell mit getoenter Leder-Ebene. Koecher nicht: die
 * sind Buendel nur dem Code nach.
 *
 * <p>Wer schleichend auf ein abgestelltes Buendel schaut, sieht dessen "oberstes" Item darueber
 * schweben, immer zur eigenen Kamera gedreht (Besitzer 2026-09-29). Welches Item oben liegt, waehlt
 * der Spieler selbst: Schleichen + Mausrad schaltet reihum weiter ({@link #scroll}, ueber
 * {@code PlacedBundleScrollPayload} auf dem Server, der den Index besitzt; die Hotbar bleibt dabei
 * stehen). Rechtsklick nimmt genau das gezeigte Item heraus ({@link #takeShown}) - in die leere Hand,
 * sonst ins Inventar, sonst vor die Fuesse. Schleichen + Rechtsklick mit einem Item in der Hand legt
 * es hinein ({@link #deposit}), nach den Regeln des Buendels (Kapazitaet der Stufe, Verzauberungen,
 * Drawer, was ueberhaupt in ein Buendel darf).
 */
public final class PlacedBundles {
    /** Zusaetzliche Reichweite (Bloecke) fuer das Mausrad-Paket, wie Vanillas Toleranz beim Benutzen. */
    public static final double SCROLL_RANGE_SLACK = 1.0;

    /**
     * Die Farben der farbigen Vanilla-Buendel (Vanillas {@code DyeColor#getTextureDiffuseColor}, hier
     * als Tabelle: auf 26.4 hat {@code DyeColor} keine Farbwerte mehr).
     */
    private static final Map<String, Integer> VANILLA_COLOURS = Map.ofEntries(
            Map.entry("white_bundle", 0xF9FFFE), Map.entry("orange_bundle", 0xF9801D),
            Map.entry("magenta_bundle", 0xC74EBD), Map.entry("light_blue_bundle", 0x3AB3DA),
            Map.entry("yellow_bundle", 0xFED83D), Map.entry("lime_bundle", 0x80C71F),
            Map.entry("pink_bundle", 0xF38BAA), Map.entry("gray_bundle", 0x474F52),
            Map.entry("light_gray_bundle", 0x9D9D97), Map.entry("cyan_bundle", 0x169C9C),
            Map.entry("purple_bundle", 0x8932B8), Map.entry("blue_bundle", 0x3C44AA),
            Map.entry("brown_bundle", 0x835432), Map.entry("green_bundle", 0x5E7C16),
            Map.entry("red_bundle", 0xB02E26), Map.entry("black_bundle", 0x1D1D21));

    private PlacedBundles() {
    }

    // =====================================================================================
    // Was sich abstellen laesst
    // =====================================================================================

    /** Die Modell-Stufe eines abstellbaren Buendels, oder null fuer alles andere (auch Koecher). */
    public static PlacedBundleBlock.@Nullable Tier tierOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == ModItems.REINFORCED_BUNDLE) {
            return PlacedBundleBlock.Tier.REINFORCED;
        }
        if (item == ModItems.NETHERITE_BUNDLE) {
            return PlacedBundleBlock.Tier.NETHERITE;
        }
        if (item == ModItems.ENDERITE_BUNDLE) {
            return PlacedBundleBlock.Tier.ENDERITE;
        }
        if (item instanceof BundleItem && "minecraft".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
            return PlacedBundleBlock.Tier.BUNDLE;
        }
        return null;
    }

    /**
     * Die Farbe (0xRRGGBB) der Leder-Ebene: die Komponente {@code dyed_color}, sonst die Farbe eines
     * farbigen Vanilla-Buendels, sonst {@link DyedStorage#UNDYED}.
     */
    public static int dyeColor(ItemStack stack) {
        if (stack.isEmpty()) {
            return DyedStorage.UNDYED;
        }
        int dyed = DyedStorage.colour(stack);
        if (dyed != DyedStorage.UNDYED) {
            return dyed;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Integer vanilla = "minecraft".equals(id.getNamespace()) ? VANILLA_COLOURS.get(id.getPath()) : null;
        return vanilla == null ? DyedStorage.UNDYED : vanilla;
    }

    // =====================================================================================
    // Inhalt
    // =====================================================================================

    /** Der Inhalt als Kopien, oberstes (zuletzt eingelegtes) Item zuerst. */
    public static List<ItemStack> contents(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? List.of() : contents.items().stream().map(ItemStackTemplate::create).toList();
    }

    /** Setzt den Inhalt (Reihenfolge wie {@link #contents}). */
    public static void setContents(ItemStack bundle, List<ItemStack> items) {
        List<ItemStackTemplate> templates = new ArrayList<>();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                templates.add(ItemStackTemplate.fromNonEmptyStack(item));
            }
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(templates));
    }

    // =====================================================================================
    // Abstellen
    // =====================================================================================

    /**
     * Rechtsklick eines Buendels auf einen Block (aus {@code Item#useOn} bzw.
     * {@code ReinforcedBundleItem#useOn}). Null, wenn nichts abgestellt wird - dann laeuft das
     * gewohnte Verhalten des Buendels weiter.
     */
    public static @Nullable InteractionResult tryPlace(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isSecondaryUseActive() || context.getClickedFace() != Direction.UP
                || tierOf(stack) == null || !player.mayBuild()) {
            return null;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        BlockState state = ModBlocks.PLACED_BUNDLE.getStateForPlacement(place);
        if (state == null) {
            return null;
        }
        if (!level.isClientSide()) {
            if (!level.setBlock(pos, state, 11)) {
                return null;
            }
            if (level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity be) {
                be.setBundle(stack.copyWithCount(1));
            }
            SoundType sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    // =====================================================================================
    // Anschauen und Herausnehmen
    // =====================================================================================

    /**
     * Schleichen + Mausrad auf ein abgestelltes Buendel (aus {@code PlacedBundleScrollPayload}): das
     * gezeigte Item rueckt um einen Schritt weiter ({@code step} &gt; 0, Rad nach unten wie in der
     * Hotbar) oder zurueck. Nur fuer einen schleichenden Spieler in Reichweite und nur bei mindestens
     * zwei Items. Liefert true, wenn weitergeschaltet wurde.
     */
    public static boolean scroll(Player player, BlockPos pos, int step) {
        Level level = player.level();
        if (step == 0 || !player.isAlive() || player.isSpectator() || !player.isShiftKeyDown()
                || !level.isLoaded(pos) || !player.isWithinBlockInteractionRange(pos, SCROLL_RANGE_SLACK)
                || !(level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity be) || be.contents().size() < 2) {
            return false;
        }
        be.cycle(Integer.signum(step));
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.4F, 1.4F + level.getRandom().nextFloat() * 0.2F);
        return true;
    }

    /**
     * Schleichen + Rechtsklick mit {@code held} (Haupthand) auf das abgestellte Buendel: so viel wie
     * hineinpasst kommt hinein und wird oben gezeigt. Die Buendel der Mod rechnen wie beim Einsammeln
     * ({@link ReinforcedBundleItem#tryInsertStackFromWorld}: Stufe, Deep Pockets, Drawer), Vanillas
     * Buendel wie Vanilla ({@code BundleContents.Mutable#tryInsert}). Liefert die Zahl der
     * hineingelegten Items; {@code held} schrumpft um genau so viele.
     */
    public static int deposit(Level level, BlockPos pos, PlacedBundleBlockEntity be, Player player, ItemStack held) {
        if (held.isEmpty() || be.getBundle().isEmpty()) {
            return 0;
        }
        ItemStack bundle = be.getBundle().copy();
        int before = held.getCount();
        boolean modBundle = bundle.getItem() instanceof ReinforcedBundleItem;
        if (bundle.getItem() instanceof ReinforcedBundleItem reinforced) {
            // Spielt selbst Vanillas Einlege-Ton am Spieler.
            reinforced.tryInsertStackFromWorld(bundle, held, player);
        } else {
            BundleContents contents = bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            BundleContents.Mutable mutable = McVersion.bundleMutable(contents);
            if (mutable.tryInsert(held) > 0) {
                bundle.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            }
        }
        int inserted = before - held.getCount();
        if (inserted > 0) {
            be.setBundle(bundle);
            be.setShownIndex(0);
            if (!modBundle) {
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            }
        } else {
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return inserted;
    }

    /**
     * Der Haken fuer Schleichen + Rechtsklick mit einem Item (aus {@code ItemStack#useOn}, weil
     * Vanilla den Block beim Schleichen mit Item in der Hand gar nicht fragt). Null, wenn das Ziel kein
     * abgestelltes Buendel ist oder nicht geschlichen wird - dann laeuft das Item normal weiter.
     */
    public static @Nullable InteractionResult tryDeposit(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack held = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || held.isEmpty() || context.getHand() != InteractionHand.MAIN_HAND
                || !player.isSecondaryUseActive() || !(level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity be)) {
            return null;
        }
        if (!level.isClientSide()) {
            deposit(level, pos, be, player, held);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Nimmt das gezeigte Item (den ganzen Stapel dieses Eintrags) aus dem Buendel: in die leere
     * Haupthand, sonst ins Inventar, was dort keinen Platz hat, faellt vor die Fuesse. Das naechste
     * Item rueckt an dieselbe Stelle. Liefert das herausgenommene Item (leer, wenn nichts drin war).
     */
    public static ItemStack takeShown(Level level, BlockPos pos, PlacedBundleBlockEntity be, Player player) {
        List<ItemStack> items = new ArrayList<>(be.contents());
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = be.shownIndex();
        ItemStack taken = items.remove(index);
        ItemStack bundle = be.getBundle().copy();
        setContents(bundle, items);
        be.setBundle(bundle);
        be.setShownIndex(index);
        ItemStack given = taken.copy();
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, given);
        } else if (!player.getInventory().add(given) && !given.isEmpty()) {
            McVersion.drop(player, given, false, false);
        }
        level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        return taken;
    }
}
