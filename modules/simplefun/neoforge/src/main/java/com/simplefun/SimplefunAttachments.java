package com.simplefun;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * NeoForge-only synced data attachment carrying the piggy state. Unlike a mixin-added
 * SynchedEntityData id (which breaks NeoForge's registry init), an attachment is the
 * loader-native way to attach + sync data to entities, and it is sent to all tracking
 * clients so remote players see the pig head too.
 */
public class SimplefunAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PIGGY =
            ATTACHMENT_TYPES.register("piggy", () -> AttachmentType.<Boolean>builder(() -> false)
                    .sync((holder, player) -> true, ByteBufCodecs.BOOL)
                    .build());
}
