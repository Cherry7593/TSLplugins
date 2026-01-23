package org.tsl.webbridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.tsl.webbridge.config.ModConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PlayerListReporter {
    private static final Logger LOGGER = Logger.getLogger("TSLWebBridge");
    private static final Gson GSON = new Gson();
    
    private final ModConfig config;
    private final WebBridgeClient client;
    private final MinecraftServer server;
    
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> reportTask;
    private ScheduledFuture<?> heartbeatTask;
    
    public PlayerListReporter(ModConfig config, WebBridgeClient client, MinecraftServer server) {
        this.config = config;
        this.client = client;
        this.server = server;
    }
    
    public void start() {
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Initial report
        sendPlayerList();
        
        // Schedule periodic reports
        reportTask = scheduler.scheduleAtFixedRate(
            this::sendPlayerList,
            config.getPlayerListInterval(),
            config.getPlayerListInterval(),
            TimeUnit.SECONDS
        );
        
        // Schedule heartbeat
        heartbeatTask = scheduler.scheduleAtFixedRate(
            this::sendHeartbeat,
            config.getHeartbeatInterval(),
            config.getHeartbeatInterval(),
            TimeUnit.SECONDS
        );
        
        LOGGER.info("[WebBridge] Reporter started (playerList: " + config.getPlayerListInterval() + 
                    "s, heartbeat: " + config.getHeartbeatInterval() + "s)");
    }
    
    public void stop() {
        if (reportTask != null) {
            reportTask.cancel(false);
            reportTask = null;
        }
        
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }
    
    public void sendPlayerList() {
        if (!client.isConnected()) {
            return;
        }
        
        // Collect data on server main thread for thread safety
        server.execute(() -> {
            try {
                JsonObject message = new JsonObject();
                message.addProperty("type", "event");
                message.addProperty("source", "mc");
                message.addProperty("timestamp", System.currentTimeMillis());
                
                JsonObject data = new JsonObject();
                data.addProperty("event", "PLAYER_LIST");
                data.addProperty("id", "pl-" + System.currentTimeMillis());
                data.addProperty("serverId", config.getServerId());
                
                // Player list
                JsonArray players = new JsonArray();
                int online = 0;
                
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    JsonObject playerInfo = new JsonObject();
                    playerInfo.addProperty("uuid", player.getStringUUID());
                    playerInfo.addProperty("name", player.getName().getString());
                    players.add(playerInfo);
                    online++;
                }
                
                data.addProperty("online", online);
                data.add("players", players);
                
                // TPS and MSPT
                double mspt = getAverageMspt();
                double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));
                
                data.addProperty("tps", Math.round(tps * 100.0) / 100.0);
                data.addProperty("mspt", Math.round(mspt * 100.0) / 100.0);
                
                message.add("data", data);
                
                String json = GSON.toJson(message);
                client.enqueue(json);
                if (config.isDebug()) {
                    LOGGER.info("[WebBridge] Enqueued player list: " + online + " players");
                } else {
                    LOGGER.fine("[WebBridge] Enqueued player list: " + online + " players");
                }
                
            } catch (Exception e) {
                LOGGER.warning("[WebBridge] Failed to send player list: " + e.getMessage());
            }
        });
    }
    
    private void sendHeartbeat() {
        if (!client.isConnected()) {
            return;
        }
        
        // Execute on server main thread for consistency
        server.execute(() -> {
            try {
                JsonObject message = new JsonObject();
                message.addProperty("type", "heartbeat");
                message.addProperty("source", "mc");
                message.addProperty("timestamp", System.currentTimeMillis());
                
                JsonObject data = new JsonObject();
                data.addProperty("serverId", config.getServerId());
                message.add("data", data);
                
                String json = GSON.toJson(message);
                client.enqueue(json);
                if (config.isDebug()) {
                    LOGGER.info("[WebBridge] Enqueued heartbeat");
                } else {
                    LOGGER.fine("[WebBridge] Enqueued heartbeat");
                }
                
            } catch (Exception e) {
                LOGGER.warning("[WebBridge] Failed to send heartbeat: " + e.getMessage());
            }
        });
    }
    
    private double getAverageMspt() {
        return server.getAverageTickTimeNanos() / 1_000_000.0;
    }
}
