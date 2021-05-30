package tasks.chord;

import chord.ChordNode;
import messages.chord.ChordMessage;
import messages.chord.GuidMessage;
import messages.protocol.BackupMessage;
import messages.protocol.OkMessage;
import messages.protocol.ProtocolMessage;
import peer.Peer;
import tasks.Task;

import javax.net.ssl.SSLEngine;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class BackupTask extends Task {
    public BackupTask(ProtocolMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        System.out.println("BACKUP TASKLKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK");
        String fileId = ((BackupMessage) message).getFileId();
        long size = ((BackupMessage) message).getSize();
        String filename = ((BackupMessage) message).getFilename();
        int replicationDegree = ((BackupMessage) message).getReplicationDegree();

        // TODO check if peer has space or already has file, send ErrorMessage
        System.out.println("Testing capacity.. (to be implemented)");

        try {
            FileOutputStream outputStream = new FileOutputStream(((Peer)node).getPeerStorage().getStoragePath() + filename);
            FileChannel fileChannel = outputStream.getChannel();
            System.out.println("[BACKUP] Ready to receive file...");

            OkMessage okay = new OkMessage(node.getSelfReference());
            node.write(channel, engine, okay.encode());
            //System.out.println("Server sent: " + response);

            channel.configureBlocking(true);
            this.node.receiveFile(channel, engine, fileChannel, size);
            fileChannel.close();
            System.out.println("Received file!");
            System.out.println("Sending OK to client so they can close connection");

            node.write(channel, engine, okay.encode());
            //System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.out.println("[ERROR-BACKUP] Error receiving file");
            e.printStackTrace();
        }

    }
}
