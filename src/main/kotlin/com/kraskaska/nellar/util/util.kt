package com.kraskaska.nellar.util

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.kotlinModule

import com.github.f4b6a3.uuid.UuidCreator
import com.kraskaska.nellar.bank.Bank
import com.kraskaska.nellar.bank.BankDeserializer
import com.kraskaska.nellar.bank.BankSerializer

fun uuid() = UuidCreator.getTimeOrderedEpoch()
val rawCbor = CBORMapper.builder().addModules(kotlinModule()).build()
val cbor = rawCbor.copy().registerModule(SimpleModule().apply {
    addSerializer(Bank::class.java, BankSerializer)
    addDeserializer(Bank::class.java, BankDeserializer)
})