package me.emyar.rediscollection

import redis.clients.jedis.AbstractTransaction
import redis.clients.jedis.Response
import redis.clients.jedis.UnifiedJedis
import kotlin.time.Duration
import kotlin.time.TimeSource.Monotonic.markNow

inline fun <T> UnifiedJedis.optimisticLockTransaction(
    key: String,
    timeout: Duration,
    onSuccess: () -> Unit,
    block: (AbstractTransaction) -> Response<T>,
): T {
    val startMark = markNow()
    do {
        val result = transaction(false).use { transaction ->
            transaction.watch(key)
            transaction.multi()
            val result = block(transaction)
            transaction.exec()
                ?: return@use null
            result.get()
        }
        if (result != null) {
            onSuccess()
            return result
        }
    } while (startMark.elapsedNow() < timeout)
    throw WatchTimeoutException()
}
