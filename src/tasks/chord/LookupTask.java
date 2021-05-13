package tasks.chord;

import chord.ChordNodeReference;
import chord.Peer;
import messages.chord.ChordMessage;
import messages.chord.JoinMessage;
import messages.chord.LookupMessage;
import utils.Utils;

import java.net.InetSocketAddress;

public class LookupTask extends Task{
    public LookupTask(Peer peer, LookupMessage message) {
        super(peer, message);
    }

    @Override
    public void run() {
       //lookup algorithm
    }
}
