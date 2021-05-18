package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.AliveTask;
import tasks.GuidTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//GUID <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n <NEWGUID>
public class AliveMessage extends ChordMessage {

    public AliveMessage(ChordNodeReference senderReference) {
        super("ALIVE", senderReference, null);
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
    public AliveTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new AliveTask(this, node, channel, engine);
    }

    @Override
    public String toString() {
        return "ErrorMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
