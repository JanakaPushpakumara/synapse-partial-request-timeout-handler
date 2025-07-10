package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.http.conn.LoggingNHttpServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionTimeoutManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionTimeoutManager.class);

    private static long cleanupInterval;
    private static long connectionTimeout;
    private static List<String> restUrlPostfixList;

    private static final String requestConnectionCleanupInterval = System.getenv("REQUEST_CONNECTION_CLEANUP_INTERVAL");
    private static final String requestReadTimeout = System.getenv("REQUEST_READ_TIMEOUT");
    private static final String connectionTimeoutManagerEnable = System.getenv("CONNECTION_TIMEOUT_MANAGER_ENABLE");
    private static final String apiListFileLocation = System.getProperty("carbon.home") + "/repository/conf/CTM_api_context_list.txt";

    private final Map<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService connectionCleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    // Volatile Singleton instance
    private static final ConnectionTimeoutManager INSTANCE = new ConnectionTimeoutManager();

    // Private constructor to prevent instantiation
    private ConnectionTimeoutManager() {
        if ("true".equalsIgnoreCase(connectionTimeoutManagerEnable)) {
            LOGGER.info("ConnectionTimeoutManager is initializing ....");

            cleanupInterval = requestConnectionCleanupInterval != null
                    ? Long.parseLong(requestConnectionCleanupInterval)
                    : 10000;
            connectionTimeout = requestReadTimeout != null
                    ? Long.parseLong(requestReadTimeout)
                    : 180000;
            restUrlPostfixList = loadRestUrlPostfixList();
            startCleanupTask();
        }
    }

    // Double-checked locking for thread-safe Singleton
    public static ConnectionTimeoutManager getInstance() {
        return INSTANCE;
    }

    private void startCleanupTask() {
        connectionCleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupStaleConnections();
            } catch (Exception e) {
                LOGGER.error("Error in connection cleanup task", e);
            }
        }, cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
    }

    private void cleanupStaleConnections() {
        for (Map.Entry<String, ConnectionInfo> entry : activeConnections.entrySet()) {
            ConnectionInfo connectionInfo = entry.getValue();
            if (connectionInfo.isTimedOut(connectionTimeout)) {
                LOGGER.warn("Closing timed out connection for connection ID: {}", entry.getKey());
                connectionInfo.close();
                activeConnections.remove(entry.getKey());
            }
        }
    }

    public void registerConnection(MessageContext mc) {

        if ("true".equalsIgnoreCase(connectionTimeoutManagerEnable)) {
            String restUrlPostfix = (String) ((Axis2MessageContext) mc).getAxis2MessageContext()
                    .getProperty("REST_URL_POSTFIX");

            if (!restUrlPostfixList.isEmpty()){
                for (String restUrlPostfixItem : restUrlPostfixList) {
                    if (restUrlPostfix.startsWith(restUrlPostfixItem)) {
                        LoggingNHttpServerConnection conn = (LoggingNHttpServerConnection) ((Axis2MessageContext) mc)
                                .getAxis2MessageContext().getProperty("pass-through.Source-Connection");
                        String connID = conn.toString() + mc.getMessageID();
                        activeConnections.put(connID, new ConnectionInfo(mc));
                    }
                }
            } else {
                LoggingNHttpServerConnection conn = (LoggingNHttpServerConnection) ((Axis2MessageContext) mc)
                        .getAxis2MessageContext().getProperty("pass-through.Source-Connection");
                String connID = conn.toString() + mc.getMessageID();
                activeConnections.put(connID, new ConnectionInfo(mc));
            }
        }
    }

    public void unregisterConnection(MessageContext mc) {
        if ("true".equalsIgnoreCase(connectionTimeoutManagerEnable)) {
            LoggingNHttpServerConnection conn = (LoggingNHttpServerConnection) ((Axis2MessageContext) mc).getAxis2MessageContext()
                    .getProperty("pass-through.Source-Connection");
            String connid = conn.toString() + mc.getMessageID();
            activeConnections.remove(connid);
        }
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    private List<String> loadRestUrlPostfixList() {
        List<String> apiList = new ArrayList<>();
        try (Stream<String> lines = Files.lines(Paths.get(apiListFileLocation))) {
            apiList = lines.collect(Collectors.toList());
            if (apiList.isEmpty()) {
                LOGGER.warn("No API context found for ConnectionTimeoutManager connection registration from the file : {} hence all the API context getting registered ", apiListFileLocation);
            }
        } catch (IOException e) {
            LOGGER.warn("File not found or unreadable: {} hence all the API context getting registered ", apiListFileLocation);
        }
        return apiList;
    }
}