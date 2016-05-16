# A Distributed Agent

This small example shows how to implement a simple distributed, fault-tolerant application using the Apache Curator
framework.

The application is an agent which, if started on different machines, performs leader election. All the instances are 
aware of their co-workers and could take over the leader's business in case of a failure.

## Demo Setup

For demonstration purposes we can start several instances of the agent in Docker containers. However, you need a running
instance of ZooKeeper, which we will also run in a container:

```
docker run --name zookeeper-1 digitalwonderland/zookeeper
```

Now, compile the package and create the Docker image:

```
mvn clean package docker:build
```

Start as many instances of the agent as you like:

```
docker run -it --link zookeeper-1:zookeeper --rm distributed-agent
```