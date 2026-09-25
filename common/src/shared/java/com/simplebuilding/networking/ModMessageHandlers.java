package com.simplebuilding.networking;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.platform.BackpackMenus;
import com.simplebuilding.screen.BackpackMenuProviders;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

public final class ModMessageHandlers {
    private ModMessageHandlers() {
    }

    public static void handleDoubleJump(DoubleJumpPayload payload, ServerPlayer player) {
        var registry = player.level().registryAccess();
        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var doubleJump = enchantments.get(ModEnchantments.DOUBLE_JUMP);

        if (doubleJump.isPresent()) {
            ItemStack bootStack = player.getItemBySlot(EquipmentSlot.FEET);
            if (EnchantmentHelper.getItemEnchantmentLevel(doubleJump.get(), bootStack) > 0) {
                player.fallDistance = 0;
                if (!player.isCreative()) {
                    bootStack.hurtAndBreak(1, player, EquipmentSlot.FEET);
                }
            }
        }
    }

    public static void handleToggleHopperFilter(ToggleHopperFilterPayload payload, ServerPlayer player) {
        if (player.containerMenu instanceof ModHopperScreenHandler screenHandler
                && screenHandler.getBlockEntity() instanceof ModHopperBlockEntity blockEntity) {
            blockEntity.toggleFilterMode();
        }
    }

    public static void handleSetHopperGhostItem(SetHopperGhostItemPayload payload, ServerPlayer player) {
        if (player.containerMenu instanceof ModHopperScreenHandler screenHandler
                && screenHandler.getBlockEntity() instanceof ModHopperBlockEntity blockEntity) {
            blockEntity.setGhostItem(payload.slotIndex(), payload.stack());
        }
    }

    public static void handleSpaceKey(SpaceKeyPayload payload, ServerPlayer player) {
        if (player instanceof ISpaceKeyTracker tracker) {
            tracker.simplebuilding$setSpacePressed(payload.pressed());
        }
    }

    public static void handleTrimBenefit(TrimBenefitPayload payload, ServerPlayer player) {
        if (player instanceof TrimBenefitUser user) {
            user.simplebuilding$setTrimBenefitsEnabled(payload.enabled());
        }
    }

    public static void handleReinforcedBundleSelection(ReinforcedBundleSelectionPayload payload, ServerPlayer player) {
        if (player.containerMenu == null) {
            return;
        }
        int slotId = payload.slotId();
        if (slotId >= 0 && slotId < player.containerMenu.slots.size()) {
            Slot slot = player.containerMenu.getSlot(slotId);
            if (slot != null && slot.hasItem() && slot.getItem().getItem() instanceof ReinforcedBundleItem) {
                ReinforcedBundleItem.setBundleSelectedItem(slot.getItem(), payload.selectedIndex());
            }
        }
    }

