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

    data class GetSlice(val customer: ActorRef)
    data class AddToOrder(val customer: ActorRef)

    class CustomerActor(val waiter: ActorRef) : AbstractLoggingActor() {

        override fun createReceive() =
                ReceiveBuilder()
                        .match(String::class.java, this::onMessage)
                        .build()

        private fun onMessage(message: String) {
            when (message) {
                "hungry for pie" -> waiter.tell("order", self())
                "put on table" -> log().info("tasty pie")
                "no pie left" -> log().info("sad story")
                else -> log().error("Unknown message: $message")
            }
        }
    }

    class WaiterActor(val pieCase: ActorRef) : AbstractLoggingActor() {

        override fun createReceive() =
                ReceiveBuilder()
                        .match(String::class.java, this::onMessage)
                        .match(AddToOrder::class.java, this::addToOrder)
                        .build()

        private fun onMessage(message: String) {
            when (message) {
                "order" -> pieCase.tell(GetSlice(sender), self)
                else -> log().error("Unknown message: $message")
            }
        }

        private fun addToOrder(addToOrder: AddToOrder) {
            addToOrder.customer.tell("put on table", self)
        }
    }

    class PieCaseActor(var slices: MutableList<String>) : AbstractLoggingActor() {

        override fun createReceive() =
                ReceiveBuilder()
                        .match(GetSlice::class.java, this::onGetSlice)
                        .build()

        private fun onGetSlice(getSlice: GetSlice) {
            if (slices.size <= 0) {
                        log().error("no pie left")
                        getSender().tell("error", self)
                    } else {
                        val slice = slices.removeFirst()
                        log().info("Slice: $slice")
                        getSender().tell(AddToOrder(getSlice.customer), self)
                    }
        }
    }

    val actorSystem = ActorSystem.create("akka-diner")

    val pieCase = actorSystem.actorOf(Props.create(PieCaseActor::class.java, mutableListOf<String>("apple", "peach", "cherry")), "pieCase")
    val waiter = actorSystem.actorOf(Props.create(WaiterActor::class.java, pieCase), "waiter")
    val customer1 = actorSystem.actorOf(Props.create(CustomerActor::class.java, waiter), "customer1")
    val customer2 = actorSystem.actorOf(Props.create(CustomerActor::class.java, waiter), "customer2")

    actorSystem.log().info("Starting diner")
    customer1.tell("hungry for pie", ActorRef.noSender())
    customer2.tell("hungry for pie", ActorRef.noSender())
    customer1.tell("hungry for pie", ActorRef.noSender())
    customer2.tell("hungry for pie", ActorRef.noSender())
    customer1.tell("hungry for pie", ActorRef.noSender())
}