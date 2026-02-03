package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.util.TrimBenefitUser;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class ModMessages {

    private static boolean registered = false;

    public static void registerC2SPackets() {
        if (registered) {
            Simplebuilding.LOGGER.info("ModMessages already registered, skipping.");
            return;
        }
        registered = true;

        // --- 1. REGISTRIERUNG DER PAYLOAD-TYPEN (Beide Seiten müssen diese kennen) ---

        // Client -> Server (C2S)
        PayloadTypeRegistry.playC2S().register(ToggleHopperFilterPayload.ID, ToggleHopperFilterPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetHopperGhostItemPayload.ID, SetHopperGhostItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SpaceKeyPayload.ID, SpaceKeyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TrimBenefitPayload.ID, TrimBenefitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReinforcedBundleSelectionPayload.ID, ReinforcedBundleSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OctantConfigurePayload.ID, OctantConfigurePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OctantScrollPayload.ID, OctantScrollPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuildingWandConfigurePayload.ID, BuildingWandConfigurePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MasterBuilderPickPayload.ID, MasterBuilderPickPayload.CODEC);


        // Server -> Client (S2C)
        PayloadTypeRegistry.playS2C().register(SyncHopperGhostItemPayload.ID, SyncHopperGhostItemPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TrimDataPayload.ID, TrimDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SurvivalSyncPayload.ID, SurvivalSyncPayload.CODEC);

        // --- 2. SERVER-RECEIVER ---

        // Double Jump (Mit Logik aus ModRegistries übertragen!)
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                var registry = player.getEntityWorld().getRegistryManager();
                var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
                var doubleJump = enchantments.getOptional(ModEnchantments.DOUBLE_JUMP);

                if (doubleJump.isPresent()) {
                    ItemStack bootStack = player.getEquippedStack(EquipmentSlot.FEET);

                    // Prüfen, ob die Schuhe die Verzauberung haben
                    if (EnchantmentHelper.getLevel(doubleJump.get(), bootStack) > 0) {
                        // 1. Fallschaden zurücksetzen
                        player.fallDistance = 0;

                        // 2. Haltbarkeit abziehen (1 Punkt), wenn nicht Creative
                        if (!player.isCreative()) {
                            bootStack.damage(1, player, EquipmentSlot.FEET);
                        }
                    }
                }
            });
        });

        // ... [Restliche Receiver wie zuvor: Hopper, SpaceKey, TrimBenefit, Bundle, Octant, Wand, MasterBuilder etc.] ...
        // (Ich kürze das hier ab, kopiere einfach den Rest deiner alten ModMessages.java ab "Hopper Filter" hier hin)

        registerOtherReceivers(); // Platzhalter für deine restlichen Receiver aus der vorherigen Datei

        // Events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player instanceof SurvivalTracerAccessor accessor) {
                accessor.simplebuilding$syncTrimData();
            }
        });
    }

    // Kopiere hier einfach die restlichen registerGlobalReceiver Aufrufe aus deiner alten Datei rein
    private static void registerOtherReceivers() {
         ServerPlayNetworking.registerGlobalReceiver(ToggleHopperFilterPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ModHopperScreenHandler screenHandler) {
                    if (screenHandler.getBlockEntity() instanceof ModHopperBlockEntity blockEntity) {
                        blockEntity.toggleFilterMode();
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SetHopperGhostItemPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ModHopperScreenHandler screenHandler) {
                    if (screenHandler.getBlockEntity() instanceof ModHopperBlockEntity blockEntity) {
                        blockEntity.setGhostItem(payload.slotIndex(), payload.stack());
                    }
                }
            });
        });

        // Space Key
        ServerPlayNetworking.registerGlobalReceiver(SpaceKeyPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player() instanceof ISpaceKeyTracker tracker) {
                    tracker.simplebuilding$setSpacePressed(payload.pressed());
                }
            });
        });

        // Trim Benefit
        ServerPlayNetworking.registerGlobalReceiver(TrimBenefitPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player() instanceof TrimBenefitUser user) {
                    user.simplebuilding$setTrimBenefitsEnabled(payload.enabled());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ReinforcedBundleSelectionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player.currentScreenHandler != null) {
                    int slotId = payload.slotId();
                    if (slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
                        Slot slot = player.currentScreenHandler.getSlot(slotId);
                        if (slot != null && slot.hasStack() && slot.getStack().getItem() instanceof ReinforcedBundleItem) {
                            ReinforcedBundleItem.setBundleSelectedItem(slot.getStack(), payload.selectedIndex());
                        }
                    }
                }
            });
        });

        // Octant Configure
        ServerPlayNetworking.registerGlobalReceiver(OctantConfigurePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof OctantItem) {
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    payload.pos1().ifPresent(p -> nbt.putIntArray("Pos1", new int[]{p.getX(), p.getY(), p.getZ()}));
                    payload.pos2().ifPresent(p -> nbt.putIntArray("Pos2", new int[]{p.getX(), p.getY(), p.getZ()}));
                    if (payload.shapeName() != null && !payload.shapeName().isEmpty()) {
                        nbt.putString("Shape", payload.shapeName());
                    }
                    nbt.putBoolean("Locked", payload.locked());
                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            });
        });

        // Octant Scroll
        ServerPlayNetworking.registerGlobalReceiver(OctantScrollPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof OctantItem) {
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    boolean changed = false;

                    if (payload.alt()) {
                        String currentShapeName = nbt.getString("Shape").orElse("");
                        OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
                        if (!currentShapeName.isEmpty()) {
                            try { currentShape = OctantItem.SelectionShape.valueOf(currentShapeName); } catch (Exception ignored) {}
                        }
                        OctantItem.SelectionShape[] values = OctantItem.SelectionShape.values();
                        int nextIndex = (currentShape.ordinal() + payload.amount()) % values.length;
                        if (nextIndex < 0) nextIndex += values.length;
                        nbt.putString("Shape", values[nextIndex].name());
                        changed = true;
                    } else {
                        net.minecraft.util.math.Direction direction = player.getHorizontalFacing();
                        if (player.getPitch() < -60) direction = net.minecraft.util.math.Direction.UP;
                        else if (player.getPitch() > 60) direction = net.minecraft.util.math.Direction.DOWN;
                        int dx = direction.getOffsetX() * payload.amount();
                        int dy = direction.getOffsetY() * payload.amount();
                        int dz = direction.getOffsetZ() * payload.amount();

                        if (payload.control() && nbt.contains("Pos1")) {
                            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
                            if (p1.length == 3) { p1[0] += dx; p1[1] += dy; p1[2] += dz; nbt.putIntArray("Pos1", p1); changed = true; }
                        }
                        if (payload.shift() && nbt.contains("Pos2")) {
                            int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
                            if (p2.length == 3) { p2[0] += dx; p2[1] += dy; p2[2] += dz; nbt.putIntArray("Pos2", p2); changed = true; }
                        }
                    }
                    if (changed) stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            });
        });

        // Building Wand Config
        ServerPlayNetworking.registerGlobalReceiver(BuildingWandConfigurePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof BuildingWandItem) {
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    nbt.putInt("SettingsRadius", payload.selectedRadius());
                    nbt.putInt("SettingsAxis", payload.axisMode());
                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            });
        });

        // Master Builder Pick
        ServerPlayNetworking.registerGlobalReceiver(MasterBuilderPickPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ItemStack requestedItem = payload.itemToPick();
            context.server().execute(() -> {
                var inv = player.getInventory();
                var registryManager = player.getRegistryManager();
                var enchantRegistry = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);
                var masterBuilderEntry = enchantRegistry.getOptional(ModEnchantments.MASTER_BUILDER);
                if (masterBuilderEntry.isEmpty()) return;

                for (int i = 0; i < inv.size(); i++) {
                    ItemStack bundleStack = inv.getStack(i);
                    if (bundleStack.getItem() instanceof ReinforcedBundleItem &&
                            EnchantmentHelper.getLevel(masterBuilderEntry.get(), bundleStack) > 0) {
                        BundleContentsComponent contents = bundleStack.get(DataComponentTypes.BUNDLE_CONTENTS);
                        if (contents != null) {
                            List<ItemStack> stacks = new ArrayList<>();
                            contents.iterate().forEach(s -> stacks.add(s.copy()));
                            ItemStack foundStack = ItemStack.EMPTY;
                            int indexToRemove = -1;
                            for (int j = 0; j < stacks.size(); j++) {
                                if (ItemStack.areItemsEqual(stacks.get(j), requestedItem)) {
                                    indexToRemove = j;
                                    foundStack = stacks.get(j);
                                    break;
                                }
                            }
                            if (indexToRemove != -1) {
                                int selectedSlot = inv.getSelectedSlot();
                                ItemStack currentHandStack = player.getMainHandStack();
                                if (currentHandStack.isEmpty()) {
                                    stacks.remove(indexToRemove);
                                    bundleStack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(stacks));
                                    inv.setStack(selectedSlot, foundStack);
                                } else {
                                    int emptySlot = inv.getEmptySlot();
                                    if (emptySlot != -1) {
                                        stacks.remove(indexToRemove);
                                        bundleStack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(stacks));
                                        inv.setStack(emptySlot, currentHandStack);
                                        inv.setStack(selectedSlot, foundStack);
                                    } else return;
                                }
                                player.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 1.0f, 1.0f);
                                inv.markDirty();
                                player.playerScreenHandler.sendContentUpdates();
                                return;
                            }
                        }
                    }
                }
            });
        });
    }
}