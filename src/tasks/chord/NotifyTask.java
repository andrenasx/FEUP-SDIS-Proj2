package tasks.chord;

import messages.chord.NotifyMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class NotifyTask extends Task {
    public NotifyTask(NotifyMessage message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        if (this.peer.getPredecessor() == null || this.peer.between(this.message.getSenderGuid(), this.peer.getPredecessor().getGuid(), this.peer.getSelfReference().getGuid(), false)) {
            //System.out.println("SETTING PREDECESSOR " + this.message.getSenderGuid() + " FOR peer " + this.peer.getSelfReference().getGuid());
            this.peer.setPredecessor(this.message.getSenderNodeReference());
        }

        try {
            this.peer.closeClient(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
