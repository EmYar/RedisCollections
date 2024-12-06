package me.emyar.rediscollection

import com.redis.testcontainers.RedisContainer
import io.kotest.matchers.collections.shouldContainExactly
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import redis.clients.jedis.Jedis
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val list = listOf("Cat", "Dog", "Home", "Garage")

@Testcontainers
@OptIn(ExperimentalUuidApi::class)
class StringRedisListTests {

    @Test
    fun iterateTest() {
        val jedis = Jedis(redisContainer.redisHost, redisContainer.redisPort)
        val redisList = StringRedisList(jedis, Uuid.random().toString())
        redisList.addAll(list)
        redisList shouldContainExactly list
    }

    companion object {
        @JvmStatic
        @Container
        val redisContainer = RedisContainer(DockerImageName.parse("redis:6.2.6"))
    }
}