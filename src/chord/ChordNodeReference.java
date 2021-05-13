package chord;

import java.net.InetSocketAddress;

public class ChordNodeReference {
    private int id;
    private InetSocketAddress address;

    public ChordNodeReference(InetSocketAddress address, int id) {
        this.id = id;
        this.address = address;
    }

    public InetSocketAddress getSocketAddress() {
        return address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
