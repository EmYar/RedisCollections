package me.emyar.rediscollection

import redis.clients.jedis.Jedis
import redis.clients.jedis.params.LPosParams
import java.util.*
import java.util.Collection as JvmCollection

abstract class AbstractRedisList<T>(
    private val jedis: Jedis,
    private val key: String,
) : AbstractList<T>(), RandomAccess {

    protected abstract fun T.serialize(): String
    protected abstract fun String.deserialize(): T

    override val size: Int
        get() = jedis.llen(key).toInt()

    override fun get(index: Int): T {
        checkBounds(index)
        return jedis.lindex(key, index.toLong())
            .deserialize()
    }

    override fun set(index: Int, element: T): T {
        checkBounds(index)
        val oldValue = jedis.lindex(key, index.toLong())
            ?.deserialize()
        jedis.lset(key, index.toLong(), element.serialize())
        registerModification()
        @Suppress("UNCHECKED_CAST")
        return oldValue as T
    }

    override fun add(element: T): Boolean {
        jedis.rpush(key, element.serialize())
        registerModification()
        return true
    }

    override fun add(index: Int, element: T) {
        checkBounds(index)
        jedis.lset(key, index.toLong(), element.serialize())
        registerModification()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        registerModification()
        jedis.rpush(key, *elements.map { it.serialize() }.toTypedArray())
        return true
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val size = size
        registerModification()
        when {
            index == 0 && size == 0 -> return addAll(elements)
            index < 0 || index >= size -> throw IndexOutOfBoundsException("Index: $index")
        }
        val tail = jedis.rpop(key, size - index)
        jedis.rpush(key, *elements.map { it.serialize() }.toTypedArray())
        jedis.rpush(key, *tail.toTypedArray())
        return true
    }

    override fun addFirst(e: T) {
        registerModification()
        jedis.lpush(key, e?.serialize())
    }

    override fun addLast(e: T) {
        registerModification()
        jedis.rpush(key, e?.serialize())
    }

    override fun remove(element: T): Boolean {
        registerModification()
        jedis.lrem(key, 1, element.serialize())
        return true
    }

    override fun removeAt(index: Int): T {
        checkBounds(index)
        val size = size
        return when {
            index == 0 -> jedis.lpop(key).deserialize()
            index == size - 1 -> jedis.rpop(key).deserialize()
            index <= size / 2 -> removeLeft(index)
            else -> removeRight(index)
        }.also { registerModification() }
    }

    override fun removeLast(): T =
        jedis.rpop(key)
            .deserialize()
            .also { registerModification() }

    override fun removeFirst(): T =
        jedis.lpop(key)
            .deserialize()
            .also { registerModification() }

    override fun indexOf(element: T): Int =
        jedis.lpos(key, element.serialize())?.toInt()
            ?: -1

    override fun lastIndexOf(element: T): Int =
        jedis.lpos(key, element.serialize(), LPosParams().rank(-1))
            ?.toInt()
            ?: -1

    override fun contains(element: T?): Boolean =
        indexOf(element) >= 0

    override fun clear() {
        if (isNotEmpty()) {
            registerModification()
            jedis.del(key)
        }
    }

    override fun sort(c: Comparator<in T>?) {
        registerModification()
        if (c == null) {
            jedis.sort(key)
        } else {
            val values = jedis.lrange(key, 0, -1).asSequence()
                .map { it.deserialize() }
                .toMutableList()
            jedis.del(key)
            values.sortWith(c)
            jedis.rpush(key, *values.map { it.serialize() }.toTypedArray())
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun toArray(): Array<out Any?> =
        (jedis.lrange(key, 0L, size.toLong())
            .map { it.deserialize() } as JvmCollection<out Any?>)
            .toArray()

    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun <T : Any?> toArray(a: Array<out T?>): Array<out T?> =
        (jedis.lrange(key, 0L, size.toLong())
            .map { it.deserialize() } as JvmCollection<out T?>)
            .toArray(a)

    private fun removeLeft(index: Int): T {
        val head = jedis.lpop(key, index)
        val removed = jedis.lpop(key)
        jedis.lpush(key, *head.toTypedArray())
        registerModification()
        return removed.deserialize()
    }

    private fun removeRight(index: Int): T {
        val tail = jedis.rpop(key, size - 1 - index)
        val removed = jedis.rpop(key)
        jedis.rpush(key, *tail.toTypedArray())
        registerModification()
        return removed.deserialize()
    }

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    private fun registerModification() {
        modCount++
    }
}