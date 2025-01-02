@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package me.emyar.rediscollection

import redis.clients.jedis.Jedis
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.Collection as JvmCollection

class StringByIntRedisMap(
    private val jedis: Jedis,
    private val redisKey: String,
) : AbstractMutableMap<String, Int>() {

    @Transient
    private var modCount: Int = 0

    override val size: Int
        get() = jedis.hlen(redisKey).toInt()

    override val keys: MutableSet<String>
        get() = KeySet()

    override val entries: MutableSet<MutableMap.MutableEntry<String, Int>>
        get() = EntrySet()

    override val values: MutableCollection<Int>
        get() = Values()

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
            modCount++
        }
        return value
    }

    override fun put(key: String, value: Int): Int? {
        val previous = jedis.hget(redisKey, value.toString())?.toInt()
        jedis.hset(redisKey, key, value.toString())
        modCount++
        return previous
    }

    override fun putAll(from: Map<out String, Int>) {
        jedis.hset(redisKey, from.mapValues { (_, value) -> value.toString() })
        modCount++
    }

    override fun clear() {
        jedis.del(redisKey)
        modCount++
    }

    override fun forEach(action: BiConsumer<in String, in Int>) {
        val mc = modCount
        for ((key, value) in jedis.hgetAll(redisKey)) {
            action.accept(key, value.toInt())
            if (modCount != mc) throw ConcurrentModificationException()
        }
    }

    class Node(
        private val map: StringByIntRedisMap,
        override val key: String,
    ) : MutableMap.MutableEntry<String, Int> {

        override val value: Int
            get() = map[key]!!

        override fun setValue(newValue: Int): Int =
            map.put(key, newValue)!!

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Node

            if (value != other.value) return false
            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value
            result = 31 * result + key.hashCode()
            return result
        }
    }

    private inner class KeySet : AbstractSet<String>(), MutableSet<String> {

        override val size: Int
            get() = this@StringByIntRedisMap.size

        override fun clear() =
            this@StringByIntRedisMap.clear()

        override fun iterator(): MutableIterator<String> =
            KeyIterator()

        override fun contains(element: String): Boolean =
            this@StringByIntRedisMap.containsKey(element)

        override fun remove(element: String): Boolean =
            this@StringByIntRedisMap.remove(element) != null

        override fun toArray(): Array<Any> =
            jedis.hkeys(redisKey).toTypedArray()

        @Suppress("UNCHECKED_CAST")
        override fun <T> toArray(a: Array<T>): Array<T> =
            (jedis.hkeys(redisKey) as JvmCollection<String>).toArray(a)

        override fun forEach(action: Consumer<in String>) {
            val mc = modCount
            for (key in jedis.hkeys(redisKey)) {
                action.accept(key)
                if (modCount != mc) throw ConcurrentModificationException()
            }
        }
    }

    private inner class Values : AbstractCollection<Int>(), MutableCollection<Int> {

        override val size: Int
            get() = this@StringByIntRedisMap.size

        override fun clear() =
            this@StringByIntRedisMap.clear()

        override fun iterator(): MutableIterator<Int> =
            ValueIterator()

        override fun contains(element: Int): Boolean =
            this@StringByIntRedisMap.containsValue(element)

        override fun toArray(): Array<Any> =
            jedis.hvals(redisKey).toTypedArray()

        @Suppress("UNCHECKED_CAST")
        override fun <T> toArray(a: Array<T>): Array<T> =
            (jedis.hvals(redisKey).map(String::toInt) as JvmCollection<Int>).toArray(a)

        override fun forEach(action: Consumer<in Int>) {
            val mc = modCount
            for (value in jedis.hvals(redisKey)) {
                val deserializedValue = value.toInt()
                action.accept(deserializedValue)
                if (modCount != mc) throw ConcurrentModificationException()
            }
        }
    }

    private open inner class RedisMapIterator {
        private var current: Node? = null
        private var expectedModCount: Int = modCount
        private val cachedKeysIterator = jedis.hkeys(redisKey).iterator()

        fun hasNext(): Boolean = cachedKeysIterator.hasNext()

        fun nextNode(): Node {
            if (modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
            val key = cachedKeysIterator.next()
            val toReturn = Node(this@StringByIntRedisMap, key)
            current = toReturn
            return toReturn
        }

        fun remove() {
            val entry = current ?: throw IllegalStateException()
            if (modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
            this@StringByIntRedisMap.remove(entry.key)
            cachedKeysIterator.remove()
            current = null
            expectedModCount = modCount
        }
    }

    inner class EntrySet : AbstractSet<MutableMap.MutableEntry<String, Int>>(),
        MutableSet<MutableMap.MutableEntry<String, Int>> {

        override val size: Int
            get() = this@StringByIntRedisMap.size

        override fun clear() =
            this@StringByIntRedisMap.clear()

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, Int>> =
            EntryIterator()

        override fun contains(element: MutableMap.MutableEntry<String, Int>): Boolean =
            Node(this@StringByIntRedisMap, element.key) == element

        override fun remove(element: MutableMap.MutableEntry<String, Int>): Boolean =
            this@StringByIntRedisMap.remove(element.key, element.value)

        override fun forEach(action: Consumer<in MutableMap.MutableEntry<String, Int>>) {
            val mc = modCount
            for (key in jedis.hkeys(redisKey)) {
                action.accept(Node(this@StringByIntRedisMap, key))
                if (modCount != mc) throw ConcurrentModificationException()
            }
        }
    }

    private inner class KeyIterator : RedisMapIterator(), MutableIterator<String> {
        override fun next(): String = nextNode().key
    }

    private inner class ValueIterator : RedisMapIterator(), MutableIterator<Int> {
        override fun next(): Int = nextNode().value
    }

    private inner class EntryIterator : RedisMapIterator(), MutableIterator<MutableMap.MutableEntry<String, Int>> {
        override fun next(): MutableMap.MutableEntry<String, Int> = nextNode()
    }
}