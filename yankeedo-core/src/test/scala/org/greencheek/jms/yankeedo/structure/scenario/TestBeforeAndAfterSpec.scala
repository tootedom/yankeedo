package org.greencheek.jms.yankeedo.structure.scenario

import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}

/**
 * Created by dominictootell on 12/03/2014.
 */
class TestBeforeAndAfterSpec extends XXXBasedSpec {

  "terminate after 10 messages" in {
    System.out.println("1")
    System.out.flush();
  }
  "terminate after 10 messages" in {
    System.out.println("2")
    System.out.flush();
  }
  "terminate after 10 messages" in {
    System.out.println("3")
    System.out.flush();
  }
  "terminate after 10 messages" in {
    System.out.println("4")
    System.out.flush();
  }
  "terminate after 10 messages" in {
    System.out.println("5")
    System.out.flush();
  }

}

trait XXXBasedSpec extends Specification {
  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: =>Fragments) = Step(startBroker) ^ fs ^ Step(stopBroker)

  def startBroker() {
    System.out.println("BEFORE")
  }

  def stopBroker() {
    System.out.println("AFTER")
  }
}