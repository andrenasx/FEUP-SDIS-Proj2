package messages.chord;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.protocol.LookupTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//CHORD LOOKUP <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <GUID>
public class LookupMessage extends ChordMessage {
    private final int guid;

    public LookupMessage(ChordNodeReference senderReference, byte[] body) {
        super("LOOKUP", senderReference, body);
        this.guid = Integer.parseInt(new String(body));
    }

    public LookupMessage(ChordNodeReference senderReference, int guid) {
        super("LOOKUP", senderReference, String.valueOf(guid).getBytes(StandardCharsets.UTF_8));
        this.guid = guid;
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %s %d %s %d \r\n\r\n",
                "CHORD",
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
    public LookupTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new LookupTask(this, node, channel, engine);
    }

    @Override
    public String toString() {
        return "LookupMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", guid=" + this.guid +
                '}';
    }

    public int getRequestedGuid() {
        return this.guid;
    }
}
