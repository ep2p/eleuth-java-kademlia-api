[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.ep2p/kademlia-api/badge.png?gav=true)](https://maven-badges.herokuapp.com/maven-central/io.ep2p/kademlia-api)


# Kademlia Api
Abstract Java Kademlia API (Originally written to be used in Eleuth network).

This API mainly focuses on routing table and peer choosing logic, and moves data persistence & networking to an abstraction layer.
Therefore, its your responsibility to choose how you want your nodes contact each other. There is enough comment on each method on KademliaNode implementations in repository to help you override them to satisfy your needs of p2p network, since this abstract api can be used in many sort of projects.

## Abstraction Layer

### ConnectionInfo
Create your implementation of `com.github.ep2p.kademlia.connection.ConnectionInfo` which can represent connection information for each node to contact.
This can simply be `ip` and `port` for a TCP/UDP connection. Keep your implementation simple and Serializable.


```
.=====================================================.             .=====================================================.
|                                    .----------------|             |                                                     |
|                                    | Connection API |----         |-----------------------------------------------------|
|                                    '----------------|    \        |----------------.                   |                |
|-----------------------------------------------------|     \------>|    Node API    |   KADEMLIA NODE   |  Routing Table |
|                |                   .----------------|             |----------------'                   |                |
| Routing Table  |   KADEMLIA NODE   |    Node API    |<---         |-----------------------------------------------------|
|                |                   '----------------|    \        |----------------.                                    |
|-----------------------------------------------------|     \-------| Connection API |                                    |
|                                                     |             |----------------'                                    |
'====================================================='             '====================================================='

                                                         Figure 1
```


### NodeConnectionApi
This API is called when a node wants to send requests/data to other nodes. Create your implementation of `com.github.ep2p.kademlia.connection.NodeConnectionApi` based on earlier implemented ConnectionInfo.

Example:

Lets assume that `ConnectionInfo` is a string for HTTP address to send requests to: `http://localhost/8080/`. Your `NodeConnectionApi` implementation might want to use an HTTP Client to send different requests (`Ping`, `Bootstrap`, `Find`, ...) to this address by serializing the request into JSON format. The point is that you can choose HTTP, RPC, TCP Socket or even UDP packets to send such requests. You have a wide range of choices on how to implemenet the protocl of which your nodes use to contact eachother. 


Note that there are 4 methods which their implementation is **not mandatory** unless you want to also store data on your nodes (**DHT**):
```java
default <K, V> void storeAsync(Node<C> caller, Node<C> requester,  Node<C> node, K key, V value){} //Make sure its async
default <K> void getRequest(Node<C> caller, Node<C> requester, Node<C> node, K key){}
default <K, V> void sendGetResults(Node<C> caller, Node<C> requester, K key, V value){}
default <K> void sendStoreResults(Node<C> caller, Node<C> requester, K key, boolean success){}
```

### NodeApi
If you call any Node Api methods on one end through `NodeConnectionApi`. on the other end you need to receive them and call appropriate method on you KademliaNode.
For example, if you used an HTTP Client to send requests, you would need an HTTP Server on the other side to retrieve the requests, parse them and pass them to KademliaNode.

Make sure your protocol supports requests for methods available in `com.github.ep2p.kademlia.connection.NodeApi` and also `com.github.ep2p.kademlia.connection.StorageNodeApi` if you are storing data as well.

To make it more clear, you implement **NodeConnectionApi** for requests that are leaving the node, and **NodeApi** (or **StorageNodeApi**) for incoming requests to your node. A big advantage is that you can control the incoming requests, sign them, validate them, or anything you want to do with them before you pass them to Kademlia node.

### RoutingTable
Notice that `RoutingTable` and its buckets are Serializable. So you will easily be able to write it to a file. When you are creating an instance of your node, you can pass a new routing table or use one from disk.
At this stage, there is no helper class for writing routing table on disk.

## Usage of KademliaNode class

Currently there are 3 classes that can be used to create a kademlia node.

To create a KademliaNode **without capability of storing data**, try:

```java
KademliaNode<?, ?> node = new KademliaNode<>(yourNodeId, nodeConnectionApi, connectionInfo);
```

And to create a KademliaNode **capable of storing data**, try:

```java
KademliaRepositoryNode<?, ?, Integer, String> aNode = new KademliaRepositoryNode<>(yourNodeId, nodeConnectionApi, connectionInfo, repository);
```
or
```java
KademliaSyncRepositoryNode<?, ?, Integer, String> aNode = new KademliaSyncRepositoryNode<>(yourNodeId, routingTable, nodeConnectionApi, connectionInfo, repository);
```
Where `Integer` is data storage key type, `String` is data storage value type, and `repository` is data storage implementation. It's a good practice to keep storage outside ram and avoid data loss on node shutdowns, but the choice is yours.

Difference between `KademliaRepositoryNode` and `KademliaSyncRepositoryNode` is that second one waits for `store()` and `get()` to reply, while first one returns an answer even if store or get requests are forwarded to other nodes.

After creating a node instance for first time, you need to bootstrap it using another node you know in network:
```java
node.bootstrap(bootstrapNode);
```

If your node is already bootstrapped, just start it:
```java
node.start();
``` 

Also you can listen to some events that happen in node, such as start, shutdown, data store and lookup results (specially if u are using `KademliaRepositoryNode`) by setting your listener on the node.
```java
node.setKademliaNodeListener(...)
```
Once you get used to API, this listener can be a big help in making some changes to your nodes behaviour. See next section, Re-Distribution.

## Generics
Note that KademliaNode and all its subclasses are Generic. 

The first generic type is for ID of the node, and you can choose between `Integer`, `Long`, `BigInteger` as these are the only ones supported and you should decide on which on to use based on your GUID space size. For example on **Eleuth Node System** Biginteger is being used since node IDs are `SHA1` of a public keys. You can [choose the appropriate `RoutingTable`](https://github.com/ep2p/kademlia-api/tree/main/src/main/java/com/github/ep2p/kademlia/table) implementation based on this key size. There is an implementation available for all of the three supported ID types.

The second generic type is type of your class that implements `ConnectionInfo`.

## Configuration

You can configure your node by passing an instance of `NodeSettings` to `KademliaNode` or it's subclasses. By default they use the configuration from `NodeSettings.Default` which has static fields. You can edit `NodeSettings.Default` if all of the nodes that you want to have in a single application are having the same configuration.

## Republishing Keys

To make sure keys are distributed in valid nodes we can define a strategy which will publish keys every `N` `TimeUnit` (for example, every 15 minutes).
To enable the publishing strategy set `enabledRepublishing` to `True` in `NodeSettings`. The default strategy is set in `RepublishStrategyFactory` and is configurable through `NodeSettings.RepublishSettings`. Note that republish strategy needs to know when each `<K, V>` has entered your repository implementation. So your kademlia repository also has to impelement `TimestampAwareKademliaRepository<K,V>`.

## Re-Distribution

There are situations where best node to hold a data is not available on network, So closest node will be chosen to keep the data. After certain time, a node with closer id to `key hash` might appear and it would be good practice to pass the data to the new node since it has closer id.

Or there are situations where a node is shutting down and it's good practice for this node to pass all its data to other nodes.

Of course one important parameter here is size of values as it will take much longer to pass large values to other nodes and you must prevent application shutdown till this re-distribution ends.

For these cases, you can use or extend `com.github.ep2p.kademlia.node.RedistributionKademliaNodeListener` as your listener. It will handle data redistribution when new nodes appear in the routing table. Also by passing `ShutdownDistributionListener` it handles data redistribution when node is shutting down.

## Key Hashing

One of the most important aspects of a DHT (Distributed hash table) is how we use the hash for keys to make them unique or identifiable. You can pass an implementation of [KeyHashGenerator](https://github.com/ep2p/kademlia-api/blob/main/src/main/java/com/github/ep2p/kademlia/util/KeyHashGenerator.java). The Default one will use `key.hashCode()` and changes it's size to ID size of your node.

---

## Sample
A sample DHT using this repository and spring boot is made to help you understand the concepts.
Check [sample dht](https://github.com/ep2p/dht-sample) repository.

---

## Installation

Using **maven central**

```xml
<dependency>
    <groupId>io.ep2p</groupId>
    <artifactId>kademlia-api</artifactId>
    <version>3.0.0-RELEASE</version>
</dependency>
```

Github releases page is not uptodate anymore. See [all maven releases](https://search.maven.org/artifact/io.ep2p/kademlia-api).

## Acknowledgments

Since this project is completely experimental (at least for now), I'd like to thank to some people around the internet that helped me gain some knowledge about Kademlia and its implementations beyond the paper.
I know my implementation as a sample and abstract kademlia api, not most complete.

- A huge thank to  **Jakob Jenkov** for his great [tutorial](http://tutorials.jenkov.com/p2p/index.html) on p2p network and kademlia.
- Stackoverflow Community
    - **Joshua Kissoon** for his great explanation on [Adding new nodes to Kademlia, building Kademlia routing tables](https://stackoverflow.com/a/22740578/5197662)
    - **Nawras** for asking [What exactly K-Bucket means in Kademlia DHT?](https://stackoverflow.com/q/54341261/5197662)
- **Kasra Faghihi** ([@offbynull](https://github.com/offbynull)) for his very mathematical implementation of Kademlia.
- And a great shout out to **SimGrid** Team, for keeping history of [their work on Kademlia](https://gitlab.inria.fr/simgrid/simgrid/-/tree/ce2e676ad127f782b9c959499ab1c042195e411a/examples/java/kademlia) which this repository is inspired by it to implement RoutingTables, Buckets and some more stuff. 

## Other implementations

[@temissimon01](https://github.com/temissimon01) has implemented a kademlia api similar to this one using C++ and GRPC [here](https://github.com/temissimon01/Kademlia-cpp-grpc).

## Videos

[![](https://img.shields.io/badge/youtube-Kademlia%20Overview%201-FFF?style=for-the-badge&logo=youtube&logoColor=red)](https://youtu.be/7o0pfKDq9KE)

In first video of EP2P lecture about Kademlia Algorithm, we are going to gain brief understanding about elements of Kademlia nodes, network structure and Distributed Hash Tables (DHT) in theory.


[![](https://img.shields.io/badge/youtube-Kademlia%20Overview%202-FFF?style=for-the-badge&logo=youtube&logoColor=red)](https://www.youtube.com/watch?v=86tsT0g43iQ)

In this video we see some code example in Java for Kademlia API using an abstract library written by EP2P.
