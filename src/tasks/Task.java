package tasks;

import messages.Message;
import peer.Peer;

import javax.net.ssl.SSLSocket;

public abstract class Task implements Runnable {
    protected final Message message;
    protected final Peer peer;
    protected final SSLSocket socket;


    public Task(Message message, Peer peer, SSLSocket socket) {
        this.message = message;
        this.peer = peer;
        this.socket = socket;
    }
}
