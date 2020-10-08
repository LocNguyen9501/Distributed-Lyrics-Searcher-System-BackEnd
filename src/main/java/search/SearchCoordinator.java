package search;

import cluster.management.ServiceRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import model.DocumentData;
import model.Result;
import model.SerializableUnits;
import model.Task;
import model.proto.SearchModel;
import network.OnRequestCallBack;
import network.WebClient;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SearchCoordinator implements OnRequestCallBack {
    public static final String LYRICS_DIRECTORY = "./resources/lyrics";
    public static final String COORDINATOR_END_POINT = "/search";
    public final WebClient webClient;
    public final ServiceRegistry workerServiceRegistry;

    public SearchCoordinator(WebClient webClient, ServiceRegistry workerServiceRegistry){
        this.workerServiceRegistry = workerServiceRegistry;
        this.webClient = webClient;
    }

    @Override
    public String getEndPoint() {
        return COORDINATOR_END_POINT;
    }

    @Override
    public byte[] handleTask(byte[] requestFromFrontEnd) {
        try {
            SearchModel.Request request = SearchModel.Request.parseFrom(requestFromFrontEnd);
            SearchModel.Response response = createResponse(request);
            return response.toByteArray();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
        return SearchModel.Response.getDefaultInstance().toByteArray();
    }

    public SearchModel.Response createResponse(SearchModel.Request request) throws KeeperException, InterruptedException {
        SearchModel.Response.Builder response = SearchModel.Response.newBuilder();

        System.out.println("The Coordinator received the queries: "+request.getSearchQuery());

        List<String> searchTerms = TFIDF.getWordsFromLine(request.getSearchQuery());
        List<String> workerAddresses = workerServiceRegistry.getAllChildrenAddresses();
        if(workerAddresses.isEmpty()){
            System.out.println("There is currently no workers available!");
            return response.build();
        }

        List<Task> tasks = createTask(searchTerms, workerAddresses.size());
        List<Result> results = sendTaskToWorkers(tasks, workerAddresses);
        List<SearchModel.Response.DocumentStats> documentStats = aggregateResult(results, searchTerms);
        response.addAllRelevantDocuments(documentStats);
        return response.build();
    }

    public List<SearchModel.Response.DocumentStats> aggregateResult(List<Result> results, List<String> searchTerms){
        Map<String, DocumentData> allResults = new HashMap<>();

        for(Result result : results){
            allResults.putAll(result.getDocumentData());
        }

        System.out.println("Coordinator received back "+allResults.size()+" documents!");

        return sortDocumentByScore(allResults,searchTerms);
    }

    public List<SearchModel.Response.DocumentStats> sortDocumentByScore(Map<String, DocumentData> documentResults,
                                                                        List<String> searchTerms){
        List<SearchModel.Response.DocumentStats> listOfSortedDocument = new ArrayList<>();
        Map<Double, List<String>> allDocumentsSortedWithScore = TFIDF.getDocumentSortedByScore(searchTerms, documentResults);

        for(Map.Entry<Double, List<String>> docs : allDocumentsSortedWithScore.entrySet()){
            double score = docs.getKey();
            for(String documentPath : docs.getValue()){
                File file = new File(documentPath);
                SearchModel.Response.DocumentStats documentStats = SearchModel.Response.DocumentStats.newBuilder()
                        .setDocumentName(documentPath.replace(LYRICS_DIRECTORY+"/", ""))
                        .setScore(score)
                        .setDocumentSize(file.length())
                        .build();
                listOfSortedDocument.add(documentStats);
            }
        }

        return listOfSortedDocument;
    }
    

    public List<Result> sendTaskToWorkers(List<Task> tasks, List<String> workers){
        CompletableFuture<Result>[]futures = new CompletableFuture[workers.size()];

        for(int i = 0; i < workers.size(); i++){
            String workerAddress = workers.get(i);
            Task task = tasks.get(i);
            byte[] taskPayLoad = SerializableUnits.serialize(task);
            futures[i] = webClient.sendTask(workerAddress, taskPayLoad);
        }

        List<Result> results = new ArrayList<>();
        for(int i =0; i < futures.length;i++){
            Result result = futures[i].join();
            results.add(result);
        }

        return results;
    }

    public List<Task> createTask(List<String> searchTerms, int numberOfWorker){
        List<Task> listOfTasks = new ArrayList<>();
        List<String> documents = readDocsList();
        List<List<String>> documentsList = splitDocumentList(numberOfWorker, documents);

        for(List<String> listOfDocumentPerWorker : documentsList){
            Task task = new Task(listOfDocumentPerWorker, searchTerms);
            listOfTasks.add(task);
        }

        return listOfTasks;
    }

    public List<List<String>> splitDocumentList(int numberOfWorkers, List<String> documents){
        List<List<String>> listOfDocuments = new ArrayList<>();

        int numberOfDocsPerWorker = documents.size()/numberOfWorkers + 1;

        for(int idx = 0; idx < documents.size() ; idx++){
            ArrayList<String> listOfDocsForWorker = new ArrayList<>();
            for(int i =0; i < numberOfDocsPerWorker; i++){
                if(idx >= documents.size()){
                    break;
                }
                listOfDocsForWorker.add(documents.get(idx));
            }
            listOfDocuments.add(listOfDocsForWorker);
            idx = idx + numberOfDocsPerWorker -1;
        }

        return listOfDocuments;
    }

    public List<String> readDocsList() {
        File documentsPath = new File(LYRICS_DIRECTORY);

        return Arrays.asList(documentsPath.list())
                .stream()
                .map(documentName -> LYRICS_DIRECTORY+"/"+documentName)
                .collect(Collectors.toList());
    }


}
