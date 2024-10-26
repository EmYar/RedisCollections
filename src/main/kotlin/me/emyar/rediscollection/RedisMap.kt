package me.emyar.rediscollection

import redis.clients.jedis.Jedis
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function

private const val BATCH_SIZE = 1000

class StringRedisMap(private val jedis: Jedis) : MutableMap<String, String> {

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = keys.asSequence()
            .zip(values.asSequence(), ::Entry)
            .toMutableSet()

    override val keys: MutableSet<String>
        get() = jedis.keys("*")

    override val size: Int
        get() = jedis.dbSize().toInt() // TODO

    override val values: MutableCollection<String>
        get() = jedis.mget(*jedis.keys("*").toTypedArray())

    override fun clear() {
        jedis.flushDB()
    }

    override fun isEmpty(): Boolean =
        size == 0

    override fun remove(key: String): String? =
        jedis.getDel(key)
            .nilAsNull()

    override fun putAll(from: Map<out String, String>) {
        from.forEach { (k, v) -> jedis.set(k, v) }
    }

    override fun put(key: String, value: String): String? =
        jedis.getSet(key, value)
            .nilAsNull()

    override fun get(key: String): String? =
        jedis.get(key)
            .nilAsNull()

    override fun containsValue(value: String): Boolean =
        keys.asSequence()
            .chunked(BATCH_SIZE)
            .any { keys ->
                jedis.mget(*keys.toTypedArray()).contains(value)
            }

    override fun containsKey(key: String): Boolean =
        jedis.exists(key)

    override fun remove(key: String, value: String): Boolean =
        jedis.get(key) == value &&
                jedis.del(key) == 1L

    override fun compute(key: String, remappingFunction: BiFunction<in String, in String?, out String?>): String? {
        return super.compute(key, remappingFunction)
    }

    override fun computeIfAbsent(key: String, mappingFunction: Function<in String, out String>): String {
        return super.computeIfAbsent(key, mappingFunction)
    }

    override fun computeIfPresent(
        key: String,
        remappingFunction: BiFunction<in String, in String, out String?>
    ): String? {
        return super.computeIfPresent(key, remappingFunction)
    }

    override fun merge(
        key: String,
        value: String,
        remappingFunction: BiFunction<in String, in String, out String?>
    ): String? {
        return super.merge(key, value, remappingFunction)
    }

    override fun putIfAbsent(key: String, value: String): String? {
        return super.putIfAbsent(key, value)
    }

    override fun replace(key: String, oldValue: String, newValue: String): Boolean {
        return super.replace(key, oldValue, newValue)
    }

    override fun replace(key: String, value: String): String? {
        return super.replace(key, value)
    }

    override fun replaceAll(function: BiFunction<in String, in String, out String>) {
        super.replaceAll(function)
    }

    override fun getOrDefault(key: String, defaultValue: String): String =
        get(key) ?: defaultValue

    //TODO - выяснить, может ли храниться ключ без значения
    override fun forEach(action: BiConsumer<in String, in String>) {
        keys.asSequence()
            .chunked(BATCH_SIZE)
            .flatMap { keys ->
                val values = jedis.mget(*keys.toTypedArray())
                keys.zip(values) { key, value -> value.nilAsNull()?.let { key to it } }
            }
            .filterNotNull()
            .forEach { (key, value) -> action.accept(key, value) }
    }
}

private class Entry(
    override val key: String,
    value: String
) : MutableMap.MutableEntry<String, String> {
    override var value = value
        private set

    override fun setValue(newValue: String): String {
        val oldValue = value
        value = newValue
        return oldValue
    }
}

private fun String.nilAsNull() = takeIf { it != "nil" }