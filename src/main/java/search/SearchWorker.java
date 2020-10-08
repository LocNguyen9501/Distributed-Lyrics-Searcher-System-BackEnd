package search;

import cluster.management.ServiceRegistry;
import model.DocumentData;
import model.Result;
import model.SerializableUnits;
import model.Task;
import network.OnRequestCallBack;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;

public class SearchWorker implements OnRequestCallBack {
    public static final String SEARCH_ENDPOINT = "/task";

    @Override
    public byte[] handleTask(byte[] exchange) {
        Task task = (Task) SerializableUnits.deserialize(exchange);
        Result result = createResult(task);
        return SerializableUnits.serialize(result);
    }

    public Result createResult(Task task){
        Result result = new Result();

        List<String> documents = task.getDocuments();
        System.out.println("Received " + documents.size()+" documents from coordinator!");
        List<String> terms = task.getSearchTerms();

        for(String document : documents){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(document));
                List<String> lines = reader.lines().collect(Collectors.toList());
                List<String> words = TFIDF.getWordsFromDocument(lines);
                DocumentData documentData = TFIDF.calculateAllTermsFrequency(words, terms);
                result.addDocumentData(document, documentData);
            } catch (FileNotFoundException e) {
                System.out.println("Can't find files in the SearchWorker!");
                return null;
            }
        }

        return result;
    }

    @Override
    public String getEndPoint() {
        return SEARCH_ENDPOINT;
    }
}
