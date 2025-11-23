package com.simplemoney.items;

import com.simplemoney.Simplemoney;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.util.Rarity.*;

/**
 * Verwaltet die Registrierung aller benutzerdefinierten Gegenstände (Items) des Simplemoney Mods.
 * Definiert die Eigenschaften der Währungskomponenten und des endgültigen Geldscheins.
 */
public class ModItems {


    /**
     * BANKNOTE_FIBER: Die Rohfaser für die Währung.
     * Eigenschaften: Max. Stapelgröße 1 (Einzelstück), Standard-Rarity (COMMON).
     */
    public static final Item BANKNOTE_PAPER = registerItem("banknote_paper", setting -> new Item(setting.maxCount(64))
            {
                @Override
                public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
                    textConsumer.accept(Text.translatable("tooltip.simplemoney.banknote_paper.tooltip"));
                    super.appendTooltip(stack, context, displayComponent, textConsumer, type);
                }
            }
    );
    /**
     * BANKNOTE_FIBER: Die Rohfaser für die Währung.
     * Eigenschaften: Max. Stapelgröße 1 (Einzelstück), Standard-Rarity (COMMON).
     */
    public static final Item BANKNOTE_FIBER = registerItem("banknote_fiber", setting -> new Item(setting.maxCount(16))
            {
                @Override
                public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
                    textConsumer.accept(Text.translatable("tooltip.simplemoney.banknote_fiber.tooltip"));
                    super.appendTooltip(stack, context, displayComponent, textConsumer, type);
                }
            }
    );
    /**
     * BANKNOTE_BLANK: Der unbedruckte Rohling.
     * Eigenschaften: Rarity UNCOMMON, Max. Stapelgröße 64 (standardmäßig, da maxCount fehlt).
     */
    public static final Item BANKNOTE_BLANK = registerItem("banknote_blank", setting -> new Item(setting.rarity(UNCOMMON))
            {
                @Override
                public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
                    textConsumer.accept(Text.translatable("tooltip.simplemoney.banknote_blank.tooltip"));
                    super.appendTooltip(stack, context, displayComponent, textConsumer, type);
                }
            }
    );
    /**
     * FRESH_BILL: Der frisch gedruckte, aber noch nicht endgültige Geldschein.
     * Eigenschaften: Rarity RARE, Max. Stapelgröße 64 (standardmäßig).
     */
    public static final Item FRESH_BILL = registerItem("fresh_bill", setting -> new Item(setting.rarity(RARE))
            {
                @Override
                public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
                    textConsumer.accept(Text.translatable("tooltip.simplemoney.fresh_bill.tooltip"));
                    super.appendTooltip(stack, context, displayComponent, textConsumer, type);
                }
            }
    );
    /**
     * MONEY_BILL: Der fertige, gültige Geldschein und die Custom Currency des Mods.
     * Eigenschaften: Feuerfest (fireproof), Rarity EPIC, permanenter Glint (Glühen-Effekt).
     */
    public static final Item MONEY_BILL = registerItem("money_bill", setting -> new Item(setting.fireproof().rarity(EPIC))

            {
                /**
                 * Überschreibt hasGlint(), um dem Gegenstand einen permanenten Glühen-Effekt zu verleihen.
                 * @param stack Der aktuelle ItemStack.
                 * @return Gibt immer true zurück, um den Glint zu erzwingen.
                 */
                @Override
                public boolean hasGlint(ItemStack stack) {
                    return true;
                }
                @Override
                public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
                    textConsumer.accept(Text.translatable("tooltip.simplemoney.money_bill.tooltip"));
                    super.appendTooltip(stack, context, displayComponent, textConsumer, type);
                }
                @Override
                public ActionResult use(World world, PlayerEntity user, Hand hand) {

                    if (!world.isClient()) {
                        // Throw only one item from stack like clicking q
                        user.dropItem(user.getStackInHand(hand).split(1), false);
                        // Play sound effect for using the money bill
                        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.6f, 6.0f);
                    }

                    return ActionResult.SUCCESS;
                }
            }
    );


    /**
     * Registriert ein Item unter einem einfachen Namen und der Mod-ID.
     * @param name Der Bezeichner des Items (z.B. "banknote_fiber").
     * @param item Das zu registrierende Item-Objekt.
     * @return Das registrierte Item.
     */
    public static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Simplemoney.MOD_ID, name), item);
    }

    /**
     * Hilfsmethode zur Registrierung eines Items unter Verwendung einer Funktion,
     * die die Item.Settings-Konfiguration anwendet.
     * @param name Der Bezeichner des Items.
     * @param function Die Funktion, die die Item.Settings verarbeitet und das Item erstellt.
     * @return Das registrierte Item.
     */
    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        return Registry.register(Registries.ITEM, Identifier.of(Simplemoney.MOD_ID, name),
                function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Simplemoney.MOD_ID, name)))));
    }

    /**
     * Registriert alle Items des Mods.
     * Führt die eigentliche Registrierung durch und fügt die Items zu den entsprechenden Item Groups hinzu.
     */
    public static void registerModItems() {
        Simplemoney.LOGGER.info("Registering Mod Items for " + Simplemoney.MOD_ID);

        // Fügt alle Währungs-Items zur Standard "Ingredients" Item Group hinzu.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(BANKNOTE_PAPER);
            entries.add(BANKNOTE_FIBER);
            entries.add(BANKNOTE_BLANK);
            entries.add(FRESH_BILL);
            entries.add(MONEY_BILL);
        });
    }

}