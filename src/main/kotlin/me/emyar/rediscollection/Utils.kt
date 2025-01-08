package me.emyar.rediscollection

import redis.clients.jedis.AbstractTransaction
import redis.clients.jedis.Response
import redis.clients.jedis.UnifiedJedis
import kotlin.time.Duration
import kotlin.time.TimeSource.Monotonic.markNow

inline fun <T> UnifiedJedis.optimisticLockTransaction(
    key: String,
    timeout: Duration,
    block: (AbstractTransaction) -> Response<T>,
): T {
    val startMark = markNow()
    do {
        transaction(false).use { transaction ->
            transaction.watch(key)
            transaction.multi()
            val result = block(transaction)
            if (transaction.exec() != null) {
                return result.get()
            }
        }
    } while (startMark.elapsedNow() < timeout)
    throw WatchTimeoutException()
}
