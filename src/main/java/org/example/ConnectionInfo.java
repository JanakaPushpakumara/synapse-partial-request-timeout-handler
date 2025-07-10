package org.example;

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.http.conn.LoggingNHttpServerConnection;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionInfo.class);
    private final MessageContext messageContext;
    private final long connectionStartTime;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public ConnectionInfo( MessageContext messageContext) {
        this.messageContext = messageContext;
        this.connectionStartTime = System.currentTimeMillis();
    }

    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                messageContext.setProperty("IS_PARTIAL_REQUEST", true);

//                LoggingNHttpServerConnection conn = (LoggingNHttpServerConnection)((Axis2MessageContext) messageContext).getAxis2MessageContext()
//                        .getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);

                LoggingNHttpServerConnection conn = (LoggingNHttpServerConnection)((Axis2MessageContext) messageContext).getAxis2MessageContext()
                        .getProperty("pass-through.Source-Connection");

                conn.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Error closing timed out connection", e);
            }
        }
    }

    public boolean isTimedOut( Long readtimeout) {
        return System.currentTimeMillis() - connectionStartTime > readtimeout;
    }
}