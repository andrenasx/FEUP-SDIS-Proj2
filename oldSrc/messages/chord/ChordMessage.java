package messages.chord;

import java.net.InetSocketAddress;

public abstract class ChordMessage {
    protected String messageType;
    protected int senderId;
    protected InetSocketAddress address;

    ChordMessage(String messageType, int senderId, InetSocketAddress address) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.address = address;
    }

}
