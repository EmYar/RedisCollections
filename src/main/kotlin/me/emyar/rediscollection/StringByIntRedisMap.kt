package me.emyar.rediscollection

import redis.clients.jedis.Jedis

abstract class StringByIntRedisMap(
    private val jedis: Jedis,
    private val redisKey: String,
) : AbstractMutableMap<String, Int>() {

    override val size: Int
        get() = jedis.hlen(redisKey).toInt()

    override val keys: MutableSet<String>
        get() = jedis.hkeys(redisKey)
            .toMutableSet()

    override val values: MutableCollection<Int>
        get() = jedis.hvals(redisKey)
            .mapTo(mutableListOf(), String::toInt)

    override fun containsKey(key: String): Boolean =
        jedis.hexists(redisKey, key)

    override fun containsValue(value: Int): Boolean =
        value.toString() in jedis.hvals(redisKey)

    override fun get(key: String): Int? =
        jedis.hget(redisKey, key)
            ?.toInt()

    override fun isEmpty(): Boolean =
        jedis.hlen(redisKey) == 0L

    override fun remove(key: String): Int? {
        val value = get(key)
        if (value != null) {
            jedis.hdel(redisKey, key)
        }
        return value
    }

    override fun put(key: String, value: Int): Int? {
        val previous = jedis.hget(redisKey, value.toString())?.toInt()
        jedis.hset(redisKey, key, value.toString())
        return previous
    }

    override fun putAll(from: Map<out String, Int>) {
        val map = LinkedHashMap<String, String>(from.size)
        for ((key, value) in from) {
            map[key] = value.toString()
        }
        jedis.hset(redisKey, map)
    }

    override fun clear() {
        jedis.del(redisKey)
    }
}