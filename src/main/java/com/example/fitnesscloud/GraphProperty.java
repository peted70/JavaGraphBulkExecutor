package com.example.fitnesscloud;

import java.util.ArrayList;
import java.util.UUID;

public class GraphProperty extends ArrayList<GraphPropertyValue> {

    public void Add(String name){
        GraphPropertyValue val = new GraphPropertyValue();
        val._value = name;
        val.id = UUID.randomUUID().toString();
        add(val);
    }
}
