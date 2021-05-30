package messages.protocol;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ErrorMessage extends ProtocolMessage {
    private final String error;
    public ErrorMessage(ChordNodeReference senderReference, byte[] body) {
        super("ERROR", senderReference, body);
        this.error = new String(body);
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %s %d %s %d \r\n\r\n",
                "PROTOCOL",
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

    public String getError() {
        return this.error;
    }
}
