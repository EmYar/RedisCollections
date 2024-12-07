package me.emyar.rediscollection

import com.redis.testcontainers.RedisContainer
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import redis.clients.jedis.Jedis
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Testcontainers
@OptIn(ExperimentalUuidApi::class)
class StringRedisListTests {

    private val key = Uuid.random().toString()

    @BeforeEach
    fun beforeEach() {
        jedis.rpush(key, *testList.toTypedArray())
    }

    @AfterEach
    fun afterEach() {
        jedis.del(key)
    }

    @Test
    fun iterate() {
        val redisList = StringRedisList(jedis, key)
        redisList shouldContainExactly testList
    }

    companion object {
        @JvmStatic
        @Container
        private val redisContainer = RedisContainer(DockerImageName.parse("redis:6.2.6"))

        private lateinit var jedis: Jedis

        private val testList = listOf("Cat", "Dog", "Home", "Garage")

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            jedis = Jedis(redisContainer.redisHost, redisContainer.redisPort)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jedis.close()
        }
    }
}