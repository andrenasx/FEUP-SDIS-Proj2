package tasks.chord;

import chord.Peer;
import messages.chord.ChordMessage;

public abstract class Task implements Runnable {
    protected Peer peer;
    protected ChordMessage message;


    public Task(Peer peer, ChordMessage message) {
        this.peer = peer;
        this.message = message;
    }
}
