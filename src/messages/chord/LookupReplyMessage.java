package messages.chord;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//CHORD LOOKUPREPLY <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <CLOSEST PRED. GUID> <CLOSEST PRED. IP> <CLOSEST PRED. PORT>
public class LookupReplyMessage extends ChordMessage {
    private final ChordNodeReference node;

    public LookupReplyMessage(ChordNodeReference senderReference, byte[] body) {
        super("LOOKUPREPLY", senderReference, body);
        this.node = new ChordNodeReference(body);
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %d %s %d \r\n\r\n",
                this.action,
                this.getSenderGuid(),
                this.getSenderHostAddress(),
                this.getSenderPort()).getBytes(StandardCharsets.UTF_8);

        // Create Message array
        byte[] message = new byte[header.length + this.body.length];

        // Copy Header and Body to Message array
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(this.body, 0, message, header.length, body.length);

        return message;
    }

    @Override
    public Task getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return null;
    }

    @Override
    public String toString() {
        return "LookupReplyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", successor=" + this.node +
                '}';
    }

    public ChordNodeReference getNode() {
        return this.node;
    }
}
