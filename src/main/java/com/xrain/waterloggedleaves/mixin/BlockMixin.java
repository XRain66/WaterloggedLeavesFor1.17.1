package com.xrain.waterloggedleaves.mixin;

import com.xrain.waterloggedleaves.WaterloggedLeavesMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 监控Block类获取默认状态的方法，用于调试树叶方块状态
@Mixin(Block.class)
public class BlockMixin {
    
    // 注入getDefaultState方法，记录树叶方块的默认状态
    @Inject(method = "getDefaultState", at = @At("RETURN"))
    private void onGetDefaultState(CallbackInfoReturnable<BlockState> cir) {
        Block self = (Block) (Object) this;
        
        if (self instanceof LeavesBlock) {
            BlockState originalState = cir.getReturnValue();
            WaterloggedLeavesMod.debug("获取树叶方块默认状态: " + originalState);
        }
    }
} 