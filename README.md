# Java Graph Database Cosmos BulkExecutor

> If you already know the background and reasoning then you can skip to the instructions for configuring the code [here](#configuration)

## Introduction

When faced with a task that includes populating a graph database in Cosmos DB there are two competing options:

- Construct Gremlin Queries and send these to Cosmos using the Gremlin SDK
  
- Send JSON documents representing the graph data directly to Cosmos via the Cosmos SQL API (see [Azure Cosmos DB Graph and SQL API multi-model functionality](https://github.com/LuisBosquez/azure-cosmos-db-graph-working-guides/blob/master/graph-backend-json.md))

From reading the documentation available there is a strong suggestion that using the SQL API may provide better performance and potentially reduce the associated cost. The SQL API usage can be achieved using the [Bulk Executor Library](https://docs.microsoft.com/en-us/azure/cosmos-db/bulk-executor-overview#key-features-of-the-bulk-executor-library). The perceived advantages would be:

- Increased performance

- Reduced Cost

- More maintainable and simpler code (since the BulkExecutor handles retries, timeouts and transient errors)

The code in this repository can be used to investigate the costs and performance of this further and inform any future implementation decisions you may have.

### Code

The code in the repository allows for the creation of a single in-memory representation of a graph data structure which can be submitted to a Cosmos DB either as a series of gremlin queries or as a list of JSON documents. Each run will print out data including the elapsed time in milliseconds and the number of RUs associated with the operations. Ths allows a direct comparison of submitting Gremlin queries and using the Bulk Executor.

The data model used represents `companies` and `persons of significant control` (PSCs) as graph vertices where there can be a relationship between a company and multiple PCSs and a PSC and multiple companies. However, the data model can easily be extended to explore more complex relationships.

The Java Gremlin SDK is used to submit the queries and the [Java Bulk Uploader library](https://docs.microsoft.com/en-us/azure/cosmos-db/bulk-executor-overview) is used for the bulk upload. The resulting graph representation in Cosmos will be identical. This is an example of the results for various amounts of graph data:

#### Notes

It should also be noted that there has been no attempt to optimise the queries.
The data is uploaded against an empty database on each run.

This spike code could easily be extended to allow further experimentation with closer to real world data for your particular scenario but that would need some effort to either model that data or import a file with data to process.

#### Test Runs

> Note that [Request Units (RUs)](https://docs.microsoft.com/en-us/azure/cosmos-db/request-units) are normalised units to help understand relative costs of the operations.  

##### Bulk Uploader

| Test Run                                           | Total RUs | Total Time Taken (ms) |
|----------------------------------------------------|-----------|-----------------------|
| 1 Company, 3 PSCs, 3 edges                         | 54.77     | 92                    |
| 20 companies, 2 PSCs per company, 1 edge per PSC   | 749.45    | 96                    |
| 24 companies, 2 PSCs per company, 1 edge per PSC   | 898.38    | 89                    |
| 26 companies, 3 PSCs per company, 1 edge per PSC   | 1355.99   | 100                   |
| 50 companies, 3 PSCs per company, 1 edge per PSC   | 2606.67   | 103                   |
| 200 companies, 5 PSCs per company, 1 edge per PSC  | 16320.77  | 109                   |
| 2000 companies, 5 PSCs per company, 1 edge per PSC | 163011.61 | 514                   |

##### Gremlin Queries

| Test Run                                           | Total RUs | Total Time Taken (ms) |
|----------------------------------------------------|-----------|-----------------------|
| 1 Company, 3 PSCs, 3 edges                         | 93.45     | 223                   |
| 20 companies, 2 PSCs per company, 1 edge per PSC   | 1330.0    | 3206                  |
| 24 companies, 2 PSCs per company, 1 edge per PSC   | 1596.0    | 3792                  |
| 26 companies, 3 PSCs per company, 1 edge per PSC   | 2429.70   | 5842                  |
| 50 companies, 3 PSCs per company, 1 edge per PSC   | 4672.5    | 11526                 |
| 200 companies, 5 PSCs per company, 1 edge per PSC  | 29470.0   | 66360                 |
| 2000 companies, 5 PSCs per company, 1 edge per PSC | 294700.0  | 643803                |

### Conclusions

The results would suggest that the Bulk Executor is never slower than using gremlin queries and the more graph data there is the more of a performance increase we see. For example, the test with 2000 companies above represents a **1,252 X** speed-up whilst the example with 26 companies shows approx a **60 X** speed gain.  

The cost saving when using the Bulk Executor is roughly half consistently throughout all tests.

In order to execute the console application you will need to provide access information for a Cosmos DB Graph Database:

The data needs to be updated in two different places:

## Configuration

### src/remote.yaml

The configuration for the bulk uploader.

Requires you to add host, username and password where the host is the SQL API endpoint, the username is the collection link in this format:

> /dbs/{databasename}/colls/{collectionName}

``` yaml
hosts: [<your-gremlin-endpoint>.gremlin.cosmosdb.azure.com]
port: 443
username: /dbs/<your-database>/colls/<your-collection>
password: <key goes here>
```

### Gremlin SDK

The configuration for the Gremlin SDK.

In addition, it is necessary to update some static strings in src/GraphExecutor.java:

```java
    private static String GraphEndpoint = "https://<your-graph-endpoint>.documents.azure.com:443/";
    private static String GraphEndpointKey = "";

    // These need to match what is in remote.yaml
    private static String GraphDatabaseName = "<your-database>";
    private static String GraphCollectionName = "<your-collection>";
```

Then you can comment in/out the following lines as appropriate:

```java
        // Convert the in-memory graph into a list of queries and submit them to 
        // re-create the graph in Cosmos 
        //executor.SubmitQueries(graph);

        // Convert the graph into a list of documents for bulk upload
        executor.BulkUpload(graph);
```

> Note. Any data will be removed from the database before these lines are executed.

The output from running the application will look something like this:

Bulk Uploader

Number of docs imported = 7
Total RUs = 54.650000
Total Time Taken = 97 ms

Gremlin Queries

Elapsed Time (ms) = 167
Number of queries = 4
Total RUs = 92.850000
Server Time (ms) = 71.628600
