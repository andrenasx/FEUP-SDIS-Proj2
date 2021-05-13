package messages;

import peer.Peer;
import tasks.DeleteTask;

import java.nio.charset.StandardCharsets;

public class DeleteMessage extends Message {
    public DeleteMessage(String protocolVersion, int senderId, String fileId) {
        super(protocolVersion, "DELETE", senderId, fileId, -1, 0, null);
    }

    public static DeleteMessage create(byte[] data) {
        String message = new String(data);
        String[] parts = message.split(" ");

        return new DeleteMessage(parts[0], Integer.parseInt(parts[2]), parts[3]);
    }

    @Override
    public byte[] encode() {
        return String.format("%s %s %d %s \r\n\r\n",
                this.protocolVersion,
                this.messageType,
                this.senderId,
                this.fileId).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void submitTask(Peer peer) {
        DeleteTask task = new DeleteTask(peer, this);
        peer.submitControlThread(task);
    }
}
