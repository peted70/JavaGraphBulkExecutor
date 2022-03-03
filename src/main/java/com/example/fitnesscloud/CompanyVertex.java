package com.example.fitnesscloud;

public class CompanyVertex extends GraphVertex {
    public GraphProperty CRN = new GraphProperty();

    public CompanyVertex() {
        super();
        label = "Company";
    }

    public CompanyVertex(String crn, String companyName, String _partitionKey) {
        super();
        label = "Company";
        CRN.Add(crn);
        this.id = companyName;
        partitionKey = _partitionKey;
    }
}
