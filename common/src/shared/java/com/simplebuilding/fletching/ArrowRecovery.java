package com.simplebuilding.fletching;

import com.simplebuilding.config.ServerTuning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Pfeile, die ein Lebewesen getroffen haben, kommen bei dessen Tod zurueck (Besitzer 2026-10-02: "teure Pfeile
 * lohnen sich dann"). Vanilla verwirft einen Pfeil nach einem Treffer ohne Durchschlag; hier merkt sich das Ziel
 * eine Kopie des Aufhebe-Stapels (alle Komponenten: Befiederungs-Teile, Trankinhalt) und laesst sie beim Tod fallen
 * ({@code LivingEntityMixin}). Stirbt das Ziel schon am Treffer, faellt der Pfeil sofort.
 *
 * <p>Keine Vermehrung: nur Pfeile mit {@code pickup == ALLOWED} (Unendlichkeit, Kreativ und die Zusatzpfeile von
 * Mehrfachschuss sind {@code CREATIVE_ONLY}), nur mit einem Spieler als Besitzer (Skelette, Werfer nicht), nur ohne
 * Durchschlag (ein durchbohrender Pfeil fliegt weiter und bleibt selbst aufsammelbar), nie Spieler als Ziel und nie
 * Pfeile mit Amethyst-Spitze, die beim Treffer zerspringen. Schalter und Obergrenze: {@code server.arrows}.
 */
public final class ArrowRecovery {
    public static final String SAVE_KEY = "SimpleBuildingStuckArrows";

    private ArrowRecovery() {
    }

    /** Vom Lebewesen per Mixin umgesetzt: die gemerkten Pfeile. */
    public interface Holder {
        List<ItemStack> simplebuilding$stuckArrows();
    }

    /** Ob dieser Pfeil nach dem Treffer auf {@code target} zurueckkommen darf. */
    public static boolean recoverable(AbstractArrow arrow, LivingEntity target) {
        if (!(arrow.level() instanceof ServerLevel) || !ServerTuning.get().arrows.recoverFromMobs) {
            return false;
        }
        if (arrow.pickup != AbstractArrow.Pickup.ALLOWED || !(arrow.getOwner() instanceof Player) || arrow.getPierceLevel() > 0) {
            return false;
        }
        if (target instanceof Player || arrow.getPickupItemStackOrigin().isEmpty()) {
            return false;
        }
        return !(arrow instanceof CraftedArrow crafted && crafted.parts().tip() == ArrowParts.Tip.AMETHYST);
    }

    /** Nach einem erfolgreichen Treffer (vor dem Verwerfen des Pfeils): merken oder sofort fallen lassen. */
    public static void onHit(AbstractArrow arrow, LivingEntity target) {
        if (!recoverable(arrow, target)) {
            return;
        }
        ItemStack stack = arrow.getPickupItemStackOrigin().copyWithCount(1);
        ServerLevel level = (ServerLevel) arrow.level();
        if (!target.isAlive()) {
            if (level.getGameRules().get(GameRules.MOB_DROPS)) {
                target.spawnAtLocation(level, stack);
            }
            return;
        }
        if (target instanceof Holder holder) {
            List<ItemStack> arrows = holder.simplebuilding$stuckArrows();
            if (arrows.size() < ServerTuning.arrowsPerMob()) {
                arrows.add(stack);
            }
        }
    }

    /** Beim Tod: alle gemerkten Pfeile fallen lassen (Spielregel {@code mob_drops}) und vergessen. */
    public static void dropAll(LivingEntity entity, ServerLevel level) {
        if (!(entity instanceof Holder holder)) {
            return;
        }
        List<ItemStack> arrows = holder.simplebuilding$stuckArrows();
        if (arrows.isEmpty()) {
            return;
        }
        List<ItemStack> copy = new ArrayList<>(arrows);
        arrows.clear();
        if (level.getGameRules().get(GameRules.MOB_DROPS)) {
            for (ItemStack stack : copy) {
                entity.spawnAtLocation(level, stack);
            }
        }
    }
}
