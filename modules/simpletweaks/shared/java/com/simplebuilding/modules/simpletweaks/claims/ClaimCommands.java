package com.simplebuilding.modules.simpletweaks.claims;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;

public final class ClaimCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher,Claims.settings());
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ClaimConfig config) {
        if (!config.enabled()) return;
        if (dispatcher.getRoot().getChild("claim")!=null)
            throw new IllegalStateException("Enabled Simple Tweaks claims conflict with an existing /claim command; disable one claim provider.");
        dispatcher.register(Commands.literal("claim").requires(s->Claims.enabled(s.getServer()))
                .executes(c->{
                    var s=c.getSource(); var p=s.getPlayerOrException(); var claims=Claims.get(s.getServer());
                    if (claims.locked()) { s.sendFailure(Component.translatable("command.simpletweaks.claim.locked")); return 0; }
                    var key=new ClaimStore.Key(p.level().dimension().identifier().toString(),p.chunkPosition().pack());
                    var data=claims.view().get(key);
                    s.sendSuccess(()->Component.translatable(data==null?"command.simpletweaks.claim.empty":"command.simpletweaks.claim.owner",data==null?"":data.owner()),false);
                    return 1;
                })
                .then(Commands.literal("admin").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .executes(c->{c.getSource().sendSuccess(()->Component.translatable("command.simpletweaks.claim.stage",3),false);return 1;})));
    }
    private ClaimCommands() {}
}
