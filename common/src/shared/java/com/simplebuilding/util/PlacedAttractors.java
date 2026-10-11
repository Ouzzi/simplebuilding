package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.MagnetItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
    public static final double RANGE = 8.0;
    /** Ticks zwischen zwei Zuegen. */
    public static final int INTERVAL = 2;
    /** Zug pro Intervall in Bloecken/Tick. */
    public static final double PULL = 0.16;
    /** Hoechstgeschwindigkeit eines gezogenen Items. */
    public static final double MAX_SPEED = 0.6;
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
        // Filter nur mit Beruehrung des Konstrukteurs (Besitzer 2026-09-29), wie in der Hand.
        String filter = MagnetItem.effectiveFilter(be.getTemplate(), level);
        double rangeSq = range * range;
        long now = level.getGameTime();
        Map<Long, Active> active = ACTIVE.computeIfAbsent(level, l -> new HashMap<>());
        active.put(pos.asLong(), new Active(target, filter, now));
        active.values().removeIf(a -> now - a.tick() > INTERVAL || a.tick() > now);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(range),
                entity -> entity.distanceToSqr(target) <= rangeSq && canPull(entity, filter)
                        && com.simplebuilding.api.WorldPermissions.mayAutomateEntity(level, pos, entity));
        for (ItemEntity entity : items) {
            // Besitzer N23: ein Item zieht nur einmal je Takt, auch wenn mehrere Attractors es erreichen.
            Pulled last = PULLED.get(entity);
            if (last != null && last.pos() != pos.asLong() && now - last.tick() < INTERVAL) continue;
            PULLED.put(entity, new Pulled(pos.asLong(), now));
            applyPull(entity, centre(entity, active, rangeSq), now);
        }
        return items.size();
    }

    /** Zuletzt ziehende Attractors je Welt (Zielpunkt, Filter, Tick) - fuer den gemeinsamen Schwerpunkt. */
    private record Active(Vec3 target, @Nullable String filter, long tick) {}
    private record Pulled(long pos, long tick) {}
    private static final Map<ServerLevel, Map<Long, Active>> ACTIVE = new WeakHashMap<>();
    private static final Map<ItemEntity, Pulled> PULLED = new WeakHashMap<>();

    /**
     * Besitzer N23: liegt ein Item im Bereich mehrerer Attractors, zieht es zum Schwerpunkt aller, die es
     * erreichen und durchlassen - dort ruht es in der toten Zone, statt zwischen ihnen zu pendeln.
     */
    private static Vec3 centre(ItemEntity entity, Map<Long, Active> active, double rangeSq) {
        List<Vec3> targets = new ArrayList<>();
        for (Active a : active.values()) {
            if (entity.distanceToSqr(a.target()) <= rangeSq && canPull(entity, a.filter())) targets.add(a.target());
        }
        Vec3 sum = Vec3.ZERO;
        for (Vec3 v : targets) sum = sum.add(v);
        return sum.scale(1.0 / targets.size());
    }

    /** Markiert ein Item, dem ein Attractor die Schwerkraft abgenommen hat (bleibt beim Speichern erhalten). */
    public static final String HOVER_TAG = "simplebuilding_attractor_hover";
    /** Letzter Takt, in dem ein Attractor das schwebende Item gehalten hat. */
    private static final Map<ItemEntity, Long> HOVER = new WeakHashMap<>();

    private static void applyPull(ItemEntity entity, Vec3 target, long now) {
        Vec3 offset = target.subtract(entity.position());
        double distance = offset.length();
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 next;
        if (distance <= MagnetItem.minimumDistance()) {
            next = velocity.scale(0.2);
            if (HOVER.containsKey(entity) || (!entity.onGround() && !entity.isNoGravity())) {
                // Besitzer N23/N34 Sweetspot: ein schwebendes Item haelt die Hoehe des Zielpunkts. Frueher glich ein Zug je
                // Takt die Schwerkraft zweier Ticks aus - das Item sprang jeden Tick 0,02 auf und ab (Zittern, Client
                // verschluckt sich). Jetzt nimmt der Attractor ihm die Schwerkraft ab, solange er es haelt, und federt es
                // gedaempft zur Mitte; der Client sieht NoGravity und haelt es ebenso still.
                hold(entity, now);
                next = next.add(offset.scale(0.1));
            }
        } else {
            release(entity);
            next = velocity.scale(0.8).add(offset.scale(PULL / distance));
            if (entity.onGround() && (offset.y > 0.2 || entity.horizontalCollision)) {
                next = next.add(0.0, 0.2, 0.0);
            }
            double speed = next.length();
            if (speed > MAX_SPEED) {
                next = next.scale(MAX_SPEED / speed);
            }
        }
        // push() statt setDeltaMovement(): setzt das Sync-Flag, der Client sieht den Zug sofort. Ein ruhendes Item
        // bekommt keine Pakete mehr.
        Vec3 change = next.subtract(velocity);
        if (change.lengthSqr() > 1.0E-8) {
            entity.push(change.x, change.y, change.z);
        }
    }

    private static void hold(ItemEntity entity, long now) {
        if (!HOVER.containsKey(entity)) {
            if (entity.isNoGravity()) {
                return; // schwebt schon aus anderem Grund (Befehl, andere Mod) - nicht anfassen
            }
            entity.setNoGravity(true);
            entity.addTag(HOVER_TAG);
        }
        HOVER.put(entity, now);
    }

    private static void release(ItemEntity entity) {
        if (HOVER.remove(entity) != null || entity.entityTags().contains(HOVER_TAG)) {
            entity.removeTag(HOVER_TAG);
            entity.setNoGravity(false);
        }
    }

    /**
     * Jeden Server-Tick eines schwebenden Items (ItemEntityMixin): haelt es kein Attractor mehr (abgebaut, ausser Reichweite,
     * Welt neu geladen), faellt es wieder.
     */
    public static void releaseIfAbandoned(ItemEntity entity) {
        if (!entity.entityTags().contains(HOVER_TAG)) {
            return;
        }
        Long last = HOVER.get(entity);
        if (last == null || entity.level().getGameTime() - last > 2L * INTERVAL) {
            release(entity);
        }
    }
}
