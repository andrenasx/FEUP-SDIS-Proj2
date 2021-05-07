package tasks;

import messages.ChunkMessage;
import messages.GetChunkMessage;
import peer.Peer;
import storage.Chunk;
import utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class GetchunkTask extends Task {
    public GetchunkTask(Peer peer, GetChunkMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
        // Get corresponding stored chunk
        String chunkId = this.message.getFileId() + "_" + this.message.getChunkNo();
        Chunk chunk = this.peer.getStorage().getStoredChunk(chunkId);

        // Abort if peer does not have chunk stored
        if (chunk == null || !chunk.isStoredLocally()) {
            return;
        }

        // Set sent flag to false, flag is set true when another peers sends this chunk
        chunk.setSent(false);

        this.peer.getScheduler().schedule(() -> sendChunk(chunk), Utils.getRandom(400), TimeUnit.MILLISECONDS);
    }

    private void sendChunk(Chunk chunk) {
        // Send this chunk if no other peer sent it before me
        if (!chunk.getSent()) {
            String chunkId = chunk.getUniqueId();
            try {
                // Get chunk body
                byte[] body = this.peer.getStorage().restoreChunkBody(chunkId);

                // If this peer and message are enhanced send message with ports for TCP connection
                if (this.peer.isEnhanced() && this.message.isEnhanced()) {
                    try {
                        // Create ServerSocket in a new port, 2sec to timeout and 64KBytes buffer
                        ServerSocket serverSocket = new ServerSocket(0);
                        serverSocket.setSoTimeout(2000);
                        serverSocket.setReceiveBufferSize(Utils.CHUNK_SIZE);

                        // Get connection ports and create byte array to send in CHUNK message
                        String connection = serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
                        byte[] content = connection.getBytes(StandardCharsets.UTF_8);

                        // Send CHUNK message with TCP ports
                        ChunkMessage chunkMessage = new ChunkMessage(this.peer.getProtocolVersion(), this.peer.getId(), this.message.getFileId(), this.message.getChunkNo(), content);
                        this.peer.sendRestoreMessage(chunkMessage);
                        System.out.println("[RESTORE-TCP] Sent CHUNK message for chunk :" + chunkId);

                        // Create socket and write chunk body though TCP
                        Socket socket = serverSocket.accept();
                        OutputStream out = socket.getOutputStream();
                        out.write(body);

                        // Close buffer and sockets after writing chunk body
                        out.close();
                        socket.close();
                        serverSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error in TCP socket");
                    }
                }
                // If default just send "default" CHUNK message
                else {
                    ChunkMessage chunkMessage = new ChunkMessage(this.peer.getProtocolVersion(), this.peer.getId(), this.message.getFileId(), this.message.getChunkNo(), body);
                    this.peer.sendRestoreMessage(chunkMessage);
                    System.out.println("[RESTORE] Sent CHUNK message for chunk :" + chunkId);
                }
            } catch (IOException e) {
                System.err.println("Unable to restore chunk, id:" + chunkId);
            }
        }
    }
}
