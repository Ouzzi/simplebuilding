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

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    public static void setClientOpener(BiConsumer<Player, InteractionHand> opener) {
        clientOpener = opener != null ? opener : (player, hand) -> {
        };
    }

    public static BlueprintContent content(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Im Baumodus (Baustab in der Haupthand) oeffnet ein Luftklick nicht den Editor.
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof BuildingWandItem) {
            return InteractionResult.PASS;
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

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        BlueprintContent content = content(stack);
        if (content.signed() && !content.author().isBlank()) {
            lines.accept(Component.translatable("book.byAuthor", content.author()).withStyle(ChatFormatting.GRAY));
        }
        if (content.isBlank()) {
            lines.accept(Component.translatable("simplebuilding.blueprint.tooltip.empty").withStyle(ChatFormatting.GRAY));
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
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
