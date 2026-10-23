package com.simplequestionary.net;

import com.simplequestionary.QuestionaryFacts;
import com.simplequestionary.SimpleQuestionary;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the recipe facts of the item in the table's slot ({@link QuestionaryFacts}). */
public record FactsPayload(Identifier item, List<Identifier> madeFrom, Identifier station, int makes, int usedIn) implements CustomPacketPayload {
    public static final Type<FactsPayload> TYPE = new Type<>(SimpleQuestionary.id("facts"));
    public static final StreamCodec<ByteBuf, FactsPayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, FactsPayload::item,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(QuestionaryFacts.MAX_INGREDIENTS)), FactsPayload::madeFrom,
            Identifier.STREAM_CODEC, FactsPayload::station,
            ByteBufCodecs.VAR_INT, FactsPayload::makes, ByteBufCodecs.VAR_INT, FactsPayload::usedIn,
            FactsPayload::new);

    /** The facts the client shows; set on receipt (any thread hop is done by the loader). */
    public static volatile QuestionaryFacts.Facts latest = QuestionaryFacts.Facts.NONE;

    public static FactsPayload of(QuestionaryFacts.Facts facts) {
        return new FactsPayload(facts.item(), facts.madeFrom(), facts.station(), facts.makes(), facts.usedIn());
    }

    /** Client receipt (shared code, no client classes). */
    public static void receive(FactsPayload payload) {
        latest = new QuestionaryFacts.Facts(payload.item(), List.copyOf(payload.madeFrom()), payload.station(), payload.makes(), payload.usedIn());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
