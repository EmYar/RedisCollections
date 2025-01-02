@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package me.emyar.rediscollection

import redis.clients.jedis.Jedis
import redis.clients.jedis.params.LPosParams
import java.util.*
import java.util.Collection as JvmCollection

class StringRedisList(
    private val jedis: Jedis,
    private val key: String,
) : AbstractList<String>(), RandomAccess {

    override val size: Int
        get() = jedis.llen(key).toInt()

    override fun get(index: Int): String {
        checkBounds(index)
        return jedis.lindex(key, index.toLong())
    }

    override fun set(index: Int, element: String): String {
        checkBounds(index)
        val oldValue = jedis.lindex(key, index.toLong())
        jedis.lset(key, index.toLong(), element)
        registerModification()
        return oldValue
    }

    override fun add(element: String): Boolean {
        jedis.rpush(key, element)
        registerModification()
        return true
    }

    override fun add(index: Int, element: String) {
        checkBounds(index)
        addAll(index, listOf(element))
    }

    override fun addAll(elements: Collection<String>): Boolean {
        registerModification()
        jedis.rpush(key, *elements.toTypedArray())
        return true
    }

    override fun addAll(index: Int, elements: Collection<String>): Boolean {
        val size = size
        registerModification()
        when {
            index == 0 && size == 0 -> return addAll(elements)
            index < 0 || index >= size -> throw IndexOutOfBoundsException("Index: $index")
        }
        val tail = jedis.rpop(key, size - index).reversed()
        jedis.rpush(key, *elements.toTypedArray())
        jedis.rpush(key, *tail.toTypedArray())
        return true
    }

    override fun addFirst(e: String) {
        registerModification()
        jedis.lpush(key, e)
    }

    override fun addLast(e: String) {
        registerModification()
        jedis.rpush(key, e)
    }

    override fun remove(element: String): Boolean {
        registerModification()
        jedis.lrem(key, 1, element)
        return true
    }

    override fun removeAt(index: Int): String {
        checkBounds(index)
        val size = size
        return when {
            index == 0 -> jedis.lpop(key)
            index == size - 1 -> jedis.rpop(key)
            index <= size / 2 -> removeLeft(index)
            else -> removeRight(index)
        }.also { registerModification() }
    }

    override fun removeLast(): String =
        jedis.rpop(key)
            .also { registerModification() }

    override fun removeFirst(): String =
        jedis.lpop(key)
            .also { registerModification() }

    override fun indexOf(element: String): Int =
        jedis.lpos(key, element)?.toInt()
            ?: -1

    override fun lastIndexOf(element: String): Int =
        jedis.lpos(key, element, LPosParams().rank(-1))
            ?.toInt()
            ?: -1

    override fun contains(element: String?): Boolean =
        indexOf(element) >= 0

    override fun clear() {
        if (isNotEmpty()) {
            registerModification()
            jedis.del(key)
        }
    }

    override fun sort(c: Comparator<in String>?) {
        registerModification()
        if (c == null) {
            jedis.sort(key)
        } else {
            val values = jedis.lrange(key, 0, -1).toMutableList()
            jedis.del(key)
            values.sortWith(c)
            jedis.rpush(key, *values.toTypedArray())
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun toArray(): Array<out Any?> =
        (jedis.lrange(key, 0L, size.toLong()) as JvmCollection<out Any?>)
            .toArray()

    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun <T : Any?> toArray(a: Array<out T?>): Array<out T?> =
        (jedis.lrange(key, 0L, size.toLong()) as JvmCollection<out T?>)
            .toArray(a)

    private fun removeLeft(index: Int): String {
        val head = jedis.lpop(key, index)
        val removed = jedis.lpop(key)
        jedis.lpush(key, *head.reversed().toTypedArray())
        registerModification()
        return removed
    }

    private fun removeRight(index: Int): String {
        val tail = jedis.rpop(key, size - 1 - index)
        val removed = jedis.rpop(key)
        jedis.rpush(key, *tail.reversed().toTypedArray())
        registerModification()
        return removed
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