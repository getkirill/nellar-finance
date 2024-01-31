package com.kraskaska.nekodollar

import java.util.*

class Bank {
    val actors = mutableListOf<Actor>()
    val promises = mutableListOf<Promise>()
    val transactions = mutableListOf<Transaction>()

    /**
     * Account to do deposits and withdrawals.
     */
    val etherealActor = Actor(uuid()).apply { submit(this@Bank) }

    fun findPromise(id: UUID) = promises.first { it.id == id }
}