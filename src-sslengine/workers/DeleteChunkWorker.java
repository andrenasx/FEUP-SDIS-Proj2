package workers;

import messages.RemovedMessage;
import peer.Peer;
import storage.Chunk;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class DeleteChunkWorker implements Runnable {
    private final Peer peer;
    private final Chunk chunk;

    public DeleteChunkWorker(Peer peer, Chunk chunk) {
        this.peer = peer;
        this.chunk = chunk;
    }

    @Override
    public void run() {
        this.peer.getStorage().deleteStoredChunk(this.chunk, "RECLAIMING");

        RemovedMessage removedMessage = new RemovedMessage(this.peer.getProtocolVersion(), this.peer.getId(), this.chunk.getFileId(), this.chunk.getChunkNo());
        // Try to send REMOVED message max 3 times
        this.peer.sendControlMessage(removedMessage);
        this.peer.getScheduler().schedule(() -> this.sendRemovedMessage(removedMessage, 1), 1000, TimeUnit.MILLISECONDS);
    }

    private void sendRemovedMessage(RemovedMessage removedMessage, int attempt) {
        if (attempt < Utils.MAX_3_ATTEMPTS) {
            this.peer.sendControlMessage(removedMessage);

            int currentAttempt = attempt + 1;
            this.peer.getScheduler().schedule(() -> this.sendRemovedMessage(removedMessage, currentAttempt), (long) (Math.pow(2, attempt) * 1000), TimeUnit.MILLISECONDS);
        }
    }
}
