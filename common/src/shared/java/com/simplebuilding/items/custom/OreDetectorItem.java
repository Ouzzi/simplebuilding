package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.simplebuilding.blocks.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class OreDetectorItem extends Item {

    // =====================================================================================
    // STELLSCHRAUBEN - die einzige Stelle, an der der Detektor abgestimmt wird (2026-09-25)
    // =====================================================================================

    /**
     * Signal und Reichweite je Seltenheitsklasse des GEFUNDENEN Erzes (nicht des Modus).
     *
     * <p>Ein Erz wird gefunden, wenn es hoechstens {@link #range} Bloecke vom Augenblock entfernt
     * liegt UND sein Signal den Weg dorthin uebersteht: jeder Block, den die Sichtlinie zwischen
     * Augenblock und Erz schneidet, zieht den Verlust seines {@link Material}s ab (Augenblock und
     * Erz selbst zaehlen nicht). Die Reichweite ist zugleich der Radius der Suchkugel, also die
     * groesste Entfernung, ueber die der Detektor selbst durch offene Hoehlen etwas meldet.
     * Radius (dieselbe Verzauberung wie am Vorschlaghammer) hebt beides an, das Signal der
     * seltenen Klassen am staerksten.
     * <pre>
     *   Klasse     Erze                                          Signal Reichweite | mit Radius
     *   COMMON     Kohle, Kupfer, Eisen, Redstone, Lapis, Quarz    18      24      |  22    28
     *   MEDIUM     Gold (auch Nethergold)                          13      20      |  17    24
     *   RARE       Diamant, Smaragd                                 9      16      |  17    24
     *   VERY_RARE  Antiker Schrott, Astralit-, Nihiliterz           5      16      |   9    20
     * </pre>
     * Mit den Verlusten aus {@link Material} heisst das ohne Radius: gewoehnliche Erze durch 4
     * Stein, Gold durch 3, Diamant und Smaragd durch 2, Antiker Schrott durch 2 Netherrack (die
     * End-Erze durch 2 Endstein) oder 1 Stein - jeweils mit bis zu 8 Bloecken Luft dazu. Mit
     * Radius: 5 / 4 / 4 Stein und 4 Netherrack. Ein kalibrierter Block, der keines dieser Erze
     * ist, zaehlt als COMMON.
     */
    public enum OreClass {
        COMMON(18, 24, 22, 28),
        MEDIUM(13, 20, 17, 24),
        RARE(9, 16, 17, 24),
        VERY_RARE(5, 16, 9, 20);

        private final int signal;
        private final int range;
        private final int signalWithRadius;
        private final int rangeWithRadius;

        OreClass(int signal, int range, int signalWithRadius, int rangeWithRadius) {
            this.signal = signal;
            this.range = range;
            this.signalWithRadius = signalWithRadius;
            this.rangeWithRadius = rangeWithRadius;
        }

        /** Wie viel Materialverlust das Signal dieser Klasse verkraftet. */
        public int signal(boolean radius) {
            return radius ? signalWithRadius : signal;
        }

        /** Groesste Entfernung in Bloecken (Augenblock bis Erz), auch durch reine Luft. */
        public int range(boolean radius) {
            return radius ? rangeWithRadius : range;
        }
    }

    /**
     * Signalverlust je Block, den die Sichtlinie schneidet. Welche Stufe ein Block hat, entscheidet
     * {@link #material}: Luft und alles, was nicht verdeckt (Glas, Laub, Wasser), ist fast frei;
     * sonst die Abbauhaerte des Blocks, mit Endstein als einziger Ausnahme.
     * <pre>
     *   AIR         0.125   Luft, nicht verdeckende Bloecke, Fluessigkeiten
     *   SOFT        2       Haerte unter 1.0 (Netherrack, Erde, Sand, Kies) und Endstein
     *   STONE       4       Haerte 1.0 bis unter 3.0 (Stein, Bruchstein, Tuff, Basalt, Schwarzstein, Holz)
     *   DENSE       6       Haerte 3.0 bis unter 10 (Tiefenschiefer, Erze)
     *   VERY_DENSE  16      Haerte ab 10 oder unzerstoerbar (Obsidian, Antiker Schrott, Grundgestein)
     * </pre>
     * Endstein zaehlt als SOFT, obwohl er Haerte 3.0 hat: er ist das Wirtsgestein der End-Erze wie
     * Netherrack das des Antiken Schrotts, und die End-Erze sollen sich genauso anfuehlen.
     */
    public enum Material {
        AIR(0.125),
        SOFT(2.0),
        STONE(4.0),
        DENSE(6.0),
        VERY_DENSE(16.0);

        public final double loss;

        Material(double loss) {
            this.loss = loss;
        }
    }

    /** Untere Haertegrenzen der Stufen STONE, DENSE und VERY_DENSE, siehe {@link Material}. */
    private static final float STONE_HARDNESS = 1.0f;
    private static final float DENSE_HARDNESS = 3.0f;
    private static final float VERY_DENSE_HARDNESS = 10.0f;

    /** Konstrukteurs Beruehrung: jeder Blockverlust zaehlt nur so viel, das Signal traegt doppelt so weit durch Gestein. */
    public static final double TOUCH_LOSS_FACTOR = 0.5;

    // =====================================================================================

    private static final int SCAN_INTERVAL = 20;   // Ping alle 1 Sekunde

    /** {@code oreClass == null}: der Modus findet Erze mehrerer Klassen (ALL) oder den kalibrierten Block (CUSTOM). */
    private enum DetectMode {
        IRON(ChatFormatting.GRAY, "Iron", OreClass.COMMON),
        GOLD(ChatFormatting.GOLD, "Gold", OreClass.MEDIUM),
        DIAMOND(ChatFormatting.AQUA, "Diamond", OreClass.RARE),
        NETHERITE(ChatFormatting.DARK_PURPLE, "Netherite", OreClass.VERY_RARE),
        ALL(ChatFormatting.WHITE, "All Ores", null),
        CUSTOM(ChatFormatting.YELLOW, "Custom", null);

        final ChatFormatting color;
        final String name;
        final @Nullable OreClass oreClass;

        DetectMode(ChatFormatting color, String name, @Nullable OreClass oreClass) {
            this.color = color;
            this.name = name;
            this.oreClass = oreClass;
        }

        /** Suchradius: die groesste Reichweite, die ein Treffer dieses Modus haben kann. */
        int scanRadius(@Nullable BlockState customTarget, boolean radius) {
            if (oreClass != null) return oreClass.range(radius);
            if (this == CUSTOM && customTarget != null) return classify(customTarget).range(radius);
            return OreClass.COMMON.range(radius);
        }
    }

    public OreDetectorItem(Properties settings) {
        super(settings.stacksTo(1).durability(1024));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        if (!isHeldInHands(slot)) return;

        // Jeder gehaltene Detektor kostet einen vollen Kugelscan. Ohne Versatz faellt der Scan
        // aller Spieler auf denselben Tick, sodass ein Server die gesamte Suchlast jede Sekunde
        // gebuendelt in einen einzigen Tick bekommt. Der Versatz je Spieler verteilt sie ueber
        // die Sekunde; die Taktrate bleibt exakt SCAN_INTERVAL, nur die Phase haengt jetzt am
        // Spieler - und die ist nirgends beobachtbar, weil der Detektor mit nichts synchron laeuft.
        if (Math.floorMod(world.getGameTime() + player.getId(), SCAN_INTERVAL) != 0) return;

        BlockPos playerPos = BlockPos.containing(player.getEyePosition());

        // 2. Suche
        BlockPos targetPos = findTarget(world, stack, player.getEyePosition());

        if (targetPos != null) {
            BlockState targetState = world.getBlockState(targetPos);
            double distance = Math.sqrt(playerPos.distSqr(targetPos));
            float pitch = getPingPitch(distance);

            world.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, pitch);
            world.playSound(null, targetPos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.6f, 2.0f);
            SoundEvent blockSound = targetState.getSoundType().getBreakSound();
            world.playSound(null, targetPos, blockSound, SoundSource.BLOCKS, 0.55f, pitch);

            spawnSonarBeam(world, player.getEyePosition(), targetPos, targetState);
        }
    }

    private static boolean isHeldInHands(@Nullable EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    /**
     * Der Block, auf den dieser Detektor mit {@code stack} von {@code eyesPos} aus anschlagen
     * wuerde, oder {@code null}, wenn nichts zugleich in Reichweite und erreichbar ist.
     *
     * <p>Aus {@link #inventoryTick} herausgezogen, damit die Suche ueberhaupt pruefbar ist:
     * alles, was der Tick mit dem Ergebnis noch anstellt, sind Toene und Partikel, und die
     * verschluckt die Verbindung eines Test-Spielers. Der Tick ruft weiterhin nur diese
     * Methode, es gibt also keinen zweiten Suchpfad. Siehe {@code OreDetectorTests}.
     */
    @Nullable
    public BlockPos findTarget(ServerLevel world, ItemStack stack, Vec3 eyesPos) {
        return findTarget(world, world, stack, eyesPos);
    }

    /**
     * Dieselbe Suche auf einer beliebigen Blockquelle: {@code blocks} liefert die Bloecke,
     * {@code level} nur Verzauberungen und Registry. Die Server-Variante oben reicht beide Male
     * die Welt durch; die Tests geben hier eine eigene Blockquelle, um Sichtlinien ueber 20 und
     * mehr Bloecke zu legen, die in keinen Testraum passen.
     */
    @Nullable
    public BlockPos findTarget(BlockGetter blocks, Level level, ItemStack stack, Vec3 eyesPos) {
        double lossFactor = hasEnchantment(stack, level, ModEnchantments.CONSTRUCTORS_TOUCH) ? TOUCH_LOSS_FACTOR : 1.0;
        boolean radius = hasEnchantment(stack, level, ModEnchantments.RADIUS);
        DetectMode mode = getMode(stack);
        BlockState customTarget = (mode == DetectMode.CUSTOM) ? getCustomBlock(stack, level.registryAccess()) : null;
        return findNearestReachable(blocks, eyesPos, mode, customTarget, lossFactor, radius);
    }

    private static float getPingPitch(double distance) {
        float pitch = (float) (1.8f - (distance / 32.0f));
        return Math.max(0.6f, Math.min(2.0f, pitch));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (user.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                ItemStack stack = user.getItemInHand(hand);
                cycleMode(stack, user, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level world = context.getLevel();
            if (!world.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                BlockState state = world.getBlockState(context.getClickedPos());

                setMode(stack, DetectMode.CUSTOM);
                setCustomBlock(stack, state);

                context.getPlayer().sendOverlayMessage(Component.literal("Calibrated to: ").withStyle(ChatFormatting.GREEN)
                        .append(state.getBlock().getName().copy().withStyle(ChatFormatting.WHITE)));

                world.playSound(null, context.getClickedPos(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    private static BlockPos findNearestReachable(BlockGetter world, Vec3 eyesPos, DetectMode mode,
                                                 @Nullable BlockState customTarget, double lossFactor, boolean radius) {
        BlockPos origin = BlockPos.containing(eyesPos);

        int scanRadius = mode.scanRadius(customTarget, radius);
        double maxScanDistanceSq = scanRadius * scanRadius;
        BlockPos bestTarget = null;
        double bestDistanceSq = Double.MAX_VALUE;

        // Ein einziger wandernder Cursor statt eines BlockPos je Wuerfelzelle: bei Radius 28 sind
        // das 185.193 Zellen pro Scan und pro gehaltenem Detektor. Der Cursor wird bei jedem
        // Schritt ueberschrieben, darf also weder weitergereicht noch behalten werden - siehe das
        // immutable() unten. Weitergereicht wird er nur an getBlockState und pathLoss, und beide
        // lesen ihn bloss.
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    // Derselbe Wert, den origin.distSqr(origin.offset(x, y, z)) lieferte: die drei
                    // Differenzen sind genau -x, -y und -z, und das Quadrat verliert das Vorzeichen.
                    // Vorgezogen, damit die knapp halbe Wuerfelecke ausserhalb der Kugel gar nicht
                    // erst in den Cursor geschrieben wird.
                    double distanceSq = x * x + y * y + z * z;
                    if (distanceSq > maxScanDistanceSq || distanceSq >= bestDistanceSq) continue;

                    checkPos.setWithOffset(origin, x, y, z);

                    BlockState state = world.getBlockState(checkPos);
                    if (!isTarget(state, mode, customTarget)) continue;

                    // Reichweite und Signal des gefundenen Erzes: im ALL-Modus sieht derselbe
                    // Detektor Eisen weiter und durch mehr Gestein als Diamant.
                    OreClass oreClass = classify(state);
                    int range = oreClass.range(radius);
                    if (distanceSq > (double) range * range) continue;

                    int signal = oreClass.signal(radius);
                    if (pathLoss(world, eyesPos, checkPos, lossFactor, signal) <= signal) {
                        // Der naechste Schleifenschritt ueberschreibt den Cursor; behalten werden
                        // darf nur eine Kopie.
                        bestTarget = checkPos.immutable();
                        bestDistanceSq = distanceSq;
                    }
                }
            }
        }

        return bestTarget;
    }

    /**
     * Was das Signal auf dem Weg von {@code start} zur Mitte von {@code target} verliert: die
     * Summe der {@link Material}-Verluste jedes Blocks, den die Sichtlinie schneidet, mal
     * {@code lossFactor}. Der Block von {@code start} und das Ziel selbst zaehlen nicht. Sobald
     * die Summe {@code limit} uebersteigt, bricht die Rechnung ab und liefert den Zwischenstand.
     *
     * <p>Die Bloecke werden exakt abgelaufen (Amanatides-Woo): die Linie endet in der Mitte des
     * Ziels, kreuzt also genau so viele Blockgrenzen, wie Augen- und Zielblock in x, y und z
     * zusammen auseinanderliegen; alle Bloecke davor sind die dazwischen. Laeuft sie genau durch
     * eine Kante, zaehlt einer der beiden angrenzenden Bloecke.
     */
    public static double pathLoss(BlockGetter world, Vec3 start, BlockPos target, double lossFactor, double limit) {
        // MC 26.2: BlockPos.getCenter() entfiel; Vec3.atCenterOf(Vec3i) ist der identische Ersatz.
        Vec3 end = Vec3.atCenterOf(target);
        int x = Mth.floor(start.x);
        int y = Mth.floor(start.y);
        int z = Mth.floor(start.z);
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;
        double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
        double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
        double deltaZ = dz == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);
        double nextX = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? x + 1 - start.x : start.x - x) * deltaX;
        double nextY = dy == 0 ? Double.POSITIVE_INFINITY : (dy > 0 ? y + 1 - start.y : start.y - y) * deltaY;
        double nextZ = dz == 0 ? Double.POSITIVE_INFINITY : (dz > 0 ? z + 1 - start.z : start.z - z) * deltaZ;
        int crossings = Math.abs(target.getX() - x) + Math.abs(target.getY() - y) + Math.abs(target.getZ() - z);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double loss = 0;
        for (int i = 1; i < crossings; i++) {
            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                y += stepY;
                nextY += deltaY;
            } else {
                z += stepZ;
                nextZ += deltaZ;
            }
            cursor.set(x, y, z);
            loss += lossFactor * material(world, cursor, world.getBlockState(cursor)).loss;
            if (loss > limit) return loss;
        }
        return loss;
    }

    /** Die Verluststufe eines Blocks, siehe {@link Material}. */
    public static Material material(BlockGetter world, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.canOcclude()) return Material.AIR;
        if (state.is(Blocks.END_STONE)) return Material.SOFT;
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0 || hardness >= VERY_DENSE_HARDNESS) return Material.VERY_DENSE;
        if (hardness >= DENSE_HARDNESS) return Material.DENSE;
        if (hardness >= STONE_HARDNESS) return Material.STONE;
        return Material.SOFT;
    }

    // MC 26.2: Die gepaarten Block-/Item-Tags liegen jetzt in BlockItemTags (BlockItemTagId).
    // BlockTags behielt nur noch die von Vanilla-Code benutzten Erz-Konstanten; die uebrigen
    // (coal/lapis/redstone/diamond/emerald) sind dort weggefallen. BlockItemTags.X.block() liefert
    // exakt denselben TagKey wie frueher BlockTags.X (BlockTags initialisiert sich selbst daraus),
    // die Tag-IDs und -Inhalte in data/minecraft/tags/block sind unveraendert.
    private static boolean isTarget(BlockState state, DetectMode mode, @Nullable BlockState customTarget) {
        return switch (mode) {
            case IRON -> state.is(BlockItemTags.IRON_ORES.block());
            case GOLD -> state.is(BlockItemTags.GOLD_ORES.block());
            case DIAMOND -> state.is(BlockItemTags.DIAMOND_ORES.block());
            case NETHERITE -> state.is(Blocks.ANCIENT_DEBRIS);
            case ALL -> state.is(BlockItemTags.COAL_ORES.block()) || state.is(BlockItemTags.IRON_ORES.block()) ||
                    state.is(BlockItemTags.COPPER_ORES.block()) || state.is(BlockItemTags.GOLD_ORES.block()) ||
                    state.is(BlockItemTags.REDSTONE_ORES.block()) || state.is(BlockItemTags.LAPIS_ORES.block()) ||
                    state.is(BlockItemTags.DIAMOND_ORES.block()) || state.is(BlockItemTags.EMERALD_ORES.block()) ||
                    state.is(Blocks.ANCIENT_DEBRIS) || state.is(Blocks.NETHER_QUARTZ_ORE) ||
                    state.is(ModBlocks.ASTRALIT_ORE) || state.is(ModBlocks.NIHILITH_ORE);
            case CUSTOM -> customTarget != null && state.is(customTarget.getBlock());
        };
    }

    /** Seltenheitsklasse eines Blocks, siehe {@link OreClass}. */
    public static OreClass classify(BlockState state) {
        if (state.is(Blocks.ANCIENT_DEBRIS) || state.is(ModBlocks.ASTRALIT_ORE) || state.is(ModBlocks.NIHILITH_ORE)) {
            return OreClass.VERY_RARE;
        }
        if (state.is(BlockItemTags.DIAMOND_ORES.block()) || state.is(BlockItemTags.EMERALD_ORES.block())) return OreClass.RARE;
        if (state.is(BlockItemTags.GOLD_ORES.block())) return OreClass.MEDIUM;
        return OreClass.COMMON;
    }

    /**
     * Farbe des Randschimmers im Inventar (RGB, ohne Alpha) oder {@code -1}, wenn der Detektor
     * keinen Block ausgewaehlt hat. Nur der kalibrierte Modus waehlt einen Block aus; Erze bekommen
     * die Farbe ihres Minerals, alles andere die Kartenfarbe des Blocks.
     */
    public static int targetColor(ItemStack stack) {
        if (!(stack.getItem() instanceof OreDetectorItem) || getMode(stack) != DetectMode.CUSTOM) return -1;
        Block block = getCustomTargetBlock(stack);
        return block == null ? -1 : blockColor(block.defaultBlockState());
    }

    static int blockColor(BlockState state) {
        if (state.is(ModBlocks.ASTRALIT_ORE)) return 0xE49DD6;
        if (state.is(ModBlocks.NIHILITH_ORE)) return 0x7BB4B8;
        if (state.is(Blocks.ANCIENT_DEBRIS)) return 0x9A6A58;
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return 0xEAE4DC;
        if (state.is(BlockItemTags.DIAMOND_ORES.block())) return 0x5DECF5;
        if (state.is(BlockItemTags.EMERALD_ORES.block())) return 0x17DD62;
        if (state.is(BlockItemTags.GOLD_ORES.block())) return 0xFCEE4B;
        if (state.is(BlockItemTags.IRON_ORES.block())) return 0xD8AF93;
        if (state.is(BlockItemTags.COPPER_ORES.block())) return 0xE0734D;
        if (state.is(BlockItemTags.REDSTONE_ORES.block())) return 0xFF3A2A;
        if (state.is(BlockItemTags.LAPIS_ORES.block())) return 0x3F6FDB;
        if (state.is(BlockItemTags.COAL_ORES.block())) return 0x5C5C5C;
        int map = state.getBlock().defaultMapColor().col;
        return map == 0 ? 0xA0A0A0 : map;
    }

    /**
     * Der kalibrierte Block, direkt aus dem gespeicherten Namen gelesen - ohne Registry-Lookup,
     * damit ihn auch der Inventar-Renderer jedes Bild billig fragen kann.
     */
    @Nullable
    private static Block getCustomTargetBlock(ItemStack stack) {
        CompoundTag nbt = getCustomData(stack);
        if (!nbt.contains("CustomBlock")) return null;
        String name = nbt.getCompoundOrEmpty("CustomBlock").getStringOr("Name", "");
        Identifier id = Identifier.tryParse(name);
        if (id == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    private void spawnSonarBeam(ServerLevel world, Vec3 startPos, BlockPos endPos, BlockState targetState) {
        // MC 26.2: siehe pathLoss() — BlockPos.getCenter() -> Vec3.atCenterOf(Vec3i)
        Vec3 targetCenter = Vec3.atCenterOf(endPos);
        Vec3 direction = targetCenter.subtract(startPos).normalize();
        double distance = startPos.distanceTo(targetCenter);

        double stepSize = 0.4;
        for (double d = 0.5; d < distance; d += stepSize) {
            Vec3 p = startPos.add(direction.scale(d));
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, targetState),
                    p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        world.sendParticles(ParticleTypes.END_ROD, targetCenter.x, targetCenter.y, targetCenter.z, 3, 0.1, 0.1, 0.1, 0.02);
        world.sendParticles(ParticleTypes.WAX_ON, targetCenter.x, targetCenter.y, targetCenter.z, 5, 0.3, 0.3, 0.3, 0.05);

    }

    private void cycleMode(ItemStack stack, Player player, InteractionHand hand) {
        DetectMode current = getMode(stack);
        DetectMode[] modes = DetectMode.values();
        DetectMode next = modes[(current.ordinal() + 1) % modes.length];
        setMode(stack, next);

        player.sendOverlayMessage(Component.literal("Detector Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(next.name).withStyle(next.color)));

        // Server side only, so Player#playSound - which leaves out the player it is called on -
        // would reach everyone but the one who switched the mode.
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), player.getSoundSource(), 0.5f, 1.5f);

        if (!player.isCreative()) {
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
    }

    private static DetectMode getMode(ItemStack stack) {
        CompoundTag nbt = getCustomData(stack);
        int modeIndex = 0;

        if (nbt.contains("Mode")) {
            modeIndex = nbt.getIntOr("Mode", 0);
        }

        modeIndex = Math.max(0, Math.min(DetectMode.values().length - 1, modeIndex));

        return DetectMode.values()[modeIndex];
    }

    private void setMode(ItemStack stack, DetectMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> nbt.putInt("Mode", mode.ordinal()));
    }

    private void setCustomBlock(ItemStack stack, BlockState state) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt ->
                nbt.put("CustomBlock", NbtUtils.writeBlockState(state))
        );
    }

    private static BlockState getCustomBlock(ItemStack stack, HolderLookup.Provider registryLookup) {
        if (registryLookup == null) return null;

        CompoundTag nbt = getCustomData(stack);
        if (nbt.contains("CustomBlock")) {
            var blockRegistry = registryLookup.lookupOrThrow(Registries.BLOCK);
            try {
                return NbtUtils.readBlockState(blockRegistry, nbt.getCompoundOrEmpty("CustomBlock"));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        DetectMode mode = getMode(stack);
        textConsumer.accept(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(mode.name).withStyle(mode.color)));

        if (mode == DetectMode.CUSTOM) {
            BlockState custom = getCustomBlock(stack, context.registries());
            if (custom != null) {
                textConsumer.accept(Component.literal("Target: ").withStyle(ChatFormatting.GRAY)
                        .append(custom.getBlock().getName().copy().withStyle(ChatFormatting.GREEN)));
            } else {
                textConsumer.accept(Component.literal("Target: None (Sneak-Use on block)").withStyle(ChatFormatting.RED));
            }
        } else {
            textConsumer.accept(Component.literal("Sneak + Use to cycle modes").withStyle(ChatFormatting.DARK_GRAY));
        }

        textConsumer.accept(Component.empty());
        boolean radius = stack.getEnchantments().keySet().stream().anyMatch(h -> h.is(ModEnchantments.RADIUS));
        BlockState custom = mode == DetectMode.CUSTOM ? getCustomBlock(stack, context.registries()) : null;
        OreClass shown = mode.oreClass != null ? mode.oreClass
                : (custom != null ? classify(custom) : OreClass.COMMON);
        textConsumer.accept(Component.literal("Signal: " + shown.signal(radius) + ", Range: " + shown.range(radius))
                .withStyle(ChatFormatting.DARK_AQUA));
        if (mode == DetectMode.ALL) {
            textConsumer.accept(Component.literal("Gold " + describe(OreClass.MEDIUM, radius)
                    + ", Diamond/Emerald " + describe(OreClass.RARE, radius)
                    + ", Debris/End ores " + describe(OreClass.VERY_RARE, radius)).withStyle(ChatFormatting.DARK_AQUA));
        }
        textConsumer.accept(Component.literal("Rock damps the signal, dense rock more.").withStyle(ChatFormatting.GRAY));
    }

    private static String describe(OreClass oreClass, boolean radius) {
        return oreClass.signal(radius) + "/" + oreClass.range(radius);
    }
}
