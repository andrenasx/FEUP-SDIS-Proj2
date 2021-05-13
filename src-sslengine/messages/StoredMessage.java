package messages;

import peer.Peer;
import tasks.StoredTask;

import java.nio.charset.StandardCharsets;

public class StoredMessage extends Message {
    public StoredMessage(String protocolVersion, int senderId, String fileId, int chunkNo) {
        super(protocolVersion, "STORED", senderId, fileId, chunkNo, 0, null);
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
        StoredTask task = new StoredTask(peer, this);
        peer.submitControlThread(task);
    }
}
