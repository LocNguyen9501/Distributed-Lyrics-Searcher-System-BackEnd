package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Task implements Serializable {
    private final List<String> documents;
    private final List<String> searchTerms;

    public Task(List<String> documents, List<String> searchTerms) {
        this.documents = documents;
        this.searchTerms = searchTerms;
    }

   public List<String> getDocuments(){
        return Collections.unmodifiableList(documents);
    }

    public List<String> getSearchTerms(){
        return Collections.unmodifiableList(searchTerms);
    }
}
