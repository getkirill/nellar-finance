package com.kraskaska.nekodollar

import java.util.*

data class Actor(val id: UUID) {
    /**
     * @return sum of all promises to and from this actor inside a {@link Bank}
     */
    fun getBalance(bank: Bank): Long {
        return bank.promises.filter { it.to == this || it.from == this }
            .sumOf { if (it.to == this) it.target else if (it.from == this) -it.target else 0 }
    }

    /**
     * @return sum of all transactions to and from this actor inside a {@link Bank}
     */
    fun getActualBalance(bank: Bank): Long {
        return bank.transactions.filter { it.fulfills.to == this || it.fulfills.from == this }
            .sumOf { if (it.fulfills.to == this) it.amount else if (it.fulfills.from == this) -it.amount else 0 }
    }

    /**
     * Submits instant promise and transaction to fulfill that promise
     */
    fun instantPromiseTo(to: Actor, bank: Bank, amount: Long) {
        Promise.instant(this, to, amount).run {
            submit(bank)
            Transaction.of(this, amount).submit(bank)
        }
    }
    /**
     * Submits indefinite promise
     * @return indefinite promise
     */
    fun indefinitePromiseTo(to: Actor, bank: Bank, amount: Long): IndefinitePromise {
        return Promise.indefinite(this, to, amount).apply {
            submit(bank)
        }
    }

    fun deposit(bank: Bank, amount: Long) {
        bank.etherealActor.instantPromiseTo(this, bank, amount)
    }
    fun withdraw(bank: Bank, amount: Long) {
        instantPromiseTo(bank.etherealActor, bank, amount)
    }

    fun submit(bank: Bank) {
        bank.actors.add(this)
    }

    fun toString(bank: Bank): String {
        return "Actor(id=${id}, balance=${getBalance(bank)}, actualBalance=${getActualBalance(bank)}, balanceDiff=${getBalance(bank) - getActualBalance(bank)})"
    }

    companion object {
        fun make() = Actor(uuid())
    }
}