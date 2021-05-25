package messages;

import chord.ChordNode;
import chord.ChordNodeReference;
import tasks.BackupTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class BackupMessage extends ChordMessage {
    public BackupMessage(ChordNodeReference senderReference, byte[] body) {
        super("BACKUP", senderReference, body);
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
    public BackupTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new BackupTask(this, node, channel, engine);
    }
}
