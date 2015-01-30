/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.akkademo.service

import akka.actor.{ ActorLogging, Actor }
import com.typesafe.akkademo.common.{ PlayerBet, Bet, RetrieveBets }

class BettingService extends Actor with ActorLogging {

  /**
   * TASKS:
   * Create unique sequence/transaction number
   * Create PlayerBet and call betting processor (remotely)
   * Retrieve all bets from betting processor (remotely)
   * Handle timed out transactions (scheduler)
   * Handle registration message from betting processor
   * Handle crash of/unavailable betting processor
   * Keep any message locally until there is a processor service available
   */
  var i: Int = 0
  var lastSender = context.system.deadLetters

  def receive = {
    case bet: Bet ⇒ {
      val pb = new PlayerBet(i, bet);
      i += 1;
      if (lastSender == context.system.deadLetters) {
        println("lastSender is dead")
        // Do sth :o)
      } else {
        lastSender ! pb
      }
    }
    case bet: PlayerBet ⇒ {
      println("Got it back from the processor")
    }
    case "Spam" ⇒ println("OMG I received Spam")
    case "ProcessorRegistration" ⇒ {
      lastSender = context.sender
      println("Processor Registered")
      println(lastSender)
    }
    case RetrieveBets ⇒
  }
}
