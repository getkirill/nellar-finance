package com.kraskaska.nekodollar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

import com.github.f4b6a3.uuid.UuidCreator
import java.util.*

fun uuid() = UuidCreator.getTimeOrderedEpoch()
val cbor = CBORMapper.builder().addModules(kotlinModule()).build()