package com.simplebuilding.modules.simplequalityoflife.forge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.common.util.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
/** Real Forge event probes; installed exclusively in the isolated GameTest server. */
public final class QolForgeChecks {
 private static BlockPos refused;private static boolean cancel;
 public static void register(){BlockEvent.BreakEvent.BUS.addListener(e->{if(!e.getPos().equals(refused))return false;if(cancel)return true;e.setResult(Result.DENY);return false;});}
 public static void veto(GameTestHelper h){
  var pos=new BlockPos(2,2,2);h.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.FARMLAND);var crop=net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7);h.setBlock(pos,crop);
  var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)));player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HOE));
  try{refused=h.absolutePos(pos);for(boolean mode:new boolean[]{false,true}){cancel=mode;player.tickCount+=4;h.assertTrue(!com.simplequalityoflife.event.InteractionGuard.permission.test(player,refused),"Forge DENY/cancel both refuse");h.assertTrue(com.simplequalityoflife.event.HoeHarvestHandler.onRightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,refused,net.minecraft.core.Direction.UP)==net.minecraft.world.InteractionResult.PASS,"Refused crop action passes without mutation");h.assertTrue(h.getBlockState(pos).equals(crop)&&player.getMainHandItem().getDamageValue()==0,"No harvest or durability cost after Forge veto");}}finally{refused=null;cancel=false;}
 }
}
