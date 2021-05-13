package messages.chord;

import chord.ChordNodeReference;
import chord.Peer;
import tasks.chord.JoinTask;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

//JOIN <ID REQUESTED> <ADDRESS IP> <PORT>
public class JoinMessage extends ChordMessage{
    public JoinMessage(ChordNodeReference node) {
        super("JOIN", node);
    }

    @Override
    public byte[] encode() {
        return String.format("%s %d %s %d \r\n\r\n",
                this.messageType,
                this.getId(),
                this.getSocketAddress().getAddress().getHostAddress(),
                this.getPort()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return messageType + " " + this.getId() + " " + this.getSocketAddress().getAddress().getHostAddress() + " " + this.getPort();
    }

    @Override
    public void submitTask(Peer peer) {
        JoinTask task = new JoinTask(peer, this);
        peer.submitThread(task);
    }
}
