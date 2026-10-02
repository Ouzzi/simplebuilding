package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Messuhr / Gauge (Registry-Id {@code velocity_gauge} bleibt; bis 2026-09-29 "Geschwindigkeitsmesser").
 * Die Anzeige selbst ist das HUD ({@code client.gui.SpeedometerHudOverlay}) und die Nadel auf dem
 * Item (Modell-Eigenschaft {@code simplebuilding:gauge_needle}, {@link #needleFraction}); hier nur die
 * Rechnungen, die Server-Tests pruefen koennen, und der Tooltip.
 *
 * <p>Hoehenmesser (2026-09-29): in der Luft zeigt die Messuhr den Abstand zum Boden unter den Fuessen
 * und - faellt man - wie viel Fallschaden eine Landung jetzt kaeme ({@link #impactDamage}, ohne
 * Ruestung/Federfall gerechnet, also eine Obergrenze). Reicht der Boden weiter hinab als
 * {@link #altimeterRange}, steht dort "tief". Die Verzauberung Reichweite verlaengert den Messstrahl
 * ({@link #ALTIMETER_RANGE_PER_LEVEL} je Stufe, hoechstens {@link #ALTIMETER_MAX_RANGE}); an der
 * Blockreichweite aendert sie auf der Messuhr nichts (siehe {@code RangeReach}).
 */
public class VelocityGaugeItem extends Item {

    /** Wie weit der Hoehenmesser ohne Verzauberung hinab misst (Bloecke). */
    public static final int ALTIMETER_BASE_RANGE = 24;
    /** Zusaetzliche Messweite je Stufe Reichweite. */
    public static final int ALTIMETER_RANGE_PER_LEVEL = 16;
    /** Obergrenze der Messweite (Reichweite III). */
    public static final int ALTIMETER_MAX_RANGE = 64;
    /** Geschwindigkeit bei Vollausschlag der Nadel (Bloecke/s), fuer HUD und Item-Nadel. */
    public static final double FULL_SCALE_BPS = 50.0;

    public VelocityGaugeItem(Item.Properties properties) {
        super(properties);
    }

    /** Client-Hook fuer den Autowalk (Besitzer 2026-10-01), gesetzt von {@code GaugeAutowalk}; nie auf dem Server. */
    public static java.util.function.Consumer<net.minecraft.world.entity.player.Player> clientAutowalkToggle = player -> { };

    /**
     * Laeuft der Autowalk unter diesem Bildschirm weiter? Ohne Bildschirm und unter jedem nicht pausierenden
     * (Inventar, Chat, Truhe, Werkbank ...) ja, unter einem pausierenden (Pausenmenue) nein (Besitzer 2026-10-02).
     */
    public static boolean autowalkContinuesUnder(boolean screenOpen, boolean screenPausesGame) {
        return !screenOpen || !screenPausesGame;
    }

    /**
     * Haelt der Spieler noch die Messuhr, mit der er den Autowalk gestartet hat? Gleicher Haupthand-Slot und dort
     * weiter eine Messuhr - ein vom Server neu gesendeter Stapel (etwa beim Oeffnen einer Truhe) zaehlt als dieselbe.
     */
    public static boolean autowalkKeepsGauge(int selectedSlot, int startSlot, ItemStack mainHand) {
        return selectedSlot == startSlot && mainHand.getItem() instanceof VelocityGaugeItem;
    }

    /** Rechtsklick in der Haupthand schaltet den Autowalk um ({@code client.GaugeAutowalk}). */
    @Override
    public net.minecraft.world.InteractionResult use(Level level, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            clientAutowalkToggle.accept(player);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /** Messweite des Hoehenmessers fuer diese Messuhr: Grundwert plus Reichweite, gedeckelt. */
    public static int altimeterRange(ItemStack stack, Level level) {
        int rangeLevel = com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel(stack, level, ModEnchantments.RANGE);
        return altimeterRange(rangeLevel);
    }

    /** Messweite zu einer Stufe Reichweite (0 = ohne). */
    public static int altimeterRange(int rangeLevel) {
        return Math.min(ALTIMETER_MAX_RANGE, ALTIMETER_BASE_RANGE + Math.max(0, rangeLevel) * ALTIMETER_RANGE_PER_LEVEL);
    }

    /**
     * Abstand der Fuesse (Hoehe {@code feetY} ueber Block {@code x}/{@code z}) zur Oberkante des
     * ersten festen Blocks darunter, hoechstens {@code range} Bloecke tief gesucht; {@code -1}, wenn
     * dort kein Boden ist. Fluessigkeiten zaehlen nicht als Boden (ihre Kollisionsform ist leer).
     */
    public static double heightAboveGround(BlockGetter level, double x, double feetY, double z, int range) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(feetY), Mth.floor(z));
        int bottom = Mth.floor(feetY) - range;
        for (int y = Mth.floor(feetY); y >= bottom && y >= level.getMinY(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                double top = y + shape.max(net.minecraft.core.Direction.Axis.Y);
                if (top <= feetY + 1.0E-6) {
                    return Math.max(0.0, feetY - top);
                }
            }
        }
        return -1.0;
    }

    /**
     * Fallschaden (halbe Herzen), wenn jemand, der schon {@code fallDistance} gefallen ist, noch
     * {@code drop} Bloecke tiefer landet - Vanillas Formel ({@code LivingEntity#calculateFallDamage})
     * mit sicherer Fallhoehe {@code safeFall} und Schadensfaktor {@code multiplier}, ohne Ruestung.
     */
    public static int impactDamage(double fallDistance, double drop, double safeFall, double multiplier) {
        double power = fallDistance + Math.max(0.0, drop) + 1.0E-6 - safeFall;
        return Math.max(0, Mth.floor(power * multiplier));
    }

    /** Nadelausschlag 0..1 zu einer Geschwindigkeit (Wurzelskala, oben abgeschnitten) - HUD und Item gleich. */
    public static double needleFraction(double bps) {
        if (!(bps > 0.0)) {
            return 0.0;
        }
        return Math.min(1.0, Math.sqrt(bps / FULL_SCALE_BPS));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplebuilding.velocity_gauge.tooltip").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.simplebuilding.velocity_gauge.tooltip.2").withStyle(ChatFormatting.GRAY));
        if (!stack.isEnchanted()) {
            lines.accept(Component.translatable("tooltip.simplebuilding.velocity_gauge.touch_hint").withStyle(ChatFormatting.DARK_GRAY));
            lines.accept(Component.translatable("tooltip.simplebuilding.velocity_gauge.touch_hint.2").withStyle(ChatFormatting.DARK_GRAY));
            lines.accept(Component.translatable("tooltip.simplebuilding.velocity_gauge.touch_hint.3").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Die neue Blickrichtung, wenn der Weg unter {@code pos} geradeaus endet und genau links oder rechts
     * weitergeht; sonst null. Weg = Trampelpfad oder Schiene (fuer Schienen die Hoehe der Lore).
     */
    public static net.minecraft.core.Direction followTurn(Level level, BlockPos pos, net.minecraft.core.Direction facing) {
        if (!isWay(level.getBlockState(pos))) {
            return null;
        }
        if (isWay(level.getBlockState(pos.relative(facing)))) {
            return null;
        }
        boolean left = isWay(level.getBlockState(pos.relative(facing.getCounterClockWise())));
        boolean right = isWay(level.getBlockState(pos.relative(facing.getClockWise())));
        if (left == right) {
            return null;
        }
        return left ? facing.getCounterClockWise() : facing.getClockWise();
    }

    private static boolean isWay(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH) || state.is(net.minecraft.tags.BlockTags.RAILS);
    }
}
