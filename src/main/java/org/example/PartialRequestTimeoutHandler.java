package org.example;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.AbstractSynapseHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;

/**
 * This is a sample handler that log for all message flows.
 */
public class PartialRequestTimeoutHandler extends AbstractSynapseHandler

{
    private static final Log log = LogFactory.getLog(PartialRequestTimeoutHandler.class);
    private ConnectionTimeoutManager connectionTimeoutManager;



    // Handle incoming request message flow of the WSO2 server
    public boolean handleRequestInFlow(MessageContext messageContext)
    {
        connectionTimeoutManager = ConnectionTimeoutManager.getInstance();

        try {

            connectionTimeoutManager.registerConnection(messageContext);

            // Valid payload consuming logic needs to be implement to read the complete request
            // Thread should not cont until it read the full content of the request body.

            connectionTimeoutManager.unregisterConnection(messageContext);

            if (Boolean.TRUE.equals(messageContext.getProperty("IS_PARTIAL_REQUEST"))) {
                throw new SynapseException("Client failed to send the request within the timeout");
            }

        } catch (SynapseException e){
            log.error("Client failed to send the request within the timeout of " + ConnectionTimeoutManager.getInstance().getConnectionTimeout() + "ms" );
            throw new SynapseException("Client failed to send the request within the timeout ");
        }

        return true;
    }

    public boolean handleRequestOutFlow(MessageContext messageContext)
    {
        log.info("Executing Request Outflow");
        return true;
    }

    // Handle outgoing response message flow of the WSO2 server
    public boolean handleResponseInFlow(MessageContext messageContext)
    {
        log.info("Executing Response Inflow");
        return true;
    }

    public boolean handleResponseOutFlow(MessageContext messageContext)
    {
        log.info("Executing Response Outflow");
        return true;
    }


}