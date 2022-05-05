package org.prebid.cache.functional.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
