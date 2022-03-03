package com.example.fitnesscloud;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.DocumentClientException;
import com.microsoft.azure.documentdb.bulkexecutor.BulkImportResponse;
import com.microsoft.azure.documentdb.bulkexecutor.DocumentBulkExecutor;
import com.microsoft.azure.documentdb.ConnectionPolicy;
import com.microsoft.azure.documentdb.ConsistencyLevel;
import com.microsoft.azure.documentdb.DocumentCollection;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;

public class GraphExecutor {
    private static String GraphEndpoint = "https://<your-graph-endpoint>.documents.azure.com:443/";
    private static String GraphEndpointKey = "";

    // These need to match what is in remote.yaml
    private static String GraphDatabaseName = "<your-database>";
    private static String GraphCollectionName = "<your-collection>";
    
    private Cluster cluster;
    private Client gremlinclient;
    private DocumentBulkExecutor bulkExecutor;
    private DocumentClient docClient;
    private DocumentCollection collection;

    private Client GetGremlinClient() {
        if (gremlinclient == null) {
            try {
                // Attempt to create the connection objects
                cluster = Cluster.build(new File("src/remote.yaml")).create();
                gremlinclient = cluster.connect();
            } catch (FileNotFoundException e) {
                // Handle file errors.
                System.out.println("Couldn't find the configuration file.");
                e.printStackTrace();
            }
        }
        return gremlinclient;
    }

    DocumentClient GetDocumentClient() {
        if (docClient == null) {
            ConnectionPolicy connectionPolicy = new ConnectionPolicy();
            connectionPolicy.setMaxPoolSize(1000);
            docClient = new DocumentClient(
                GraphEndpoint,
                GraphEndpointKey, 
                connectionPolicy,
                ConsistencyLevel.Session);
    
            // Set client's retry options high for initialization
            docClient.getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(30);
            docClient.getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(9);
    
        }
        return docClient;
    }

    DocumentCollection GetDocumentCollection() {
        if (collection == null) {
            // This assumes database and collection already exist
            String collectionLink = String.format("/dbs/%s/colls/%s", GraphDatabaseName, GraphCollectionName);
            try {
                collection = GetDocumentClient().readCollection(collectionLink, null).getResource();
            } catch (DocumentClientException e) {
                e.printStackTrace();
            }
        }
        return collection;
    }

    private DocumentBulkExecutor GetBulkExecutor() {
        if (bulkExecutor == null) {
            // Builder pattern
            DocumentBulkExecutor.Builder bulkExecutorBuilder = DocumentBulkExecutor.builder().from(
                GetDocumentClient(),
                GraphDatabaseName,
                GraphCollectionName,
                GetDocumentCollection().getPartitionKey(),
                1000); // throughput you want to allocate for bulk import out of the container's total throughput

            // Set retries to 0 to pass complete control to bulk executor
            GetDocumentClient().getConnectionPolicy().getRetryOptions().setMaxRetryWaitTimeInSeconds(0);
            GetDocumentClient().getConnectionPolicy().getRetryOptions().setMaxRetryAttemptsOnThrottledRequests(0);

            // // Instantiate DocumentBulkExecutor
            try {
                bulkExecutor = bulkExecutorBuilder.build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bulkExecutor;
    }

    public void Drop() {
        String deleteEQuery = new String("g.E().drop()");
        ResultSet resultSet = GetGremlinClient().submit(deleteEQuery);
        List<Result> results = null;
        try {
            results = resultSet.all().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        String deleteVQuery = new String("g.V().drop()");
        resultSet = GetGremlinClient().submit(deleteVQuery);
        try {
            results = resultSet.all().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // See server responses documentation here 
    // https://docs.microsoft.com/en-us/azure/cosmos-db/graph/gremlin-headers#headers
    // "x-ms-status-code": 200,
    // "x-ms-request-charge": 0,
    // "x-ms-total-request-charge": 123.85999999999989,
    // "x-ms-server-time-ms": 0.0419,
    // "x-ms-total-server-time-ms": 129.73709999999994,
    // "x-ms-activity-id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    public void SubmitQueries(Graph graph) throws InterruptedException, ExecutionException {
        ArrayList<String> queryList = graph.ToQueryList();
        List<Result> results = null;
        Double requestCharge = 0.0;
        Double totalRequestCharge = 0.0;
        Double serverTime = 0.0;
        Double totalServerTime = 0.0;

        long start = System.currentTimeMillis();

        for (String query : queryList) {
            ResultSet resultSet = GetGremlinClient().submit(query);
            requestCharge += (Double)resultSet.statusAttributes().get().get("x-ms-request-charge");
            totalRequestCharge += (Double)resultSet.statusAttributes().get().get("x-ms-total-request-charge");
            serverTime += (Double)resultSet.statusAttributes().get().get("x-ms-server-time-ms");
            totalServerTime += (Double)resultSet.statusAttributes().get().get("x-ms-total-server-time-ms");

            // Check results..
            try {
                results = resultSet.all().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        System.out.println("-----------------------------------------------------");
        System.out.println("Gremlin Queries");
        System.out.println("-----------------------------------------------------");
        System.out.println(String.format("Total Time Taken = %d ms", timeElapsed));
        System.out.println(String.format("Total RUs = %f", totalRequestCharge));
        System.out.println(String.format("Server Time (ms) = %f", totalServerTime));
        System.out.println(String.format("Number of queries = %d", queryList.size()));
        System.out.println("-----------------------------------------------------");
    }

    public void BulkUpload(Graph graph) {
        ArrayList<String> docList = graph.ToDocumentList();

        try {
            BulkImportResponse resp = GetBulkExecutor().importAll(docList, false, true, null);

            System.out.println("-----------------------------------------------------");
            System.out.println("Bulk Executor");
            System.out.println("-----------------------------------------------------");
            System.out.println(String.format("Total Time Taken = %d ms", Duration.of(resp.getTotalTimeTaken().getNano(), ChronoUnit.NANOS).toMillis()));
            System.out.println(String.format("Total RUs = %f", resp.getTotalRequestUnitsConsumed()));
            System.out.println(String.format("Number of docs imported = %d", resp.getNumberOfDocumentsImported()));
            System.out.println("-----------------------------------------------------");
        } catch (DocumentClientException e) {
            e.printStackTrace();
        }
    }
}
