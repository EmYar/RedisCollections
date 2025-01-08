@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@file:OptIn(ExperimentalUuidApi::class)

package me.emyar.rediscollection

import redis.clients.jedis.UnifiedJedis
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.params.LPosParams
import java.util.*
import kotlin.collections.Collection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import java.util.Collection as JvmCollection

class StringRedisList(
    var jedis: UnifiedJedis,
    private val key: String,
    private val optLockRetriesTimeout: Duration = 10.seconds
) : AbstractList<String>(), RandomAccess {

    override val size: Int
        get() = jedis.llen(key).toInt()

    override fun get(index: Int): String =
        jedis.lindex(key, index.toLong())
            ?: throw IndexOutOfBoundsException("Index $index is out of bounds")

    override fun set(index: Int, element: String): String {
        registerModification()
        return jedis.optimisticLockTransaction(key, optLockRetriesTimeout) { t ->
            val oldValue = t.lindex(key, index.toLong())
                ?: throw IndexOutOfBoundsException("Index $index is out of bounds")
            t.lset(key, index.toLong(), element)
            oldValue
        }
    }

    override fun add(element: String): Boolean {
        registerModification()
        jedis.rpush(key, element)
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
        val removedReplacer = Uuid.random().toString()
        return checkOutOfBounds(index) {
            registerModification()
            jedis.optimisticLockTransaction(key, optLockRetriesTimeout) { t ->
                t.lset(key, index.toLong(), removedReplacer)
                    .also { t.lrem(key, 1L, removedReplacer) }
            }
        }
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
        registerModification()
        val result = jedis.eval(
            InsertElementsToListInPositionScript.text,
            listOf(key),
            listOf((index + 1).toString()) + elements,
        ).let { resultObj ->
            InsertElementsToListInPositionScript.Result.valueOf(resultObj as String)
        }
        if (result === InsertElementsToListInPositionScript.Result.INDEX_OUT_OF_BOUNDS) {
            throw IndexOutOfBoundsException("Index $index is out of bounds")
        }
    }

    private fun registerModification() {
        modCount++
    }
}

private inline fun <T> checkOutOfBounds(index: Int, block: () -> T): T =
    try {
        block()
    } catch (e: JedisDataException) {
        if (e.isIndexOutOfBounds()) {
            throw IndexOutOfBoundsException("Index $index out of range")
        } else {
            throw e
        }
    }

private fun JedisDataException.isIndexOutOfBounds() =
    message == "ERR index out of range"