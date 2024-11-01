package me.emyar.rediscollection

import redis.clients.jedis.Jedis
import java.util.function.BiConsumer
import java.util.function.BiFunction

private const val CHUNK_SIZE = 1000

class StringRedisMap(private val jedis: Jedis) : MutableMap<String, Int> {

    override val entries: MutableSet<MutableMap.MutableEntry<String, Int>>
        get() =
            keys.let { keys ->
                keys.asSequence()
                    .zip(jedis.mget(*keys.toTypedArray()).asSequence()) { key, value ->
                        Entry(key, value.toInt(), jedis)
                    }
                    .toMutableSet()

            }

    override val keys: MutableSet<String>
        get() = jedis.keys("*")

    override val size: Int
        get() = jedis.dbSize().toInt()

    override val values: MutableCollection<Int>
        get() = jedis.mget(*jedis.keys("*").toTypedArray())
            .mapTo(mutableListOf(), String::toInt)

    override fun clear() {
        jedis.flushDB()
    }

    override fun isEmpty(): Boolean =
        size == 0

    override fun remove(key: String): Int? =
        jedis.getDel(key)
            .nilAsNull()
            ?.toInt()

    override fun putAll(from: Map<out String, Int>) {
        val keysvalues = ArrayList<String>(from.size * 2)
        from.forEach { (key, value) ->
            keysvalues += key
            keysvalues += value.toString()
        }
        jedis.mset(*keysvalues.toTypedArray())
    }

    override fun put(key: String, value: Int): Int? =
        jedis.getSet(key, value.toString())
            .nilAsNull()
            ?.toInt()

    override fun get(key: String): Int? =
        jedis.get(key)
            .nilAsNull()
            ?.toInt()

    override fun containsValue(value: Int): Boolean {
        val stringValue = value.toString()
        return keys.asSequence()
            .chunked(CHUNK_SIZE)
            .any { keysChunk ->
                jedis.mget(*keysChunk.toTypedArray())
                    .contains(stringValue)
            }
    }

    override fun containsKey(key: String): Boolean =
        jedis.exists(key)

    override fun remove(key: String, value: Int): Boolean =
        jedis.get(key).nilAsNull()?.toInt() == value &&
                jedis.del(key) == 1L

    override fun replace(key: String, oldValue: Int, newValue: Int): Boolean =
        if (jedis.get(key).nilAsNull()?.toInt() == oldValue) {
            jedis.set(key, newValue.toString())
            true
        } else {
            false
        }

    override fun replace(key: String, value: Int): Int? =
        if (jedis.exists(key)) {
            jedis.getSet(key, value.toString())
                .nilAsNull()
                ?.toInt()
        } else {
            null
        }

    override fun replaceAll(function: BiFunction<in String, in Int, out Int>) {
        keys.asSequence()
            .chunked(CHUNK_SIZE)
            .forEach { keysChunk ->
                val keysvalues = ArrayList<String>(keysChunk.size * 2)
                jedis.mget(*keysChunk.toTypedArray())
                    .forEachIndexed { i, value ->
                        val key = keysChunk[i]
                        keysvalues += key
                        keysvalues += function.apply(key, value.toInt()).toString()
                    }
                jedis.mset(*keysvalues.toTypedArray())
            }
    }

    override fun getOrDefault(key: String, defaultValue: Int): Int =
        get(key) ?: defaultValue

    override fun forEach(action: BiConsumer<in String, in Int>) {
        keys.asSequence()
            .chunked(CHUNK_SIZE)
            .forEach { keysChunk ->
                jedis.mget(*keysChunk.toTypedArray())
                    .forEachIndexed { i, value -> action.accept(keysChunk[i], value.toInt()) }
            }
    }
}

private class Entry(
    override val key: String,
    value: Int,
    private val jedis: Jedis,
) : MutableMap.MutableEntry<String, Int> {
    override var value = value
        private set

    override fun setValue(newValue: Int): Int {
        val oldValue = value
        jedis.set(key, newValue.toString())
        value = newValue
        return oldValue
    }
}

private fun String.nilAsNull(): String? = takeIf { it != "nil" }