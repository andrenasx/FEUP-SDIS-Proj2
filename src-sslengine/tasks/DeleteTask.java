package tasks;

import messages.DeleteMessage;
import peer.Peer;
import storage.Chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeleteTask extends Task {
    public DeleteTask(Peer peer, DeleteMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        // Delete all corresponding fileId chunks after receiving DELETE message
        ConcurrentHashMap<String, Chunk> storedChunks = this.peer.getStorage().getStoredChunks();
        for (Map.Entry<String, Chunk> entry : storedChunks.entrySet()) {
            Chunk chunk = entry.getValue();
            if (chunk.getFileId().equals(this.message.getFileId())) {
                this.peer.getStorage().deleteStoredChunk(chunk, "DELETION");
            }
        }
    }
}
