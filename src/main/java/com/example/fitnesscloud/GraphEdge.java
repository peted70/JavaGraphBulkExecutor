package com.example.fitnesscloud;

import java.util.UUID;

public class GraphEdge {
    String label;
    String id = UUID.randomUUID().toString();
    String relationship;
    String _sink;
    String _sinkLabel;
    String _vertexId; 
    String _vertexLabel;
    String partitionKey;
    String _sinkPartition;

    boolean _isEdge =  true; 
}
