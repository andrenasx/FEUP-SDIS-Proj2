package messages.chord;

import chord.ChordNodeReference;
import chord.Peer;
import tasks.chord.JoinTask;
import tasks.chord.LookupTask;

import java.nio.charset.StandardCharsets;

//JOIN <TARGET ID> <ADDRESS IP> <PORT>
public class LookupMessage extends ChordMessage{
    private final int targetId;

    public LookupMessage(ChordNodeReference node, int targetId) {
        super("JOIN", node);
        this.targetId = targetId;
    }

    @Override
    public byte[] encode() {
        return String.format("%s %d %s %d \r\n\r\n",
                this.messageType,
                this.targetId,
                this.getSocketAddress().getAddress().getHostAddress(),
                this.getPort()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return messageType + " " + this.targetId + " " + this.getSocketAddress().getAddress().getHostAddress() + " " + this.getPort();
    }

    @Override
    public void submitTask(Peer peer) {
        LookupTask task = new LookupTask(peer, this);
        peer.submitThread(task);
    }

    public int getTargetId(){
        return this.targetId;
    }
}
