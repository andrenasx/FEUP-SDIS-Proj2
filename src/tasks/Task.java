package tasks;

import chord.ChordNode;
import messages.Message;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;

public abstract class Task implements Runnable {
    protected final Message message;
    protected ChordNode node;
    protected SocketChannel channel;
    protected SSLEngine engine;


    public Task(Message message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        this.message = message;
        this.node = node;
        this.channel = channel;
        this.engine = engine;
    }
}
