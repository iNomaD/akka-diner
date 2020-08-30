package com.devexperts.pie

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder

/**
 * @author Denis Korobov
 * @since 28/08/2020
 */
fun main(args: Array<String>) {

    class CustomerActor (val waiter: ActorRef) : AbstractLoggingActor() {
        override fun createReceive() =
                ReceiveBuilder()
                        .match(String::class.java, this::onMessage)
                        .build()

        private fun onMessage(message: String) {
            when (message) {
                "hungry for pie" -> waiter.tell("order", self())
                "put on table" -> throw NotImplementedError()
                "no pie left" -> throw NotImplementedError()
                else -> log().error("Unknown message: $message")
            }
        }
    }

    class WaiterActor : AbstractLoggingActor() {
        override fun createReceive() =
                ReceiveBuilder()
                        .match(String::class.java, this::onMessage)
                        .build()

        private fun onMessage(message: String) {
            when (message) {
                "order" -> log().info("Don't know how to order order from ${sender.path().name()}")
                "add to order" -> throw NotImplementedError()
                "error" -> throw NotImplementedError()
                else -> log().error("Unknown message: $message")
            }
        }
    }

    val actorSystem = ActorSystem.create("akka-diner")

    val waiter = actorSystem.actorOf(Props.create(WaiterActor::class.java), "waiter")
    val customer1 = actorSystem.actorOf(Props.create(CustomerActor::class.java, waiter), "customer1")
    val customer2 = actorSystem.actorOf(Props.create(CustomerActor::class.java, waiter), "customer2")

    actorSystem.log().info("Starting diner")
    customer1.tell("hungry for pie", ActorRef.noSender())
    customer2.tell("hungry for pie", ActorRef.noSender())
    customer1.tell("hungry for pie", ActorRef.noSender())
    customer2.tell("hungry for pie", ActorRef.noSender())
    customer1.tell("hungry for pie", ActorRef.noSender())
}