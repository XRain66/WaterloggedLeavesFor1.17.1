package com.xrain.waterloggedleaves;

import com.xrain.waterloggedleaves.particle.LeafParticleManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WaterloggedLeavesMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "waterloggedleaves";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public static final boolean DEBUG_MODE = false;

    private LeafParticleManager particleManager;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Waterlogged Leaves Mod 初始化中...");
        
        if (DEBUG_MODE) {
            LOGGER.info("调试模式已启用，将输出更多日志信息");
        }
        
        // 初始化粒子管理器，负责显示含水树叶的水滴粒子效果
        particleManager = new LeafParticleManager();
        
        // 注册服务器tick事件，用于更新粒子效果
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            particleManager.tick(server);
        });
        
        LOGGER.info("Waterlogged Leaves Mod 已成功初始化");
    }
    
    //debug
    public static void debug(String message) {
        if (DEBUG_MODE) {
            LOGGER.info("[DEBUG] " + message);
        }
    }
} 