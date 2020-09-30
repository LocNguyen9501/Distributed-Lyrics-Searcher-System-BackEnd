package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    public static final String ELECTION_ADDRESS = "/election";
    public final ZooKeeper zooKeeper;
    public String zNodeName;
    public final OnElectionCallback onElectionCallback;

    public LeaderElection(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback){
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
    }

    public void volunteerForLeaderShip() throws KeeperException, InterruptedException {
        String prefix = ELECTION_ADDRESS+"/c_";
        String fullPath = zooKeeper.create(prefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        this.zNodeName = fullPath.replace(ELECTION_ADDRESS+"/", "");
        System.out.println("zNode is created with pathName "+ fullPath);
    }

    public void reelectLeader() throws KeeperException, InterruptedException {
        Stat stat = null;
        String watchedZNode = "";

        while(stat == null){
            List<String> children = zooKeeper.getChildren(ELECTION_ADDRESS, false);
            Collections.sort(children);
            String leader = children.get(0);

            if(zNodeName.equals(leader)){
                System.out.println("I'm the leader!");
                onElectionCallback.onElectedToBeLeader();
                return;
            }else{
                int watchedZnodeIdx = Collections.binarySearch(children, zNodeName) -1;
                watchedZNode = children.get(watchedZnodeIdx);
                stat = zooKeeper.exists(ELECTION_ADDRESS+"/"+watchedZNode, this);
                System.out.println("I'm not the leader!");
            }
        }

        onElectionCallback.onElectedToBeWorker();
        System.out.println("Watching "+ watchedZNode);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
            switch (watchedEvent.getType()){
                case NodeDeleted:
                    try {
                        reelectLeader();
                    } catch (KeeperException e) {
                    } catch (InterruptedException e) {
                    }
            }

    }
}
