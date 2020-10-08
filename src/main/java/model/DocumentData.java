package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/* Class that stores the data of each term and its term frequency */
public class DocumentData implements Serializable {
    private Map<String, Double> map = new HashMap<>();

    public void putTermFrequency(String term, Double termFrequency){
        map.put(term, termFrequency);
    }

    public double getTermFrequecy(String term){
        return map.get(term);
    }
}
