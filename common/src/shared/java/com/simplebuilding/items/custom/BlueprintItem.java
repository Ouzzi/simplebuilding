package com.simplebuilding.items.custom;

import com.simplebuilding.blueprint.BlueprintCode;
import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.blueprint.BlueprintModel;
import com.simplebuilding.blueprint.BlueprintTiers;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.tooltip.BlueprintTooltipData;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Blaupause: traegt ein Bauwerk als lesbaren Bau-Code ({@link BlueprintContent}). Benutzen oeffnet
 * den Editor; am Kartentisch fuellt ein Oktant sie mit seiner Auswahl; Baustab in der Haupthand
 * und Blaupause in der Nebenhand bauen sie (docs/BLUEPRINT.md).
 */
public class BlueprintItem extends Item {

    /** Oeffnet auf dem Client den Editor; die Loader setzen es beim Client-Start (sonst nichts). */
    private static BiConsumer<Player, InteractionHand> clientOpener = (player, hand) -> {
    };

    /** Oeffnet auf dem Client den Editor fuer eine abgelegte Blaupause (Position, gespeicherter Stapel). */
    private static BiConsumer<net.minecraft.core.BlockPos, ItemStack> placedOpener = (pos, stack) -> {
    };

    /** Kreativ-Blaupause (Queue N29): unbegrenzte Reichweite beim Bauen, signiert endgueltig (auch die Kopie). */
    private final boolean creative;

    public BlueprintItem(Properties properties) {
        this(properties, false);
    }

    public BlueprintItem(Properties properties, boolean creative) {
        super(properties);
        this.creative = creative;
    }

    /** Ob {@code stack} eine Kreativ-Blaupause ist. */
    public static boolean isCreative(ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem item && item.creative;
    }

    public static void setClientOpener(BiConsumer<Player, InteractionHand> opener) {
        clientOpener = opener != null ? opener : (player, hand) -> {
        };
    }

    public static void setPlacedOpener(BiConsumer<net.minecraft.core.BlockPos, ItemStack> opener) {
        placedOpener = opener != null ? opener : (pos, stack) -> {
        };
    }

    /** Nur auf dem Client: Editor fuer die abgelegte Blaupause an {@code pos} (Queue N23). */
    public static void openPlaced(net.minecraft.core.BlockPos pos, ItemStack stack) {
        placedOpener.accept(pos, stack);
    }

    public static BlueprintContent content(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
    }

    /** Modellzustand fuer {@code simplebuilding:blueprint_state}: frisch gebaut bzw. leer geraeumt. */
    public static final String STATE_EMPTY = "empty";
    /** Beschrieben (Code oder Titel), aber nicht signiert. */
    public static final String STATE_EDITED = "edited";
    /** Signiert (schreibgeschuetzt). */
    public static final String STATE_SIGNED = "signed";

    /**
     * Welche Textur die Blaupause zeigt (Besitzer 2026-09-28): die normale, solange nichts
     * darauf steht, eine leicht veraenderte fuer eine bearbeitete, eine deutlich andere fuer eine
     * signierte. Das Item-Modell fragt zuerst {@code minecraft:has_component} und dann diesen Wert.
     */
    public static String modelState(ItemStack stack) {
        BlueprintContent content = stack.get(ModDataComponentTypes.BLUEPRINT);
        if (content == null) {
            return STATE_EMPTY;
        }
        if (content.signed()) {
            return STATE_SIGNED;
        }
        return content.isBlank() && content.title().isBlank() ? STATE_EMPTY : STATE_EDITED;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Im Baumodus (Baustab in der Haupthand) oeffnet ein Luftklick nicht den Editor.
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof BuildingWandItem) {
            return InteractionResult.PASS;
        }
        if (com.simplebuilding.config.ServerTuning.featureDenied(com.simplebuilding.config.ServerTuning.get().features.blueprint, player)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            clientOpener.accept(player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        BlueprintContent content = content(stack);
        if (content.signed() && !content.title().isBlank()) {
            return Component.literal(content.title());
        }
        return super.getName(stack);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        BlueprintContent content = content(stack);
        if (content.isBlank()) {
            return Optional.empty();
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(content.code());
        if (parsed.model().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlueprintTooltipData(content.code()));
    }

    /** Kreativ-Blaupause: ein Satz zu Reichweite und Signatur. */
    private void creativeHint(ItemStack stack, Consumer<Component> lines) {
        if (creative) {
            lines.accept(Component.translatable("simplebuilding.creative_blueprint.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        BlueprintContent content = content(stack);
        if (content.signed() && !content.author().isBlank()) {
            lines.accept(Component.translatable("book.byAuthor", content.author()).withStyle(ChatFormatting.GRAY));
        }
        if (content.isBlank()) {
            lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.empty").withStyle(ChatFormatting.GRAY));
            lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.empty.2").withStyle(ChatFormatting.GRAY));
            creativeHint(stack, lines);
            super.appendHoverText(stack, context, display, lines, flag);
            return;
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parseCached(content.code());
        BlueprintModel model = parsed.model();
        if (!parsed.ok()) {
            lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.errors", parsed.problems().size()).withStyle(ChatFormatting.RED));
        }
        if (!model.isEmpty()) {
            lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.size",
                    model.sizeX(), model.sizeY(), model.sizeZ(), model.size()).withStyle(ChatFormatting.GRAY));
            int tier = BlueprintTiers.tierIndexFor(model.maxEdge());
            if (tier >= 0) {
                lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.needs", BlueprintTiers.wandName(tier))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        if (!content.signed()) {
            lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.unsigned").withStyle(ChatFormatting.DARK_GRAY));
        }
        creativeHint(stack, lines);
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
