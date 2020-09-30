import cluster.management.OnElectionCallback;
import cluster.management.ServiceRegistry;
import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AfterElectionAction implements OnElectionCallback {
    public final ServiceRegistry serviceRegistry;
    public final int port;

    public AfterElectionAction(ServiceRegistry serviceRegistry, int port){
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader() {
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.updateAddresses();
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void onElectedToBeWorker(){
        try {
            String address = String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(),this.port);
            serviceRegistry.registerToCluster(address);
        } catch (UnknownHostException e) {
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }
    }
}
