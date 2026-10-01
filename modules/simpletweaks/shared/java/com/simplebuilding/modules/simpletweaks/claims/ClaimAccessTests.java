package com.simplebuilding.modules.simpletweaks.claims;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.util.*;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

public final class ClaimAccessTests {
    static CommandDispatcher<CommandSourceStack> dispatcher(ClaimConfig config) {
        var d=new CommandDispatcher<CommandSourceStack>();ClaimCommands.register(d,config);return d;
    }
    static int execute(CommandDispatcher<CommandSourceStack> d,ServerPlayer p,String text) {
        try { return d.execute(text,p.createCommandSourceStack().withSuppressedOutput()); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) { return -1; }
    }
    static ClaimStore.Key key(ServerPlayer p) { return new ClaimStore.Key(p.level().dimension().identifier().toString(),p.chunkPosition().pack()); }
    static void seed(Path root,ClaimStore.Key key,UUID owner,Set<UUID> guests) {
        try {new ClaimStore(root.resolve("simpletweaks-claims.json"),Map.of()).replace(Map.of(key,new ClaimStore.Claim(owner,guests)));}
        catch (java.io.IOException e) {throw new AssertionError(e);}
    }
    public static void commandsAndAccess(GameTestHelper h) {
        var server=h.getLevel().getServer();var previous=Claims.get(server);var root=temp();
        var pos=h.absolutePos(new BlockPos(3,3,3));var owner=player(h,pos);var guest=player(h,pos);
        seed(root,key(owner),owner.getUUID(),Set.of());var c=new Claims(on(),root,Map.of());Claims.install(server,c);
        try {
            var d=dispatcher(on());h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            yes(h,!guest.gameMode.destroyBlock(pos),"Untrusted real break denied");
            yes(h,execute(d,guest,"claim trust "+guest.getUUID())==0,"Outsider cannot self-grant");
            yes(h,execute(d,owner,"claim trust "+guest.getUUID())==1,"Owner dispatch grants offline UUID");
            var snapshot=c.view();yes(h,snapshot.get(key(owner)).permits(guest.getUUID()),"Grant published");
            yes(h,guest.gameMode.destroyBlock(pos),"Trusted real break succeeds");
            yes(h,execute(d,guest,"claim trust "+UUID.randomUUID())==0,"Trust grants no delegation");
            yes(h,execute(d,owner,"claim untrust "+guest.getUUID())==0,"Mutation cooldown enforced by dispatch");
            yes(h,!c.create(new ClaimStore.Key("minecraft:the_nether",200),owner.getUUID(),server.overworld().getGameTime()),"Trust shares claim cooldown across dimensions");
            c=new Claims(on(),root,Map.of());Claims.install(server,c);
            yes(h,Claims.allow(guest,h.getLevel(),pos),"Trusted access survives ledger reload");
            yes(h,execute(d,owner,"claim untrust "+guest.getUUID())==1,"Owner dispatch revokes after restart");
            yes(h,snapshot.get(key(owner)).permits(guest.getUUID()),"Old snapshot is unchanged");
            h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            yes(h,!guest.gameMode.destroyBlock(pos),"Revocation immediately blocks actual break");
            Claims.install(server,new Claims(on(),root,Map.of()));
            yes(h,!guest.gameMode.destroyBlock(pos),"Revocation survives reload");
            yes(h,owner.gameMode.destroyBlock(pos),"Owner retains real access");
            for(String bad:List.of("1-1-1-1-1","00000000-0000-0000-0000-000000000000","@a","UnknownOffline","../owner","abcdefghijklmnopq"))
                yes(h,execute(d,owner,"claim trust "+bad)==-1,"Unsafe/unknown identity refused: "+bad);
            yes(h,execute(d,owner,"claim trust "+owner.getUUID())==0,"Owner cannot consume a trust slot");
            var online=new ServerPlayer(server,h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AccessGuest"),net.minecraft.server.level.ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
            server.getPlayerList().placeNewPlayer(connection,online,net.minecraft.server.network.CommonListenerCookie.createInitial(online.getGameProfile(),false));
            try {yes(h,execute(d,owner,"claim trust accessguest")==1&&Claims.allow(h.getLevel(),online.getUUID(),pos),"Online name resolves to server player UUID, case insensitive");}
            finally {server.getPlayerList().remove(online);channel.finishAndReleaseAll();}
            UUID offline=UUID.randomUUID();Claims.install(server,new Claims(on(),root,Map.of()));
            yes(h,server.getPlayerList().getPlayer(offline)==null&&execute(d,owner,"claim trust "+offline)==1,"Absent player UUID can be trusted");
            Claims.install(server,new Claims(on(),root,Map.of()));var returning=player(h,pos);returning.setUUID(offline);
            h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            yes(h,returning.gameMode.destroyBlock(pos),"Offline grant works when that UUID joins after reload");
            yes(h,execute(d,owner,"claim untrust "+offline)==1,"Offline UUID can be revoked without name resolution");
        } finally {Claims.install(server,previous);}
        h.succeed();
    }
    public static void capsAndFailures(GameTestHelper h) {
        var server=h.getLevel().getServer();var previous=Claims.get(server);var pos=h.absolutePos(new BlockPos(3,3,3));
        var owner=player(h,pos);var guest=player(h,pos);
        try {
            for(boolean revoke:List.of(false,true)) {
                var root=temp();seed(root,key(owner),owner.getUUID(),revoke?Set.of(guest.getUUID()):Set.of());
                var c=new Claims(on(),root,Map.of());Claims.install(server,c);var old=c.view();
                Path file=root.resolve("simpletweaks-claims.json"),backup=root.resolve("saved.json");
                try {Files.move(file,backup);Files.createDirectory(file);}catch(java.io.IOException e){throw new AssertionError(e);}
                yes(h,execute(dispatcher(on()),owner,"claim "+(revoke?"untrust ":"trust ")+guest.getUUID())==0,"Atomic failure refuses command");
                yes(h,c.locked()&&!Claims.allow(guest,h.getLevel(),pos),"Failed grant/revocation locks access");
                try {yes(h,new ClaimStore(backup,Map.of()).view().equals(old),"Saved authority unchanged after failed write");}catch(java.io.IOException e){throw new AssertionError(e);}
            }
            var root=temp();seed(root,key(owner),owner.getUUID(),Set.of(guest.getUUID()));
            var zero=new ClaimConfig(true,1,0,1,20,false,0,on().dimensions());Claims.install(server,new Claims(zero,root,Map.of()));
            var d=dispatcher(zero);
            yes(h,Claims.allow(guest,h.getLevel(),pos),"Lowered cap preserves existing rights");
            yes(h,execute(d,owner,"claim trust "+UUID.randomUUID())==0,"Configured zero cap refuses grant");
            yes(h,execute(d,owner,"claim untrust "+guest.getUUID())==1,"Lowered cap never prevents revocation");
            var cappedRoot=temp();seed(cappedRoot,key(owner),owner.getUUID(),Set.of(guest.getUUID(),UUID.randomUUID()));
            Claims.install(server,new Claims(on(),cappedRoot,Map.of()));
            yes(h,execute(dispatcher(on()),owner,"claim trust "+UUID.randomUUID())==0,"Configured positive trust cap enforced");
            var full=new HashSet<UUID>();for(int i=0;i<64;i++)full.add(UUID.randomUUID());
            new ClaimStore.Claim(owner.getUUID(),full);full.add(UUID.randomUUID());
            try {new ClaimStore.Claim(owner.getUUID(),full);throw new AssertionError("Hard trust cap bypass");}catch(IllegalArgumentException expected){}
        } finally {Claims.install(server,previous);}
        h.succeed();
    }
    public static void adminPolicy(GameTestHelper h) {
        var server=h.getLevel().getServer();var previous=Claims.get(server);var players=server.getPlayerList();
        var pos=h.absolutePos(new BlockPos(3,3,3));var operator=player(h,pos);UUID owner=UUID.randomUUID();
        try {
            for(boolean bypass:List.of(false,true)) {
                var config=new ClaimConfig(true,2,2,3,20,bypass,0,on().dimensions());var root=temp();seed(root,key(operator),owner,Set.of());
                Claims.install(server,new Claims(config,root,Map.of()));var d=dispatcher(config);
                players.op(operator.nameAndId(),Optional.of(LevelBasedPermissionSet.ADMIN),Optional.empty());
                yes(h,execute(d,operator,"claim admin listall")==-1&&!Claims.allow(operator,h.getLevel(),pos),"OP3 cannot administer or bypass");
                players.op(operator.nameAndId(),Optional.of(LevelBasedPermissionSet.OWNER),Optional.empty());
                yes(h,Claims.allow(operator,h.getLevel(),pos)==bypass,"OP4 protection obeys config");
                yes(h,execute(d,operator,"claim admin list "+owner)==1,"OP4 lists offline owner independent of bypass");
                yes(h,execute(d,operator,"claim trust "+operator.getUUID())==0,"OP4 cannot silently grant itself trust");
                yes(h,execute(d,operator,"claim unclaim")==0,"Owner command does not inherit admin bypass");
                yes(h,execute(d,operator,"claim admin unclaim")==1,"Explicit OP4 removal works independent of bypass");
                yes(h,Claims.allow(operator,h.getLevel(),pos),"Admin removal publishes unclaimed access");
            }
        } finally {players.deop(operator.nameAndId());Claims.install(server,previous);}
        h.succeed();
    }
}
