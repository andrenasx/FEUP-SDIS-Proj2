package tasks;

import chord.ChordNode;
import messages.ChordMessage;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;

public abstract class ChordTask implements Runnable {
    protected ChordMessage message;
    protected ChordNode node;
    protected SocketChannel channel;
    protected SSLEngine engine;


    public ChordTask(ChordMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        this.message = message;
        this.node = node;
        this.channel = channel;
        this.engine = engine;
    }
}
