package com.kraskaska.nellar.cli

import com.fasterxml.jackson.module.kotlin.readValue
import com.kraskaska.nellar.bank.Actor
import com.kraskaska.nellar.bank.Bank
import com.kraskaska.nellar.bank.Transaction
import com.kraskaska.nellar.util.cbor
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object CLIActorConverter : ITypeConverter<Actor> {
    override fun convert(p0: String?): Actor = Actor(UUID.fromString(p0))
}

@Command(
    name = "bank-cli",
    description = ["handle bank stuffs"],
    subcommands = [CLICreateActor::class, CLIListActors::class, CLIPromise::class, CLIListPromises::class, CLITransact::class, CLIDeposit::class, CLIWithdraw::class, CLIListTransactions::class],
    mixinStandardHelpOptions = true
)
object BankCLI : Runnable {
    @Option(names = ["--bank-file", "--bank"])
    var storageLocation = File("./bank.neko")
    val bank = try {
        cbor.readValue<Bank>(storageLocation)
    } catch (e: FileNotFoundException) {
        println("Bank doesn't exist, making new one...")
        Bank()
    }
    override fun run() {
        println("hello :3")
        println("use one of the other cool command pwease :3")
    }

    fun pre() {
        Runtime.getRuntime().addShutdownHook(thread(false) {
            cbor.writeValue(storageLocation, bank)
        })
    }
}

//@Command(name = "debug-print-ethereal-actor")
//object CLIDebugPrintEtherealActor : Runnable {
//    @ParentCommand
//    lateinit var parent: BankCLI
//    override fun run() {
//        println("ethereal actor in current bank:")
//        println(parent.bank.etherealActor)
//    }
//}

@Command(name = "create-actor", description = ["creates a new actor"])
object CLICreateActor : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI
    override fun run() {
        val actor = Actor.make().apply { submit(BankCLI.bank) }
        println("New actor was created!")
        println(actor)
    }
}

@Command(name = "list-actors", description = ["lists known actors"])
object CLIListActors : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI
    override fun run() {
        println("All actors:")
        println("Ethereal: ${BankCLI.bank.etherealActor}")
        println(BankCLI.bank.actors.joinToString("\n") { it.toString(BankCLI.bank) })
    }
}

@Command(name = "promise", description = ["create a new promise - intent to pay"])
object CLIPromise : Runnable {
    enum class PromiseType {
        INSTANT, INDEFINITE
    }

    @ParentCommand
    lateinit var parent: BankCLI

    @Option(names = ["--type"], defaultValue = "instant")
    var promiseType = PromiseType.INSTANT

    @Parameters(index = "0")
    lateinit var from: Actor

    @Parameters(index = "1")
    lateinit var to: Actor

    @Parameters(index = "2")
    var amount: Long = 0

    override fun run() {
        when (promiseType) {
            PromiseType.INSTANT -> {
                from.instantPromiseTo(to, BankCLI.bank, amount)
                println("Successfully made promise!")
            }

            PromiseType.INDEFINITE -> {
                val promise = from.indefinitePromiseTo(to, BankCLI.bank, amount)
                println("Successfully made promise!")
                println(promise)
            }
        }
    }
}

@Command(name = "list-promises", description = ["lists all known promises"], mixinStandardHelpOptions = true)
object CLIListPromises : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI

    @Option(names = ["--unfulfilled"], description = ["only list those which haven't been paid in full"])
    var listUnfulfilled = false;

    @Parameters(index = "0", arity = "0..1")
    var actor: Actor? = null;
    override fun run() {
        val promises = if (actor != null) {
            // if we have unfulfilled flag we probably don't want to know unfulfilled promises TO specified actor
            BankCLI.bank.promises.filter {
                (it.from == actor || (!listUnfulfilled && it.to == actor)) && (!listUnfulfilled || !it.isFulfilled(
                    BankCLI.bank
                ))
            }
        } else {
            BankCLI.bank.promises.filter { !listUnfulfilled || !it.isFulfilled(BankCLI.bank) }
        }
        println("${if (listUnfulfilled) "Unfulfilled p" else "P"}romises${if (actor != null) " for $actor" else ""}:")
        println(promises.joinToString("\n") {"$it (${it.getFulfilled(BankCLI.bank)})"})
    }
}

@Command(name = "list-transactions", description = ["lists all known transactions"])
object CLIListTransactions : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI

    @Parameters(index = "0", arity = "0..1")
    var actor: Actor? = null;
    override fun run() {
        val promises = if (actor != null) {
            BankCLI.bank.transactions.filter {
                it.fulfills.from == actor || it.fulfills.to == actor
            }
        } else {
            BankCLI.bank.transactions
        }
        println("Transactions${if (actor != null) " for $actor" else ""}:")
        println(promises.joinToString("\n"))
    }
}

@Command(name = "transact", description = ["make a transaction - fulfill promise made earlier"])
object CLITransact : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI

    @Parameters(index = "0")
    lateinit var promise: UUID

    @Parameters(index = "1")
    var amount: Long = 0
    override fun run() {
        Transaction.of(BankCLI.bank.findPromise(promise), amount).submit(BankCLI.bank)
        println("Success!")
    }
}

@Command(name = "deposit", description = ["deposit money - makes an instant promise from ethereal account for that amount of money (because we cannot transact without promise)"])
object CLIDeposit : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI

    @Parameters(index = "0")
    lateinit var actor: Actor

    @Parameters(index = "1")
    var amount: Long = 0
    override fun run() {
        actor.deposit(BankCLI.bank, amount)
        println("Success!")
    }
}

@Command(name = "withdraw", description = ["withdraw money - makes an instant promise to ethereal account for that amount of money (because we cannot transact without promise)"])
object CLIWithdraw : Runnable {
    @ParentCommand
    lateinit var parent: BankCLI

    @Parameters(index = "0")
    lateinit var actor: Actor

    @Parameters(index = "1")
    var amount: Long = 0
    override fun run() {
        actor.withdraw(BankCLI.bank, amount)
        println("Success!")
    }
}

fun main(args: Array<String>) {
    exitProcess(
        CommandLine(BankCLI)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setExecutionStrategy { BankCLI.pre(); RunLast().execute(it) }
            .registerConverter(Actor::class.java, CLIActorConverter)
            .execute(*args)
    )
}