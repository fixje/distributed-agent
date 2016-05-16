package org.mfuchs.distagent

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.ConnectionState
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery._
import org.apache.curator.x.discovery.details.{JsonInstanceSerializer, ServiceCacheListener}

import scala.collection.JavaConverters._

/**
  * Encapsulates Service Discovery to make us aware of other active instances
  *
  * @author Markus Fuchs (web-code@mfuchs.org)
  */
class ServiceDetails(client: CuratorFramework, path: String, serviceName: String, descr: String) extends java.io.Closeable {
  private val serializer: JsonInstanceSerializer[ServiceInstanceDetails] = new JsonInstanceSerializer[ServiceInstanceDetails](classOf[ServiceInstanceDetails])

  val thisInstance = ServiceInstance.builder[ServiceInstanceDetails]().name(serviceName)
    .payload(new ServiceInstanceDetails(descr)).build()

  /**
    * Register our instance with the service discovery
    */
  private val serviceDiscovery = ServiceDiscoveryBuilder.builder[ServiceInstanceDetails](classOf[ServiceInstanceDetails])
    .client(client).basePath(path).thisInstance(thisInstance).serializer(serializer).build()

  /**
    * Keeps track of of other instances with the same serviceName; refreshes in a separate Thread
    */
  val serviceCache: ServiceCache[ServiceInstanceDetails] = serviceDiscovery.serviceCacheBuilder()
    .name(serviceName).build()

  /**
    * @return all active instances fond in the cache
    */
  def getInstances = serviceCache.getInstances.asScala

  /**
    * @return all other active instances fond in the cache excluding this instance
    */
  def getOtherInstances = serviceCache.getInstances.asScala.filter(_.getId != thisInstance.getId)

  /**
    * Register a listener for the event that services have changed (added/deleted), i.e., hosts joined or went down
    * @param f a function that is called when services have changed in the cache
    */
  def onServiceChange(f: () => Unit) =
    serviceCache.addListener(new ServiceCacheListener {
      override def cacheChanged(): Unit = f()
      override def stateChanged(client: CuratorFramework, state: ConnectionState): Unit = Unit
    })

  /**
    * Start service discovery - you need to do that!
    */
  def start() = {
    serviceDiscovery.start()
    serviceCache.start()
  }

  /**
    * gracefully shutdown service discovery - you need to do that!
    */
  override def close(): Unit = {
    CloseableUtils.closeQuietly(serviceCache)
    CloseableUtils.closeQuietly(serviceDiscovery)
  }
}
