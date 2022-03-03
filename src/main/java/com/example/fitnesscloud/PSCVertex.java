package com.example.fitnesscloud;

public class PSCVertex extends GraphVertex {
    public PSCVertex() {
        super();
        label = "Person";
    }    
    public PSCVertex(String PSCName, String _partitionKey) {
        super();
        label = "Person";
        id = PSCName;
        partitionKey = _partitionKey;
    }    
}
