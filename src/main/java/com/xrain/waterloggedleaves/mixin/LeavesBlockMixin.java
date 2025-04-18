package com.xrain.waterloggedleaves.mixin;

import com.xrain.waterloggedleaves.api.LeafWaterHandler;
import com.xrain.waterloggedleaves.WaterloggedLeavesMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 使树叶方块可以被水浸透，添加WATERLOGGED属性和相关逻辑
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block implements Waterloggable {
    private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    
    public LeavesBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Settings settings, CallbackInfo ci) {
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
        // 在这里添加调试输出，监控树叶方块的默认状态
        WaterloggedLeavesMod.debug("初始化树叶方块默认状态: " + this.getDefaultState());
    }
    
    // 将WATERLOGGED属性添加到方块状态
    @Inject(method = "appendProperties", at = @At("RETURN"))
    private void appendProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(WATERLOGGED);
        WaterloggedLeavesMod.debug("向树叶方块添加WATERLOGGED属性: " + WATERLOGGED);
    }
    
    // 实现Waterloggable接口，返回水的流体状态
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    // 处理相邻方块更新时的水流逻辑和粒子效果
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, 
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
            
            if (world instanceof ServerWorld && world.getRandom().nextInt(4) == 0) {
                ServerWorld serverWorld = (ServerWorld) world;
                
                double x = pos.getX() + 0.5 + direction.getOffsetX() * 0.5;
                double y = pos.getY() + 0.5 + direction.getOffsetY() * 0.5;
                double z = pos.getZ() + 0.5 + direction.getOffsetZ() * 0.5;
                
                serverWorld.spawnParticles(
                    ParticleTypes.BUBBLE, 
                    x, y, z,
                    5, 0.1, 0.1, 0.1, 0.01
                );
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    // 处理树叶方块放置时的含水状态和特效
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void onGetPlacementState(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null) {
            BlockPos pos = ctx.getBlockPos();
            FluidState fluidState = ctx.getWorld().getFluidState(pos);
            boolean shouldBeWaterlogged = fluidState.getFluid() == Fluids.WATER;
            
            BlockState state = cir.getReturnValue().with(WATERLOGGED, shouldBeWaterlogged);
            cir.setReturnValue(state);
            
            LeafWaterHandler.setWaterlogged(pos, shouldBeWaterlogged);
            
            if (shouldBeWaterlogged && ctx.getWorld() instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) ctx.getWorld();
                
                for (int i = 0; i < 10; i++) {
                    double offsetX = (serverWorld.getRandom().nextDouble() - 0.5) * 1.2;
                    double offsetY = (serverWorld.getRandom().nextDouble() - 0.5) * 1.2;
                    double offsetZ = (serverWorld.getRandom().nextDouble() - 0.5) * 1.2;
                    
                    serverWorld.spawnParticles(
                        ParticleTypes.SPLASH,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 0.5 + offsetY,
                        pos.getZ() + 0.5 + offsetZ,
                        2, 0, 0, 0, 0.1
                    );
                }
            }
        }
    }
} 