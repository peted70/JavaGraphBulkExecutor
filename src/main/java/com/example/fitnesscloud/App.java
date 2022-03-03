package com.example.fitnesscloud;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        // Construct simple graph with one company related to 3 PSCs using custom DSL
        String PartitionKey = "part1";

        // Generate some random data 
        // Graph graph = Graph.generate(100, 3, PartitionKey);

        String companyName = "fcl";
        Graph graph = Graph.create()
            .AddV(new CompanyVertex("123456", companyName, PartitionKey))
            .AddV(new PSCVertex("Freya", PartitionKey))
            .AddV(new PSCVertex("Mei", PartitionKey))
            .AddV(new PSCVertex("Jared", PartitionKey))
            .AddE(new PSCToCompanyEdge("Freya", companyName, PartitionKey))
            .AddE(new PSCToCompanyEdge("Mei", companyName, PartitionKey))
            .AddE(new PSCToCompanyEdge("Jared", companyName, PartitionKey));

        // Minimal sample useful to demonstrate when things are not working as expected.    
        // Graph graph = Graph.create()
        //     .AddV(new CompanyVertex("123456", companyName, PartitionKey))
        //     .AddV(new PSCVertex("Freya", PartitionKey))
        //     .AddE(new PSCToCompanyEdge("Freya", companyName, PartitionKey));

        // Create a graph executor for the Cosmos gremlin DB    
        GraphExecutor executor = new GraphExecutor();    
        executor.Drop();

        // Convert the in-memory graph into a list of queries and submit them to 
        // re-create the graph in Cosmos 
        //executor.SubmitQueries(graph);

        // Convert the graph into a list of documents for bulk upload
        executor.BulkUpload(graph);
    }
}
