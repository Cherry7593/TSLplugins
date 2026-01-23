package org.tsl.webbridge;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.tsl.webbridge.config.ModConfig;

import java.util.logging.Logger;

public class TSLWebBridgeMod implements DedicatedServerModInitializer {
    private static final Logger LOGGER = Logger.getLogger("TSLWebBridge");
    
    private ModConfig config;
    private WebBridgeClient client;
    private PlayerListReporter reporter;
    private MinecraftServer server;
    
    @Override
    public void onInitializeServer() {
        LOGGER.info("[TSLWebBridge] Initializing...");
        
        // Load config
        config = ModConfig.getInstance();
        
        if (!config.isEnabled()) {
            LOGGER.info("[TSLWebBridge] Mod is disabled in config");
            return;
        }
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        
        // Register player events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Delay player list update slightly to ensure player is fully joined
            server.execute(() -> {
                if (reporter != null) {
                    reporter.sendPlayerList();
                }
            });
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Send updated player list after disconnect
            server.execute(() -> {
                if (reporter != null) {
                    reporter.sendPlayerList();
                }
            });
        });
        
        LOGGER.info("[TSLWebBridge] Initialized");
    }
    
    private void onServerStarted(MinecraftServer server) {
        this.server = server;
        LOGGER.info("[TSLWebBridge] Server started, connecting to WebSocket...");
        
        // Create client with callback to start reporter
        client = new WebBridgeClient(config, this::onWebSocketConnected);
        client.start();
    }
    
    private void onWebSocketConnected() {
        if (reporter == null && server != null) {
            reporter = new PlayerListReporter(config, client, server);
            reporter.start();
        } else if (reporter != null) {
            // Already have reporter, just send an update
            reporter.sendPlayerList();
        }
    }
    
    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("[TSLWebBridge] Server stopping, disconnecting...");
        
        if (reporter != null) {
            reporter.stop();
            reporter = null;
        }
        
        if (client != null) {
            client.stop();
            client = null;
        }
        
        this.server = null;
    }
}
