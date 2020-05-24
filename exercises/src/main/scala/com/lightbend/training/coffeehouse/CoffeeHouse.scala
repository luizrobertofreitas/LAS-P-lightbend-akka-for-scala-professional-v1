package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.routing.FromConfig
import com.lightbend.training.coffeehouse.CoffeeHouse.ApproveCoffee

import scala.concurrent.duration._

object CoffeeHouse {
  case class CreateGuest(favoriteCoffee: Coffee, caffeineLimit: Int)
  case class ApproveCoffee(coffee: Coffee, guest: ActorRef)

  def props(caffeineLimit: Int): Props = Props(new CoffeeHouse(caffeineLimit))
}

class CoffeeHouse(caffeineLimit: Int) extends Actor with ActorLogging {

  log.debug("CoffeeHouse Open")

  override def supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case Guest.CaffeineException => SupervisorStrategy.Stop
      case Waiter.FrustratedException(coffee, guest) =>
        barista.forward(Barista.PrepareCoffee(coffee, guest))
        SupervisorStrategy.Restart
    }

    OneForOneStrategy()(decider.orElse(super.supervisorStrategy.decider))
  }

  private var guestBook: Map[ActorRef, Int] = Map.empty.withDefaultValue(0)

  private val baristaAccuracy: Int = context.system.settings.config.getInt("coffee-house.barista.accuracy")

  private val waiterMaxComplaintCount: Int = context.system.settings.config.getInt("coffee-house.waiter.max-complaint-count")

  private val finishCoffeeDuration: FiniteDuration =
  context.system.settings.config.getDuration("coffee-house.barista.prepare-coffee-duration",
  TimeUnit.MILLISECONDS).millis

  private val prepareCoffeeDuration: FiniteDuration =
  context.system.settings.config.getDuration("coffee-house.guest.finish-coffee-duration",
  TimeUnit.MILLISECONDS).millis

  private val barista: ActorRef = createBarista()
  private val waiter: ActorRef = createWaiter()

  protected def createGuest(favoriteCoffee: Coffee, caffeineLimit: Int): ActorRef = {
    val guest = context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration, caffeineLimit))
    context.watch(guest)
    guest
  }

  protected def createWaiter(): ActorRef = context.actorOf(Waiter.props(self, barista, waiterMaxComplaintCount), "waiter")

  protected def createBarista(): ActorRef = context.actorOf(
    FromConfig.props(Barista.props(prepareCoffeeDuration, baristaAccuracy)), "barista")

  override def receive: Receive = {
    case CoffeeHouse.CreateGuest(favoriteCoffee, caffeineLimit) =>
      val guest = createGuest(favoriteCoffee, caffeineLimit)
      guestBook += guest -> 0
      log.info(s"Guest $guest added to guest book")
    case ApproveCoffee(coffee, guest) if guestBook(guest) < caffeineLimit =>
      guestBook += guest -> (guestBook(guest) + 1)
      log.info(s"Guest $guest caffeine count incremented")
      barista.forward(Barista.PrepareCoffee(coffee, guest))
    case ApproveCoffee(coffee, guest) =>
      log.info(s"Sorry $guest, but you have reached your limit.")
      context.stop(guest)
    case Terminated(guest) =>
      guestBook -= guest
      log.info(s"Thanks $guest, for being our guest!")
  }
}
