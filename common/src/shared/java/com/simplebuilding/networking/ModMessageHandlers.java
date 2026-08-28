package com.simplebuilding.networking;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
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
                player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE, 1.0f, 1.0f);
                inv.setChanged();
                player.inventoryMenu.broadcastChanges();
                return;
            }
        }
    }
}
