package com.simplebuilding.modules.simpletweaks.claims;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Registry-resolved gameplay entry points, not permission-helper assertions. */
final class ToolClaimTests {
    static Item item(String id){return BuiltInRegistries.ITEM.getValue(Identifier.parse("simplebuilding:"+id));}
    static BlockPos boundary(GameTestHelper h){var p=h.absolutePos(new BlockPos(2,64,2));return new BlockPos((p.getX()>>4)*16+16,p.getY(),(p.getZ()>>4)*16+8);}
    static void wandHammer(GameTestHelper h){with(h,c->{
        var l=h.getLevel();var inside=boundary(h);var origin=inside.west();var owner=player(h,origin.west(4));var other=player(h,origin.west(4));
        yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Tool border fixture");
        var hit=new BlockHitResult(Vec3.atCenterOf(origin).add(0,.5,0),Direction.UP,origin,false);
        for(var actor:List.of(other,owner)){
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=origin.offset(x,0,z);l.setBlock(at,Blocks.STONE.defaultBlockState(),3);l.setBlock(at.above(),Blocks.AIR.defaultBlockState(),3);}
            var wand=new ItemStack(item("copper_building_wand"));actor.setItemInHand(InteractionHand.MAIN_HAND,wand);actor.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,64));
            yes(h,wand.getItem().useOn(new UseOnContext(actor,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Wand starts on unclaimed support");
            for(int t=0;t<120;t++)wand.getItem().inventoryTick(wand,l,actor,EquipmentSlot.MAINHAND);
            yes(h,l.getBlockState(origin.above()).is(Blocks.STONE),"Wand actually places unclaimed target");
            yes(h,l.getBlockState(inside.above()).isAir()==(actor==other),"Every wand target honors claim border");
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=origin.offset(x,0,z);l.setBlock(at,Blocks.STONE.defaultBlockState(),3);l.setBlock(at.above(),Blocks.AIR.defaultBlockState(),3);}
            actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("diamond_sledgehammer")));actor.setXRot(90);
            yes(h,actor.gameMode.destroyBlock(origin)&&l.getBlockState(origin.west()).isAir(),"Real loader hammer break hook removes an unclaimed neighbor");
            yes(h,l.getBlockState(inside).is(Blocks.STONE)==(actor==other),"Every hammer target honors claim border");
        }
    });h.succeed();}
    static void bedHammer(GameTestHelper h){with(h,c->{
        var l=h.getLevel();var head=boundary(h);var foot=head.west();var owner=player(h,foot.west(4));var other=player(h,foot.west(4));
        yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(head)),owner.getUUID(),100),"Hammer bed fixture");
        for(var bed:List.of(Items.BED.red(),Items.STRAW_BED)){
            for(var at:List.of(foot,head)){l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(at,Blocks.AIR.defaultBlockState(),3);}
            owner.setYRot(270);owner.setYHeadRot(270);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(bed));
            var hit=new BlockHitResult(Vec3.atCenterOf(foot.below()).add(0,.5,0),Direction.UP,foot.below(),false);
            yes(h,((BlockItem)bed).place(new net.minecraft.world.item.context.BlockPlaceContext(owner,InteractionHand.MAIN_HAND,owner.getMainHandItem(),hit)).consumesAction(),"Actual complete bed fixture");
            other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("diamond_sledgehammer")));
            yes(h,!other.gameMode.destroyBlock(foot)&&!l.getBlockState(head).isAir(),"Real hammer cannot destroy an unclaimed bed foot and protected head");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("diamond_sledgehammer")));
            yes(h,owner.gameMode.destroyBlock(foot)&&l.getBlockState(head).isAir(),"Owner hammer destroys both bed halves");
        }
    });h.succeed();}
    static void beam(GameTestHelper h){with(h,c->{
        // Keep the entity in the GameTest anchor chunk, whose entity sections are already visible.
        var l=h.getLevel();var inside=h.absolutePos(new BlockPos(0,64,0));var owner=player(h,inside.west(5));var other=player(h,inside.west(5));
        yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Beam fixture");
        l.getChunkAt(inside);
        var cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);cow.setPos(Vec3.atBottomCenterOf(inside));yes(h,l.addFreshEntity(cow),"Beam target spawns in a loaded chunk");
        for(var actor:List.of(other,owner)){
            var idle=actor==owner?other:owner;idle.setPos(Vec3.atBottomCenterOf(inside.west(12)));
            actor.setPos(Vec3.atBottomCenterOf(inside.west(5)));actor.setYRot(270);actor.setYHeadRot(270);actor.setXRot(10);
            var rod=new ItemStack(item("amethyst_lens"));actor.setItemInHand(InteractionHand.MAIN_HAND,rod);
            yes(h,rod.getItem().use(l,actor,InteractionHand.MAIN_HAND).consumesAction(),"Actual beam starts");
            try {
                var aimed=(HitResult)rod.getItem().getClass().getMethod("aim",net.minecraft.server.level.ServerPlayer.class).invoke(null,actor);
                yes(h,aimed instanceof EntityHitResult hit && hit.getEntity()==cow,"Beam fixture must aim at cow: "+aimed.getType()+" "+aimed.getLocation());
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            for(int t=0;t<200;t++)rod.getItem().onUseTick(l,actor,rod,72000-t);
            yes(h,cow.isOnFire()==(actor==owner),"Actual beam ignition, owner="+(actor==owner)+", fire="+cow.getRemainingFireTicks()+", charge="+rod.getDamageValue());
        }cow.discard();
    });h.succeed();}
    static void echo(GameTestHelper h){with(h,c->{
        var l=h.getLevel();var target=boundary(h).east(4);var owner=player(h,target.west(10));var other=player(h,target.west(10));
        yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(target)),owner.getUUID(),100),"Echo fixture");
        l.setBlock(target,Blocks.LODESTONE.defaultBlockState(),3);l.setBlock(target.above(),Blocks.AIR.defaultBlockState(),3);l.setBlock(target.above(2),Blocks.AIR.defaultBlockState(),3);
        for(var actor:List.of(other,owner)){
            var stack=new ItemStack(item("echo_sounder"));stack.set(DataComponents.LODESTONE_TRACKER,new LodestoneTracker(Optional.of(GlobalPos.of(l.dimension(),target)),false));actor.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var before=actor.position();stack.getItem().finishUsingItem(stack,l,actor);
            yes(h,actor.position().equals(before)==(actor==other),"Actual echo completion checks destination authority");
            if(actor==other)yes(h,stack.getDamageValue()==0&&!actor.getCooldowns().isOnCooldown(stack),"Denied echo has no cost or cooldown");
        }
    });h.succeed();}
    @SuppressWarnings({"unchecked","rawtypes"})
    static void pad(GameTestHelper h){with(h,c->{
        var l=h.getLevel();var pos=boundary(h).east(4);var owner=player(h,pos.east(20));var other=player(h,pos);
        yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(pos)),owner.getUUID(),100),"Pad fixture");
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:enderite_spawn_teleporter"));l.setBlock(pos,block.defaultBlockState(),3);var be=l.getBlockEntity(pos);
        var ticker=(BlockEntityTicker<BlockEntity>)(BlockEntityTicker)((EntityBlock)block).getTicker(l,l.getBlockState(pos),be.getType());
        for(var actor:List.of(other,owner)){
            if(actor==owner)other.setPos(Vec3.atBottomCenterOf(pos.east(20)));
            actor.setPos(pos.getX()+.5,pos.getY()+.125,pos.getZ()+.5);var before=actor.position();
            for(int t=0;t<1200;t++)ticker.tick(l,pos,l.getBlockState(pos),be);
            yes(h,actor.position().equals(before)==(actor==other),"Actual pad ticker checks source claim before teleporting");
        }
    });h.succeed();}
}
