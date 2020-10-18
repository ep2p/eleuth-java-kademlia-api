/* Copyright (c) 2012-2014, 2016. The SimGrid Team.
 * All rights reserved.                                                     */

/* This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. */

package com.github.ep2p.kademlia.table;
import com.github.ep2p.kademlia.connection.ConnectionInfo;
import com.github.ep2p.kademlia.node.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Bucket<C extends ConnectionInfo> implements Serializable {
  private static final long serialVersionUID = 3300560211925346757L;
  private int id;
  private ArrayList<Integer> nodeIds;
  private Map<Integer, Node<C>> nodeMap = new HashMap<>();

  /* Create a bucket for prefix `id` */
  public Bucket(int id) {
    this.nodeIds = new ArrayList<Integer>();
    this.id = id;
  }

  public int getId() {
    return this.id;
  }

  public int size() {
    return nodeIds.size();
  }

  public boolean contains(int id) {
    return nodeIds.contains(id);
  }

  public boolean contains(Node<C> node){
    return nodeIds.contains(node.getId());
  }

  /* Add a node to the front of the bucket */
  public void add(Node<C> node) {
    nodeIds.add(0,node.getId());
    nodeMap.put(node.getId(), node);
  }

  /* Push a node to the front of a bucket */
  /* Called when a node is already in bucket and brings them to front of the bucket as they are a living node */
  public void pushToFront(int id) {
    int i = nodeIds.indexOf(id);
    nodeIds.remove(i);
    nodeIds.add(0, id);
  }

  public Node<C> getNode(int id) {
    Integer nodeId = nodeIds.get(id);
    return nodeMap.get(nodeId);
  }

  public ArrayList<Integer> getNodeIds() {
    return nodeIds;
  }

  @Override
  public String toString() {
    return "Bucket [id= " + id + " nodeIds=" + nodeIds + "]";
  }
}
