package org.tsl.webbridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "tsl-webbridge.json";
    
    private static ModConfig instance;
    
    // Config fields
    private boolean enabled = true;
    private String serverId = "fabric-server-1";
    private WebSocketConfig websocket = new WebSocketConfig();
    private int playerListInterval = 30;
    private int heartbeatInterval = 30;
    private boolean autoReconnect = true;
    private int reconnectInterval = 30;
    private int maxReconnectAttempts = -1;
    private boolean debug = false;
    
    public static class WebSocketConfig {
        private String url = "ws://127.0.0.1:4001/mc-bridge";
        private String token = "";
        
        public String getUrl() { return url; }
        public String getToken() { return token; }
        
        public String getFullUrl(String serverId) {
            StringBuilder sb = new StringBuilder(url);
            
            // Add query string separator
            sb.append(url.contains("?") ? "&" : "?");
            
            // Add from parameter
            sb.append("from=mc");
            
            // Add serverId
            sb.append("&serverId=").append(serverId);
            
            // Add token if present
            if (token != null && !token.isEmpty()) {
                sb.append("&token=").append(token);
            }
            
            return sb.toString();
        }
    }
    
    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    public static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                instance = GSON.fromJson(json, ModConfig.class);
                return instance;
            } catch (IOException e) {
                System.err.println("[TSLWebBridge] Failed to load config: " + e.getMessage());
            }
        }
        
        // Create default config
        instance = new ModConfig();
        instance.save();
        return instance;
    }
    
    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[TSLWebBridge] Failed to save config: " + e.getMessage());
        }
    }
    
    public static void reload() {
        instance = load();
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getServerId() { return serverId; }
    public WebSocketConfig getWebsocket() { return websocket; }
    public int getPlayerListInterval() { return playerListInterval; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public boolean isAutoReconnect() { return autoReconnect; }
    public int getReconnectInterval() { return reconnectInterval; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public boolean isDebug() { return debug; }
}
