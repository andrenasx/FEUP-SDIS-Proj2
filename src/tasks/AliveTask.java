package tasks;

import chord.ChordNode;
import messages.AliveMessage;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class AliveTask extends ChordTask {
    public AliveTask(AliveMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, null, null);
    }

    @Override
    public void run() {

        AliveMessage response = new AliveMessage(this.node.getSelfReference());

        try {
            node.write(channel, engine, response.encode());
            System.out.println("Server sent: " + response);
        } catch (IOException e) {
            System.err.println("Couldn't send GUID");
            e.printStackTrace();
        }
    }
}
