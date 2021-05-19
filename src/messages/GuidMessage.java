package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.GuidTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//GUID <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <NEWGUID> <SUCCESSOR>
public class GuidMessage extends ChordMessage {
    private final int guid;
    private final ChordNodeReference successor;

    public GuidMessage(ChordNodeReference senderReference, byte[] body) {
        super("GUID", senderReference, body);

        String[] bodyParts = new String(body).split(" ", 2);

        this.guid = Integer.parseInt(bodyParts[0]);
        this.successor = new ChordNodeReference(bodyParts[1].getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %d %s %d \r\n\r\n",
                this.messageType,
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
    public GuidTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new GuidTask(this, node);
    }

    @Override
    public String toString() {
        return "GuidMessage {" +
                "sender=" + this.getSenderNodeReference() +
                ", newGuid=" + this.guid +
                ", successor=" + this.successor +
                '}';
    }

    public int getNewGuid() {
        return this.guid;
    }

    public ChordNodeReference getSuccessorReference() {
        return this.successor;
    }
}
