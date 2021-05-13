package messages;

import peer.Peer;
import tasks.RemovedTask;

import java.nio.charset.StandardCharsets;

public class RemovedMessage extends Message {
    public RemovedMessage(String protocolVersion, int senderId, String fileId, int chunkNo) {
        super(protocolVersion, "REMOVED", senderId, fileId, chunkNo, 0, null);
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
        RemovedTask removedTask = new RemovedTask(peer, this);
        peer.submitControlThread(removedTask);
    }
}
