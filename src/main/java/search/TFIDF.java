package search;

import model.DocumentData;

import java.util.*;

public class TFIDF {
    public static double calculateTermFrequency(List<String> words, String term){
        double ans = 0.0;
        double size = words.size();
        for(String word : words){
            if(word.equalsIgnoreCase(term)){
                ans++;
            }
        }
        return ans/words.size();
    }

    public static DocumentData calculateAllTermsFrequency(List<String> words, List<String> terms){
        DocumentData documentData = new DocumentData();
        for(String term : terms){
            double termFrequency = calculateTermFrequency(words, term);
            documentData.putTermFrequency(term,termFrequency);
        }

        return documentData;
    }

    public static double getTermInverseDocumentFrequency(String term, Map<String, DocumentData> documentResults){
        double count =0.0;
        //Iterates over all documents
        for (Map.Entry<String, DocumentData> entry : documentResults.entrySet()) {
            String document = entry.getKey();
            double termFrequency = documentResults.get(document).getTermFrequecy(term);
            if(termFrequency > 0)
                count++;
        }
        if(count == 0)
            return 0;
        return Math.log10(documentResults.size()/count);
    }

    public static Map<String,Double> getAllTermsInverseDocumentFrequency(List<String> terms,
                                                                         Map<String, DocumentData> documentResults){
        Map<String, Double> termsToInverseFrequency = new HashMap<>();

        for(String term : terms){
            double inverseDocumentFrequency = getTermInverseDocumentFrequency(term, documentResults);
            termsToInverseFrequency.put(term, inverseDocumentFrequency);
        }

        return termsToInverseFrequency;
    }

    public static double calculateScore(List<String> terms, DocumentData documentData,
                                       Map<String, Double> termsToInverseFrequency){
        double score = 0.0;
        for(String term : terms){
            double termFrequency = documentData.getTermFrequecy(term);
            double inverseDocumentFrequency = termsToInverseFrequency.get(term);
            score += termFrequency * inverseDocumentFrequency;
        }

        return score;
    }

    public static Map<Double, List<String>> getDocumentSortedByScore(List<String> terms,
                                                                     Map<String, DocumentData> documentResults){
        TreeMap<Double, List<String>> sortedDocument = new TreeMap<>();

        Map<String, Double> termsToInverseFrequency = getAllTermsInverseDocumentFrequency(terms, documentResults);

        for (Map.Entry<String, DocumentData> entry : documentResults.entrySet()){
            String document = entry.getKey();
            double score = calculateScore(terms, entry.getValue(), termsToInverseFrequency);
            if(sortedDocument.get(score) == null){
                sortedDocument.put(score,new ArrayList<>());
            }
            sortedDocument.get(score).add(document);
        }
        return sortedDocument.descendingMap();
    }

    public static List<String> getWordsFromLine(String line){
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)+"));
    }

    public static List<String> getWordsFromDocument(List<String> lines){
        List<String> wordsFromDocument = new ArrayList<>();
        for(String line : lines){
            wordsFromDocument.addAll(getWordsFromLine(line));
        }
        return wordsFromDocument;
    }
}
