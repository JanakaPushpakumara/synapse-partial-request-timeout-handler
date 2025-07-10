# synapse-partial-request-timeout-handler

The custom **synapse-partial-request-timeout-handler** handler, specifically the PartialRequestTimeoutHandler class with the newly embedded ConnectionTimeoutManager, is designed to manage and terminate 
connections for partial requests that do not receive the full payload within a specified timeout interval.

## How to Enable the partial request timeout Handler 

#### Clone the repository and Build the handler with the command ```mavn clean install```
#### Copy the generated JAR file into the <APIM_HOME>/repository/components/lib directory to deploy the handler.
#### The deployment.toml configuration 
```
[synapse_handlers]
TestSynapseHandler.enabled = true
TestSynapseHandler.class = "org.example.PartialRequestTimeoutHandler"
```
#### **Syntax for old EI/ESB/APIM products**
To engage the handler, you need to add the following configuration to the <WSO2_HOME>/conf/synapse-handlers.xml file.
```
<handlers>
    <handler name="TestSynapseHandler" class="com.sample.PartialRequestTimeoutHandler"/>
</handlers>
```

### Set the following environment variables:

```
export REQUEST_CONNECTION_CLEANUP_INTERVAL=1000;
export REQUEST_READ_TIMEOUT=10000;
export CONNECTION_TIMEOUT_MANAGER_ENABLE=true;
```

* REQUEST_CONNECTION_CLEANUP_INTERVAL: Sets the interval (in milliseconds) at which the system checks and cleans up stale or incomplete connections. 
* REQUEST_READ_TIMEOUT: Defines the maximum time (in milliseconds) allowed to receive the full request body. If the body is not received within this period, the connection is terminated. 
* CONNECTION_TIMEOUT_MANAGER_ENABLE: Enables (true) or disables (false) the connection timeout manager logic. When enabled, the timeout logic is applied based on the API context list or to all APIs if the list is missing or empty.

### Create a configuration file to provide the API list:

Create a file named CTM_api_context_list.txt ( CTM denotes connection timeout manager) in the <APIM_HOME>/repository/conf/ directory. and update the file with the List of API contexts you want to apply the ConnectionTimeoutManager logic to. This logic will terminate connections associated with partial requests if the full request body is not received within the REQUEST_READ_TIMEOUT interval.

Ex: You may specify:

* An API context (ignores version) add line :```pizzashack/```
* A specific API version add  line :```pizzashack/1.0/```
* A specific API version and resource add line : ```pizzashack/1.0/order```

**Note:**

If CONNECTION_TIMEOUT_MANAGER_ENABLE=true is set, and the context list file is missing or empty, the timeout logic will be applied to all APIs.

**Important Note:**

Before deploying this to your production environment, please thoroughly test it in a production similar lower environment. While we have conducted local testing, those tests may not cover all scenarios specific to your production. Therefore, we strongly recommend validating the solution in your staging or QA environments first to ensure functionality and stability.