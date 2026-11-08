package com.simplepads.compat.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** The pad status lines from the server's data (switchable as {@code config.jade.plugin_simplepads.pad_status}). */
final class PadsJadeClient implements IBlockComponentProvider {
    /** At most this many effect lines, then "...". */
    static final int MAX_EFFECT_LINES = 3;
    private static final String KEY = "jade.simplepads.";

    @Override
    public Identifier getUid() {
        return PadsJadeServer.UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        int a = data.getIntOr(PadsJadeServer.A, 0);
        switch (data.getStringOr(PadsJadeServer.KIND, "")) {
            case "launchpad" -> tooltip.add(Component.translatable(KEY + "launchpad.charges", a, data.getIntOr(PadsJadeServer.B, 0)));
            case "potion_pad" -> {
                String effects = data.getStringOr(PadsJadeServer.EFFECTS, "");
                if (effects.isEmpty()) {
                    tooltip.add(Component.translatable(KEY + "potion_pad.empty"));
                } else {
                    int shown = 0;
                    for (String effect : effects.split(",")) {
                        if (shown++ == MAX_EFFECT_LINES) {
                            tooltip.add(Component.translatable(KEY + "potion_pad.more"));
                            break;
                        }
                        tooltip.add(Component.translatable(KEY + "potion_pad.effect", effectName(effect)));
                    }
                }
                tooltip.add(a > 0 ? Component.translatable(KEY + "potion_pad.cooldown", (a + 19) / 20)
                        : Component.translatable(KEY + "potion_pad.ready"));
            }
            case "chunk_loader" -> tooltip.add(a > 0 ? Component.translatable(KEY + "chunk_loader.active", a)
                    : Component.translatable(KEY + "chunk_loader.idle"));
            default -> {
            }
        }
    }

    /** "effect.minecraft.speed@1" -> "Speed II". */
    private static Component effectName(String encoded) {
        int at = encoded.lastIndexOf('@');
        String id = at < 0 ? encoded : encoded.substring(0, at);
        int amplifier = 0;
        try {
            amplifier = at < 0 ? 0 : Integer.parseInt(encoded.substring(at + 1));
        } catch (NumberFormatException ignored) {
        }
        MutableComponent name = Component.translatable(id);
        return amplifier > 0 ? name.append(" ").append(Component.translatable("potion.potency." + amplifier)) : name;
    }
}
