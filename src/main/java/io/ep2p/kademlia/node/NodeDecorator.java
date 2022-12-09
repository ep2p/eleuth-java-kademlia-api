package io.ep2p.kademlia.node;

import io.ep2p.kademlia.connection.ConnectionInfo;

public class NodeDecorator<ID extends Number, C extends ConnectionInfo> implements Node<ID, C> {
    protected final Node<ID, C> node;

    public NodeDecorator(Node<ID, C> node) {
        this.node = node;
    }

    @Override
    public C getConnectionInfo() {
        return this.node.getConnectionInfo();
    }

    @Override
    public ID getId() {
        return this.node.getId();
    }

    @Override
    public String toString() {
        return "NodeDecorator{" +
                "node=" + node +
                '}';
    }
}
