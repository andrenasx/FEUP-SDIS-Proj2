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
        //int replicationDegree = ((BackupMessage) message).getReplicationDegree();

        //test capacity
        System.out.println("Testing capacity.. (to be implemented)");

        try {
            FileOutputStream outputStream = new FileOutputStream(((Peer)node).getPeerStorage().getStoragePath() + filename);
            FileChannel fileChannel = outputStream.getChannel();
            System.out.println("Ready to receive file...");

            OkMessage response = new OkMessage(node.getSelfReference());

            try {
                node.write(channel, engine, response.encode());
                System.out.println("Server sent: " + response);
            } catch (IOException e) {
                System.err.println("Couldn't send GUID");
                e.printStackTrace();
            }

            channel.configureBlocking(true);
            this.node.receiveFile(channel, engine, fileChannel, size);
            fileChannel.close();
            System.out.println("Received file!");
            System.out.println("Sending OK to client so they can close connection");

            OkMessage confirm = new OkMessage(node.getSelfReference());

            try {
                node.write(channel, engine, confirm.encode());
                //System.out.println("Server sent: " + response);
            } catch (IOException e) {
                System.err.println("Couldn't send GUID");
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("Error receiving file");
            e.printStackTrace();
        }

    }
}
