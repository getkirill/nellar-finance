package com.kraskaska.nellar.legacy.preser

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.kraskaska.nellar.util.uuid
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "instant", value = InstantPromise::class),
    JsonSubTypes.Type(name = "indefinite", value = IndefinitePromise::class)
)
abstract class Promise(val id: UUID, val from: Actor, val to: Actor) {
    abstract val target: Long
//    abstract val paid: Long
    fun isFulfilled(bank: Bank): Boolean {
        return getFulfilled(bank) >= target
    }

    fun getFulfilled(bank: Bank): Long {
        return bank.transactions.filter { it.fulfills == this }.sumOf { it.amount }
    }

    fun submit(bank: Bank) {
        bank.promises.add(this)
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, from=$from, to=$to, target=$target)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Promise

        if (id != other.id) return false
        if (from != other.from) return false
        if (to != other.to) return false
        if (target != other.target) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun instant(from: Actor, to: Actor, amount: Long): InstantPromise {
            return InstantPromise(uuid(), from, to, amount)
        }
        fun indefinite(from: Actor, to: Actor, amount: Long): IndefinitePromise {
            return IndefinitePromise(uuid(), from, to, amount)
        }
    }


}

data class Transaction(val id: UUID, val fulfills: Promise, val amount: Long = fulfills.target) {
    fun submit(bank: Bank) {
        bank.transactions.add(this)
    }

    companion object {
        fun of(fulfills: Promise, amount: Long) = Transaction(uuid(), fulfills, amount)
    }
}

/**
 * This promise can be fulfilled whenever
 */
class IndefinitePromise(id: UUID, from: Actor, to: Actor, override val target: Long): Promise(id, from, to)

/**
 * This promise should be fulfilled immediately
 */
class InstantPromise(id: UUID, from: Actor, to: Actor, override val target: Long): Promise(id, from, to)

