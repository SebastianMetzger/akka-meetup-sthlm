/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.akkademo.processor.service

import akka.actor.SupervisorStrategy.{ Escalate, Restart, Resume }
import akka.actor.{ OneForOneStrategy, Props, Actor, ActorLogging }
import com.typesafe.akkademo.common.{ PlayerBet, RetrieveBets }
import com.typesafe.akkademo.processor.repository.{ DatabaseFailureException, ReallyUnstableResource }
import scala.concurrent.duration._

class BettingProcessor extends Actor with ActorLogging {

  /**
   * TASKS :
   * Send remote registration message to service
   * Create worker for dangerous task (using UnstableRepository actor with ReallyUnstableResource)
   * Supervise worker -> handle errors
   * Send confirmation message back to Betting service
   */
  val service = context.actorFor("akka://BettingServiceActorSystem@127.0.0.1:2552/user/bettingService")
  val resourceHandler = context.actorOf(Props[UnstableRepositoryActor], "resource")

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: RuntimeException ⇒ {
        println("SuperVisor triggered")
        Resume
      }
      case _: DatabaseFailureException ⇒ {

        Resume
      }
    }

  override def preStart = {
    service ! "ProcessorRegistration"
  }

  def receive = {
    case bet: PlayerBet ⇒ {
      resourceHandler ! bet
    }
  }
}

class UnstableRepositoryActor extends Actor with ActorLogging {

  val resource = new ReallyUnstableResource()

  override def preStart = {
    println("Actor started");
  }

  def receive = {
    case bet: PlayerBet ⇒ {
      try {
        println("Attempting to save: " + bet.id)
        resource.save(bet.id, bet.bet.player, bet.bet.game, bet.bet.amount)
      } catch {
        case _: RuntimeException ⇒ {
          println("Caught RuntimeException while saving " + bet.id)
          sender ! bet
        }
        case _: DatabaseFailureException ⇒ {
          println("Caught DatabaseFailureException while saving " + bet.id)
          sender ! bet
        }
      }
    }
  }
}
