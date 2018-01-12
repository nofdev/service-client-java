package org.nofdev.client.http

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

/**
 * Created by Qiang on 11/11/15.
 */
class ObjectMapperFactory {
    static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
        objectMapper.registerModule(new JodaModule())
        objectMapper.registerModule(new JavaTimeModule())
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        return objectMapper
    }
}
