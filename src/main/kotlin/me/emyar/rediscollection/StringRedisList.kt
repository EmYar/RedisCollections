package me.emyar.rediscollection

import redis.clients.jedis.Jedis

class StringRedisList(jedis: Jedis, key: String) : AbstractRedisList<String>(jedis, key) {

    override fun String.serialize(): String = this@serialize

    override fun String.deserialize(): String = this@deserialize
}