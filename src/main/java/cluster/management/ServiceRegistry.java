package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    public static final String WORKER_SERVICE_REGISTRY = "/worker_service_registry";
    public static final String COORDINATOR_SERVICE_REGISTRY = "/coordinator_service_registry";
    private final String SERVICE_REGISTRY_ADDRESS;
    private final ZooKeeper zooKeeper;
    private String currentzNodeFullPath;
    private List<String> allChildrenAddresses;

    public ServiceRegistry(ZooKeeper zooKeeper, String serviceAdresss) throws KeeperException, InterruptedException {
        this.zooKeeper = zooKeeper;
        this.SERVICE_REGISTRY_ADDRESS = serviceAdresss;
        createServiceRegistry();
    }

    public void createServiceRegistry() throws KeeperException, InterruptedException {
        if(zooKeeper.exists(SERVICE_REGISTRY_ADDRESS, false) == null){
            zooKeeper.create(SERVICE_REGISTRY_ADDRESS, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public void registerToCluster(String data) throws KeeperException, InterruptedException {
        if(currentzNodeFullPath != null){
            System.out.println("Already registered with service registry!");
            return;
        }
        String prefix = SERVICE_REGISTRY_ADDRESS+"/n_";
        currentzNodeFullPath = zooKeeper.create(prefix, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered successfully with service registry! with name "+currentzNodeFullPath);
    }

    public void unregisterFromCluster() throws KeeperException, InterruptedException {
        if(currentzNodeFullPath == null || zooKeeper.exists(currentzNodeFullPath, false) == null)
            return;

        zooKeeper.delete(currentzNodeFullPath, -1);
    }

    public void updateAddresses() throws KeeperException, InterruptedException {
        List<String> children = zooKeeper.getChildren(SERVICE_REGISTRY_ADDRESS, this);

        List<String> childrenAddresses = new ArrayList<>();

        for(String child : children){
            Stat stat = zooKeeper.exists(SERVICE_REGISTRY_ADDRESS+"/"+child, false);
            if(stat == null)
                continue;

            byte[] address = zooKeeper.getData(SERVICE_REGISTRY_ADDRESS+"/"+child, false, stat);
            String addressInString = new String(address);
            childrenAddresses.add(addressInString);
        }
        this.allChildrenAddresses = Collections.unmodifiableList(childrenAddresses);
        System.out.println("Worker addresses are: "+ this.allChildrenAddresses);
    }

    public List<String> getAllChildrenAddresses() throws KeeperException, InterruptedException {
        updateAddresses();
        return this.allChildrenAddresses;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
            switch(watchedEvent.getType()){
                case NodeChildrenChanged:
                    try {
                        updateAddresses();
                    } catch (KeeperException e) {
                    } catch (InterruptedException e) {
                    }
            }
    }
}
