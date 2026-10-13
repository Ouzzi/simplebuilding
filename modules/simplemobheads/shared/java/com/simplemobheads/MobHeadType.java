package com.simplemobheads;

import com.simplemobheads.data.HeadDef;
import net.minecraft.world.level.block.SkullBlock;

/**
 * A skull type of Simple Mobheads: one per head ({@code simplemobheads:cat}, the type of its blocks) and one per further
 * variant ({@code simplemobheads:cat/siamese}, render-only: the block renderer, the worn head and the item model switch
 * to it by the {@code simplemobheads:variant} component). Each one enters {@link SkullBlock.Type#TYPES} so the block
 * codec and the item model field {@code kind} find it by name. No client class here: the look is plain data.
 */
public final class MobHeadType implements SkullBlock.Type {
    private final String name;
    private final HeadDef head;
    private final HeadDef.Look look;

    MobHeadType(HeadDef head, HeadDef.Look look) {
        this.name = look.kind();
        this.head = head;
        this.look = look;
        SkullBlock.Type.TYPES.put(name, this);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public HeadDef head() {
        return head;
    }

    public HeadDef.Look look() {
        return look;
    }

    @Override
    public String toString() {
        return name;
    }
}
