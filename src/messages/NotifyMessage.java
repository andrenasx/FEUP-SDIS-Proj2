package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.LookupTask;
import tasks.NotifyTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//GUID <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <GUID>
public class NotifyMessage extends ChordMessage {

    public NotifyMessage(ChordNodeReference senderReference) {
        super("NOTIFY", senderReference, null);
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        return String.format("%s %d %s %d \r\n\r\n",
                this.messageType,
                this.getSenderGuid(),
                this.getSenderHostAddress(),
                this.getSenderPort()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public NotifyTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new NotifyTask(this, node, channel, engine);
    }

    @Override
    public String toString() {
        return "NotifyMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
