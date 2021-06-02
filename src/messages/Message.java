package messages;

import chord.ChordNodeReference;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLSocket;
import java.io.Serializable;
import java.net.InetSocketAddress;

public abstract class Message implements Serializable {
    protected ChordNodeReference senderReference;

    public Message(ChordNodeReference senderReference) {
        this.senderReference = senderReference;
    }


    public abstract Task getTask(Peer peer, SSLSocket socket);

    public ChordNodeReference getSenderNodeReference() {
        return senderReference;
    }

    public InetSocketAddress getSenderSocketAddress() {
        return this.senderReference.getSocketAddress();
    }

    public int getSenderGuid() {
        return this.senderReference.getGuid();
    }
}
