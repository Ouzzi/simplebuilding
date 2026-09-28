package com.simplebuilding.tweaks.block;

import net.minecraft.world.level.block.SkullBlock;

/**
 * Kopf-Typ des Lohenkopfs ({@code simplebuilding:blaze_head}). Vanillas Koepfe sind ein Enum
 * ({@code SkullBlock.Types}); ein Mod-Kopf traegt sich selbst in {@link SkullBlock.Type#TYPES} ein,
 * damit Block-Codec und das Item-Modell ({@code minecraft:head}, Feld {@code kind}) ihn per Namen
 * finden. Das Modell (Vanillas Mob-Kopf-Wuerfel 8x8x8) und die Textur haengt
 * {@code SkullModelMixin} beim Client an {@code SkullBlockRenderer}.
 */
public enum BlazeHeadType implements SkullBlock.Type {
    BLAZE("simplebuilding:blaze");

    private final String name;

    BlazeHeadType(String name) {
        this.name = name;
        SkullBlock.Type.TYPES.put(name, this);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
