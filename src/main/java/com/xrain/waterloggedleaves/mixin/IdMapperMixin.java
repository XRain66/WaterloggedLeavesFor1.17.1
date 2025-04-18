package com.xrain.waterloggedleaves.mixin;

import com.xrain.waterloggedleaves.WaterloggedLeavesMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.IdList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 处理含水树叶方块状态的ID映射
@Mixin(IdList.class)
public class IdMapperMixin<T> {
    
    // 拦截添加含水树叶方块状态，防止其被添加到ID映射表中
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(Object object, CallbackInfo ci) {
        if (object instanceof BlockState) {
            BlockState state = (BlockState) object;
            if (state.getBlock() instanceof LeavesBlock && 
                state.contains(Properties.WATERLOGGED) && 
                state.get(Properties.WATERLOGGED)) {
                
                WaterloggedLeavesMod.debug("拦截含水树叶方块状态添加: " + state);
                ci.cancel();
            }
        }
    }
    
    // 重定向含水树叶方块状态的ID查询，使用对应的非含水状态的ID
    @Inject(method = "getRawId", at = @At("HEAD"), cancellable = true)
    private void onGetRawId(Object object, CallbackInfoReturnable<Integer> cir) {
        if (object instanceof BlockState) {
            BlockState state = (BlockState) object;
            if (state.getBlock() instanceof LeavesBlock && 
                state.contains(Properties.WATERLOGGED) && 
                state.get(Properties.WATERLOGGED)) {
                
                BlockState dryState = state.with(Properties.WATERLOGGED, false);
                
                IdList<Object> self = (IdList<Object>) (Object) this;
                Integer id = self.getRawId(dryState);
                
                if (id != null) {
                    WaterloggedLeavesMod.debug("重定向含水树叶方块状态ID查询: " + state + " -> " + id);
                    cir.setReturnValue(id);
                }
            }
        }
    }
} 