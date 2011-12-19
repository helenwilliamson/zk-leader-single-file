package net.spike.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * User: cyberroadie
 * Date: 30/11/2011
 */
public class ZNodeMonitor implements Watcher {

    final Logger logger = LoggerFactory.getLogger(ZNodeMonitor.class);
    private final String ROOT = "/SPEAKER";
    private ZNodeMonitorListener listener;
    private RecoverableZookeeper zk;
    private String connectionString;

    public void setListener(ZNodeMonitorListener listener) {
        this.listener = listener;
    }

    public ZNodeMonitor(String connectionString) {
        this.connectionString = connectionString;
    }

    public void start() throws IOException {
        this.zk = new RecoverableZookeeper(connectionString, this);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                processNoneEvent(watchedEvent);
                break;
            case NodeDeleted:
                listener.stopSpeaking();
                createZNode();
        }
        try {
            zk.exists(ROOT, this);
        } catch (Exception e) {
            shutdown(e);
        }
    }

    private void createZNode() {
        try {
            zk.create(ROOT, listener.getProcessName().getBytes());
            listener.startSpeaking();
        } catch (Exception e) {
            // Something went wrong, lets try set a watch first before
            // we take any action
        }
    }

    public void shutdown(Exception e) {
        logger.error("Unrecoverable error whilst trying to set a watch on election znode, shutting down client", e);
        System.exit(1);
    }

    /**
     * Something changed related to the connection or session
     *
     * @param event
     */
    public void processNoneEvent(WatchedEvent event) {
        switch (event.getState()) {
            case SyncConnected:
                createZNode();
                break;
            case AuthFailed:
            case Disconnected:
            case Expired:
            default:
                listener.stopSpeaking();
                break;
        }
    }

    public interface ZNodeMonitorListener {
        public void startSpeaking();

        public void stopSpeaking();

        public String getProcessName();
    }

}
