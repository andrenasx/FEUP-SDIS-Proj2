package tasks;

import chord.ChordNode;
import messages.GuidMessage;
import messages.JoinMessage;
import utils.Utils;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class JoinTask extends ChordTask {
    public JoinTask(JoinMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        InetSocketAddress socketAddress = message.getSenderSocketAddress();

        int guidToSend = Utils.generateId(socketAddress);

        GuidMessage response = new GuidMessage(node.getSelfReference(), String.valueOf(guidToSend).getBytes(StandardCharsets.UTF_8));

        try {
            node.write(channel, engine, response.encode());
            System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("Couldn't send GUID");
            e.printStackTrace();
        }
    }
}
