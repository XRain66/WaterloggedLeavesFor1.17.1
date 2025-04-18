package com.xrain.waterloggedleaves.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LeavesBlock.class)
public class LeavesBlockParticleMixin {


    @Inject(method = "randomTick", at = @At("HEAD"))
    private void onRandomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (state.get(Properties.WATERLOGGED)) {
            FluidState fluidState = world.getFluidState(pos);
            world.getFluidTickScheduler().schedule(
                pos,
                fluidState.getFluid(),
                fluidState.getFluid().getTickRate(world)
            );
        }
    }
} 