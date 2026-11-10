package com.simplebuilding.modules.simplevault.block;

import com.simplebuilding.modules.simplevault.gen.VaultLayout;
import net.minecraft.util.StringRepresentable;

/** The three seal parts of the Bedrock Vault and their colour (UNDER21): light = yellow, sound = violet, path = green. */
public enum SealPart implements StringRepresentable {
    LIGHT("light", 0xE8C860), SOUND("sound", 0xC890E8), PATH("path", 0x88D870);

    public final String name;
    public final int color;

    SealPart(String name, int color) {
        this.name = name;
        this.color = color;
    }

    /** Index in {@link VaultLayout} arrays (LIGHT, SOUND, PATH). */
    public int index() {
        return ordinal();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
