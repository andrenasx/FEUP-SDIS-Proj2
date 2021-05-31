package messages.protocol;

import chord.ChordNodeReference;
import messages.Message;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;

public class ErrorMessage extends Message {
    private final String error;

    public ErrorMessage(ChordNodeReference senderReference, String error) {
        super("ERROR", senderReference);
        this.error = error;
    }

    @Override
    public Task getTask(Peer peer, SSLSocket socket) {
        return null;
    }

    public String getError() {
        return this.error;
    }
}
