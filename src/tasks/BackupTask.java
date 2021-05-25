package tasks;

import chord.ChordNode;
import messages.ChordMessage;

import javax.net.ssl.SSLEngine;
import java.io.FileOutputStream;
import java.nio.channels.SocketChannel;

public class BackupTask extends ChordTask {
    public BackupTask(ChordMessage message, ChordNode node, SocketChannel channel, SSLEngine engine) {
        super(message, node, channel, engine);
    }

    @Override
    public void run() {
        try {
            FileOutputStream out = new FileOutputStream("../files/restored");
            out.write(this.message.getBody());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldnt restore file");
        }
    }
}
