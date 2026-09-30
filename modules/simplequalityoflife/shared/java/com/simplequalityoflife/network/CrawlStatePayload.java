package com.simplequalityoflife.network;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
/** Server-owned pose state. This payload is never registered in the C2S direction. */
public record CrawlStatePayload(UUID player,boolean crawling) implements CustomPacketPayload {
 public static final Type<CrawlStatePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("simplequalityoflife","crawl_state"));
 public static final StreamCodec<io.netty.buffer.ByteBuf,CrawlStatePayload> CODEC=StreamCodec.composite(UUIDUtil.STREAM_CODEC,CrawlStatePayload::player,ByteBufCodecs.BOOL,CrawlStatePayload::crawling,CrawlStatePayload::new);
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
