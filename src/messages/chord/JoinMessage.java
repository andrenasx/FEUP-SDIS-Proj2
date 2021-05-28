package messages.chord;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.protocol.JoinTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

//CHORD JOIN <SENDER GUID> <SENDER ADDRESS IP> <SENDER PORT> \r\n\r\n
public class JoinMessage extends ChordMessage {
    public JoinMessage(ChordNodeReference senderReference) {
        super("JOIN", senderReference);
    }

    @Override
    public byte[] encode() {
        return String.format("%S %s %d %s %d \r\n\r\n",
                "CHORD",
                this.action,
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
