package com.simplebuilding;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.datagen.ModLootTableProvider;
import com.simplebuilding.datagen.ModTradeOffers;
import com.simplebuilding.enchantment.ModEnchantmentEffects;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.BuildingWandConfigurePayload;
import com.simplebuilding.networking.OctantConfigurePayload;
import com.simplebuilding.networking.OctantScrollPayload;
import com.simplebuilding.util.*;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        ModBlocks.registerModBlocks();
        ModItems.registerModItems();

        ModBlockEntities.registerBlockEntities();

        ModItemGroups.registerItemGroups();
        ModLootTableProvider.modifyLootTables();
        ModTradeOffers.registerModTradeOffers();
        ModDataComponentTypes.registerDataComponentTypes();
        ModEnchantmentEffects.registerEnchantmentEffects();

        ModRegistries.registerModStuffs();

        registerCauldronBehavior();



        PlayerBlockBreakEvents.BEFORE.register(new SledgehammerUsageEvent());
        PlayerBlockBreakEvents.BEFORE.register(new StripMinerUsageEvent());
        PlayerBlockBreakEvents.BEFORE.register(new VeinMinerUsageEvent());
        AttackBlockCallback.EVENT.register(new VersatilityUsageEvent());


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

        // 2. Server Receiver registrieren
        ServerPlayNetworking.registerGlobalReceiver(BuildingWandConfigurePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                // Code, der auf dem Server Thread läuft
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof BuildingWandItem) {
                    // NBT updaten
                    NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();

                    nbt.putBoolean("UseFullInventory", payload.useFullInventory());

                    stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                }
            });
        });
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
