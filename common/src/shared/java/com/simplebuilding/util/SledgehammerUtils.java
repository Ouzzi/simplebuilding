package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModToolMaterials;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.SledgehammerItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Mining time relative to the same-tier pickaxe. 26.2 retains the previous model. */
public final class SledgehammerUtils {

    /** Legacy 26.2 single-block slowdown; 26.3 uses FIRST_BLOCK_TIME_FACTOR. */
    public static final float SINGLE_BLOCK_SLOWDOWN = 1.2F;
    public static final float FIRST_BLOCK_TIME_FACTOR = 1.5F;
    public static final float EARLY_BLOCK_TIME_FACTOR = 0.8F;
    public static final float LATE_BLOCK_TIME_FACTOR = 0.7F;
    public static final int EARLY_BLOCK_COUNT_LIMIT = 9;

    /** Total same-tier pickaxe block times; octants retain the explicit 2x time per block. */
    public static float swingTimeFactor(int blocks, boolean octant) {
        if (blocks <= 0) return 1.0F;
        if (octant) return blocks * OCTANT_TIME_FACTOR;
        return FIRST_BLOCK_TIME_FACTOR
                + Math.min(blocks - 1, EARLY_BLOCK_COUNT_LIMIT - 1) * EARLY_BLOCK_TIME_FACTOR
                + Math.max(0, blocks - EARLY_BLOCK_COUNT_LIMIT) * LATE_BLOCK_TIME_FACTOR;
    }
    /** 26.3: same-tier pickaxe time per octant block; 26.2: multiplier on the legacy area time. */
    public static final float OCTANT_TIME_FACTOR = 2.0F;
    /** Groesste Oktant-Auswahl, die der Hammer bricht: so viele Stellen in der Box ... */
    public static final int OCTANT_MAX_VOLUME = 4096;
    /** ... und keine Kante laenger als so viele Bloecke. */
    public static final int OCTANT_MAX_EDGE = 32;

    /**
     * Die Leiter der Spitzhacken-Materialien nach Tempo (Holz 2, Stein 4, Kupfer 5, Eisen 6,
     * Diamant 8, Netherit 9, Enderit 10, Gold 12). "Eine Stufe darunter" ist das naechst langsamere
     * Material; fuer Gold, das schnellste, ist das Enderit.
     */
    private static final ToolMaterial[] SPEED_LADDER = {
            ToolMaterial.WOOD, ToolMaterial.STONE, ToolMaterial.COPPER, ToolMaterial.IRON,
            ToolMaterial.DIAMOND, ToolMaterial.NETHERITE, ModToolMaterials.ENDERITE, ToolMaterial.GOLD};

    private SledgehammerUtils() {
    }

    /**
     * Tempo der Spitzhacke eine Stufe unter {@code material}: das hoechste Materialtempo der Leiter,
     * das noch unter dem eigenen liegt. Holz (das langsamste) hat keine Stufe darunter und bleibt bei
     * sich selbst.
     */
    public static float lowerTierSpeed(ToolMaterial material) {
        float own = material.speed();
        float best = -1.0F;
        for (ToolMaterial candidate : SPEED_LADDER) {
            float speed = candidate.speed();
            if (speed < own && speed > best) {
                best = speed;
            }
        }
        return best > 0.0F ? best : own;
    }

    /**
     * Wie viele Bloecke ein Schlag auf {@code origin} wirklich abbaut: der Ursprung plus jede
     * Position aus {@link SledgehammerItem#getBlocksToBeDestroyed}, die {@link #shouldBreak}
     * durchlaesst - dieselbe Auswahl, die {@code SledgehammerUsageEvent} abbaut. 0, wenn der Hammer
     * den Ursprung gar nicht bearbeitet (oder keiner in der Haupthand liegt).
     */
    public static int countBlocksBroken(Player player, BlockPos origin) {
        ItemStack stack = player.getMainHandItem();
        Level world = player.level();
        List<BlockPos> positions = SledgehammerItem.getBlocksToBeDestroyed(1, origin, player);
        if (positions.isEmpty()) {
            return 0;
        }
        BlockState originState = world.getBlockState(origin);
        int overrideLevel = EnchantmentHelper.getEnchantmentLevel(stack, world, ModEnchantments.OVERRIDE);
        int count = 0;
        for (BlockPos pos : positions) {
            if (pos.equals(origin) || shouldBreak(world, pos, originState, stack, overrideLevel)) {
                count++;
            }
        }
        return count;
    }

