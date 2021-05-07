package messages;

import peer.Peer;
import tasks.ChunkTask;

import java.nio.charset.StandardCharsets;

public class ChunkMessage extends Message {
    public ChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNo, byte[] body) {
        super(protocolVersion, "CHUNK", senderId, fileId, chunkNo, 0, body);
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %s %d %s %d \r\n\r\n",
                this.protocolVersion,
                this.messageType,
                this.senderId,
                this.fileId,
                this.chunkNo).getBytes(StandardCharsets.UTF_8);

        // Create Message array
        byte[] message = new byte[header.length + this.body.length];

        // Copy Header and Body to Message array
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(this.body, 0, message, header.length, body.length);

        return message;
    }

    @Override
    public void submitTask(Peer peer) {
        ChunkTask task = new ChunkTask(peer, this);
        peer.submitRestoreThread(task);
    }
}
