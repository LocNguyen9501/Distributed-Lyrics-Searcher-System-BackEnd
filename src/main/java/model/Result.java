package model;

import java.io.Serializable;
import java.util.HashMap;

public class Result implements Serializable {
    private HashMap<String, DocumentData> documentData = new HashMap<>();

    public void addDocumentData(String document, DocumentData documentData){
        this.documentData.put(document, documentData);
    }

    public HashMap<String, DocumentData> getDocumentData(){
        return this.documentData;
    }
}
