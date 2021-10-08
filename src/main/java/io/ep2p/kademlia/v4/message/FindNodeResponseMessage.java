package io.ep2p.kademlia.v4.message;

import io.ep2p.kademlia.model.FindNodeAnswer;
import io.ep2p.kademlia.v4.connection.ConnectionInfo;

public class FindNodeResponseMessage<ID extends Number, C extends ConnectionInfo> extends KademliaMessage<ID, C, FindNodeAnswer<ID, C>> {
    private final static String TYPE = "FIND_NODE_RESPONSE";

    public FindNodeResponseMessage() {
        super(TYPE);
    }
}