    public static void handleOctantConfigure(OctantConfigurePayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof OctantItem) {
            CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtComponent.copyTag();
            payload.pos1().ifPresent(p -> nbt.putIntArray("Pos1", new int[]{p.getX(), p.getY(), p.getZ()}));
            payload.pos2().ifPresent(p -> nbt.putIntArray("Pos2", new int[]{p.getX(), p.getY(), p.getZ()}));
            if (payload.shapeName() != null && !payload.shapeName().isEmpty()) {
                nbt.putString("Shape", payload.shapeName());
            }
            nbt.putBoolean("Locked", payload.locked());
            nbt.putInt("Orientation", payload.orientationOrdinal());
            nbt.putBoolean("Hollow", payload.hollow());
            nbt.putBoolean("LayerMode", payload.layerMode());
            if (payload.fillOrder() != null && !payload.fillOrder().isEmpty()) {
                nbt.putString("FillOrder", payload.fillOrder());
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    public static void handleOctantScroll(OctantScrollPayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof OctantItem)) {
            return;
        }
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        // A locked octant keeps its selection against the scroll packet too, not only in the
        // client's MouseMixin: a client that sends the packet anyway must not move the corners.
        // Unlocking goes through the manager (handleOctantConfigure), which stays open.
        if (nbt.getBooleanOr("Locked", false)) {
            return;
        }
        boolean changed = false;

        if (payload.alt()) {
            String currentShapeName = nbt.getString("Shape").orElse("");
            OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
            if (!currentShapeName.isEmpty()) {
                try {
                    currentShape = OctantItem.SelectionShape.valueOf(currentShapeName);
                } catch (Exception ignored) {
                }
            }
            OctantItem.SelectionShape[] values = OctantItem.SelectionShape.values();
            int nextIndex = (currentShape.ordinal() + payload.amount()) % values.length;
            if (nextIndex < 0) {
                nextIndex += values.length;
            }
            nbt.putString("Shape", values[nextIndex].name());
            changed = true;
        } else {
            net.minecraft.core.Direction direction = player.getDirection();
            if (player.getXRot() < -60) {
                direction = net.minecraft.core.Direction.UP;
            } else if (player.getXRot() > 60) {
                direction = net.minecraft.core.Direction.DOWN;
            }
            int dx = direction.getStepX() * payload.amount();
            int dy = direction.getStepY() * payload.amount();
            int dz = direction.getStepZ() * payload.amount();

            if (payload.control() && nbt.contains("Pos1")) {
                int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
                if (p1.length == 3) {
                    p1[0] += dx;
                    p1[1] += dy;
                    p1[2] += dz;
                    nbt.putIntArray("Pos1", p1);
                    changed = true;
                }
            }
            if (payload.shift() && nbt.contains("Pos2")) {
                int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
                if (p2.length == 3) {
                    p2[0] += dx;
                    p2[1] += dy;
                    p2[2] += dz;
                    nbt.putIntArray("Pos2", p2);
                    changed = true;
                }
            }
        }
        if (changed) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    public static void handleBuildingWandConfigure(BuildingWandConfigurePayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof BuildingWandItem) {
            CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtComponent.copyTag();
            nbt.putInt("SettingsRadius", payload.selectedRadius());
            nbt.putInt("SettingsAxis", payload.axisMode());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    public static void handleMasterBuilderPick(MasterBuilderPickPayload payload, ServerPlayer player) {
        ItemStack requestedItem = payload.itemToPick();
        var inv = player.getInventory();
        var registryManager = player.registryAccess();
        var enchantRegistry = registryManager.lookupOrThrow(Registries.ENCHANTMENT);
        var masterBuilderEntry = enchantRegistry.get(ModEnchantments.MASTER_BUILDER);
        if (masterBuilderEntry.isEmpty()) {
            return;
        }

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack bundleStack = inv.getItem(i);
            if (bundleStack.getItem() instanceof ReinforcedBundleItem
                    && EnchantmentHelper.getItemEnchantmentLevel(masterBuilderEntry.get(), bundleStack) > 0) {
                BundleContents contents = bundleStack.get(DataComponents.BUNDLE_CONTENTS);
                if (contents == null) {
                    continue;
                }
                List<ItemStack> stacks = new ArrayList<>();
                contents.items().forEach(s -> stacks.add(s.create()));
                ItemStack foundStack = ItemStack.EMPTY;
                int indexToRemove = -1;
                for (int j = 0; j < stacks.size(); j++) {
                    if (ItemStack.isSameItem(stacks.get(j), requestedItem)) {
                        indexToRemove = j;
                        foundStack = stacks.get(j);
                        break;
                    }
                }
                if (indexToRemove == -1) {
                    continue;
                }
                int selectedSlot = inv.getSelectedSlot();
                ItemStack currentHandStack = player.getMainHandItem();
                if (currentHandStack.isEmpty()) {
                    stacks.remove(indexToRemove);
                    bundleStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(stacks.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
                    inv.setItem(selectedSlot, foundStack);
                } else {
                    int emptySlot = inv.getFreeSlot();
                    if (emptySlot == -1) {
                        return;
                    }
                    stacks.remove(indexToRemove);
                    bundleStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(stacks.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
                    inv.setItem(emptySlot, currentHandStack);
                    inv.setItem(selectedSlot, foundStack);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE, player.getSoundSource(), 1.0f, 1.0f);
                inv.setChanged();
                player.inventoryMenu.broadcastChanges();
                return;
            }
        }

        // Getragener Rucksack - wie bei Buendeln nur mit Meisterbauer auf dem Rucksack selbst.
        // Genommen wird hoechstens ein normaler Stapel (ein Tiefe-Taschen-Stapel kann groesser sein).
        ItemStack backpack = BackpackItem.wornBackpackWith(player, ModEnchantments.MASTER_BUILDER);
        if (backpack.isEmpty()) {
            return;
        }
        int entry = BackpackItem.findEntry(backpack, s -> ItemStack.isSameItem(s, requestedItem));
        if (entry < 0) {
            return;
        }
        int selectedSlot = inv.getSelectedSlot();
        ItemStack currentHandStack = player.getMainHandItem();
        int freeSlot = -1;
        if (!currentHandStack.isEmpty()) {
            freeSlot = inv.getFreeSlot();
            if (freeSlot == -1) {
                return;
            }
        }
        ItemStack picked = BackpackItem.take(backpack, entry, BackpackItem.entryStack(backpack, entry).getMaxStackSize());
        if (picked.isEmpty()) {
            return;
        }
        if (freeSlot != -1) {
            inv.setItem(freeSlot, currentHandStack);
        }
        inv.setItem(selectedSlot, picked);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE, player.getSoundSource(), 1.0f, 1.0f);
        inv.setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    /**
     * Rucksack-Taste: oeffnet das Menue des getragenen Rucksacks. Ohne getragenen Rucksack, tot,
     * als Zuschauer oder bei schon offenem Menue passiert nichts
     * ({@link BackpackMenuProviders#canOpenWorn}).
     */
    public static void handleOpenBackpack(OpenBackpackPayload payload, ServerPlayer player) {
        if (!BackpackMenuProviders.canOpenWorn(player)) {
            return;
        }
        BackpackMenus.openWorn(player);
    }

    // =====================================================================================
    // BLAUPAUSE
    // =====================================================================================

    /**
     * Neuer Code aus dem Editor. Wie beim Buch prueft der Server alles selbst: Slot (Hotbar oder
     * Nebenhand), eine unsignierte Blaupause darin, Laenge des Codes; beim Signieren zusaetzlich
     * Titel (1-32 Zeichen) und fehlerfreien, nicht leeren Code. Liegen mehrere leere Blaupausen
     * im Slot, wird nur eine beschrieben und abgespalten.
     */
    public static void handleBlueprintEdit(BlueprintEditPayload payload, ServerPlayer player) {
        int slot = payload.slot();
        if (!(net.minecraft.world.entity.player.Inventory.isHotbarSlot(slot) || slot == net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND)) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem)) {
            return;
        }
        com.simplebuilding.blueprint.BlueprintContent old = com.simplebuilding.items.custom.BlueprintItem.content(stack);
        if (old.signed()) {
            return;
        }
        String code = payload.code().replace("\r", "");
        if (code.length() > com.simplebuilding.blueprint.BlueprintCode.MAX_CODE_LENGTH) {
            return;
        }
        com.simplebuilding.blueprint.BlueprintContent written;
        if (payload.sign()) {
            String title = payload.title().strip();
            if (title.isEmpty() || title.length() > com.simplebuilding.blueprint.BlueprintCode.MAX_TITLE_LENGTH) {
                return;
            }
            com.simplebuilding.blueprint.BlueprintCode.ParseResult parsed = com.simplebuilding.blueprint.BlueprintCode.parse(code);
            if (!parsed.ok() || parsed.model().isEmpty()) {
                return;
            }
            written = new com.simplebuilding.blueprint.BlueprintContent(code, title, player.getName().getString(), true);
        } else {
            if (code.equals(old.code())) {
                return;
            }
            written = new com.simplebuilding.blueprint.BlueprintContent(code, old.title(), "", false);
        }
        if (stack.getCount() > 1) {
            ItemStack single = stack.split(1);
            single.set(com.simplebuilding.component.ModDataComponentTypes.BLUEPRINT, written);
            if (!player.getInventory().add(single)) {
                player.drop(single, false);
            }
            return;
        }
        stack.set(com.simplebuilding.component.ModDataComponentTypes.BLUEPRINT, written);
    }

    /** Strg+Mausrad im Baumodus: nur mit Baustab in der Haupthand und Blaupause in der Nebenhand. */
    public static void handleBlueprintRotate(BlueprintRotatePayload payload, ServerPlayer player) {
        ItemStack blueprint = player.getOffhandItem();
        if (!(blueprint.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem)
                || !(player.getMainHandItem().getItem() instanceof BuildingWandItem) || payload.amount() == 0) {
            return;
        }
        int steps = Math.floorMod(com.simplebuilding.blueprint.BlueprintBuilder.rotationSteps(blueprint) + Integer.signum(payload.amount()), 4);
        blueprint.set(com.simplebuilding.component.ModDataComponentTypes.BLUEPRINT_ROTATION, steps);
    }
}
