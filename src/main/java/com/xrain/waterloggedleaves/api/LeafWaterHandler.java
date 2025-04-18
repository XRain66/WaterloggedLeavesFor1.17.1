package com.xrain.waterloggedleaves.api;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 管理树叶方块的含水状态，提供存储和查询接口
public class LeafWaterHandler {
    
    private static final Map<BlockPos, Boolean> WATERLOGGED_STATES = new ConcurrentHashMap<>();
    
    // 设置方块的含水状态
    public static void setWaterlogged(BlockPos pos, boolean waterlogged) {
        if (waterlogged) {
            WATERLOGGED_STATES.put(pos, true);
        } else {
            WATERLOGGED_STATES.remove(pos);
        }
    }
    
    // 检查方块是否含水(通过世界状态或内部存储)
    public static boolean isWaterlogged(BlockPos pos, World world) {
        if (world != null) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock && state.contains(Properties.WATERLOGGED)) {
                return state.get(Properties.WATERLOGGED);
            }
        }
        return WATERLOGGED_STATES.getOrDefault(pos, false);
    }
    
    // 获取方块对应的流体状态
    public static FluidState getFluidState(BlockState state, BlockPos pos) {
        if (isWaterlogged(pos, null)) {
            return Fluids.WATER.getStill(false);
        }
        return Fluids.EMPTY.getDefaultState();
    }
    
    // 处理树叶方块在水中放置的逻辑
    public static void onLeavesPlacedInWater(BlockPos pos, World world) {
        setWaterlogged(pos, true);
        world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    }
    
    // 处理树叶方块被移除时的逻辑
    public static void onLeavesRemoved(BlockPos pos, World world) {
        WATERLOGGED_STATES.remove(pos);
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof LeavesBlock && 
            state.contains(Properties.WATERLOGGED) && 
            state.get(Properties.WATERLOGGED)) {
            world.setBlockState(pos, Fluids.WATER.getDefaultState().getBlockState());
        }
    }
}