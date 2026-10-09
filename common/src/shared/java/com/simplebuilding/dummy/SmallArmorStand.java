package com.simplebuilding.dummy;

import com.simplebuilding.items.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Kleiner Ruestungsstaender (Nachtrag 29, 2026-10-09, docs/ai/PLAN-STAENDER-2026-10-09.md): ein Holzpfosten mit Querholz
 * auf einer Steinplatte, der genau ein Item zeigt - ein Ruestungsteil (Kopf, Brust, Beine, Fuesse) oder eine Tier-Ruestung
 * (Pferd, Wolf, Nautilus). Das Item liegt in Vanillas passendem Slot (Tier-Ruestung im Koerper-Slot); Abbau, Speichern,
 * Name und Pick-Block bleiben Vanilla ({@link ArmorStand}), das eigene Item faellt ueber {@code ArmorStandStandsMixin}.
 * Rechtsklick legt ab/nimmt ab, Fremdes wird abgelehnt; Spender ueber {@code EquipmentDispenseStandMixin}.
 */
public class SmallArmorStand extends ArmorStand {
    /** Reihenfolge, in der ein Staender mit mehreren Teilen (alter mittlerer Staender) eines behaelt. */
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.BODY};

    /** Welche Tier-Ruestung der Staender in Tierform zeigt. */
    public enum Animal { HORSE, WOLF, NAUTILUS }

    public SmallArmorStand(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        this.setShowArms(false);
    }

    /** Der Slot, in den {@code stack} auf diesem Staender gehoert, oder {@code null}, wenn er es nicht annimmt. */
    public static @Nullable EquipmentSlot slotFor(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) {
            return null;
        }
        EquipmentSlot slot = equippable.slot();
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            // Elytren (und andere Gleiter) haben ein Asset, aber keine Ruestungsform.
            return stack.has(DataComponents.GLIDER) ? null : slot;
        }
        return slot == EquipmentSlot.BODY && animal(stack) != null ? slot : null;
    }

    /** Pferde-, Wolfs- oder Nautilus-Ruestung (auch aus anderen Mods, ueber die erlaubten Tiere), sonst {@code null}. */
    public static @Nullable Animal animal(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != EquipmentSlot.BODY || equippable.allowedEntities().isEmpty()) {
            return null;
        }
        if (equippable.canBeEquippedBy(EntityTypes.HORSE.builtInRegistryHolder())) {
            return Animal.HORSE;
        }
        if (equippable.canBeEquippedBy(EntityTypes.WOLF.builtInRegistryHolder())) {
            return Animal.WOLF;
        }
        return equippable.canBeEquippedBy(EntityTypes.NAUTILUS.builtInRegistryHolder()) ? Animal.NAUTILUS : null;
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && slotFor(stack) != null;
    }

    /** Der Slot mit dem gezeigten Item, oder {@code null}, wenn der Staender leer ist. */
    public @Nullable EquipmentSlot shownSlot() {
        for (EquipmentSlot slot : SLOTS) {
            if (!this.getItemBySlot(slot).isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    public ItemStack shown() {
        EquipmentSlot slot = shownSlot();
        return slot == null ? ItemStack.EMPTY : this.getItemBySlot(slot);
    }

    /** Legt ein Item auf den leeren Staender (Spender); {@code false}, wenn er schon etwas zeigt oder es nicht passt. */
    public boolean put(ItemStack stack) {
        EquipmentSlot slot = slotFor(stack);
        if (slot == null || shownSlot() != null) {
            return false;
        }
        this.setItemSlot(slot, stack.copyWithCount(1));
        equipped(stack, null);
        return true;
    }

    private void equipped(ItemStack stack, @Nullable Player player) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), equippable.equipSound().value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        this.gameEvent(GameEvent.EQUIP, player);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);
        if (this.isMarker() || held.is(Items.NAME_TAG)) {
            return super.interact(player, hand, location);
        }
        if (player.isSpectator()) {
            return InteractionResult.SUCCESS;
        }
        boolean client = player.level().isClientSide();
        if (ArmorStandSwap.wants(this, player, hand)) {
            if (!client) {
                swapWithWearer(player);
            }
            return client ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        EquipmentSlot shown = shownSlot();
        EquipmentSlot target = held.isEmpty() ? shown : slotFor(held);
        if (target == null) {
            return held.isEmpty() ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        if (!held.isEmpty() && shown != null && held.getCount() > 1) {
            return InteractionResult.FAIL;
        }
        if (client) {
            return InteractionResult.SUCCESS;
        }
        ItemStack old = shown == null ? ItemStack.EMPTY : this.getItemBySlot(shown);
        if (shown != null) {
            this.setItemSlot(shown, ItemStack.EMPTY);
        }
        if (!held.isEmpty()) {
            // Kreativ legt eine Kopie ab (wie Vanillas Staender), nur auf einen leeren Staender.
            this.setItemSlot(target, player.hasInfiniteMaterials() && shown == null ? held.copyWithCount(1) : held.split(1));
            equipped(this.getItemBySlot(target), player);
        }
        if (!old.isEmpty()) {
            if (held.isEmpty()) {
                player.setItemInHand(hand, old);
            } else if (!player.getInventory().add(old)) {
                Block.popResource(this.level(), player.blockPosition(), old);
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Ruestungstausch (Schleichen + leere Hand oder bestromt) fuer genau das eine Teil: das gezeigte Teil wechselt mit dem
     * gleichen Slot des Spielers; ein leerer Staender nimmt das erste getragene Teil (Brust, Kopf, Hose, Fuesse).
     * Fluch der Bindung bleibt am Spieler (ausser Kreativ); eine Tier-Ruestung tauscht nicht. Liefert, ob getauscht wurde.
     */
    public boolean swapWithWearer(Player player) {
        EquipmentSlot slot = shownSlot();
        if (slot == EquipmentSlot.BODY) {
            return false;
        }
        if (slot == null) {
            for (int i = 0; i < 4 && slot == null; i++) {
                ItemStack worn = player.getItemBySlot(SLOTS[i]);
                if (!worn.isEmpty() && slotFor(worn) == SLOTS[i]) {
                    slot = SLOTS[i];
                }
            }
            if (slot == null) {
                return false;
            }
        }
        ItemStack worn = player.getItemBySlot(slot);
        ItemStack shown = this.getItemBySlot(slot);
        if (!player.hasInfiniteMaterials() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        }
        if (!worn.isEmpty() && slotFor(worn) != slot || !shown.isEmpty() && !player.isEquippableInSlot(shown, slot)) {
            return false;
        }
        this.setItemSlot(slot, worn.copy());
        player.setItemSlot(slot, shown.copy());
        equipped(worn.isEmpty() ? shown : worn, player);
        return true;
    }

    /** Genau ein Item: ein alter mittlerer Staender (Hose + Stiefel) behaelt eines und laesst die uebrigen fallen. */
    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && !this.isRemoved()) {
            EquipmentSlot keep = shownSlot();
            if (keep != null && slotFor(this.getItemBySlot(keep)) != keep) {
                keep = null; // per Befehl gesetzt und nicht zeigbar: faellt ebenfalls
            }
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                ItemStack extra = this.getItemBySlot(slot);
                if (slot == keep || extra.isEmpty()) {
                    continue;
                }
                this.setItemSlot(slot, ItemStack.EMPTY);
                Block.popResource(this.level(), this.blockPosition().above(), extra);
            }
        }
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.BODY;
    }

    /** Vanillas Spender-Weg bleibt zu (er wuerde auch Kuerbisse aufsetzen); der eigene steht im Spender-Mixin. */
    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return false;
    }

    /** Das eigene Item (Abbauen ueber {@code ArmorStandStandsMixin}, Pick-Block). */
    public @Nullable Item item() {
        return ModItems.SMALL_ARMOR_STAND;
    }

    @Override
    public ItemStack getPickResult() {
        Item item = item();
        return item == null ? super.getPickResult() : new ItemStack(item);
    }
}
