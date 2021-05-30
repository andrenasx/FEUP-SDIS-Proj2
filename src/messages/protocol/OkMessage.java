package messages.protocol;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class OkMessage extends ProtocolMessage {
    public OkMessage(ChordNodeReference senderReference) {
        super("OK", senderReference, null);
    }

    @Override
    public byte[] encode() {
        return String.format("%s %s %d %s %d \r\n\r\n",
                "PROTOCOL",
                this.action,
                this.getSenderGuid(),
                this.getSenderHostAddress(),
                this.getSenderPort()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Task getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return null;
    }
}
