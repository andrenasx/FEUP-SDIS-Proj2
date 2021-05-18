package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.ChordTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//GUID <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <CLOSEST PRED. GUID> <CLOSEST PRED. IP> <CLOSEST PRED. PORT>
public class PredecessorReplyMessage extends ChordMessage {
    private ChordNodeReference predecessor = null;

    public PredecessorReplyMessage(ChordNodeReference senderReference, byte[] body) {
        super("PREDECESSORREPLY", senderReference, body);
        if (body.length != 0)
            this.predecessor = new ChordNodeReference(body);
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %d %s %d \r\n\r\n",
                this.messageType,
                this.getSenderGuid(),
                this.getSenderHostAddress(),
                this.getSenderPort()).getBytes(StandardCharsets.UTF_8);

        if (body.length == 0) { return header; }

        // Create Message array
        byte[] message = new byte[header.length + this.body.length];

        // Copy Header and Body to Message array
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(this.body, 0, message, header.length, body.length);

        return message;
    }

    @Override
    public ChordTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return null;
    }

    @Override
    public String toString() {
        return "PredecessorReplyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", predecessor=" + this.predecessor +
                '}';
    }

    public ChordNodeReference getPredecessor() {
        return this.predecessor;
    }
}
