package workers;

import messages.DeleteMessage;
import peer.Peer;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class DeleteFileWorker implements Runnable {
    private final Peer peer;
    private final String fileId;

    public DeleteFileWorker(Peer peer, String fileId) {
        this.peer = peer;
        this.fileId = fileId;
    }

    @Override
    public void run() {
        DeleteMessage deleteMessage = new DeleteMessage(this.peer.getProtocolVersion(), this.peer.getId(), this.fileId);

        // Try to send DELETE message max 3 times
        this.peer.sendControlMessage(deleteMessage);
        this.peer.getScheduler().schedule(() -> this.sendDeleteMessage(deleteMessage, 1), 1000, TimeUnit.MILLISECONDS);
        this.peer.getStorage().deleteSentChunks(this.fileId);
    }

    private void sendDeleteMessage(DeleteMessage deleteMessage, int attempt) {
        if (attempt < Utils.MAX_3_ATTEMPTS) {
            this.peer.sendControlMessage(deleteMessage);

            int currentAttempt = attempt + 1;
            this.peer.getScheduler().schedule(() -> this.sendDeleteMessage(deleteMessage, currentAttempt), (long) Math.pow(2, attempt) * 1000, TimeUnit.MILLISECONDS);
        }
    }
}
