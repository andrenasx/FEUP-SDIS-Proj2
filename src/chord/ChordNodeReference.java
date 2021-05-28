package chord;

import java.net.InetSocketAddress;

public class ChordNodeReference {
    private int guid;
    private InetSocketAddress address;

    public ChordNodeReference(byte[] reference) {
        String chordReference = new String(reference);

        this.guid = Integer.parseInt(chordReference.substring(chordReference.indexOf('=') + 1, chordReference.indexOf(',')));
        this.address = new InetSocketAddress(
                chordReference.substring(chordReference.indexOf('/') + 1, chordReference.indexOf(':')),
                Integer.parseInt(chordReference.substring(chordReference.indexOf(':') + 1, chordReference.indexOf('}'))));
    }

    public ChordNodeReference(InetSocketAddress address, int guid) {
        this.guid = guid;
        this.address = address;
    }

    public InetSocketAddress getSocketAddress() {
        return address;
    }

    public int getGuid() {
        return guid;
    }

    public void setGuid(int guid) {
        this.guid = guid;
    }

    @Override
    public String toString() {
        return "ChordNodeReference{" +
                "guid=" + guid +
                ", address=" + address +
                '}';
    }
}
