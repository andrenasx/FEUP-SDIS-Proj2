package workers;

import messages.GetChunkMessage;
import peer.Peer;
import storage.Chunk;
import utils.Utils;

import java.util.concurrent.Callable;

public class RestoreChunkWorker implements Callable<Chunk> {
    private final Peer peer;
    private final Chunk chunk;

    public RestoreChunkWorker(Peer peer, Chunk chunk) {
        this.peer = peer;
        this.chunk = chunk;
    }

    @Override
    public Chunk call() {
        GetChunkMessage getChunkMessage = new GetChunkMessage(this.peer.getProtocolVersion(), this.peer.getId(), this.chunk.getFileId(), this.chunk.getChunkNo());

        // Try to send GETCHUNK message max 3 times or until current chunk body is set
        int attempt = 0;
        do {
            this.peer.sendControlMessage(getChunkMessage);

            int wait = (int) Math.pow(2, attempt) * 1000;
            Utils.sleep(wait);
        } while (++attempt < Utils.MAX_3_ATTEMPTS && this.chunk.getBody() == null);

        return this.chunk;
    }
}
