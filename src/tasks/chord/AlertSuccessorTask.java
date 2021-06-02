package tasks.chord;

import messages.chord.AlertSuccessorMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class AlertSuccessorTask extends Task {
    public AlertSuccessorTask(AlertSuccessorMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        System.out.println("ALERT SUCCESSOR TASK");
        AlertSuccessorMessage alert = (AlertSuccessorMessage) message;

        peer.setPredecessor(alert.getPredecessor());
        for (StorageFile file : alert.getDelegatedFiles()) {
            peer.getScheduler().submit(() -> peer.storeDelegatedFile(file));
        }
    }
}
