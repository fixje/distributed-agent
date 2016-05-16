package org.mfuchs.distagent

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.{CuratorFrameworkFactory, CuratorFramework}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.slf4j.{LoggerFactory, Logger}

/**
  * A simple, distributed and fault-tolerant agent
  *
  * @author Markus Fuchs (web-code@mfuchs.org)
  */
class Agent(zkServers: String) {
  val hostname = InetAddress.getLocalHost.getHostName
  val logger: Logger = LoggerFactory.getLogger(hostname)
  val zkAppPath = "/mf-dist-agent"

  val client: CuratorFramework = CuratorFrameworkFactory.newClient(zkServers, new ExponentialBackoffRetry(1000, 3))
  val serviceDetails: ServiceDetails = new ServiceDetails(client, zkAppPath, "AgentGossip", hostname)
  val leaderLatch: LeaderLatch = new LeaderLatch(client, zkAppPath, hostname)

  def start() {
    client.start()
    serviceDetails.start()

    serviceDetails.onServiceChange(() => {
      val otherInstances = serviceDetails.getOtherInstances
      if (otherInstances.nonEmpty) {
        logger.info("Cluster changed - Other Instances:")
        otherInstances.foreach(i => logger.info("\t* Address: %s, Id: %s".format(i.getAddress, i.getId)))
      } else {
        logger.info("I am alone :(")
      }
    })

    leaderLatch.start()

    logger.info("Agent started")
    var running = true
    while (running && !Thread.currentThread().isInterrupted) {
      if (leaderLatch.hasLeadership) {
        logger.info("I am the leader")
        Thread.sleep(5000)
      }
      try {
        leaderLatch.await(10, TimeUnit.SECONDS)
      } catch {
        case ie: InterruptedException => running = false
      }
    }
  }

  def stop() {
    logger.info("Shutting down...")
    serviceDetails.close()
    leaderLatch.close()
    client.close()
  }
}
