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
import com.simplebuilding.util.AirJumpGuard;
import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
            int level = EnchantmentHelper.getItemEnchantmentLevel(doubleJump.get(), bootStack);
            // Nicht mehr blind vertrauen (Audit #30): nur in der Luft, einmal je Sturz bzw. je Abklingzeit.
            if (level > 0 && AirJumpGuard.tryUse(player, level)) {
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

    /**
     * So weit (Bloecke, je Achse) darf eine Oktant-Ecke aus einem Paket hoechstens vom Spieler entfernt
     * liegen: die laengste Kante der groessten Stabstufe (256) plus etwas Luft, damit man an einer Ecke
     * stehend die gegenueberliegende eintippen kann. Weiter weg liegende Ecken verwirft der Server
     * (Audit 2026-09-26 #9).
     */
    public static final int OCTANT_CORNER_RANGE = 320;
    /** Groesster Betrag eines Scroll-Pakets; der Client schickt je Rastung 1. */
    public static final int OCTANT_MAX_SCROLL = 16;

    /** Liegt die Ecke in der Welt des Spielers (Bauhoehe) und in {@link #OCTANT_CORNER_RANGE} um ihn? */
    public static boolean octantCornerInRange(ServerPlayer player, int x, int y, int z) {
        net.minecraft.world.level.Level level = player.level();
        if (y < level.getMinY() || y > level.getMaxY()) {
            return false;
        }
        net.minecraft.core.BlockPos at = player.blockPosition();
        return Math.abs((long) x - at.getX()) <= OCTANT_CORNER_RANGE && Math.abs((long) y - at.getY()) <= OCTANT_CORNER_RANGE
                && Math.abs((long) z - at.getZ()) <= OCTANT_CORNER_RANGE;
    }

    private static <E extends Enum<E>> boolean isEnumName(Class<E> type, String name) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Einstellungen aus dem Oktant-Bildschirm. Der Server uebernimmt nur, was passt: Ecken in Reichweite
     * ({@link #octantCornerInRange}), Form und Reihenfolge nur als bekannter Name, Ausrichtung nur 0-5;
     * alles andere bleibt, wie es am Item steht. Eine verworfene Ecke meldet die Aktionsleiste (der
     * Bildschirm zeigt es zusaetzlich rot an; Nach-Audit 2026-09-27 N16).
     */
    public static void handleOctantConfigure(OctantConfigurePayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof OctantItem) {
            CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtComponent.copyTag();
            boolean refused = payload.pos1().filter(p -> !octantCornerInRange(player, p.getX(), p.getY(), p.getZ())).isPresent()
                    | payload.pos2().filter(p -> !octantCornerInRange(player, p.getX(), p.getY(), p.getZ())).isPresent();
            payload.pos1().filter(p -> octantCornerInRange(player, p.getX(), p.getY(), p.getZ()))
                    .ifPresent(p -> nbt.putIntArray("Pos1", new int[]{p.getX(), p.getY(), p.getZ()}));
            payload.pos2().filter(p -> octantCornerInRange(player, p.getX(), p.getY(), p.getZ()))
                    .ifPresent(p -> nbt.putIntArray("Pos2", new int[]{p.getX(), p.getY(), p.getZ()}));
            if (refused) {
                // Keine Bildschirmtexte bei Geraeten (Besitzer 2026-09-28): die Zusammenfassung im Oktant-Bildschirm
                // nennt den verworfenen Eckpunkt, hier nur ein Klang.
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.CHEST_LOCKED,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            if (payload.shapeName() != null && isEnumName(OctantItem.SelectionShape.class, payload.shapeName())) {
                nbt.putString("Shape", payload.shapeName());
            }
            nbt.putBoolean("Locked", payload.locked());
            if (payload.orientationOrdinal() >= 0 && payload.orientationOrdinal() <= 5) {
                nbt.putInt("Orientation", payload.orientationOrdinal());
            }
            nbt.putBoolean("Hollow", payload.hollow());
            nbt.putBoolean("LayerMode", payload.layerMode());
            if (payload.fillOrder() != null && isEnumName(OctantItem.FillOrder.class, payload.fillOrder())) {
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
        // Ein Paket mit riesigem Betrag liefe ueber (int) oder schoebe Ecken beliebig weit.
        int amount = Math.max(-OCTANT_MAX_SCROLL, Math.min(OCTANT_MAX_SCROLL, payload.amount()));

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
            int nextIndex = (currentShape.ordinal() + amount) % values.length;
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
            int dx = direction.getStepX() * amount;
            int dy = direction.getStepY() * amount;
            int dz = direction.getStepZ() * amount;

            if (payload.control() && nbt.contains("Pos1")) {
                int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
                if (p1.length == 3 && octantCornerInRange(player, p1[0] + dx, p1[1] + dy, p1[2] + dz)) {
                    p1[0] += dx;
                    p1[1] += dy;
                    p1[2] += dz;
                    nbt.putIntArray("Pos1", p1);
                    changed = true;
                }
            }
            if (payload.shift() && nbt.contains("Pos2")) {
                int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
                if (p2.length == 3 && octantCornerInRange(player, p2[0] + dx, p2[1] + dy, p2[2] + dz)) {
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
                contents.itemsCopy().forEach(stacks::add);
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
                    bundleStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(stacks)));
                    inv.setItem(selectedSlot, foundStack);
                } else {
                    int emptySlot = inv.getFreeSlot();
                    if (emptySlot == -1) {
                        return;
                    }
                    stacks.remove(indexToRemove);
                    bundleStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(stacks)));
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
        // Serverschalter server.features.backpack: der getragene Rucksack oeffnet nicht mehr; abgestellt
        // bleibt er zugaenglich, damit niemand seinen Inhalt verliert.
        if (com.simplebuilding.config.ServerTuning.featureDenied(com.simplebuilding.config.ServerTuning.get().features.backpack, player)) {
            return;
        }
        BackpackMenus.openWorn(player);
    }

    // =====================================================================================
    // BLAUPAUSE
    // =====================================================================================

    /**
     * Signieren parst den ganzen Code (bis 32 000 Zeichen). Jeder Spieler hat dafuer ein Budget an
     * Zeichen, das sich mit {@link #SIGN_REFILL_PER_TICK} je Tick wieder auffuellt: zwei volle Codes
     * sofort, danach einer je Sekunde. Ein Client, der Signier-Pakete in Schleife schickt, kann den
     * Server so nicht mehr mit Parsen beschaeftigen (Audit 2026-09-26 #11); Zwischenspeichern ohne
     * Signieren parst nicht und bleibt frei.
     */
    public static final int SIGN_BUDGET_CHARS = 2 * com.simplebuilding.blueprint.BlueprintCode.MAX_CODE_LENGTH;
    public static final int SIGN_REFILL_PER_TICK = com.simplebuilding.blueprint.BlueprintCode.MAX_CODE_LENGTH / 20;
    /** Budget je Spieler: {verfuegbare Zeichen, Spielzeit der letzten Abrechnung}; schwach, faellt mit dem Spieler weg. */
    private static final java.util.Map<ServerPlayer, long[]> SIGN_BUDGET = new java.util.WeakHashMap<>();

    /** Bucht {@code chars} Zeichen vom Signier-Budget des Spielers ab; {@code false} = zu viel auf einmal. */
    static boolean takeSignBudget(ServerPlayer player, int chars) {
        long now = player.level().getGameTime();
        synchronized (SIGN_BUDGET) {
            long[] budget = SIGN_BUDGET.computeIfAbsent(player, p -> new long[]{SIGN_BUDGET_CHARS, now});
            long elapsed = Math.max(0, now - budget[1]);
            budget[0] = Math.min(SIGN_BUDGET_CHARS, budget[0] + elapsed * SIGN_REFILL_PER_TICK);
            budget[1] = now;
            int cost = Math.max(1, chars);
            if (budget[0] < cost) {
                return false;
            }
            budget[0] -= cost;
            return true;
        }
    }

    /**
     * Neuer Code aus dem Editor. Wie beim Buch prueft der Server alles selbst: Slot (Hotbar oder
     * Nebenhand), eine unsignierte Blaupause darin, Laenge des Codes; beim Signieren zusaetzlich
     * Titel (1-32 Zeichen) und fehlerfreien, nicht leeren Code. Liegen mehrere leere Blaupausen
     * im Slot, bleibt die beschriebene dort und der Rest wird abgespalten. Der Editor schickt das
     * Paket entprellt waehrend des Tippens und beim Schliessen (Autospeichern); gespeichert wird
     * sofort am Item. Weist das Signier-Budget ab, sagt es die Aktionsleiste (Nach-Audit 2026-09-27
     * N16; frueher blieb der Klick auf "Signieren" stumm).
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
            if (!takeSignBudget(player, code.length())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.blueprint.sign.too_fast")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                return;
            }
            // Zwischengespeichert: der Bau (Tooltip, Vorschau, Baustab) fragt gleich denselben Code.
            com.simplebuilding.blueprint.BlueprintCode.ParseResult parsed = com.simplebuilding.blueprint.BlueprintCode.parseCached(code);
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
            // Die beschriebene bleibt im Slot (dorthin gehen auch die naechsten Autospeicherungen),
            // der Rest des Stapels wandert ins Inventar oder faellt heraus.
            ItemStack rest = stack.copyWithCount(stack.getCount() - 1);
            stack.setCount(1);
            stack.set(com.simplebuilding.component.ModDataComponentTypes.BLUEPRINT, written);
            if (!player.getInventory().add(rest)) {
                player.drop(rest, false);
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
