package com.xrain.waterloggedleaves.particle;

import com.xrain.waterloggedleaves.WaterloggedLeavesMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

// 处理含水树叶方块的粒子效果，包括粒子生成、跟踪玩家附近的含水树叶以及优化粒子显示
public class LeafParticleManager {
    private static final int DETECTION_RANGE = 24;
    private static final int MAX_BLOCKS_PER_PLAYER = 8;
    private static final int PARTICLE_INTERVAL = 1;
    private final Map<UUID, Set<BlockPos>> playerLeafBlocks = new HashMap<>();
    private int tickCounter = 0;
    
    // 每个tick执行一次，负责更新和生成含水树叶的水滴粒子
    public void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % PARTICLE_INTERVAL != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            Set<BlockPos> leafBlocks = playerLeafBlocks.computeIfAbsent(
                player.getUuid(), k -> new HashSet<>()
            );
            cleanupLeafBlocks(world, player, leafBlocks);
            if (leafBlocks.size() < MAX_BLOCKS_PER_PLAYER * 3) {
                findNewWaterloggedLeaves(world, player, leafBlocks);
            }
            generateParticles(world, player, leafBlocks);
        }
    }
    
    // 清理不再在玩家范围内或不满足条件的含水树叶方块
    private void cleanupLeafBlocks(ServerWorld world, PlayerEntity player, Set<BlockPos> leafBlocks) {
        Vec3d playerPos = player.getPos();
        Iterator<BlockPos> iterator = leafBlocks.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            double distance = pos.getSquaredDistance(
                new BlockPos(playerPos.x, playerPos.y, playerPos.z)
            );
            if (distance > DETECTION_RANGE * DETECTION_RANGE) {
                iterator.remove();
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof LeavesBlock) || 
                !state.contains(Properties.WATERLOGGED) || 
                !state.get(Properties.WATERLOGGED)) {
                iterator.remove();
            }
        }
    }

    // 为玩家查找新的含水树叶方块并添加到跟踪列表中
    private void findNewWaterloggedLeaves(ServerWorld world, PlayerEntity player, Set<BlockPos> leafBlocks) {
        BlockPos centerPos = player.getBlockPos();
        int count = 0;
        for (int r = 1; r <= DETECTION_RANGE / 2 && count < MAX_BLOCKS_PER_PLAYER; r++) {
            for (int x = -r; x <= r && count < MAX_BLOCKS_PER_PLAYER; x++) {
                for (int z = -r; z <= r && count < MAX_BLOCKS_PER_PLAYER; z++) {
                    if (Math.abs(x) < r && Math.abs(z) < r) continue;
                    for (int y = -2; y <= 2 && count < MAX_BLOCKS_PER_PLAYER; y++) {
                        BlockPos pos = centerPos.add(x, y, z);
                        if (leafBlocks.contains(pos)) continue;
                        BlockState state = world.getBlockState(pos);
                        if (state.getBlock() instanceof LeavesBlock && 
                            state.contains(Properties.WATERLOGGED) && 
                            state.get(Properties.WATERLOGGED)) {
                            leafBlocks.add(pos);
                            count++;
                        }
                    }
                }
            }
        }
        if (count > 0) {
            WaterloggedLeavesMod.debug("为玩家 " + player.getName().getString() + 
                " 找到 " + count + " 个新的含水树叶方块");
        }
    }

    // 为树叶方块生成水滴粒子效果
    private void generateParticles(ServerWorld world, PlayerEntity player, Set<BlockPos> leafBlocks) {
        if (leafBlocks.isEmpty()) return;
        List<BlockPos> blockList = new ArrayList<>(leafBlocks);
        Random random = world.getRandom();
        int blocksToProcess = Math.min(MAX_BLOCKS_PER_PLAYER, blockList.size());
        for (int i = 0; i < blocksToProcess; i++) {
            BlockPos pos = blockList.get(random.nextInt(blockList.size()));
            if (isWithinViewDistance(player, pos)) {
                generateParticlesForBlock(world, pos, random);
            }
        }
    }

    private boolean isWithinViewDistance(PlayerEntity player, BlockPos pos) {
        int viewDistance = 8; 
        int playerChunkX = player.getBlockPos().getX() >> 4;
        int playerChunkZ = player.getBlockPos().getZ() >> 4;
        int blockChunkX = pos.getX() >> 4;
        int blockChunkZ = pos.getZ() >> 4;
        int chunkDistance = Math.max(
            Math.abs(playerChunkX - blockChunkX),
            Math.abs(playerChunkZ - blockChunkZ)
        );
        return chunkDistance <= viewDistance;
    }

    // 计算基于方向的偏移位置
    private Vec3d calculateOffsetPosition(BlockPos pos, Direction dir, double offsetA, double offsetB) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        if (dir.getAxis() == Direction.Axis.X) {
            x = pos.getX() + 0.5 + dir.getOffsetX() * 0.5;
            y += offsetA;
            z += offsetB;
        } else if (dir.getAxis() == Direction.Axis.Y) {
            x += offsetA;
            y = pos.getY() + 0.5 + dir.getOffsetY() * 0.5;
            z += offsetB;
        } else {
            x += offsetA;
            y += offsetB;
            z = pos.getZ() + 0.5 + dir.getOffsetZ() * 0.5;
        }
        
        return new Vec3d(x, y, z);
    }
    
    // 生成指定粒子类型
    private void spawnDirectionalParticle(ServerWorld world, Vec3d pos, Direction dir, ParticleEffect particleType, 
                                        int count, double speed) {
        double speedX = dir.getOffsetX() * speed;
        double speedY = dir.getOffsetY() * speed;
        double speedZ = dir.getOffsetZ() * speed;
        
        world.spawnParticles(
            particleType,
            pos.x, pos.y, pos.z,
            count, speedX, speedY, speedZ, 0.01
        );
    }

    private void generateParticlesForBlock(ServerWorld world, BlockPos pos, Random random) {
        if (random.nextFloat() < 0.5f) {
            BlockPos downPos = pos.down();
            if (world.isAir(downPos)) {
                double offsetX = (random.nextDouble() - 0.5) * 0.6;
                double offsetZ = (random.nextDouble() - 0.5) * 0.6;
                world.spawnParticles(
                    ParticleTypes.DRIPPING_WATER,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() - 0.05,
                    pos.getZ() + 0.5 + offsetZ,
                    2, 0, 0, 0, 0
                );
            }
        }

        if (random.nextFloat() < 0.6f) {
            Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
            BlockPos adjacentPos = pos.offset(dir);
            
            if (world.isAir(adjacentPos)) {
                double offsetA = (random.nextDouble() - 0.5) * 0.7;
                double offsetB = (random.nextDouble() - 0.5) * 0.7;
                Vec3d particlePos = calculateOffsetPosition(pos, dir, offsetA, offsetB);
                
                int particleChoice = random.nextInt(10);
                ParticleEffect particleType;
                int count = 1;
                
                if (particleChoice < 3) {
                    particleType = ParticleTypes.BUBBLE;
                    count = 2;
                } else if (particleChoice < 5) {
                    particleType = ParticleTypes.DRIPPING_WATER;
                } else {
                    particleType = ParticleTypes.UNDERWATER;
                }
                
                spawnDirectionalParticle(world, particlePos, dir, particleType, count, 0.03);
            }
        }

        if (random.nextFloat() < 0.3f) {
            Direction randomDir = Direction.values()[random.nextInt(Direction.values().length)];
            BlockPos adjacentPos = pos.offset(randomDir);
            if (world.isAir(adjacentPos)) {
                double offsetA = random.nextDouble() * 0.8 + 0.1;
                double offsetB = random.nextDouble() * 0.8 + 0.1;
                
                Vec3d particlePos = calculateOffsetPosition(pos, randomDir, offsetA - 0.5, offsetB - 0.5);
                
                world.spawnParticles(
                    random.nextBoolean() ? ParticleTypes.BUBBLE : ParticleTypes.UNDERWATER,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.01, 0.01, 0.01, 0.01
                );
            }
        }
    }
} 