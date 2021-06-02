package tasks.chord;

import messages.chord.AlertSuccessorMessage;
import messages.chord.LookupReplyMessage;
import messages.protocol.OkMessage;
import peer.Peer;
import peer.storage.StorageFile;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AlertSuccessorTask extends Task {
    public AlertSuccessorTask(AlertSuccessorMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        AlertSuccessorMessage alert = (AlertSuccessorMessage) message;

        peer.setPredecessor(alert.getPredecessor());

        if(alert.getDelegatedFiles().isEmpty()) return;

        List<Future<Boolean>> restores = new ArrayList<>();
        for (StorageFile file : alert.getDelegatedFiles()) {
            restores.add(peer.getScheduler().submit(() -> peer.storeDelegatedFile(file, message.getSenderNodeReference())));
        }

        // Wait for restores to finish and send message to peer
        for (Future<Boolean> restore : restores) {
            try {
                restore.get();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        OkMessage response = new OkMessage(peer.getSelfReference());

        try {
            peer.sendMessage(socket, response);
            //System.out.println("Server sent: " + response);
        }catch (IOException e) {
            System.err.println("[ERROR-CHORD] Couldn't send OkMessage");
            e.printStackTrace();
        }

    }
}
