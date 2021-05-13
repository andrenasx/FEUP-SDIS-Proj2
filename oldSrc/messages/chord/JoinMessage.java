package messages.chord;

import java.net.InetSocketAddress;

public class JoinMessage extends ChordMessage{
    public JoinMessage(int senderId, InetSocketAddress address) {
        super("JOIN", senderId, address);
    }
}
