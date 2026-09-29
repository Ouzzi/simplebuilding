package com.simplebuilding.tweaks.heads;

import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Die geheimen Faehigkeiten der Mob-Koepfe beim Tragen (Besitzer 2026-09-29, docs/MOBKOEPFE.md): nichts, was das
 * Spiel stark veraendert, sich ausnutzen laesst oder andere Dinge ueberfluessig macht - Spass oder ein kleiner
 * Ausweg aus nervigen Randfaellen. Jede Frage hier prueft nur, ob der Kopf im Kopf-Slot sitzt; die Mixins in
 * {@code tweaks.mixin.HeadAbility*} fragen an der Stelle, wo Vanilla entscheidet.
 *
 * <table>
 *   <caption>Faehigkeiten</caption>
 *   <tr><td>Lohe</td><td>Magmabloecke und Lagerfeuer brennen nicht ({@link #ignoresDamage})</td></tr>
 *   <tr><td>Enderman</td><td>Enderperlen-Teleports kosten keine Herzen ({@link #ignoresDamage})</td></tr>
 *   <tr><td>Wuestenzombie</td><td>Essen macht keinen Hunger-Effekt (verdorbenes Fleisch, rohes Huhn) ({@link #blocksFoodEffect})</td></tr>
 *   <tr><td>Spinne</td><td>Spinnennetze bremsen nicht ({@link #ignoresCobweb})</td></tr>
 *   <tr><td>Hoehlenspinne</td><td>Spinnennetze bricht die blosse Hand so schnell wie ein Schwert ({@link #cobwebBreakSpeed})</td></tr>
 *   <tr><td>Eiswanderer</td><td>Pulverschnee friert nicht ein (Tag {@code minecraft:freeze_immune_wearables}, wie Leder)</td></tr>
 *   <tr><td>Sumpfskelett</td><td>Essen vergiftet nicht (giftige Kartoffel, Spinnenauge, Kugelfisch) ({@link #blocksFoodEffect})</td></tr>
 *   <tr><td>Schleim</td><td>vier statt drei Bloecke Fall ohne Schaden (Attribut am Item)</td></tr>
 *   <tr><td>Silberfischchen</td><td>der Traeger ist halb so gross (Attribut am Item)</td></tr>
 *   <tr><td>Breeze</td><td>leicht wie der Wind: zertrampelt kein Ackerland ({@link #tramplesFarmland})</td></tr>
 *   <tr><td>Shulker</td><td>oeffnet Shulkerkisten auch, wenn der Deckel verbaut ist ({@link #opensBlockedShulkerBoxes})</td></tr>
 *   <tr><td>Ertrunkener</td><td>Magma-Blasensaeulen ziehen nicht nach unten ({@link #resistsDownwardBubbles})</td></tr>
 * </table>
 */
public final class HeadAbilities {

    /** Abbautempo der blossen Hand auf Spinnennetzen mit dem Hoehlenspinnenkopf - das eines Schwerts (Vanilla: 15). */
    public static final float COBWEB_SPEED = 15.0F;

    private HeadAbilities() {
    }

    /** Ob {@code entity} den Kopf dieses Typs im Kopf-Slot traegt. */
    public static boolean wears(Entity entity, BlazeHeadType type) {
        return entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.HEAD).is(TweaksItems.head(type));
    }

    /**
     * Lohenkopf: Magmabloecke ({@code hot_floor}) und Lagerfeuer ({@code campfire}) brennen nicht - Lohen stehen
     * im Feuer. Endermankopf: der Enderperlen-Teleport ({@code ender_pearl}) kostet keine Herzen.
     */
    public static boolean ignoresDamage(LivingEntity entity, DamageSource source) {
        if ((source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.CAMPFIRE)) && wears(entity, BlazeHeadType.BLAZE)) {
            return true;
        }
        return source.is(DamageTypes.ENDER_PEARL) && wears(entity, BlazeHeadType.ENDERMAN);
    }

    /**
     * Wuestenzombiekopf: kein Hunger-Effekt vom Essen (verdorbenes Fleisch, rohes Huhn, Kugelfisch).
     * Sumpfskelettkopf: keine Vergiftung vom Essen (giftige Kartoffel, Spinnenauge, Kugelfisch). Nur Essen
     * (Komponente {@code food}), keine Traenke; andere Wirkungen desselben Essens bleiben.
     */
    public static boolean blocksFoodEffect(LivingEntity user, ItemStack consumed, MobEffectInstance effect) {
        if (!consumed.has(DataComponents.FOOD)) {
            return false;
        }
        if (effect.is(MobEffects.HUNGER) && wears(user, BlazeHeadType.HUSK)) {
            return true;
        }
        return effect.is(MobEffects.POISON) && wears(user, BlazeHeadType.BOGGED);
    }

    /** Spinnenkopf: Spinnennetze bremsen den Traeger nicht (wie die Spinne selbst, {@code Spider#makeStuckInBlock}). */
    public static boolean ignoresCobweb(Entity entity, BlockState state) {
        return state.is(Blocks.COBWEB) && wears(entity, BlazeHeadType.SPIDER);
    }

    /**
     * Hoehlenspinnenkopf: Abbautempo auf Spinnennetzen mindestens das eines Schwerts - Scheren braucht es weiter
     * fuer das Netz selbst (ohne sie faellt nur Faden).
     */
    public static float cobwebBreakSpeed(LivingEntity entity, BlockState state, float speed) {
        if (state.is(Blocks.COBWEB) && speed < COBWEB_SPEED && wears(entity, BlazeHeadType.CAVE_SPIDER)) {
            return COBWEB_SPEED;
        }
        return speed;
    }

    /** Breezekopf: leicht wie der Wind - wer ihn traegt, zertrampelt kein Ackerland. */
    public static boolean tramplesFarmland(Entity entity) {
        return !wears(entity, BlazeHeadType.BREEZE);
    }

    /** Shulkerkopf: eine Shulkerkiste geht auch auf, wenn ein Block den Deckel versperrt. */
    public static boolean opensBlockedShulkerBoxes(Entity entity) {
        return wears(entity, BlazeHeadType.SHULKER);
    }

    /** Ertrunkenenkopf: eine Magma-Blasensaeule (nach unten) zieht den Traeger nicht in die Tiefe. */
    public static boolean resistsDownwardBubbles(Entity entity, boolean dragDown) {
        return dragDown && wears(entity, BlazeHeadType.DROWNED);
    }
}
