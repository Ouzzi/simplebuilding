package com.simplebuilding.modules.simplemodels;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class Models {
    public static final String PREFIX = "@model:", RESET = "@model:reset";
    public static volatile ModelCatalogue.Snapshot server = new ModelCatalogue.Snapshot(new ModelPolicy(), List.of());
    public static Consumer<ServerPlayer> send = player -> {};
    public static Path root = Path.of("config", "simplemodels");
    public static void reload() {
        try { server = ModelCatalogue.load(root); }
        catch (Exception e) { var policy = new ModelPolicy(); policy.enabled = false; server = new ModelCatalogue.Snapshot(policy, List.of());
            org.slf4j.LoggerFactory.getLogger("simplemodels").error("Model catalogue disabled: {}", e.getMessage()); }
    }
    public static boolean request(String name) {
        return name != null && (name.startsWith(PREFIX) || server.policy().legacyNameMatching
                && server.models().stream().anyMatch(d -> d.match_name().equals(name)));
    }
    /** Anvil only: copies the input, never accepts an ItemStack or component from the client. */
    public static ItemStack assign(ItemStack input, String name, boolean operator) {
        var p = server.policy();
        if (input.isEmpty() || name == null || name.length() > 50 || !p.enabled || p.operatorsOnly && !operator) return ItemStack.EMPTY;
        var base = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        if (!p.allowModItems && !base.startsWith("minecraft:")) return ItemStack.EMPTY;
        if (RESET.equals(name)) {
            var original = new ItemStack(input.getItem()).get(DataComponents.ITEM_MODEL);
            if (java.util.Objects.equals(original, input.get(DataComponents.ITEM_MODEL))) return ItemStack.EMPTY;
            var out = input.copy();
            if (original == null) out.remove(DataComponents.ITEM_MODEL); else out.set(DataComponents.ITEM_MODEL, original);
            return out;
        }
        var def = server.models().stream().filter(d -> d.base_item().equals(base)
                && ((PREFIX + d.id()).equals(name) || p.legacyNameMatching && d.match_name().equals(name))).findFirst().orElse(null);
        if (def == null) return ItemStack.EMPTY;
        var model = ModelDefinition.safeId(def.model());
        if (model.equals(input.get(DataComponents.ITEM_MODEL))) return ItemStack.EMPTY;
        var out = input.copy(); out.set(DataComponents.ITEM_MODEL, model);
        // Identity stays visible: no UUID hiding, name/lore rewriting or gameplay components changed.
        return out;
    }
    public static void commands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("simplemodels")
                .requires(s -> net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(s.permissions()))
                .then(net.minecraft.commands.Commands.literal("reload").executes(c -> {
                    reload(); c.getSource().getServer().getPlayerList().getPlayers().forEach(send); return server.models().size();
                })));
    }
    private Models() {}
}
