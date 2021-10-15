package io.ep2p.kademlia.protocol.message;

import io.ep2p.kademlia.connection.ConnectionInfo;
import io.ep2p.kademlia.protocol.MessageType;

import java.io.Serializable;

public class ShutdownKademliaMessage<ID extends Number, C extends ConnectionInfo> extends KademliaMessage<ID, C, Serializable> {

    public ShutdownKademliaMessage() {
        super(MessageType.SHUTDOWN);
    }
}