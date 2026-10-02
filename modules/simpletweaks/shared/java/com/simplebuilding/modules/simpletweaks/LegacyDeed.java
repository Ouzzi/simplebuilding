package com.simplebuilding.modules.simpletweaks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
/**
 * Legacy custom data is retained, but only the server ledger grants authority.
 *
 * <p>Deliberately in no creative tab (audit 2026-10-02): the deed is an inactive legacy item that only
 * keeps old stacks loadable; it has no recipe and is reachable by /give only.
 */
public final class LegacyDeed {
    public static void register() {
        var id = Identifier.fromNamespaceAndPath("simpletweaks", "claim_deed");
        Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().stacksTo(16).setId(ResourceKey.create(Registries.ITEM, id))) {
            @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player user, net.minecraft.world.InteractionHand hand) {
                if (!(user instanceof net.minecraft.server.level.ServerPlayer player) || !com.simplebuilding.modules.simpletweaks.claims.Claims.enabled(player.level().getServer())) return net.minecraft.world.InteractionResult.PASS;
                boolean success=com.simplebuilding.modules.simpletweaks.claims.Claims.get(player.level().getServer()).claim(player);
                // Same cue family as SimpleBuilding and the other modules: failure is the dry dispenser click.
                if (success) player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.4f,1f);
                else player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.DISPENSER_FAIL,net.minecraft.sounds.SoundSource.PLAYERS,.5f,1.2f);
                player.level().sendParticles(success?net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER:net.minecraft.core.particles.ParticleTypes.SMOKE,player.getX(),player.getY()+1,player.getZ(),success?6:4,.3,.3,.3,0);
                return success?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.FAIL;
            }
            @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
                out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.inactive"));
                out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.inactive.2"));
                out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.data"));
                out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.data.2"));
            }
        });
    }
    private LegacyDeed() {}
}
