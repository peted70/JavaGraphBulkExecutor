package com.example.fitnesscloud;

public class PSCToCompanyEdge extends GraphEdge {

    public PSCToCompanyEdge(String PSC, String Company, String _partitionKey) {
        super();
        relationship = "Worksfor";
        label = "Employee";

        _sink = PSC;
        _sinkLabel = "Person";
        _vertexId = Company;
        _vertexLabel = "Company";
        _sinkPartition = _partitionKey;

        partitionKey = _partitionKey;
    }
}
