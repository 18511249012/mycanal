package com.hzcard.syndata

import akka.actor._


object SpringExtension {
  /**
   * The identifier used to access the SpringExtension.
   */
  def apply() : SpringExtension= new SpringExtension
}

class SpringExtension extends AbstractExtensionId[SpringExtentionImpl] {
  /**
   * Is used by Akka to instantiate the Extension identified by this
   * ExtensionId, internal use only.
   */
  override def createExtension(system: ExtendedActorSystem) = new SpringExtentionImpl

  /**
   * Java API: retrieve the SpringExt extension for the given system.
   */
  override def get(system: ActorSystem): SpringExtentionImpl = super.get(system)

}

