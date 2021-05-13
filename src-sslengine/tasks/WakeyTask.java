package tasks;

import messages.DeleteMessage;
import messages.WakeyMessage;
import peer.Peer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

public class WakeyTask extends Task {
    public WakeyTask(Peer peer, WakeyMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        System.out.printf("[WAKEY] Received WakeyWakey from Peer%d\n", this.message.getSenderId());

        String connection = new String(this.message.getBody());
        String[] parts = connection.split(":");

        try {
            // Send DELETE message for files that the peer that woke up had backed up
            for (Map.Entry<String, Set<Integer>> entry : this.peer.getStorage().getDeletedFilesMap().entrySet()) {
                if (entry.getValue().contains(this.message.getSenderId())) {
                    // Create socket and open output stream
                    Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
                    OutputStream out = socket.getOutputStream();

                    // Send respective delete message
                    DeleteMessage deleteMessage = new DeleteMessage(this.peer.getProtocolVersion(), this.peer.getId(), entry.getKey());
                    out.write(deleteMessage.encode());
                    System.out.printf("[WAKEY] Submitted delete to Peer%d for file: %s\n", this.message.getSenderId(), entry.getKey());

                    // Close outputstream and socket
                    out.close();
                    socket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Error in TCP socket");
        }
    }
}
