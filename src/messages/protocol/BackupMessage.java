package messages.protocol;

import chord.ChordNode;
import chord.ChordNodeReference;
import messages.chord.ChordMessage;
import tasks.chord.BackupTask;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class BackupMessage extends ProtocolMessage {
    String fileId;
    String filename;
    long size;

    public BackupMessage(ChordNodeReference senderReference, byte[] body) {
        super("BACKUP", senderReference, body);

        String[] bodyParts = new String(body).split(" ", 3);

        this.fileId = bodyParts[0];
        this.filename = bodyParts[1];
        this.size = Integer.parseInt(bodyParts[2]);
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

    public String getFileId(){
        return fileId;
    }

    public long getSize(){
        return size;
    }

    public String getFilename(){
        return filename;
    }

}
