package com.kraskaska.nellar.bank

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.kraskaska.nellar.util.cbor
import java.util.*

class Bank(
    val actors: MutableList<Actor> = mutableListOf<Actor>(),
    val promises: MutableList<Promise> = mutableListOf<Promise>(),
    val transactions: MutableList<Transaction> = mutableListOf<Transaction>(),
) {
    companion object {
        /**
         * Account to do deposits and withdrawals.
         */
        val etherealActor get() = Actor(UUID(0, 0))
    }

    fun findPromise(id: UUID) = promises.first { it.id == id }
}

object BankSerializer : StdSerializer<Bank>(Bank::class.java) {
    override fun serialize(bank: Bank, json: JsonGenerator, serializerProvider: SerializerProvider) {
        json.writeStartObject()
        json.writeNumberField("version", 1)
        json.writeArrayFieldStart("actors")
        bank.actors.forEach { json.writeObject(it.id) }
        json.writeEndArray()
        json.writeArrayFieldStart("promises")
        bank.promises.forEach { serializePromise(it, json) }
        json.writeEndArray()
        json.writeArrayFieldStart("transactions")
        bank.transactions.forEach { serializeTransaction(it, json) }
        json.writeEndArray()
        json.writeEndObject()
    }

    fun serializePromise(promise: Promise, json: JsonGenerator) {
        json.writeStartObject()
        json.writeObjectField("id", promise.id)
        when (promise) {
            is InstantPromise -> json.writeObjectField("type", "instant")
            is IndefinitePromise -> json.writeObjectField("type", "indefinite")
        }
        json.writeObjectField("from", promise.from.id)
        json.writeObjectField("to", promise.to.id)
        json.writeNumberField("target", promise.target)
        json.writeEndObject()
    }

    fun serializeTransaction(transaction: Transaction, json: JsonGenerator) {
        json.writeStartObject()
        json.writeObjectField("id", transaction.id)
        json.writeObjectField("fulfills", transaction.fulfills.id)
        json.writeNumberField("amount", transaction.amount)
        json.writeEndObject()
    }
}

object BankDeserializer : StdDeserializer<Bank>(Bank::class.java) {
    override fun deserialize(json: JsonParser, deserializationContext: DeserializationContext): Bank {
        val node = json.readValueAsTree<JsonNode>()
        if (!node.has("version")) {
            throw Exception("No version found while deserializing! Preser NekoDollar?")
        }
        if (!node.get("version").isLong) {
            throw Exception("Invalid version: not a number")
        }
        val version = node.get("version").numberValue().toLong()
        if (version != 1L) {
            throw Exception("Invalid version: expected 1, got $version")
        }
        val actors = node.get("actors").map { Actor(cbor.treeToValue(it, UUID::class.java)) }.toMutableList()
        val promises = node.get("promises").map {
            val id = cbor.treeToValue(it["id"], UUID::class.java)
            val type = it["type"].asText()
            val from = Actor(cbor.treeToValue(it["from"], UUID::class.java))
            val to = Actor(cbor.treeToValue(it["to"], UUID::class.java))
            val target = it["target"].numberValue().toLong()
            when (type) {
                "instant" -> InstantPromise(id, from, to, target)
                "indefinite" -> IndefinitePromise(id, from, to, target)
                else -> throw Exception("unknown promise type $type")
            }
        }.toMutableList()
        val transactions = node.get("transactions").map {
            val id = cbor.treeToValue(it["id"], UUID::class.java)
            val fulfills =
                promises.first { promise -> promise.id == cbor.treeToValue(it["fulfills"], UUID::class.java) }
            val amount = it["amount"].numberValue().toLong()
            Transaction(id, fulfills, amount)
        }.toMutableList()
        return Bank(actors, promises, transactions)
    }
}