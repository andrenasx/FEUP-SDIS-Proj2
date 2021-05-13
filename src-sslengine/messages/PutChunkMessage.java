package messages;

import peer.Peer;
import storage.Chunk;
import tasks.PutchunkTask;

import java.nio.charset.StandardCharsets;

public class PutChunkMessage extends Message {
    public PutChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNo, int replicationDeg, byte[] body) {
        super(protocolVersion, "PUTCHUNK", senderId, fileId, chunkNo, replicationDeg, body);
    }

    public PutChunkMessage(Peer peer, Chunk chunk) {
        super(peer.getProtocolVersion(), "PUTCHUNK", peer.getId(), chunk.getFileId(), chunk.getChunkNo(), chunk.getDesiredReplicationDegree(), chunk.getBody());
    }

    @Override
    public byte[] encode() {
        // Create Header in the specified format
        byte[] header = String.format("%s %s %d %s %d %d \r\n\r\n",
                this.protocolVersion,
                this.messageType,
                this.senderId,
                this.fileId,
                this.chunkNo,
                this.replicationDeg).getBytes(StandardCharsets.UTF_8);

        // Create Message array
        byte[] message = new byte[header.length + this.body.length];

        // Copy Header and Body to Message array
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(this.body, 0, message, header.length, body.length);

        return message;
    }

    @Override
    public void submitTask(Peer peer) {
        PutchunkTask task = new PutchunkTask(peer, this);
        peer.submitBackupThread(task);
    }
}
