package com.simplebuilding.tweaks.block;

import net.minecraft.world.level.block.SkullBlock;

/**
 * Kopf-Typen der Mod-Koepfe: Lohenkopf ({@code simplebuilding:blaze_head}, Trank-Pad I) und
 * Endermankopf ({@code simplebuilding:enderman_head}, Spawn-Teleporter I, seit 2026-09-28). Vanillas Koepfe sind ein Enum
 * ({@code SkullBlock.Types}); ein Mod-Kopf traegt sich selbst in {@link SkullBlock.Type#TYPES} ein,
 * damit Block-Codec und das Item-Modell ({@code minecraft:head}, Feld {@code kind}) ihn per Namen
 * finden. Modell und Textur (die echten Vanilla-Mob-Texturen, siehe {@code ModSkullModels}) haengt
 * {@code SkullModelMixin} beim Client an {@code SkullBlockRenderer}.
 */
public enum BlazeHeadType implements SkullBlock.Type {
    BLAZE("simplebuilding:blaze"),
    ENDERMAN("simplebuilding:enderman");

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
