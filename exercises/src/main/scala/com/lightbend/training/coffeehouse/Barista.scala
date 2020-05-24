package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Barista {
  case class PrepareCoffee(coffee: Coffee, guest: ActorRef)
  case class CoffeePrepared(coffee: Coffee, guest: ActorRef)

  def props(prepareCoffeeDuration: FiniteDuration, accuracy: Int): Props = Props(new Barista(prepareCoffeeDuration, accuracy))
}

class Barista(prepareCoffeeDuration: FiniteDuration, accuracy: Int) extends Actor with ActorLogging with Timers {
  override def receive: Receive = {
    case Barista.PrepareCoffee(coffee, guest) =>
      log.info(coffee.toString)
      log.info(prepareCoffeeDuration.toString())
      busy(prepareCoffeeDuration)
      sender() ! Barista.CoffeePrepared(pickCoffee(coffee), guest)
  }

  private def pickCoffee(coffee: Coffee): Coffee = {
    if (Random.nextInt(100) < accuracy) coffee
    else Coffee.anyOther(coffee)
  }
}
