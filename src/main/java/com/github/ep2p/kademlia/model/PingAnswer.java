package com.github.ep2p.kademlia.model;

public class PingAnswer extends Answer {
    public PingAnswer(int nodeId) {
        setNodeId(nodeId);
        setAlive(true);
    }

    public PingAnswer(int nodeId, boolean alive) {
        setNodeId(nodeId);
        setAlive(false);
    }

    public PingAnswer() {
    }
}
