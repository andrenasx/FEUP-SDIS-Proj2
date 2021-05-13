package messages;

import peer.Peer;
import tasks.GetchunkTask;

import java.nio.charset.StandardCharsets;

public class GetChunkMessage extends Message {
    public GetChunkMessage(String protocolVersion, int senderId, String fileId, int chunkNo) {
        super(protocolVersion, "GETCHUNK", senderId, fileId, chunkNo, 0, null);
    }

    @Override
    public byte[] encode() {
        return String.format("%s %s %d %s %d \r\n\r\n",
                this.protocolVersion,
                this.messageType,
                this.senderId,
                this.fileId,
                this.chunkNo).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void submitTask(Peer peer) {
        GetchunkTask task = new GetchunkTask(peer, this);
        peer.submitControlThread(task);
    }
}
