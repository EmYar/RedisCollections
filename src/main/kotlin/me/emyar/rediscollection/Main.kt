@file:OptIn(ExperimentalUuidApi::class)

package me.emyar.rediscollection

import redis.clients.jedis.Jedis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun main(vararg args: String) {
    val redisUrl = args[0]
    Jedis(redisUrl).use { jedis ->
        val listId = Uuid.random().toString()
        val testList = listOf("Cat", "Dog", "Home", "Garage", "Lawn")
        val redisList = StringRedisList(jedis, listId)

        println("Inserting list: ${testList}) with key: $listId")
        jedis.rpush(listId, *testList.toTypedArray())
        println("Content of RedisList: $redisList")
        println()

        println("Removing 'Dog'")
        redisList.remove("Dog")
        println("Content of RedisList: $redisList")
        println()

        println("Add the dog to the end of the list")
        redisList.add("Dog")
        println("Content of RedisList: $redisList")
        println()

        println("Add new element at position 2")
        redisList.add(2, "NewElement")
        println("Content of RedisList: $redisList")
        println()

        println("Removing list")
        redisList.clear()

        println("-----------------------------------------------")

        val hashId = Uuid.random().toString()
        val testMap = mapOf(
            "apples" to 879,
            "oranges" to 5713,
            "tomatoes" to 482,
        )
        val redisMap = StringByIntRedisMap(jedis, hashId)

        println("Inserting hash: $testMap with key: $hashId")
        jedis.hset(hashId, testMap.mapValues { (_, value) -> value.toString() })
        println("Content of RedisMap: $redisMap")
        println()

        println("Apples count: ${redisMap["apples"]}")
        println("Oranges count: ${redisMap["oranges"]}")
        println("Tomatoes count: ${redisMap["tomatoes"]}")
        println()

        println("Removing apples")
        redisMap.remove("apples")
        println("Content of RedisMap: $redisMap")
        println()

        println("Add tangerines")
        redisMap["tangerines"] = 179
        println("Content of RedisMap: $redisMap")
        println()

        println("Removing hash")
        redisMap.clear()
    }
}