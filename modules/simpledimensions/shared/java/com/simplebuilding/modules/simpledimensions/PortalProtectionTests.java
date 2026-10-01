package com.simplebuilding.modules.simpledimensions;

import com.simplebuilding.framework.api.Protection;
import dev.simpledimension.common.portal.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import static com.simplebuilding.modules.simpledimensions.DimensionTests.*;

/** Real portal mutations and teleports through the public provider contract. */
public final class PortalProtectionTests {
    private static final String PROVIDER="portal-test";
    private static boolean at(Protection.Target t,BlockPos p){return t.x()==p.getX()&&t.y()==p.getY()&&t.z()==p.getZ();}
    public static void footprints(GameTestHelper h) {
        var server=h.getLevel().getServer();var config=DimensionPortalConfig.defaultSkyblock();
        try {
            for(var axis:List.of(Direction.Axis.X,Direction.Axis.Z)) {
                var cell=build(h,config,1,axis);h.getLevel().setBlock(axis==Direction.Axis.X?cell.south(2):cell.east(2),Blocks.SEA_LANTERN.defaultBlockState(),3);
                var shape=PortalActivationService.match(new PortalWorld(h.getLevel(),config),config,cell.getX(),cell.getY(),cell.getZ()).orElseThrow();
                var cells=new HashSet<BlockPos>();shape.frame().forEach(c->cells.add(DimensionRuntime.pos(c.pos())));shape.interior().forEach(c->cells.add(DimensionRuntime.pos(c)));
                for(var blocked:cells) {
                    Protection.register(server,PROVIDER,t->!at(t,blocked));
                    var p=player(h,cell,Items.FIRE_CHARGE);
                    try {
                        h.assertTrue(ignite(p,cell)==InteractionResult.FAIL,"Every frame/interior cell is checked before activation: "+blocked);
                        h.assertTrue(p.getMainHandItem().getCount()==2,"Refused activation consumes no igniter");
                        for(var inside:shape.interior())h.assertTrue(h.getLevel().getBlockState(DimensionRuntime.pos(inside)).isAir(),"No partial portal mutation");
                    } finally {server.getPlayerList().remove(p);}
                }
                Protection.unregister(server,PROVIDER);
                h.assertTrue(ignite(player(h,cell,Items.FIRE_CHARGE),cell)==InteractionResult.SUCCESS,"No-provider positive control");
            }
        } finally {Protection.unregister(server,PROVIDER);}
        h.succeed();
    }
    public static void linkedTravel(GameTestHelper h) {
        h.runAfterDelay(155,()->{
            var cell=activate(h);var source=h.getLevel();var server=source.getServer();var rt=DimensionRuntime.get(server);
            var owner=player(h,cell,Items.AIR);var guest=player(h,cell,Items.AIR);
            var cfg=DimensionPortalConfig.defaultSkyblock();var shape=PortalActivationService.match(new PortalWorld(source,cfg),cfg,cell.getX(),cell.getY(),cell.getZ()).orElseThrow();
            try {
                // Deny a remote frame cell, leaving the contact and player position permitted.
                var remote=shape.frame().stream().map(c->DimensionRuntime.pos(c.pos())).filter(p->!p.equals(cell.below())).findFirst().orElseThrow();
                Protection.register(server,PROVIDER,t->!at(t,remote));
                h.assertTrue(!rt.travel(guest,cell)&&guest.level()==source,"Source frame denial prevents real travel");
                var sourceBox=guest.getBoundingBox();guest.setBoundingBox(sourceBox.move(.4,0,0));
                var touched=BlockPos.containing(guest.getBoundingBox().maxX,guest.getY(),guest.getZ());
                Protection.register(server,PROVIDER,t->!at(t,touched));
                try {h.assertTrue(!rt.travel(guest,cell),"Actual off-center source body cannot overlap a denied cell");}
                finally {guest.setBoundingBox(sourceBox);}
                Protection.register(server,PROVIDER,t->t.actor().equals(owner.getUUID()));
                h.assertTrue(!rt.travel(guest,cell),"Untrusted source denied");
                h.assertTrue(rt.travel(owner,cell),"Owner actually travels and generates return");
                var target=owner.level();var exit=owner.blockPosition();
                Protection.register(server,PROVIDER,t->!t.dimension().equals(target.dimension().identifier().toString())||!at(t,exit.above()));
                h.assertTrue(!rt.travel(guest,cell)&&guest.level()==source,"Linked destination portal head is checked");
                var normalBox=guest.getBoundingBox();guest.setBoundingBox(normalBox.inflate(.7,0,.7));
                Protection.register(server,PROVIDER,t->!t.dimension().equals(target.dimension().identifier().toString())||!at(t,exit.east()));
                try {h.assertTrue(!rt.travel(guest,cell),"Wider player cannot overlap an adjacent denied landing column");}
                finally {guest.setBoundingBox(normalBox);}
                Protection.register(server,PROVIDER,t->true);
                h.assertTrue(rt.travel(guest,cell)&&guest.level()==target,"Trusted traveler uses linked destination");
                Protection.register(server,PROVIDER,t->t.actor().equals(owner.getUUID())||!t.dimension().equals(source.dimension().identifier().toString())||Math.abs(t.x()-cell.getX())>20||Math.abs(t.z()-cell.getZ())>20);
                h.assertTrue(rt.travel(guest,exit)&&guest.level()==source,"Revoked original destination uses safe emergency exit");
                h.assertTrue(Math.abs(guest.getX()-cell.getX())>20||Math.abs(guest.getZ()-cell.getZ())>20,"Emergency return never enters denied source area");
                h.assertTrue(rt.travel(owner,exit)&&owner.level()==source,"Owner retains exact return");
                Protection.register(server,PROVIDER,t->true);
                h.assertTrue(rt.travel(guest,cell),"Guest returns to generated portal for source revocation check");
                Protection.register(server,PROVIDER,t->!t.dimension().equals(target.dimension().identifier().toString()));
                h.assertTrue(rt.travel(guest,exit)&&guest.level()==source,"Generated exit allows egress from newly denied source");
                Protection.register(server,PROVIDER,t->true);
                var generated=(SkyPortalBlockEntity)target.getBlockEntity(exit);var linkDimension=generated.linkDimension;var link=generated.link;
                try {
                    h.assertTrue(rt.travel(guest,cell),"Return-link recovery fixture reaches destination");generated.linkDimension="simpledimension:missing";
                    h.assertTrue(rt.travel(guest,exit)&&guest.level()==source,"Missing generated-return dimension uses checked fallback");generated.linkDimension=linkDimension;
                    h.assertTrue(rt.travel(guest,cell),"Border recovery fixture reaches destination");generated.link=new BlockPos(30000001,80,30000001);
                    h.assertTrue(rt.travel(guest,exit)&&guest.level()==source,"Out-of-border generated return uses checked fallback");
                } finally {generated.linkDimension=linkDimension;generated.link=link;}
            } finally {Protection.unregister(server,PROVIDER);}
            h.succeed();
        });
    }
    public static void unsupportedProvider(GameTestHelper h) {
        var cell=activate(h);var p=player(h,cell,Items.AIR);var server=h.getLevel().getServer();var rt=DimensionRuntime.get(server);
        boolean required=DimensionRuntime.claimIntegrationRequired;var old=DimensionRuntime.permission;
        try {
            DimensionRuntime.claimIntegrationRequired=true;DimensionRuntime.permission=null;
            Protection.register(server,PROVIDER,t->true);
            h.assertTrue(!rt.travel(p,cell)&&!rt.emergencyReturn(p),"Supported framework provider cannot mask unsupported foreign mod");
            var blank=build(h,DimensionPortalConfig.defaultSkyblock(),0,Direction.Axis.X);h.getLevel().setBlock(blank.offset(0,0,2),Blocks.SEA_LANTERN.defaultBlockState(),3);
            h.assertTrue(ignite(player(h,blank,Items.FIRE_CHARGE),blank)==InteractionResult.FAIL,"Unsupported foreign mod blocks activation");
        } finally {Protection.unregister(server,PROVIDER);DimensionRuntime.claimIntegrationRequired=required;DimensionRuntime.permission=old;}
        h.succeed();
    }
}
