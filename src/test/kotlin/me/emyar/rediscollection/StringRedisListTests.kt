package me.emyar.rediscollection

import com.redis.testcontainers.RedisContainer
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
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

    @ParameterizedTest
    @FieldSource("testList")
    fun contains(element: String) {
        StringRedisList(jedis, key) shouldContain element
    }

    @Test
    fun isEmpty() {
        val emptyListKey = Uuid.random().toString()
        StringRedisList(jedis, emptyListKey).isEmpty() shouldBe true
    }

    @Test
    fun isNotEmpty() {
        StringRedisList(jedis, key).isEmpty() shouldBe false
    }

    @Test
    fun iterator() {
        StringRedisList(jedis, key) shouldContainExactly testList
    }

    @Test
    fun size() {
        StringRedisList(jedis, key) shouldHaveSize testList.size
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun get(element: String) {
        val elementIndex = testList.indexOf(element)
        StringRedisList(jedis, key)[elementIndex] shouldBe element
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun indexOf(element: String) {
        StringRedisList(jedis, key).indexOf(element) shouldBe testList.indexOf(element)
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun lastIndexOf(element: String) {
        val key = Uuid.random().toString()
        val list = testList + element
        jedis.rpush(key, *list.toTypedArray())
        StringRedisList(jedis, key).lastIndexOf(element) shouldBe list.lastIndexOf(element)
    }

    companion object {
        @JvmStatic
        @Container
        private val redisContainer = RedisContainer(DockerImageName.parse("redis:6.2.6"))

        private lateinit var jedis: Jedis

        @JvmStatic
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