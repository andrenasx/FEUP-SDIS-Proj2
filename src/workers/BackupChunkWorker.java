package workers;

import messages.PutChunkMessage;
import peer.Peer;
import storage.Chunk;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class BackupChunkWorker implements Runnable {
    private final Peer peer;
    private final Chunk chunk;

    public BackupChunkWorker(Peer peer, Chunk chunk) {
        this.peer = peer;
        this.chunk = chunk;
    }

    @Override
    public void run() {
        // Add chunk to sent chunk map
        if (!this.peer.getStorage().hasSentChunk(this.chunk.getUniqueId())) {
            this.peer.getStorage().addSentChunk(this.chunk);
        }

        PutChunkMessage putChunkMessage = new PutChunkMessage(this.peer, this.chunk);
        // Try to send PUTCHUNK message max 5 times or until Replication degree is met
        this.peer.sendBackupMessage(putChunkMessage);
        this.peer.getScheduler().schedule(() -> this.sendPutchunkMessage(putChunkMessage, 1), 1000, TimeUnit.MILLISECONDS);
    }

    private void sendPutchunkMessage(PutChunkMessage putChunkMessage, int attempt) {
        if (attempt < Utils.MAX_5_ATTEMPTS && this.chunk.needsReplication()) {
            this.peer.sendBackupMessage(putChunkMessage);

            int finalAttempt = attempt+1;
            this.peer.getScheduler().schedule(() -> this.sendPutchunkMessage(putChunkMessage, finalAttempt), (long) Math.pow(2, attempt) * 1000, TimeUnit.MILLISECONDS);
        } else {
            this.chunk.clearBody();
        }
    }
}
