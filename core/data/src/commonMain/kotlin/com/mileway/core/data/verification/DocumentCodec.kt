package com.mileway.core.data.verification

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * PLAN_V24 P4.1: JSON codec for `DocumentEntity`'s list columns. Kept off the entity's companion so
 * `DocumentEntity` stays a pure Room POJO (a companion referencing `DocInfoField.serializer()`
 * makes Room's KSP processor fail — see [com.mileway.core.data.model.db.DocumentEntity]).
 */
object DocumentCodec {
    private val stringListSerializer = ListSerializer(String.serializer())
    private val docInfoSerializer = ListSerializer(DocInfoField.serializer())

    fun encodeDocUrls(urls: List<String>): String = Json.encodeToString(stringListSerializer, urls)

    fun decodeDocUrls(json: String): List<String> = runCatching { Json.decodeFromString(stringListSerializer, json) }.getOrDefault(emptyList())

    fun encodeDocInfo(info: List<DocInfoField>): String = Json.encodeToString(docInfoSerializer, info)

    fun decodeDocInfo(json: String): List<DocInfoField> = runCatching { Json.decodeFromString(docInfoSerializer, json) }.getOrDefault(emptyList())
}
