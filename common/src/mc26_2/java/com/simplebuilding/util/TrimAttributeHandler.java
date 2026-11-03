package com.simplebuilding.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Die Besatz-Boni auf Bewegung, Glueck, Rueckstoss und Reichweite als echte Vanilla-Attribut-
 * Modifikatoren (fluechtig, nicht gespeichert). Vorher griffen Mixins in getSpeed/getLuck ein; als
 * Attribute sieht Vanilla sie selbst (Sichtfeld beim Laufen, Attribut-Befehl, Synchronisation mit
 * dem Client) und andere Mods koennen sie lesen.
 *
 * <ul>
 *   <li>Laufgeschwindigkeit (Bolt, Redstone): movement_speed, add_multiplied_base - nur Spieler.</li>
 *   <li>Glueck (Host, Smaragd): luck, add_value - nur Spieler.</li>
 *   <li>Blockreichweite (Shaper): block_interaction_range, add_value - nur Spieler.</li>
 *   <li>Schwimmen (Tide): water_movement_efficiency, add_value - das Attribut von Wassertritt, wirkt
 *       also auch ohne Wassertritt-Stiefel. Jeder Traeger.</li>
 *   <li>Rueckstossresistenz (Harz): knockback_resistance, add_value. Jeder Traeger.</li>
 * </ul>
 *
 * Aufgerufen serverseitig aus dem LivingEntityMixin alle {@link #INTERVAL} Ticks; die Werte der
 * Spieler kommen ueber die normale Attribut-Synchronisation beim Client an.
 */
public final class TrimAttributeHandler {

    public static final int INTERVAL = 10;

    public static final Identifier WALKING_SPEED_ID = Identifier.fromNamespaceAndPath("simplebuilding", "trim_walking_speed");
    public static final Identifier LUCK_ID = Identifier.fromNamespaceAndPath("simplebuilding", "trim_luck");
    public static final Identifier SWIMMING_ID = Identifier.fromNamespaceAndPath("simplebuilding", "trim_swimming");
    public static final Identifier KNOCKBACK_ID = Identifier.fromNamespaceAndPath("simplebuilding", "trim_knockback_resistance");
    public static final Identifier REACH_ID = Identifier.fromNamespaceAndPath("simplebuilding", "trim_block_reach");

    private TrimAttributeHandler() {}

    /** Jeden Tick aufrufbar; gearbeitet wird nur alle {@link #INTERVAL} Ticks (versetzt nach ID). */
    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        if (Math.floorMod(entity.tickCount + entity.getId(), INTERVAL) != 0) return;
        update(entity);
    }

    /** Setzt alle Modifikatoren sofort auf den aktuellen Stand. */
    public static void update(LivingEntity entity) {
        boolean player = entity instanceof Player;
        apply(entity, Attributes.MOVEMENT_SPEED, WALKING_SPEED_ID,
                player ? TrimEffectUtil.getLandSpeedMultiplier(entity) - 1.0f : 0f,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(entity, Attributes.LUCK, LUCK_ID,
                player ? TrimEffectUtil.getLuckBonus(entity) : 0f,
                AttributeModifier.Operation.ADD_VALUE);
        apply(entity, Attributes.BLOCK_INTERACTION_RANGE, REACH_ID,
                player ? TrimEffectUtil.getReachBonus(entity) : 0f,
                AttributeModifier.Operation.ADD_VALUE);
        apply(entity, Attributes.WATER_MOVEMENT_EFFICIENCY, SWIMMING_ID,
                TrimEffectUtil.getSwimSpeedMultiplier(entity) - 1.0f,
                AttributeModifier.Operation.ADD_VALUE);
        apply(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID,
                TrimEffectUtil.getKnockbackResistanceBonus(entity),
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void apply(LivingEntity entity, Holder<Attribute> attribute, Identifier id, float amount,
                              AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (amount <= 1.0e-6f) {
            instance.removeModifier(id);
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current == null || Math.abs(current.amount() - amount) > 1.0e-6 || current.operation() != operation) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
