package tasks;

import messages.PutChunkMessage;
import messages.StoredMessage;
import peer.Peer;
import storage.Chunk;
import utils.Utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PutchunkTask extends Task {
    public PutchunkTask(Peer peer, PutChunkMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        // Abort if it was a chunk this peer backed up
        if (this.peer.getStorage().hasSentChunk(this.message.getFileId(), this.message.getChunkNo())) {
            return;
        }
        // Abort if peer does not have enough space to store chunk
        if (!this.peer.getStorage().hasEnoughSpace(this.message.getBody().length / 1000.0)) {
            System.err.println("[BACKUP] Not enough space to store chunk " + this.message.getFileId() + "_" + this.message.getChunkNo());
            return;
        }

        Chunk chunk;
        // If peer does not have received chunk add it to peer StoredChunk map
        if (!this.peer.getStorage().hasStoredChunk(this.message.getFileId(), this.message.getChunkNo())) {
            chunk = new Chunk(this.message);
            this.peer.getStorage().addStoredChunk(chunk.getUniqueId(), chunk);
        }
        else {
            chunk = this.peer.getStorage().getStoredChunk(this.message.getFileId(), this.message.getChunkNo());

            // If peer has current chunk stored (in map and acknowledged) send STORED message
            if (chunk.isStoredLocally()) {
                StoredMessage message = new StoredMessage(this.peer.getProtocolVersion(), this.peer.getId(), chunk.getFileId(), chunk.getChunkNo());
                this.peer.sendControlMessage(message);
                return;
            }
        }

        // After schedule verify if chunk needs replication in case peer is enhanced
        if (this.peer.isEnhanced()) {
            this.peer.getScheduler().schedule(() -> this.storeChunkEn(chunk), Utils.getRandom(400), TimeUnit.MILLISECONDS);
        }
        // Just schedule randomly between 0-400ms if default
        else {
            this.peer.getScheduler().schedule(() -> this.storeChunk(chunk), Utils.getRandom(400), TimeUnit.MILLISECONDS);
        }
    }

    private void storeChunkEn(Chunk chunk) {
        // Store chunk only if it still needs replication
        if (chunk.needsReplication()) {
            this.storeChunk(chunk);
        }
        // Else if already replicated remove from peer stored chunks map
        else {
            this.peer.getStorage().removeStoredChunk(chunk.getUniqueId());
        }
    }

    private void storeChunk(Chunk chunk) {
        // Check if peer has enough space to store chunk
        if (!this.peer.getStorage().hasEnoughSpace(chunk.getSize())) {
            System.err.println("[BACKUP] Not enough space to store chunk " + chunk.getUniqueId());
            this.peer.getStorage().removeStoredChunk(chunk.getUniqueId());
            return;
        }

        try {
            // Store chunk in file
            this.peer.getStorage().storeChunk(chunk, this.message.getBody());

            // Acknowledge that chunk is stored and add it to peer ack Set
            chunk.setStoredLocally(true);
            chunk.addPeerStoring(this.peer.getId());

            // Send stored message
            StoredMessage message = new StoredMessage(this.peer.getProtocolVersion(), this.peer.getId(), chunk.getFileId(), chunk.getChunkNo());
            this.peer.sendControlMessage(message);
        } catch (IOException e) {
            System.err.printf("Failed to store chunk %s\n", chunk.getUniqueId());
        }
    }
}
