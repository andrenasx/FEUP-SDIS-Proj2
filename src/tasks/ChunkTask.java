package tasks;

import messages.ChunkMessage;
import peer.Peer;
import storage.Chunk;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ChunkTask extends Task {
    public ChunkTask(Peer peer, ChunkMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        // If it is a stored chunk and CHUNK message was sent by another peer, acknowledge it (set sent to true) so we don't send more CHUNK messages for that chunk
        if (this.peer.getStorage().hasStoredChunk(this.message.getFileId(), this.message.getChunkNo())) {
            Chunk chunk = this.peer.getStorage().getStoredChunk(this.message.getFileId(), this.message.getChunkNo());
            chunk.setSent(true);
        }

        // If it is a sent chunk, add body to the chunk so we can restore information
        else if (this.peer.getStorage().hasSentChunk(this.message.getFileId(), this.message.getChunkNo())) {
            Chunk chunk = this.peer.getStorage().getSentChunk(this.message.getFileId(), this.message.getChunkNo());

            // If this peer and message are enhanced read body from TCP connection and set it
            if (this.peer.isEnhanced() && this.message.isEnhanced()) {
                // Get TCP ports
                String connection = new String(this.message.getBody());
                String[] parts = connection.split(":");

                try {
                    // Create socket
                    Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));

                    // Read chunk body
                    InputStream in = socket.getInputStream();
                    byte[] body = in.readAllBytes();

                    // Close buffer and socket after reading chunk body
                    in.close();
                    socket.close();

                    chunk.setBody(body);
                } catch (IOException e) {
                    System.err.println("Error in TCP socket");
                }
            }
            // If default just set the body from the CHUNK message
            else {
                chunk.setBody(message.getBody());
            }
        }
    }
}
