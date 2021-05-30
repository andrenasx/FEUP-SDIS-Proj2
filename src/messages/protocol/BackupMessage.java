package messages.protocol;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.chord.ChordMessage;
import tasks.chord.BackupTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class BackupMessage extends ProtocolMessage {
    private final String fileId;
    private final String filename;
    private final long size;
    private final int replicationDegree;

    public BackupMessage(ChordNodeReference senderReference, byte[] body) {
        super("BACKUP", senderReference, body);

        String[] bodyParts = new String(body).split(" ", 4);

        this.fileId = bodyParts[0];
        this.filename = bodyParts[1];
        this.size = Long.parseLong(bodyParts[2]);
        this.replicationDegree = Integer.parseInt(bodyParts[3]);
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
    public BackupTask getTask(ChordNode node, SocketChannel channel, SSLEngine engine) {
        return new BackupTask(this, node, channel, engine);
    }

    public String getFileId() {
        return fileId;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }
}
