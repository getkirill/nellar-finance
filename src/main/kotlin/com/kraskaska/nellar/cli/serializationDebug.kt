package com.kraskaska.nellar.cli

import com.fasterxml.jackson.databind.module.SimpleModule
import com.kraskaska.nellar.bank.Bank
import com.kraskaska.nellar.bank.BankSerializer
import com.kraskaska.nellar.util.cbor

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val newSerializationModule = SimpleModule()
    newSerializationModule.addSerializer(Bank::class.java, BankSerializer)
    val cbor = cbor.copy().registerModule(newSerializationModule)
    println(cbor.writeValueAsBytes(Bank()).toHexString(HexFormat.UpperCase))
}