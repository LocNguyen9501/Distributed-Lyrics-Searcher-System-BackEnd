import model.DocumentData;
import search.TFIDF;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class SequentialSearch {
    public static final String LYRICS_DIRECTORY = "./resources/lyrics";
    public static String query = "New York City";

    public static void main(String[] args) throws FileNotFoundException {
        File documentsPath = new File(LYRICS_DIRECTORY);
        List<String> terms = TFIDF.getWordsFromLine(query);

        List<String> documents = Arrays.asList(documentsPath.list())
                .stream()
                .map(documentName -> LYRICS_DIRECTORY+"/"+documentName)
                .collect(Collectors.toList());
        findTheSong(documents, terms);
    }

    public static void findTheSong(List<String> documents, List<String> terms) throws FileNotFoundException {
        Map<String, DocumentData> documentResults = new HashMap<>();

        for(String document : documents){
            BufferedReader reader = new BufferedReader(new FileReader(document));
            List<String> lines = reader.lines().collect(Collectors.toList());
            List<String> words = TFIDF.getWordsFromDocument(lines);
            DocumentData documentData = TFIDF.calculateAllTermsFrequency(words, terms);
            documentResults.put(document, documentData);
        }

        Map<Double, List<String>> sortedDocument = TFIDF.getDocumentSortedByScore(terms, documentResults);
        printOut(sortedDocument);
    }

    public static void printOut(Map<Double,List<String>> sortedDocument){
        for(Map.Entry<Double, List<String>> docs : sortedDocument.entrySet()){
            double score = docs.getKey();
            for(String documents: docs.getValue()){
                System.out.println(String.format("Song : %s with score: %f", documents.split("/")[3],score));
            }
        }
    }
}
