import cluster.management.OnElectionCallback;
import cluster.management.ServiceRegistry;
import network.WebClient;
import network.WebServer;
import org.apache.zookeeper.KeeperException;
import search.SearchCoordinator;
import search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AfterElectionAction implements OnElectionCallback {
    private final ServiceRegistry workerServiceRegistry;
    private final ServiceRegistry coordinatorServiceRegistry;
    private final int port;
    private WebServer webServer;
    private WebClient webClient;

    public AfterElectionAction(ServiceRegistry workerServiceRegistry, ServiceRegistry coordinatorServiceRegistry, int port){
        this.workerServiceRegistry = workerServiceRegistry;
        this.coordinatorServiceRegistry = coordinatorServiceRegistry;
        this.webServer = null;
        this.webClient = null;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader() {
        try {
            workerServiceRegistry.unregisterFromCluster();
            workerServiceRegistry.updateAddresses();

            if(this.webServer != null){
                this.webServer.stop();
            }

            SearchCoordinator searchCoordinator = new SearchCoordinator(new WebClient(), workerServiceRegistry);
            this.webServer = new WebServer(this.port, searchCoordinator);
            this.webServer.startServer();

            String address = String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(),this.port);
            coordinatorServiceRegistry.registerToCluster(address);
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        } catch (UnknownHostException e) {
        }
    }

    @Override
    public void onElectedToBeWorker(){
        try {
            if(webServer == null){
                SearchWorker searchWorker = new SearchWorker();
                this.webServer = new WebServer(this.port, searchWorker);
                this.webServer.startServer();
            }

            String address = String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(),this.port);
            workerServiceRegistry.registerToCluster(address);
        } catch (UnknownHostException e) {
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }
    }
}
