package com.simplebuilding.modules.simpleoptrender.mixin.fast_ip_ping;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.modules.simpleoptrender.FastIpPing;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M1: a literal IP needs neither DNS resolution, reverse lookup nor an SRV query (FastIpPing). */
@Mixin(ServerNameResolver.class)
abstract class ServerNameResolverMixin {
    @WrapOperation(method = "resolveAddress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/resolver/ServerAddressResolver;resolve(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;"))
    private Optional<ResolvedServerAddress> simpleoptrender$literal(ServerAddressResolver resolver, ServerAddress address,
                                                                    Operation<Optional<ResolvedServerAddress>> original) {
        InetAddress literal = resolver == ServerAddressResolver.SYSTEM ? FastIpPing.literal(address.getHost()) : null;
        return literal != null ? Optional.of(ResolvedServerAddress.from(new InetSocketAddress(literal, address.getPort())))
                : original.call(resolver, address);
    }

    @WrapOperation(method = "resolveAddress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/resolver/ServerRedirectHandler;lookupRedirect(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;"))
    private Optional<ServerAddress> simpleoptrender$noSrv(ServerRedirectHandler handler, ServerAddress address,
                                                          Operation<Optional<ServerAddress>> original) {
        return FastIpPing.literal(address.getHost()) != null ? Optional.empty() : original.call(handler, address);
    }
}
