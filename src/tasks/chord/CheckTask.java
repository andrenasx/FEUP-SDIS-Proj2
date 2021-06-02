package tasks.chord;

import messages.Message;
import messages.protocol.OkMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class CheckTask extends Task {
    public CheckTask(Message message, Peer peer, SSLSocket socket) {
        super(message, peer, socket);
    }

    @Override
    public void run() {
        try {
            peer.sendMessage(socket, new OkMessage(peer.getSelfReference()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
