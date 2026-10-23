package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The claim deed. Empty: right-click claims the chunk (a small confirmation sheet, or at once without the client
 * mod) and binds the deed to its owner. Bound: right-click opens the rights-code editor of the owner's land.
 * The component is display only; every action is decided by the server ledger ({@code Claims}).
 */
public final class ClaimDeedItem extends Item {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("simpletweaks", "claim_deed");
    private static Item item;

    private ClaimDeedItem(Properties properties) { super(properties); }

    public static void register() {
        item = Registry.register(BuiltInRegistries.ITEM, ID, new ClaimDeedItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, ID))));
    }
    /** The registered item (after registration). */
    public static Item item() { return item != null ? item : BuiltInRegistries.ITEM.getValue(ID); }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (!(user instanceof ServerPlayer player) || !Claims.enabled(player.level().getServer())) return InteractionResult.PASS;
        int slot = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        if (DeedService.open(player, slot)) return InteractionResult.SUCCESS;
        // No screen possible (no client mod, no connection): the old direct behavior.
        return DeedService.useWithoutScreen(player, slot) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        DeedData data = stack.get(DeedData.TYPE);
        if (data == null) {
            out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.inactive"));
            out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.carto"));
            return;
        }
        out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.bound", data.ownerName()));
        if (data.generation() > 0) out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.copy"));
    }
}
