package com.kraskaska.nellar.legacy.preser

fun migrate(bank: Bank): com.kraskaska.nellar.bank.Bank {
    println("Migrating promises!")
    val migratedPromises: MutableList<com.kraskaska.nellar.bank.Promise> = bank.promises.map {
        when (it) {
            is InstantPromise -> com.kraskaska.nellar.bank.InstantPromise(it.id, it.from.run {
                if (this == bank.etherealActor) com.kraskaska.nellar.bank.Bank.etherealActor
                com.kraskaska.nellar.bank.Actor(id)
            }, it.to.run {
                if (this == bank.etherealActor) com.kraskaska.nellar.bank.Bank.etherealActor
                com.kraskaska.nellar.bank.Actor(id)
            }, it.target)

            is IndefinitePromise -> com.kraskaska.nellar.bank.InstantPromise(it.id, it.from.run {
                if (this == bank.etherealActor) com.kraskaska.nellar.bank.Bank.etherealActor
                com.kraskaska.nellar.bank.Actor(id)
            }, it.to.run {
                if (this == bank.etherealActor) com.kraskaska.nellar.bank.Bank.etherealActor
                com.kraskaska.nellar.bank.Actor(id)
            }, it.target)

            else -> throw Exception()
        }
    }.toMutableList()
    println("Migrating actors!")
    val migratedActors = bank.actors.filter { it != bank.etherealActor }.map { com.kraskaska.nellar.bank.Actor(it.id) }.toMutableList()
    println("Migrating transactions!")
    val migratedTransactions = bank.transactions.map {
        com.kraskaska.nellar.bank.Transaction(
            it.id,
            migratedPromises.first { promise -> promise.id == it.fulfills.id },
            it.amount)
    }.toMutableList()
    return com.kraskaska.nellar.bank.Bank(migratedActors, migratedPromises, migratedTransactions)
}