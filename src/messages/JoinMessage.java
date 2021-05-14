package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.ChordTask;
import tasks.JoinTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//JOIN <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT>
public class JoinMessage extends ChordMessage {
    public JoinMessage(ChordNodeReference senderReference) {
        super("JOIN", senderReference, null);
    }

    @Override
    public byte[] encode() {
        return String.format("%s %d %s %d \r\n\r\n",
                this.messageType,
                this.getSenderGuid(),
                this.getSenderHostAddress(),
                this.getSenderPort()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public JoinTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new JoinTask(this, node, channel, engine);
    }

    @Override
    public String toString() {
        return "JoinMessage {" +
                "sender=" + this.getSenderNodeReference() +
                '}';
    }
}
