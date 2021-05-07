package tasks;

import messages.Message;
import messages.StoredMessage;
import peer.Peer;
import storage.Chunk;
import utils.Utils;
import workers.BackupChunkWorker;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RemovedTask extends Task {
    public RemovedTask(Peer peer, Message message) {
        super(peer, message);
    }

    @Override
    public void run() {
        // Remove peer acknowledge to received chunk
        if (this.peer.getStorage().hasSentChunk(this.message.getFileId(), this.message.getChunkNo())) {
            Chunk chunk = this.peer.getStorage().getSentChunk(this.message.getFileId(), this.message.getChunkNo());
            chunk.removePeerStoring(this.message.getSenderId());
        }
        else if (this.peer.getStorage().hasStoredChunk(this.message.getFileId(), this.message.getChunkNo())) {
            Chunk chunk = this.peer.getStorage().getStoredChunk(this.message.getFileId(), this.message.getChunkNo());
            chunk.removePeerStoring(this.message.getSenderId());

            // Check if this peer has this chunk and it needs replication
            if (chunk.needsReplication() && chunk.isStoredLocally()) {
                // Schedule to avoid collision in case another peer already replicated it
                this.peer.getScheduler().schedule(() -> this.startBackup(chunk), Utils.getRandom(400), TimeUnit.MILLISECONDS);
            }
        }
    }

    private void startBackup(Chunk chunk) {
        // If chunk still needs replication restore chunk body and start backup subprotocol
        if (chunk.needsReplication()) {
            try {
                chunk.setBody(this.peer.getStorage().restoreChunkBody(chunk.getUniqueId()));
            } catch (IOException e) {
                System.err.println("Couldn't restore chunk body");
                return;
            }

            System.out.printf("[RECLAIMING] Chunk %s needs replication\n", chunk.getUniqueId());

            BackupChunkWorker worker = new BackupChunkWorker(this.peer, chunk);
            this.peer.submitBackupThread(worker);
            System.out.printf("[BACKUP] Submitted backup for chunk: %s\n", chunk.getUniqueId());


            StoredMessage message = new StoredMessage(this.peer.getProtocolVersion(), this.peer.getId(), chunk.getFileId(), chunk.getChunkNo());
            // Sleep to make sure peer stores chunk before receiving this peer STORED message
            this.peer.getScheduler().schedule(() -> this.peer.sendControlMessage(message), 50, TimeUnit.MILLISECONDS);
        }
    }
}
