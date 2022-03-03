package com.example.fitnesscloud;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Graph {
    ArrayList<GraphVertex> vertices = new ArrayList<GraphVertex>();
    ArrayList<GraphEdge> edges = new ArrayList<GraphEdge>();

    public Graph() {
        super();
    }

    static Graph create() {
        return new Graph();
    }

    Graph AddV(GraphVertex vtx) {
        vertices.add(vtx);
        return this;
    }

    Graph AddE(GraphEdge edge) {
        edges.add(edge);
        return this;
    }

    // Edges are not appearing at the moment maybe similar to this issue:
    // https://github.com/Azure/azure-cosmosdb-bulkexecutor-dotnet-getting-started/issues/69
    ArrayList<String> ToDocumentList() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ArrayList<String> result = new ArrayList<String>();
        for (GraphVertex vertex : vertices) {
            String gs = gson.toJson(vertex);
            result.add(gs);
        }
        for (GraphEdge graphEdge : edges) {
            String es = gson.toJson(graphEdge);
            result.add(es);
        }
        return result;
    }

    ArrayList<String> ToVDocumentList() {
        Gson gson = new Gson();
        ArrayList<String> result = new ArrayList<String>();
        for (GraphVertex vertex : vertices) {
            String gs = gson.toJson(vertex);
            result.add(gs);
        }
        return result;
    }

    ArrayList<String> ToEDocumentList() {
        Gson gson = new Gson();
        ArrayList<String> result = new ArrayList<String>();
        for (GraphEdge graphEdge : edges) {
            result.add(gson.toJson(graphEdge));
        }
        return result;
    }

    ArrayList<String> ToQueryList() {
        ArrayList<String> result = new ArrayList<String>();
        String traversal = new String("g");

        for (GraphVertex vertex : vertices) {
            traversal = traversal.concat(".addV(\"" + vertex.id + "\")");
            Class<?> c = vertex.getClass();

            Field[] flds = c.getFields();
            for (Field fld : flds)
            {
                String propertyName = null;
                String propertyValue = null;
                switch (fld.getType().getSimpleName())
                {
                    case "GraphProperty":
                        propertyName = fld.getName();
                        GraphPropertyValue val = null;
                        try {
                            GraphProperty fieldVal = (GraphProperty)fld.get(vertex);
                            val = (GraphPropertyValue)fieldVal.get(0);
                            propertyValue = val._value;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            continue;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            continue;
                        }
                        break;
                    case "String":
                        propertyName = fld.getName();
                        try {
                            propertyValue = (String)fld.get(vertex);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            continue;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            continue;
                        }
                        break;
                }

                if (propertyValue != null && propertyName != null) {
                    traversal = traversal.concat(".property(\"" + propertyName + "\", \"" + propertyValue + "\")");
                }
            }
        }

        // Not sure if we can chain all of these together or not...
        // Seems like we can chain the vertex creation all into one query but then subsequent queries that refer to 
        // those vertices might need to be submitted separately
        result.add(traversal);
        traversal = "";

        for (GraphEdge graphEdge : edges) {
            traversal = "g.V(\"" + graphEdge._vertexId + "\").addE(\"" + graphEdge.label + "\").to(g.V(\"" + graphEdge._sink + "\"))";
            result.add(traversal);
        }

        return result;
    }

    // Generate a graph programmatically:
    // Could generate a pool of PSCs and then randomly select from those to make it a bit more realistic
    public static Graph generate(int numberOfCompanies, int PSCsPerCompany, String partitionKey) {
        Graph graph = new Graph();
        Random rand = new Random();

        for (int i=0;i<numberOfCompanies;i++) {
            String companyName = String.format("Company%d", i);
            graph.AddV(new CompanyVertex(String.valueOf(rand.nextInt(1000000000)), companyName, partitionKey));

            for (int j=0;j<PSCsPerCompany;j++){
                String pscName = String.format("PSC%dCompany%d", i, j);

                graph.AddV(new PSCVertex(pscName, partitionKey));
                graph.AddE(new PSCToCompanyEdge(pscName, companyName, partitionKey));
            }
        }

        return graph;
    }
}
