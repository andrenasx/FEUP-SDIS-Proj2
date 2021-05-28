package messages.chord;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.protocol.PredecessorTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//CHORD PREDECESSOR <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n
public class PredecessorMessage extends ChordMessage {

    public PredecessorMessage(ChordNodeReference senderReference) {
        super("PREDECESSOR", senderReference, null);
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        return String.format("%s %s %d %s %d \r\n\r\n",
                "CHORD",
                this.action,
                this.getSenderGuid(),
                this.getSenderHostAddress(),
                this.getSenderPort()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public PredecessorTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new PredecessorTask(this, node, channel, engine);
    }

    @Override
    public String toString() {
        return "PredecessorMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
