package me.emyar.rediscollection

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import redis.clients.jedis.Jedis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class StringByIntRedisMapTests {

    private val key = Uuid.random().toString()

    @BeforeEach
    fun beforeEach() {
        jedis.hset(key, testMap.mapValues { (_, value) -> value.toString() })
    }

    @AfterEach
    fun afterEach() {
        jedis.del(key)
    }

    @Test
    fun size() {
        StringByIntRedisMap(jedis, key).size shouldBe testMap.size
    }

    @Test
    fun keySetContains() {
        StringByIntRedisMap(jedis, key).keys shouldContainExactly testMap.keys
    }

    @Test
    fun keySetRemove() {
        val fieldToRemove = "oranges"
        val keySet = StringByIntRedisMap(jedis, key).keys
        keySet.remove(fieldToRemove)
        val testMapMutable = testMap.toMutableMap()
        testMapMutable.remove(fieldToRemove)
        keySet shouldContainExactly testMapMutable.keys
    }

    @Test
    fun entriesContains() {
        StringByIntRedisMap(jedis, key).entries shouldContainExactly testMap.entries
    }

    @Test
    fun entriesRemove() {
        val fieldToRemove = "oranges"
        val redisMap = StringByIntRedisMap(jedis, key)
        val testMapMutable = redisMap.toMutableMap()

        redisMap.entries.remove(StringByIntRedisMap.Node(redisMap, fieldToRemove))
        testMapMutable.remove(fieldToRemove)

        redisMap shouldBe testMapMutable
    }

    @Test
    fun valuesContains() {
        StringByIntRedisMap(jedis, key).values shouldContainExactly testMap.values
    }

    @Test
    fun valuesRemove() {
        val valueToRemove = 5713
        val redisMap = StringByIntRedisMap(jedis, key)
        val testMapMutable = redisMap.toMutableMap()

        redisMap.values.remove(valueToRemove)
        testMapMutable.values.remove(valueToRemove)

        redisMap shouldBe testMapMutable
    }

    @Test
    fun containsKey() {
        StringByIntRedisMap(jedis, key).containsKey("apples") shouldBe true
    }

    @Test
    fun containsValue() {
        StringByIntRedisMap(jedis, key).containsValue(879) shouldBe true
    }

    @ParameterizedTest
    @FieldSource("testMapKeys")
    fun get(field: String) {
        val redisMap = StringByIntRedisMap(jedis, key)
        redisMap[field] shouldBe testMap[field]
    }

    @ParameterizedTest
    @FieldSource("testMapKeys")
    fun remove(field: String) {
        val redisMap = StringByIntRedisMap(jedis, key)
        val testMapMutable = redisMap.toMutableMap()

        redisMap.remove(field) shouldBe testMapMutable.remove(field)
        redisMap shouldContainExactly testMapMutable
    }

    @Test
    fun put() {
        val redisMap = StringByIntRedisMap(jedis, key)
        val testMapMutable = redisMap.toMutableMap()
        val newKey = "tangerines"
        val newValue = 389

        redisMap.put(newKey, newValue) shouldBe testMapMutable.put(newKey, newValue)
        redisMap shouldContainExactly testMapMutable
    }

    @Test
    fun putAll() {
        val redisMap = StringByIntRedisMap(jedis, key)
        val testMapMutable = redisMap.toMutableMap()
        val newValues = mapOf(
            "tangerines" to 389,
            "pears" to 831,
        )

        redisMap.putAll(newValues) shouldBe testMapMutable.putAll(newValues)
        redisMap shouldContainExactly testMapMutable
    }

    @Test
    fun clear() {
        val redisMap = StringByIntRedisMap(jedis, key)
        val testMapMutable = redisMap.toMutableMap()

        redisMap.clear()
        testMapMutable.clear()

        redisMap shouldContainExactly testMapMutable
    }

    @Test
    fun forEach() {
        StringByIntRedisMap(jedis, key).forEach { key, value ->
            value shouldBe testMap[key]
        }
    }

    companion object {
        private val jedis = Jedis(redisContainer.redisHost, redisContainer.redisPort)

        private val testMap = mapOf(
            "apples" to 879,
            "oranges" to 5713,
            "tomatoes" to 482,
        )

        @JvmStatic
        private val testMapKeys = testMap.keys

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jedis.close()
        }
    }
}