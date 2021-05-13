package tasks;

import messages.StoredMessage;
import peer.Peer;
import storage.Chunk;

public class StoredTask extends Task {
    public StoredTask(Peer peer, StoredMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        // Add peer acknowledge to received STORED messages
        if (this.peer.getStorage().hasStoredChunk(this.message.getFileId(), this.message.getChunkNo())) {
            Chunk chunk = this.peer.getStorage().getStoredChunk(this.message.getFileId(), this.message.getChunkNo());
            chunk.addPeerStoring(this.message.getSenderId());
        }
        else if (this.peer.getStorage().hasSentChunk(this.message.getFileId(), this.message.getChunkNo())) {
            Chunk chunk = this.peer.getStorage().getSentChunk(this.message.getFileId(), this.message.getChunkNo());
            chunk.addPeerStoring(this.message.getSenderId());
        }
    }
}
