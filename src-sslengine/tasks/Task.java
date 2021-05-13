package tasks;

import messages.Message;
import peer.Peer;

public abstract class Task implements Runnable {
    protected Peer peer;
    protected Message message;


    public Task(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }
}
