package org.mfuchs.distagent

import com.typesafe.config.ConfigFactory

object Main extends App{
  val config = ConfigFactory.load()
  val agent = new Agent(config.getString("agent.zookeeper.servers"))

  // gracefully handle Ctrl-c
  sys.addShutdownHook({
    agent.stop()
  })

  agent.start()
}