    /** The same divisor on client and server; 26.3 uses swingTimeFactor, 26.2 the legacy tier model. */
    public static float miningSpeedDivisor(Player player, BlockPos origin) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SledgehammerItem hammer)) {
            return 1.0F;
        }
        int blocks = countBlocksBroken(player, origin);
        if (blocks <= 0) {
            return 1.0F;
        }
        boolean octant = !player.isShiftKeyDown() && octantSelection(player, origin) != null;
        if (com.simplebuilding.version.McVersion.PIECEWISE_HAMMER_TIME) return swingTimeFactor(blocks, octant);
        if (blocks == 1 && !octant) {
            return SINGLE_BLOCK_SLOWDOWN;
        }
        float divisor = blocks * lowerTierFactor(player, hammer, stack, player.level().getBlockState(origin));
        return octant ? divisor * OCTANT_TIME_FACTOR : divisor;
    }

    /**
     * Wie viel langsamer die Spitzhacke eine Stufe darunter diesen Block abbaut als der Hammer selbst:
     * {@code (s + e) / (s * s_u / s_m + e)} mit dem Werkzeugtempo {@code s} des Hammers auf dem Block,
     * dem Materialtempo {@code s_m}, dem Tempo {@code s_u} eine Stufe darunter und der
     * Abbau-Effizienz {@code e}. Ein Block, den der Hammer nur mit blossen-Haenden-Tempo bearbeitet,
     * bekommt 1.
     */
    public static float lowerTierFactor(Player player, SledgehammerItem hammer, ItemStack stack, BlockState state) {
        float speed = hammer.getDestroySpeed(stack, state);
        float material = hammer.getMaterial().speed();
        if (speed <= 1.0F || material <= 0.0F) {
            return 1.0F;
        }
        float efficiency = (float) player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        float lower = speed * lowerTierSpeed(hammer.getMaterial()) / material;
        return (speed + efficiency) / (lower + efficiency);
    }

    /**
     * Die Oktant-Auswahl, die der Hammer bricht (Besitzer 2026-09-28): ein Oktant mit beiden Ecken in
     * der Nebenhand, der angeschlagene Block liegt in seiner Figur, und die Auswahl ist nicht groesser
     * als {@value #OCTANT_MAX_VOLUME} Stellen bei hoechstens {@value #OCTANT_MAX_EDGE} Bloecken
     * Kantenlaenge. Liefert die Stellen der Figur (der Ursprung zuerst) oder {@code null}.
     */
    public static @Nullable List<BlockPos> octantSelection(Player player, BlockPos origin) {
        ItemStack octant = player.getOffhandItem();
        if (!(octant.getItem() instanceof OctantItem)) {
            return null;
        }
        AABB bounds = OctantShape.bounds(OctantShape.data(octant));
        if (bounds == null) {
            return null;
        }
        int sx = (int) Math.round(bounds.getXsize());
        int sy = (int) Math.round(bounds.getYsize());
        int sz = (int) Math.round(bounds.getZsize());
        if (sx > OCTANT_MAX_EDGE || sy > OCTANT_MAX_EDGE || sz > OCTANT_MAX_EDGE
                || (long) sx * sy * sz > OCTANT_MAX_VOLUME) {
            return null;
        }
        Predicate<BlockPos> inShape = OctantShape.of(octant);
        if (inShape == null || !inShape.test(origin)) {
            return null;
        }
        List<BlockPos> positions = new ArrayList<>();
        positions.add(origin.immutable());
        int minX = (int) Math.round(bounds.minX);
        int minY = (int) Math.round(bounds.minY);
        int minZ = (int) Math.round(bounds.minZ);
        for (int x = minX; x < minX + sx; x++) {
            for (int y = minY; y < minY + sy; y++) {
                for (int z = minZ; z < minZ + sz; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.equals(origin) && inShape.test(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

    /**
     * PrÃ¼ft, ob der Ursprungsblock Ã¼berhaupt mit dem Vorschlaghammer bearbeitet werden kann.
     */
    public static boolean canMineOrigin(Level world, BlockPos originPos, ItemStack stack) {
        return shouldBreak(world, originPos, originPos, stack);
    }

    /**
     * PrÃ¼ft, ob ein Block basierend auf Override-Stufe abgebaut werden soll.
     * <ul>
     *   <li>Stufe 0: gleicher Blocktyp + Spitzhacke</li>
     *   <li>Stufe 1: gemischte Spitzhacke-BlÃ¶cke</li>
     *   <li>Stufe 2+: beliebige abbau bare BlÃ¶cke</li>
     * </ul>
     */
    public static boolean shouldBreak(Level world, BlockPos pos, BlockPos originPos, ItemStack stack) {
        return shouldBreak(world, pos, world.getBlockState(originPos), stack,
                EnchantmentHelper.getEnchantmentLevel(stack, world, ModEnchantments.OVERRIDE));
    }

    /** Wie oben, mit schon gelesenem Ursprung und Override-Stufe (fuer grosse Auswahlen, einmal je Schlag). */
    public static boolean shouldBreak(Level world, BlockPos pos, BlockState originState, ItemStack stack, int overrideLevel) {
        BlockState targetState = world.getBlockState(pos);

        if (targetState.isAir() || targetState.getDestroySpeed(world, pos) < 0.0F) {
            return false;
        }

        if (!(stack.getItem() instanceof SledgehammerItem hammer)) {
            return false;
        }

        boolean sameBlock = targetState.getBlock() == originState.getBlock();
        boolean pickaxeBlock = targetState.is(BlockTags.MINEABLE_WITH_PICKAXE);

        if (overrideLevel <= 0) {
            return sameBlock && pickaxeBlock && hammer.isCorrectToolForDrops(stack, targetState);
        }
        if (overrideLevel == 1) {
            return pickaxeBlock && hammer.isCorrectToolForDrops(stack, targetState);
        }
        return true;
    }
}
