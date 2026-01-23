package org.tsl.webbridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.tsl.webbridge.config.ModConfig;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebBridgeClient {
    private static final Logger LOGGER = Logger.getLogger("TSLWebBridge");
    private static final Gson GSON = new Gson();
    
    private final ModConfig config;
    private final Runnable onConnectedCallback;
    
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<InternalWebSocketClient> clientRef = new AtomicReference<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> sendTask;
    private ScheduledFuture<?> reconnectTask;
    
    public WebBridgeClient(ModConfig config, Runnable onConnectedCallback) {
        this.config = config;
        this.onConnectedCallback = onConnectedCallback;
    }
    
    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            LOGGER.warning("[WebBridge] Client already running");
            return;
        }
        
        scheduler = Executors.newScheduledThreadPool(2);
        LOGGER.info("[WebBridge] Starting WebSocket client...");
        connect();
        startSendTask();
    }
    
    public void stop() {
        if (!isRunning.compareAndSet(true, false)) {
            return;
        }
        
        LOGGER.info("[WebBridge] Stopping WebSocket client...");
        
        if (sendTask != null) {
            sendTask.cancel(false);
            sendTask = null;
        }
        
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
        
        InternalWebSocketClient client = clientRef.getAndSet(null);
        if (client != null) {
            client.close();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        
        messageQueue.clear();
        LOGGER.info("[WebBridge] WebSocket client stopped");
    }
    
    public void enqueue(String json) {
        InternalWebSocketClient client = clientRef.get();
        if (client == null || !client.isOpen()) {
            return;
        }
        
        messageQueue.offer(json);
        
        int queueSize = messageQueue.size();
        if (queueSize > 100) {
            LOGGER.warning("[WebBridge] Message queue too long (" + queueSize + " messages)");
        }
    }
    
    public boolean isConnected() {
        InternalWebSocketClient client = clientRef.get();
        return client != null && client.isOpen();
    }
    
    public boolean connect() {
        try {
            InternalWebSocketClient existingClient = clientRef.get();
            if (existingClient != null && existingClient.isOpen()) {
                LOGGER.warning("[WebBridge] Already connected");
                return false;
            }
            
            String url = config.getWebsocket().getFullUrl(config.getServerId());
            LOGGER.info("[WebBridge] Connecting to: " + url);
            
            URI uri = new URI(url);
            if (!uri.getScheme().equals("ws") && !uri.getScheme().equals("wss")) {
                LOGGER.severe("[WebBridge] Invalid protocol: " + uri.getScheme());
                return false;
            }
            
            InternalWebSocketClient client = new InternalWebSocketClient(uri);
            client.setConnectionLostTimeout(30);
            clientRef.set(client);
            
            boolean connected = client.connectBlocking(5, TimeUnit.SECONDS);
            
            if (!connected) {
                LOGGER.warning("[WebBridge] Connection failed");
                clientRef.set(null);
                scheduleReconnect();
                return false;
            }
            
            reconnectAttempts.set(0);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[WebBridge] Connection error", e);
            scheduleReconnect();
            return false;
        }
    }
    
    public void disconnect() {
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
        
        InternalWebSocketClient client = clientRef.getAndSet(null);
        if (client != null && client.isOpen()) {
            client.close();
            LOGGER.info("[WebBridge] Disconnected");
        }
        
        messageQueue.clear();
    }
    
    private void startSendTask() {
        sendTask = scheduler.scheduleAtFixedRate(() -> {
            processSendQueue();
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    private void processSendQueue() {
        InternalWebSocketClient client = clientRef.get();
        if (client == null || !client.isOpen()) {
            return;
        }
        
        int sent = 0;
        while (sent < 10) {
            String message = messageQueue.poll();
            if (message == null) break;
            
            try {
                client.send(message);
                sent++;
                if (config.isDebug()) {
                    LOGGER.info("[WebBridge] Sent message: " + message.substring(0, Math.min(100, message.length())) + "...");
                } else {
                    LOGGER.fine("[WebBridge] Sent message: " + message.substring(0, Math.min(100, message.length())) + "...");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[WebBridge] Send failed", e);
                messageQueue.offer(message);
                break;
            }
        }
        
        if (sent > 0) {
            LOGGER.fine("[WebBridge] Sent " + sent + " messages, queue remaining: " + messageQueue.size());
        }
    }
    
    private void scheduleReconnect() {
        if (!isRunning.get() || !config.isAutoReconnect()) {
            return;
        }
        
        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = config.getMaxReconnectAttempts();
        
        // maxAttempts <= 0 表示无限重试
        if (maxAttempts > 0 && attempts > maxAttempts) {
            LOGGER.warning("[WebBridge] Max reconnect attempts reached");
            return;
        }
        
        if (maxAttempts <= 0) {
            LOGGER.info("[WebBridge] Scheduling reconnect attempt " + attempts + " (unlimited)");
        } else {
            LOGGER.info("[WebBridge] Scheduling reconnect attempt " + attempts + "/" + maxAttempts);
        }
        
        reconnectTask = scheduler.schedule(() -> {
            if (isRunning.get()) {
                connect();
            }
        }, config.getReconnectInterval(), TimeUnit.SECONDS);
    }
    
    private class InternalWebSocketClient extends WebSocketClient {
        
        public InternalWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            LOGGER.info("[WebBridge] ✅ Connected");
            if (onConnectedCallback != null) {
                onConnectedCallback.run();
            }
        }
        
        @Override
        public void onMessage(String message) {
            try {
                JsonObject json = GSON.fromJson(message, JsonObject.class);
                String type = json.has("type") ? json.get("type").getAsString() : null;
                
                if ("system".equals(type)) {
                    String systemMessage = json.has("message") ? json.get("message").getAsString() : "";
                    LOGGER.info("[WebBridge] System: " + systemMessage);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[WebBridge] Message parse error", e);
            }
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            String source = remote ? "server" : "client";
            LOGGER.warning("[WebBridge] Connection closed by " + source + " (code: " + code + ")");
            clientRef.compareAndSet(this, null);
            scheduleReconnect();
        }
        
        @Override
        public void onError(Exception ex) {
            LOGGER.log(Level.SEVERE, "[WebBridge] WebSocket error", ex);
        }
    }
}
