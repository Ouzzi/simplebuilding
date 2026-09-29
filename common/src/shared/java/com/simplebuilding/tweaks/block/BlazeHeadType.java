package com.simplebuilding.tweaks.block;

import net.minecraft.world.level.block.SkullBlock;

/**
 * Kopf-Typen der Mod-Koepfe (Name historisch: zuerst gab es nur den Lohenkopf). Lohenkopf
 * ({@code simplebuilding:blaze_head}, Trank-Pad I), Endermankopf (Spawn-Teleporter I, seit 2026-09-28) und seit
 * 2026-09-29 die Koepfe der Trial-Chamber-Mobs, die Vanilla nicht hat (Wuestenzombie, Spinne, Hoehlenspinne,
 * Eiswanderer, Sumpfskelett, Schleim, Silberfischchen, Breeze), dazu Shulker (Flypad I) und Ertrunkener.
 * Alle fallen nur, wenn die Explosion eines geladenen Creepers den Mob toetet, und jeder hat eine geheime,
 * harmlose Faehigkeit beim Tragen ({@code tweaks.heads.HeadAbilities}, docs/MOBKOEPFE.md).
 *
 * <p>Vanillas Koepfe sind ein Enum ({@code SkullBlock.Types}); ein Mod-Kopf traegt sich selbst in
 * {@link SkullBlock.Type#TYPES} ein, damit Block-Codec und das Item-Modell ({@code minecraft:head}, Feld
 * {@code kind}) ihn per Namen finden. Modell und Textur (die echten Vanilla-Mob-Texturen, siehe
 * {@code ModSkullModels}) haengt {@code SkullModelMixin} beim Client an {@code SkullBlockRenderer}.
 *
 * <p>Kein Verweis auf Entity- oder Item-Klassen hier: das Enum wird schon beim Lesen eines Kopf-Blockzustands
 * geladen. Welcher Mob zu welchem Kopf gehoert, steht in {@code tweaks.heads.ModHeads}.
 */
public enum BlazeHeadType implements SkullBlock.Type {
    BLAZE("simplebuilding:blaze", "blaze_head"),
    ENDERMAN("simplebuilding:enderman", "enderman_head"),
    HUSK("simplebuilding:husk", "husk_head"),
    SPIDER("simplebuilding:spider", "spider_head"),
    CAVE_SPIDER("simplebuilding:cave_spider", "cave_spider_head"),
    STRAY("simplebuilding:stray", "stray_skull"),
    BOGGED("simplebuilding:bogged", "bogged_skull"),
    SLIME("simplebuilding:slime", "slime_head"),
    SILVERFISH("simplebuilding:silverfish", "silverfish_head"),
    BREEZE("simplebuilding:breeze", "breeze_head"),
    SHULKER("simplebuilding:shulker", "shulker_head"),
    DROWNED("simplebuilding:drowned", "drowned_head");

    private final String name;
    private final String blockName;

    BlazeHeadType(String name, String blockName) {
        this.name = name;
        this.blockName = blockName;
        SkullBlock.Type.TYPES.put(name, this);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Id-Pfad des stehenden Kopfes und seines Items ({@code blaze_head}, {@code stray_skull}, ...). */
    public String blockName() {
        return blockName;
    }

    /** Id-Pfad der Wandvariante wie bei Vanilla ({@code blaze_wall_head}, {@code stray_wall_skull}). */
    public String wallBlockName() {
        return blockName.endsWith("_skull")
                ? blockName.substring(0, blockName.length() - "_skull".length()) + "_wall_skull"
                : blockName.substring(0, blockName.length() - "_head".length()) + "_wall_head";
    }
}
