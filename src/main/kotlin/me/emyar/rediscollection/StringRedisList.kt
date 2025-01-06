@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package me.emyar.rediscollection

import redis.clients.jedis.UnifiedJedis
import redis.clients.jedis.params.LPosParams
import java.util.*
import kotlin.collections.Collection
import java.util.Collection as JvmCollection

class StringRedisList(
    var jedis: UnifiedJedis,
    private val key: String,
) : AbstractList<String>(), RandomAccess {

    override val size: Int
        get() = jedis.llen(key).toInt()

    override fun get(index: Int): String {
        checkBounds(index)
        return jedis.lindex(key, index.toLong())
    }

    override fun set(index: Int, element: String): String {
        val oldValueResponse = jedis.transaction(false).use { transaction ->
            transaction.watch(key)
            transaction.multi()
            val oldVal = transaction.lindex(key, index.toLong())
            transaction.lset(key, index.toLong(), element)
            transaction.exec()
            return@use oldVal
        }
        registerModification()
        return oldValueResponse.get()
    }

    override fun add(element: String): Boolean {
        jedis.rpush(key, element)
        registerModification()
        return true
    }

    override fun add(index: Int, element: String) {
        addAtPosition(index, listOf(element))
    }

    override fun addAll(elements: Collection<String>): Boolean {
        registerModification()
        jedis.rpush(key, *elements.toTypedArray())
        return true
    }

    override fun addAll(index: Int, elements: Collection<String>): Boolean {
        addAtPosition(index, elements)
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

    private fun addAtPosition(index: Int, elements: Collection<String>) {
        rangeCheckForAdd(index)
        val size = size
        when {
            index == 0 -> jedis.lpush(key, *elements.reversed().toTypedArray())
            index == size -> addAll(elements)
            index <= size / 2 -> addLeft(index, elements)
            else -> addRight(index, elements)
        }
        registerModification()
    }

    private fun addLeft(index: Int, elements: Collection<String>) {
        val head = jedis.lpop(key, index)
        jedis.lpush(key, *elements.reversed().toTypedArray())
        jedis.lpush(key, *head.reversed().toTypedArray())
    }

    private fun addRight(index: Int, elements: Collection<String>) {
        val tail = jedis.rpop(key, size - index).reversed()
        jedis.rpush(key, *elements.toTypedArray())
        jedis.rpush(key, *tail.toTypedArray())
    }

    private fun removeLeft(index: Int): String {
        val head = jedis.lpop(key, index)
        val removed = jedis.lpop(key)
        jedis.lpush(key, *head.reversed().toTypedArray())
        return removed
    }

    private fun removeRight(index: Int): String {
        val tail = jedis.rpop(key, size - 1 - index).reversed()
        val removed = jedis.rpop(key)
        jedis.rpush(key, *tail.toTypedArray())
        return removed
    }

    private fun checkBounds(index: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    private fun rangeCheckForAdd(index: Int) {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
    }

    private fun registerModification() {
        modCount++
    }
}