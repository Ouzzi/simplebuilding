package com.simplebuilding.advancement;

import com.simplebuilding.util.TrimBenefitUser;
import com.simplebuilding.util.TrimBonusCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.jetbrains.annotations.Nullable;

/**
 * Advancement conditions that are a state rather than an action, checked once a second from the
 * player tick ({@code AdvancementHooksMixin}).
 *
 * <ul>
 *   <li>{@link ModTriggers#FULL_TRIM_SET}: all four armor slots hold armor trimmed with the same
 *       pattern, that pattern gives a bonus ({@link TrimBonusCatalog#forPattern}), and the player has
 *       not switched trim benefits off - the pattern bonus then counts four times.</li>
 * </ul>
 */
public final class AdvancementChecks {
    /** Ticks between two checks. */
    public static final int INTERVAL = 20;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private AdvancementChecks() {
    }

    /** Called every tick; does its work every {@link #INTERVAL} ticks. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % INTERVAL == 0) {
            check(player);
        }
    }

    /** Runs every check now. */
    public static void check(ServerPlayer player) {
        if (wearsFullBonusTrimSet(player)) {
            ModTriggers.feature(player, ModTriggers.FULL_TRIM_SET);
        }
    }

    /** Four armor pieces with the same trim pattern, a pattern with a bonus, trim benefits on. */
    public static boolean wearsFullBonusTrimSet(ServerPlayer player) {
        if (player instanceof TrimBenefitUser user && !user.simplebuilding$areTrimBenefitsEnabled()) {
            return false;
        }
        String pattern = null;
        for (EquipmentSlot slot : ARMOR) {
            String own = patternOf(player.getItemBySlot(slot).get(DataComponents.TRIM));
            if (own == null || (pattern != null && !pattern.equals(own))) {
                return false;
            }
            pattern = own;
        }
        return !TrimBonusCatalog.forPattern(pattern).isEmpty();
    }

    private static @Nullable String patternOf(@Nullable ArmorTrim trim) {
        return trim == null ? null : trim.pattern().value().assetId().getPath();
    }
}
