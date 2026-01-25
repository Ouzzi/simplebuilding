package com.simplebuilding;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.NetheriteHopperBlockEntity;
import com.simplebuilding.command.ModCommands;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.datagen.ModLootTableProvider;
import com.simplebuilding.datagen.ModTradeOffers;
import com.simplebuilding.enchantment.ModEnchantmentEffects;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.networking.*;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.*;
import com.simplebuilding.world.gen.ModOreGeneration;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Die Hauptklasse für den Simplemoney Mod.
 * Diese Klasse initialisiert alle Custom Items, Rezepte und registriert
 * die benutzerdefinierten Handelsangebote für Dorfbewohner (Villager Trades),
 * wobei die Custom Currency (MONEY_BILL) verwendet wird.
 * * Implementiert das Fabric ModInitializer Interface.
 */
public class Simplebuilding implements ModInitializer {
	/** Die eindeutige Mod-ID, verwendet für Registrierungen und Logger. */
	public static final String MOD_ID = "simplebuilding";
	/** Der Logger für die Protokollierung von Mod-Ereignissen und Debugging. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SimplebuildingConfig CONFIG;


    /**
	 * Die Hauptmethode, die beim Start des Mods von Fabric aufgerufen wird.
	 * Registriert alle Mod-Komponenten und Handelsangebote.
	 */
	@Override
	public void onInitialize() {
		LOGGER.info("Starting Simplebuilding initialization...");


        AutoConfig.register(SimplebuildingConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();


        ModScreenHandlers.registerScreenHandlers();

        ModItemGroups.registerItemGroups();
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();

        ModBlockEntities.registerBlockEntities();

        ModLootTableProvider.modifyLootTables();
        ModTradeOffers.registerModTradeOffers();
        ModDataComponentTypes.registerDataComponentTypes();
        ModEnchantmentEffects.registerEnchantmentEffects();

        ModRecipes.registerRecipes();

        ModRegistries.registerModStuffs();

        registerCauldronBehavior();

        PlayerBlockBreakEvents.BEFORE.register(new SledgehammerUsageEvent());
        SledgehammerEntityInteraction.register();

        PlayerBlockBreakEvents.BEFORE.register(new StripMinerUsageEvent());
        PlayerBlockBreakEvents.BEFORE.register(new VeinMinerUsageEvent());
        AttackBlockCallback.EVENT.register(new VersatilityUsageEvent());

        // ================================
        // REINFORCED BUNDLE - Selection Payload
        // ================================
        PayloadTypeRegistry.playC2S().register(ReinforcedBundleSelectionPayload.ID, ReinforcedBundleSelectionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReinforcedBundleSelectionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                if (player.currentScreenHandler != null) {
                    // Sicherheit: Prüfen ob Slot ID gültig ist
                    int slotId = payload.slotId();
                    if (slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
                        Slot slot = player.currentScreenHandler.getSlot(slotId);
                        if (slot != null && slot.hasStack() && slot.getStack().getItem() instanceof ReinforcedBundleItem) {
                            // Verwende unsere neue statische Hilfsmethode
                            ReinforcedBundleItem.setBundleSelectedItem(slot.getStack(), payload.selectedIndex());
                        }
                    }
                }
            });
        });


        // ================================
        // OCTANT - Konfigurations-Payload
        // ================================
        PayloadTypeRegistry.playC2S().register(OctantConfigurePayload.ID, OctantConfigurePayload.CODEC);
        // WICHTIG: Dies muss in onInitialize stehen!
        ServerPlayNetworking.registerGlobalReceiver(OctantConfigurePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                ItemStack stack = player.getMainHandStack();

                if (stack.getItem() instanceof OctantItem) {
                    // Daten vom Paket lesen
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();

                    payload.pos1().ifPresent(p -> nbt.putIntArray("Pos1", new int[]{p.getX(), p.getY(), p.getZ()}));
                    payload.pos2().ifPresent(p -> nbt.putIntArray("Pos2", new int[]{p.getX(), p.getY(), p.getZ()}));

                    if (payload.shapeName() != null && !payload.shapeName().isEmpty()) {
                        nbt.putString("Shape", payload.shapeName());
                    }

                    // HIER IST DER FIX FÜR DAS LOCKING:
                    nbt.putBoolean("Locked", payload.locked());

                    // Daten zurück ins Item schreiben
                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            });
        });


        // ================================
        // OCTANT - Scroll-Payload
        // ================================
        PayloadTypeRegistry.playC2S().register(OctantScrollPayload.ID, OctantScrollPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OctantScrollPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                ItemStack stack = player.getMainHandStack();

                if (stack.getItem() instanceof OctantItem) {

                    // NBT holen
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    boolean changed = false;

                    // =================================================================
                    // MODUS WECHSELN (Shift + Alt)
                    // =================================================================
                    if (payload.alt()) {
                        // Aktuellen Modus aus NBT lesen
                        String currentShapeName = nbt.getString("Shape").orElse("");
                        OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;

                        if (!currentShapeName.isEmpty()) {
                            try {
                                currentShape = OctantItem.SelectionShape.valueOf(currentShapeName);
                            } catch (IllegalArgumentException ignored) {}
                        }

                        // 2. Nächsten Modus berechnen basierend auf Scroll-Richtung (payload.amount)
                        OctantItem.SelectionShape[] values = OctantItem.SelectionShape.values();
                        int currentIndex = currentShape.ordinal();

                        // amount ist +1 (vorwärts) oder -1 (rückwärts)
                        int nextIndex = (currentIndex + payload.amount()) % values.length;

                        // Java Modulo kann negativ sein, daher Korrektur:
                        if (nextIndex < 0) nextIndex += values.length;

                        OctantItem.SelectionShape nextShape = values[nextIndex];

                        // 3. Speichern
                        nbt.putString("Shape", nextShape.name());
                        changed = true;

                        // Optional: Chat-Nachricht (Feedback)
                        // player.sendMessage(Text.of("Form: " + nextShape.name()), true);
                    }

                    // =================================================================
                    // POSITION VERSCHIEBEN (Normal / Shift / Control - OHNE Alt)
                    // =================================================================
                    else {
                        // Deine alte Bewegungslogik hier...
                        // (Richtung bestimmen, Pos1/Pos2 berechnen wie gehabt)

                        net.minecraft.util.math.Direction direction = player.getHorizontalFacing();
                        if (player.getPitch() < -60) direction = net.minecraft.util.math.Direction.UP;
                        else if (player.getPitch() > 60) direction = net.minecraft.util.math.Direction.DOWN;

                        int dx = direction.getOffsetX() * payload.amount();
                        int dy = direction.getOffsetY() * payload.amount();
                        int dz = direction.getOffsetZ() * payload.amount();

                        boolean hasPos1 = nbt.contains("Pos1");
                        boolean hasPos2 = nbt.contains("Pos2");

                        if (payload.control() && hasPos1) {
                            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
                            if (p1.length == 3) { // Sicherheitscheck
                                p1[0] += dx; p1[1] += dy; p1[2] += dz;
                                nbt.putIntArray("Pos1", p1);
                                changed = true;
                            }
                        }

                        if (payload.shift()) {
                            if (hasPos2) {
                                int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
                                if (p2.length == 3) { // Sicherheitscheck
                                    p2[0] += dx; p2[1] += dy; p2[2] += dz;
                                    nbt.putIntArray("Pos2", p2);
                                    changed = true;
                                }
                            }
                        }
                    }

                    // Speichern und Inventar-Update erzwingen
                    if (changed) {
                        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

                        // In seltenen Fällen synchronisiert 1.21 Components nicht sofort.
                        // Ein manuelles Inventar-Update hilft oft:
                        // player.playerScreenHandler.sendContentUpdates();
                        // Aber normalerweise reicht stack.set bei MainHand.
                    }
                }
            });
        });

        // ================================
        // BUILDING WAND - Konfigurations-Payload
        // ================================
        PayloadTypeRegistry.playC2S().register(BuildingWandConfigurePayload.ID, BuildingWandConfigurePayload.CODEC);

        // Receiver registrieren
        ServerPlayNetworking.registerGlobalReceiver(BuildingWandConfigurePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof BuildingWandItem) {
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();

                    // Speichere die neuen Einstellungen
                    nbt.putInt("SettingsRadius", payload.selectedRadius());
                    nbt.putInt("SettingsAxis", payload.axisMode());

                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            });
        });

        // 1. Tick Event: Licht aktualisieren
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Performance: Nur alle 2 Ticks updaten, reicht für Licht
                if (server.getTicks() % 2 == 0) {
                    DynamicLightHandler.tick(player);
                }
            }
        });

        // 2. Disconnect Event: Lichtreste entfernen
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DynamicLightHandler.onDisconnect(handler.player);
        });


        // ================================
        // TRIM BENEFIT - Aktivierungs-Payload
        // ================================
        // 1. Payload registrieren (C2S = Client to Server)
        PayloadTypeRegistry.playC2S().register(TrimBenefitPayload.ID, TrimBenefitPayload.CODEC);

        // 2. Server-Empfänger registrieren
        ServerPlayNetworking.registerGlobalReceiver(TrimBenefitPayload.ID, (payload, context) -> {
            // Wir führen das auf dem Server-Thread aus
            context.server().execute(() -> {
                if (context.player() instanceof TrimBenefitUser user) {
                    // Hier speichern wir die Einstellung des Clients im Spieler-Objekt auf dem Server
                    user.simplebuilding$setTrimBenefitsEnabled(payload.enabled());
                }
            });
        });


        // ================================
        // MASTER BUILDER PICK - Item Pick Payload
        // ================================
        PayloadTypeRegistry.playC2S().register(MasterBuilderPickPayload.ID, MasterBuilderPickPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MasterBuilderPickPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ItemStack requestedItem = payload.itemToPick();

            context.server().execute(() -> {
                var inv = player.getInventory();
                var registryManager = player.getRegistryManager();
                // FIX: get -> getOrThrow
                var enchantRegistry = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT);
                // FIX: getEntry -> getOptional (für RegistryKey)
                var masterBuilderEntry = enchantRegistry.getOptional(ModEnchantments.MASTER_BUILDER);

                if (masterBuilderEntry.isEmpty()) return;

                for (int i = 0; i < inv.size(); i++) {
                    ItemStack bundleStack = inv.getStack(i);
                    if (bundleStack.getItem() instanceof ReinforcedBundleItem &&
                            EnchantmentHelper.getLevel(masterBuilderEntry.get(), bundleStack) > 0) {

                        BundleContentsComponent contents = bundleStack.get(DataComponentTypes.BUNDLE_CONTENTS);
                        if (contents != null) {

                            // FIX: Wir konvertieren den Inhalt in eine normale Liste,
                            // da der Builder keine Lese-Methoden wie get(i) hat.
                            List<ItemStack> stacks = new ArrayList<>();
                            contents.iterate().forEach(s -> stacks.add(s.copy()));

                            ItemStack foundStack = ItemStack.EMPTY;
                            BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(contents);
                            int indexToRemove = -1;

                            // Jetzt können wir normal iterieren
                            for (int j = 0; j < stacks.size(); j++) {
                                if (ItemStack.areItemsEqual(stacks.get(j), requestedItem)) {
                                    indexToRemove = j;
                                    foundStack = stacks.get(j); // Wir nehmen den Stack
                                    break;
                                }
                            }

                            if (indexToRemove != -1) {
                                // FIX: selectedSlot ist privat, nutze den Getter
                                int selectedSlot = inv.getSelectedSlot();
                                // FIX: player.getMainHandStack() statt inv.getMainHandStack()
                                ItemStack currentHandStack = player.getMainHandStack();

                                // Platz-Logik
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
                                    } else {
                                        return;
                                    }
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



        // ================================
        // NETHERITE HOPPER - Filtermodus Umschalten Payload
        // ================================
        ModMessages.registerC2SPackets();


        ModCommands.register();
        ModOreGeneration.generateOres();
    }

    private void registerCauldronBehavior() {
        // Rangefinder reinigen:
        CauldronBehavior cleanRangefinder = (state, world, pos, player, hand, stack) -> {
            Item item = stack.getItem();
            if (!(item instanceof OctantItem) || item == ModItems.OCTANT) {return ActionResult.PASS;}
            if (!world.isClient()) {
                ItemStack newStack = new ItemStack(ModItems.OCTANT);
                if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
                    newStack.set(DataComponentTypes.CUSTOM_DATA, stack.get(DataComponentTypes.CUSTOM_DATA));
                }
                player.setStackInHand(hand, newStack);
                player.incrementStat(Stats.CLEAN_ARMOR);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
            }
            return ActionResult.SUCCESS;
        };

        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (coloredItem != null) {
                CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(coloredItem, cleanRangefinder);
            }
        }
    }

    public static SimplebuildingConfig getConfig() {
        return CONFIG;
    }

}
