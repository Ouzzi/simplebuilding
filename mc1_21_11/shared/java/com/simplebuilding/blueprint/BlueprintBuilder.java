package com.simplebuilding.blueprint;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.items.custom.BuildingWandItem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Baut eine Blaupause mit dem Baustab: Baustab in der Haupthand, Blaupause in der Nebenhand,
 * Rechtsklick auf einen Block. Nur eine <b>signierte</b> Blaupause baut (und zeigt eine Vorschau);
 * eine unsignierte meldet "Blaupause signieren, um sie zu bauen".
 *
 * <p>Ausrichtung: das Bauwerk steht vor dem Spieler, seine lokale z-Achse zeigt in Blickrichtung,
 * seine Mitte (x) liegt auf dem Zielblock, seine Unterkante auf dessen Hoehe. Blickt der Spieler
 * nach Sueden, entsteht das Bauwerk genau so, wie es gescannt wurde; Strg+Mausrad dreht es in
 * Viertelschritten dazu ({@link ModDataComponentTypes#BLUEPRINT_ROTATION}).
 *
 * <p>Gebaut wird nur, was geht: belegte Stellen und Stellen, an denen schon der richtige Block
 * steht, bleiben unberuehrt; fehlt Material, bleibt die Stelle frei (spaeter nachholen). Die
 * Materialsuche ist die des Baustabs ({@link BuildingWandItem#findSupply}), jeder gesetzte Block
 * kostet wie beim Baustab einen Haltbarkeitspunkt. Der Kreativmodus setzt alles.
 *
 * <p>Grosse Bauwerke laufen als Auftrag ueber mehrere Ticks: je Tick hoechstens
 * {@link #DEFAULT_PLACE_BUDGET} gesetzte und {@link #VISITS_PER_TICK} gepruefte Stellen, Fortschritt in
 * der Aktionsleiste. Der Auftrag laeuft weiter, solange Baustab (Haupthand) und Blaupause
 * (Nebenhand) gehalten werden; weglegen bricht ihn ab.
 */
public final class BlueprintBuilder {
    /** Gesetzte Bloecke je Tick (Spieler mit {@link BlueprintScanner#SMALL_BUDGET_TAG}: 3). */
    public static final int DEFAULT_PLACE_BUDGET = 4096;
    /** Gepruefte Stellen je Tick (Lesen ist billig, Setzen nicht). */
    public static final int VISITS_PER_TICK = 131_072;
    /** Obergrenze der Geisterbloecke in der Vorschau. */
    public static final int MAX_PREVIEW = 4096;
    /** So viele Stellen prueft die Vorschau hoechstens je Neuberechnung. */
    public static final int PREVIEW_VISITS = 200_000;

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private BlueprintBuilder() {
    }

    /** Zwischen- oder Endstand eines Bauauftrags. */
    public record Result(int placed, int already, int occupied, int blocked, int missing,
                         Map<Item, Integer> missingItems, boolean wandBroke, boolean finished, int total) {
    }

    /** Materialquelle der Planung: {@code take} bucht {@code count} Stueck ab, wenn sie reichen. */
    public interface Supply {
        boolean take(Item item, int count);
    }

    /** Setzt (Server) oder merkt sich (Vorschau) einen Block; {@code false} = nicht gesetzt. */
    public interface Placer {
        boolean place(BlockPos pos, BlockState state);

        default boolean wandBroken() {
            return false;
        }
    }

    // =====================================================================================
    // GEOMETRIE
    // =====================================================================================

    /** Drehung aus Blickrichtung (Sueden = keine) plus gespeicherte Viertelschritte im Uhrzeigersinn. */
    public static Rotation rotationFor(Direction facing, int extraSteps) {
        int base = switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        return switch (Math.floorMod(base + extraSteps, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static int rotationSteps(ItemStack blueprint) {
        return blueprint.getOrDefault(ModDataComponentTypes.BLUEPRINT_ROTATION, 0);
    }

    /** Wohin gebaut wird: in den geklickten Block, wenn er ersetzbar ist (Gras), sonst davor. */
    public static BlockPos targetFor(Level level, BlockPos clicked, Direction face) {
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
    }

    /**
     * Alle Stellen des Bauwerks in Weltkoordinaten, in Bau-Reihenfolge: zuerst alles, was etwas
     * kostet (von unten nach oben), danach die kostenlosen Gegenstuecke (obere Tuerhaelften,
     * Bettkopfteile), damit deren Partner schon steht. Schlank gespeichert (ein {@code int} je
     * Stelle), Position und Zustand werden erst beim Zugriff ausgerechnet.
     */
    public static final class Layout {
        private final BlueprintModel model;
        private final int[] order;
        private final BlockPos target;
        private final Rotation rotation;
        private final int minX, minY, minZ, half;

        Layout(BlueprintModel model, BlockPos target, Rotation rotation) {
            this.model = model;
            this.target = target;
            this.rotation = rotation;
            this.minX = model.minX();
            this.minY = model.minY();
            this.minZ = model.minZ();
            this.half = model.sizeX() / 2;
            this.order = orderOf(model);
        }

        public int size() {
            return order.length;
        }

        public BlockPos pos(int i) {
            int k = order[i];
            int lx = BlueprintModel.keyX(k) - minX - half;
            int ly = BlueprintModel.keyY(k) - minY;
            int lz = BlueprintModel.keyZ(k) - minZ;
            int rx, rz;
            switch (rotation) {
                case CLOCKWISE_90 -> { rx = -lz; rz = lx; }
                case CLOCKWISE_180 -> { rx = -lx; rz = -lz; }
                case COUNTERCLOCKWISE_90 -> { rx = lz; rz = -lx; }
                default -> { rx = lx; rz = lz; }
            }
            return target.offset(rx, ly, rz);
        }

        /** Zustand an Stelle {@code i}, schon mitgedreht. */
        public BlockState state(int i) {
            return model.blocks().get(order[i]).rotate(rotation);
        }

        /** Unveraenderter Zustand (fuer die Kosten, die sich beim Drehen nicht aendern). */
        BlockState rawState(int i) {
            return model.blocks().get(order[i]);
        }
    }

    private static final Map<BlueprintModel.Identity, int[]> ORDER_CACHE = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BlueprintModel.Identity, int[]> eldest) {
            return size() > 4;
        }
    };

    /**
     * Bau-Reihenfolge eines Modells, unabhaengig von Ziel und Drehung und deshalb je Modell nur
     * einmal berechnet (die Vorschau fragt alle vier Ticks nach einem neuen Ziel).
     */
    private static int[] orderOf(BlueprintModel model) {
        synchronized (ORDER_CACHE) {
            int[] cached = ORDER_CACHE.get(new BlueprintModel.Identity(model));
            if (cached != null) {
                return cached;
            }
        }
        IntArrayList first = new IntArrayList(model.size());
        IntArrayList partners = new IntArrayList();
        for (int k : model.sortedKeys()) {
            (BlueprintMaterials.cost(model.blocks().get(k)).count() == 0 ? partners : first).add(k);
        }
        first.addAll(partners);
        int[] order = first.toIntArray();
        synchronized (ORDER_CACHE) {
            ORDER_CACHE.put(new BlueprintModel.Identity(model), order);
        }
        return order;
    }

    public static Layout layout(BlueprintModel model, BlockPos target, Rotation rotation) {
        return new Layout(model, target, rotation);
    }

    // =====================================================================================
    // PLANUNG (Vorschau und Bau teilen sie)
    // =====================================================================================

    /** Geht die Stellen in Bau-Reihenfolge durch, Scheibe fuer Scheibe, und zaehlt mit. */
    public static final class Planner {
        private final Level level;
        private final Player player;
        private final ItemStack wand;
        private final Layout layout;
        private final Supply supply;
        private final boolean creative;
        private final LongOpenHashSet simulated;
        private int index;
        private int placed, already, occupied, blocked, missing;
        private final Map<Item, Integer> missingItems = new LinkedHashMap<>();
        private boolean broke;

        /** {@code simulate}: gesetzte Stellen merken, weil die Vorschau die Welt nicht aendert. */
        public Planner(Level level, Player player, ItemStack wand, Layout layout, Supply supply, boolean creative, boolean simulate) {
            this.level = level;
            this.player = player;
            this.wand = wand;
            this.layout = layout;
            this.supply = supply;
            this.creative = creative;
            this.simulated = simulate ? new LongOpenHashSet() : null;
        }

        public boolean finished() {
            return broke || index >= layout.size();
        }

        public int index() {
            return index;
        }

        public Result result() {
            return new Result(placed, already, occupied, blocked, missing, Map.copyOf(missingItems), broke, finished(), layout.size());
        }

        /** Bearbeitet hoechstens {@code maxVisits} Stellen und setzt hoechstens {@code maxPlaced} Bloecke. */
        public void run(int maxVisits, int maxPlaced, Placer placer) {
            int visits = 0;
            int placedHere = 0;
            while (index < layout.size() && visits < maxVisits && placedHere < maxPlaced && !broke) {
                int i = index++;
                visits++;
                BlockPos pos = layout.pos(i);
                if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.isLoaded(pos)
                        || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, wand)) {
                    blocked++;
                    continue;
                }
                BlockState state = layout.state(i);
                BlockState current = level.getBlockState(pos);
                if (current.getBlock() == state.getBlock()) {
                    already++;
                    continue;
                }
                if (!current.canBeReplaced()) {
                    occupied++;
                    continue;
                }
                BlueprintMaterials.Cost cost = BlueprintMaterials.cost(layout.rawState(i));
                if (cost.count() == 0) {
                    BlockPos partner = partnerOf(pos, state);
                    if (partner != null && !level.getBlockState(partner).is(state.getBlock())
                            && (simulated == null || !simulated.contains(partner.asLong()))) {
                        missing++;
                        continue;
                    }
                } else if (!creative) {
                    if (cost.creativeOnly() || !supply.take(cost.item(), cost.count())) {
                        missing++;
                        missingItems.merge(cost.item(), cost.count(), Integer::sum);
                        continue;
                    }
                }
                if (!creative && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    // Wasser gibt es im Ueberlebensmodus nicht umsonst dazu.
                    state = state.setValue(BlockStateProperties.WATERLOGGED, false);
                }
                if (!placer.place(pos, state)) {
                    blocked++;
                    continue;
                }
                placed++;
                placedHere++;
                if (simulated != null) {
                    simulated.add(pos.asLong());
                }
                if (placer.wandBroken()) {
                    broke = true;
                }
            }
        }
    }

    private static BlockPos partnerOf(BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        }
        return null;
    }

    /** Vorschau-Quelle: zaehlt den Vorrat einmal je Item und bucht nur in Gedanken ab. */
    public static Supply simulatedSupply(Player player, ItemStack wand) {
        Map<Item, Integer> left = new HashMap<>();
        return (item, count) -> {
            int have = left.computeIfAbsent(item, i -> BuildingWandItem.countSupply(player, wand, i));
            if (have < count) {
                return false;
            }
            left.put(item, have - count);
            return true;
        };
    }

    /** Echte Quelle: verbraucht aus Inventar, Buendeln und Rucksack wie der Baustab. */
    public static Supply realSupply(Player player, ItemStack wand) {
        return (item, count) -> {
            if (count > 1 && BuildingWandItem.countSupply(player, wand, item) < count) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                Runnable source = BuildingWandItem.findSupply(player, wand, item);
                if (source == null) {
                    return false;
                }
                source.run();
            }
            return true;
        };
    }

    // =====================================================================================
    // BAUEN (Server)
    // =====================================================================================

    public static InteractionResult useWand(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack wand = context.getItemInHand();
        ItemStack blueprint = player.getOffhandItem();
        Result result = build(level, player, wand, blueprint, context.getClickedPos(), context.getClickedFace());
        return result == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    /** Ein laufender Bauauftrag eines Spielers. */
    private static final class Job {
        final Planner planner;
        final ItemStack wand;
        final ItemStack blueprint;
        final Placer placer;
        final BlockPos target;
        final Player player;
        long lastTick;
        SoundType sound;

        Job(Planner planner, Player player, ItemStack wand, ItemStack blueprint, Placer placer, BlockPos target) {
            this.planner = planner;
            this.player = player;
            this.wand = wand;
            this.blueprint = blueprint;
            this.placer = placer;
            this.target = target;
        }
    }

    /**
     * Startet den Bau: setzt sofort die erste Scheibe und laesst den Rest (falls es einen gibt) als
     * Auftrag ueber die folgenden Ticks laufen ({@link #tick}). Liefert den Stand nach der ersten
     * Scheibe; {@code null}, wenn abgelehnt.
     */
    public static Result build(Level level, Player player, ItemStack wand, ItemStack blueprint, BlockPos clicked, Direction face) {
        if (!(wand.getItem() instanceof BuildingWandItem wandItem) || !(blueprint.getItem() instanceof BlueprintItem)) {
            return null;
        }
        BlueprintContent content = blueprint.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
        if (!content.signed()) {
            tell(player, Component.translatable("simplebuilding.blueprint.build.sign_first").withStyle(ChatFormatting.YELLOW));
            return null;
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(content.code());
        if (parsed.model().isEmpty()) {
            tell(player, Component.translatable("simplebuilding.blueprint.build.empty").withStyle(ChatFormatting.RED));
            return null;
        }
        if (!parsed.ok()) {
            tell(player, Component.translatable("simplebuilding.blueprint.build.errors", parsed.problems().size()).withStyle(ChatFormatting.RED));
            return null;
        }
        int size = parsed.model().maxEdge();
        int edge = BlueprintTiers.edgeFor(wandItem);
        if (size > edge) {
            int needed = BlueprintTiers.tierIndexFor(size);
            tell(player, Component.translatable("simplebuilding.blueprint.build.too_big", size, edge,
                    BlueprintTiers.wandName(Math.max(0, needed))).withStyle(ChatFormatting.RED));
            return null;
        }
        BlockPos target = targetFor(level, clicked, face);
        Rotation rotation = rotationFor(player.getDirection(), rotationSteps(blueprint));
        boolean creative = player.getAbilities().instabuild;
        Job[] holder = new Job[1];
        Placer placer = new Placer() {
            @Override
            public boolean place(BlockPos pos, BlockState state) {
                if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
                    return false;
                }
                if (holder[0] != null && holder[0].sound == null) {
                    holder[0].sound = state.getSoundType();
                }
                if (!creative) {
                    wand.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
                return true;
            }

            @Override
            public boolean wandBroken() {
                return wand.isEmpty();
            }
        };
        Planner planner = new Planner(level, player, wand, layout(parsed.model(), target, rotation),
                realSupply(player, wand), creative, false);
        Job job = new Job(planner, player, wand, blueprint, placer, target);
        holder[0] = job;
        JOBS.remove(player.getUUID());
        JOBS.values().removeIf(j -> j.player.isRemoved());
        job.lastTick = level.getGameTime();
        step(level, player, job);
        if (!planner.finished()) {
            JOBS.put(player.getUUID(), job);
        }
        return planner.result();
    }

    /**
     * Fuehrt den Bauauftrag des Spielers einen Tick weiter (aus {@code BuildingWandItem#inventoryTick}
     * fuer den Stab in der Haupthand). Bricht ab, wenn Stab oder Blaupause nicht mehr gehalten werden.
     */
    public static void tick(Level level, Player player, ItemStack wandInHand) {
        Job job = JOBS.get(player.getUUID());
        if (job == null || level.getGameTime() == job.lastTick) {
            return;
        }
        if (job.wand != wandInHand || player.getMainHandItem() != wandInHand || player.getOffhandItem() != job.blueprint) {
            JOBS.remove(player.getUUID());
            tell(player, Component.translatable("simplebuilding.blueprint.build.stopped", job.planner.result().placed())
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        job.lastTick = level.getGameTime();
        step(level, player, job);
        if (job.planner.finished()) {
            JOBS.remove(player.getUUID());
        }
    }

    /** Laeuft fuer diesen Spieler gerade ein Bauauftrag? */
    public static boolean building(Player player) {
        return JOBS.containsKey(player.getUUID());
    }

    private static void step(Level level, Player player, Job job) {
        int before = job.planner.result().placed();
        job.planner.run(VISITS_PER_TICK, BlueprintScanner.smallBudget(player) ? 3 : DEFAULT_PLACE_BUDGET, job.placer);
        Result result = job.planner.result();
        if (result.placed() > before && job.sound != null) {
            SoundType sound = job.sound;
            level.playSound(null, job.target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
        if (result.finished()) {
            tell(player, Component.translatable("simplebuilding.blueprint.build.done", result.placed(), result.missing())
                    .withStyle(result.missing() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        } else {
            tell(player, Component.translatable("simplebuilding.blueprint.build.progress", job.planner.index(), result.total(), result.placed())
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static void tell(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    // =====================================================================================
    // VORSCHAU (Client)
    // =====================================================================================

    private static Object previewKey;
    private static Map<BlockPos, BlockState> previewCache = Map.of();

    /**
     * Die Geisterbloecke fuer die Vorschau: genau das, was ein Klick jetzt setzen wuerde (mit
     * simuliertem Vorrat), hoechstens {@link #MAX_PREVIEW} Bloecke aus den ersten
     * {@link #PREVIEW_VISITS} Stellen. Leer, wenn die Blaupause fehlerhaft oder fuer den Stab zu
     * gross ist. Zwischengespeichert und nur alle vier Ticks neu berechnet.
     */
    public static Map<BlockPos, BlockState> preview(Level level, Player player, ItemStack wand, ItemStack blueprint, BlockHitResult hit) {
        if (!(wand.getItem() instanceof BuildingWandItem wandItem)) {
            return Map.of();
        }
        BlueprintContent content = blueprint.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
        BlockPos target = targetFor(level, hit.getBlockPos(), hit.getDirection());
        Rotation rotation = rotationFor(player.getDirection(), rotationSteps(blueprint));
        List<Object> key = List.of(content.code(), target, rotation, level.getGameTime() / 4, wand.getItem(), player.getAbilities().instabuild);
        if (Objects.equals(key, previewKey)) {
            return previewCache;
        }
        previewKey = key;
        if (!content.signed()) {
            // Gebaut wird nur eine signierte Blaupause: keine Vorschau, nur der Hinweis.
            tell(player, Component.translatable("simplebuilding.blueprint.build.sign_first").withStyle(ChatFormatting.YELLOW));
            previewCache = Map.of();
            return previewCache;
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(content.code());
        if (!parsed.ok() || parsed.model().isEmpty() || parsed.model().maxEdge() > BlueprintTiers.edgeFor(wandItem)) {
            previewCache = Map.of();
            return previewCache;
        }
        boolean creative = player.getAbilities().instabuild;
        Map<BlockPos, BlockState> map = new LinkedHashMap<>();
        Planner planner = new Planner(level, player, wand, layout(parsed.model(), target, rotation),
                simulatedSupply(player, wand), creative, true);
        planner.run(PREVIEW_VISITS, MAX_PREVIEW, (pos, state) -> {
            map.put(pos, state);
            return true;
        });
        previewCache = map;
        return map;
    }
}
