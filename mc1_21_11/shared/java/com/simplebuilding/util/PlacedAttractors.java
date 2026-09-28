package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.MagnetItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Der abgelegte Attractor (Besitzer 2026-09-28): Schleichen + Rechtsklick legt ihn wie eine
 * Schmiedevorlage ab ({@link PlacedTemplates}, derselbe Block samt pixelgenauer Trefferform), und dort
 * zieht er lose Items in {@link #RANGE} Bloecken Umkreis zu sich. Der Filter des Attractors gilt auch
 * abgelegt; Items im Tag {@link #IGNORE} bleiben liegen.
 *
 * <p>Sparsam: nur alle {@link #INTERVAL} Ticks eine Entity-Suche im Wuerfel um den Block (versetzt nach
 * Position, damit viele Attractors nicht im selben Tick suchen), sonst nichts. Die Reichweite folgt
 * derselben Config wie der gehaltene Attractor ({@code tools.magnetRangeMultiplier}; 0 schaltet ab).
 */
public final class PlacedAttractors {
    /** Reichweite in Bloecken (Kugel um die Mitte der Platte), vor dem Config-Faktor. */
    public static final double RANGE = 6.0;
    /** Ticks zwischen zwei Zuegen. */
    public static final int INTERVAL = 2;
    /** Zug pro Intervall in Bloecken/Tick. */
    public static final double PULL = 0.16;
    /** Hoechstgeschwindigkeit eines gezogenen Items. */
    public static final double MAX_SPEED = 0.6;
    /** Naeher als das ist ein Item angekommen und wird nur noch abgebremst. */
    public static final double ARRIVED = 0.6;
    /** Items in diesem Tag zieht kein Attractor an (Modpacks, Datapacks). */
    public static final TagKey<Item> IGNORE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "attractor_ignore"));

    private PlacedAttractors() {
    }

    public static boolean isAttractor(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MAGNET);
    }

    /** Wirksame Reichweite: {@link #RANGE} mal Config-Faktor. */
    public static double range() {
        return RANGE * MagnetItem.rangeMultiplier();
    }

    /** Zielpunkt: knapp vor der Schauseite der Platte. */
    public static Vec3 target(PlacedTemplateBlockEntity be) {
        Direction normal = be.normal();
        return be.surfaceCentre().add(normal.getStepX() * 0.25, normal.getStepY() * 0.25, normal.getStepZ() * 0.25);
    }

    /** Darf dieser Attractor (mit diesem Filter) das Item ziehen? */
    public static boolean canPull(ItemEntity entity, @Nullable String filter) {
        ItemStack item = entity.getItem();
        return entity.isAlive() && !item.isEmpty() && !item.is(IGNORE) && MagnetItem.passesFilter(entity, filter);
    }

    /**
     * Ein Server-Tick der abgelegten Platte: ist sie ein Attractor und ist dieser Tick dran, zieht sie
     * alle passenden Items im Umkreis. Liefert die Zahl der gezogenen Items.
     */
    public static int tick(ServerLevel level, BlockPos pos, PlacedTemplateBlockEntity be) {
        ItemStack stack = be.getTemplate();
        // Serverschalter server.features.attractor gilt auch fuer den abgelegten Attractor.
        if (!isAttractor(stack) || !com.simplebuilding.config.ServerTuning.get().features.attractor
                || Math.floorMod(level.getGameTime() + pos.asLong(), INTERVAL) != 0) {
            return 0;
        }
        return pull(level, pos, be);
    }

    /** Zieht jetzt (ohne Takt-Pruefung) alle passenden Items im Umkreis; die Zahl der gezogenen Items. */
    public static int pull(ServerLevel level, BlockPos pos, PlacedTemplateBlockEntity be) {
        double range = range();
        if (range <= 0.0) {
            return 0;
        }
        Vec3 target = target(be);
        String filter = MagnetItem.filterOf(be.getTemplate());
        double rangeSq = range * range;
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(range),
                entity -> entity.distanceToSqr(target) <= rangeSq && canPull(entity, filter));
        for (ItemEntity entity : items) {
            applyPull(entity, target);
        }
        return items.size();
    }

    private static void applyPull(ItemEntity entity, Vec3 target) {
        Vec3 offset = target.subtract(entity.position());
        double distance = offset.length();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 next;
        if (distance < ARRIVED) {
            next = velocity.scale(0.5);
        } else {
            next = velocity.scale(0.8).add(offset.scale(PULL / distance));
            if (entity.onGround() && (offset.y > 0.2 || entity.horizontalCollision)) {
                next = next.add(0.0, 0.2, 0.0);
            }
            double speed = next.length();
            if (speed > MAX_SPEED) {
                next = next.scale(MAX_SPEED / speed);
            }
        }
        // push() statt setDeltaMovement(): setzt das Sync-Flag, der Client sieht den Zug sofort.
        Vec3 change = next.subtract(velocity);
        entity.push(change.x, change.y, change.z);
    }
}
