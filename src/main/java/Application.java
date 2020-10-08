import cluster.management.LeaderElection;
import cluster.management.ServiceRegistry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Application implements Watcher {
    public static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    public static final int SESSION_TIME_OUT = 3000;
    public static final int DEFAULT_PORT = 8080;
    public ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        int port = DEFAULT_PORT;
        if(args.length == 1){
            port = Integer.parseInt(args[0]);
        }

        Application app = new Application();
        app.connectToZookeeper();

        ServiceRegistry workerServiceRegistry = new ServiceRegistry(app.zooKeeper, ServiceRegistry.WORKER_SERVICE_REGISTRY);
        ServiceRegistry coordinatorServiceRegistry = new ServiceRegistry(app.zooKeeper, ServiceRegistry.COORDINATOR_SERVICE_REGISTRY);
        AfterElectionAction afterElectionAction = new AfterElectionAction(workerServiceRegistry, coordinatorServiceRegistry, port);

        LeaderElection leaderElection = new LeaderElection(app.zooKeeper, afterElectionAction);
        leaderElection.volunteerForLeaderShip();
        leaderElection.reelectLeader();
        app.run();
        app.close();
        System.out.println("Exiting application!");
    }

    public void connectToZookeeper() throws IOException {
        //This watcher returns whether the server is still connected
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIME_OUT, this);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()){
            case None:
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("Successfully connected to zookeeper server!");
                }else{
                    synchronized (zooKeeper){
                        System.out.println("Disconnected from zookeeper!");
                        zooKeeper.notifyAll();
                    }
                }
        }
    }
}
