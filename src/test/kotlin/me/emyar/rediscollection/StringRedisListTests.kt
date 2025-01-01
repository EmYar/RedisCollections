package me.emyar.rediscollection

import com.redis.testcontainers.RedisContainer
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
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

    @Test
    fun set() {
        val expectedList = testList.toMutableList().also { it[1] = "newElement" }
        val redisList = StringRedisList(jedis, key).also { it[1] = "newElement" }
        redisList shouldBe expectedList
    }

    @Test
    fun add() {
        val redisList = StringRedisList(jedis, key)
        val newElement = "newElement"
        redisList.add(newElement)

        redisList shouldContainExactly testList + newElement
    }

    @Test
    fun addAll() {
        val redisList = StringRedisList(jedis, key)
        val newElements = listOf("newElement", "anotherElement")
        redisList.addAll(newElements)

        redisList shouldContainExactly testList.toMutableList().apply { addAll(newElements) }
    }

    @Test
    fun addAllAtPosition() {
        val redisList = StringRedisList(jedis, key)
        val newElements = listOf("newElement", "anotherElement")
        redisList.addAll(1, newElements)

        redisList shouldContainExactly testList.toMutableList().apply { addAll(1, newElements) }
    }

    @Test
    fun addFirst() {
        val redisList = StringRedisList(jedis, key)
        val newElement = "newElement"
        redisList.addFirst(newElement)

        redisList shouldContainExactly testList.toMutableList().apply { addFirst(newElement) }
    }

    @Test
    fun addLast() {
        val redisList = StringRedisList(jedis, key)
        val newElement = "newElement"
        redisList.addLast(newElement)

        redisList shouldContainExactly testList.toMutableList().apply { addLast(newElement) }
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun remove(element: String) {
        val redisList = StringRedisList(jedis, key)
        val testList = testList.toMutableList()
        redisList.remove(element) shouldBe testList.remove(element)
        redisList shouldContainExactly testList
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun removeAt(element: String) {
        val indexToRemove = testList.indexOf(element)
        val redisList = StringRedisList(jedis, key)
        redisList.removeAt(indexToRemove)
        redisList shouldContainExactly testList.toMutableList().apply { removeAt(indexToRemove) }
    }

    @Test
    fun removeAll() {
        val redisList = StringRedisList(jedis, key)
        val elementsToRemove = testList.subList(0, 2).toSet()
        redisList.removeAll(elementsToRemove)
        redisList shouldContainExactly testList.toMutableList().apply { removeAll(elementsToRemove) }
    }

    @Test
    fun removeFirst() {
        val redisList = StringRedisList(jedis, key)
        redisList.removeFirst()
        redisList shouldContainExactly testList.toMutableList().apply { removeFirst() }
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun removeIf(element: String) {
        val redisList = StringRedisList(jedis, key)
        redisList.removeIf { it == element }
        redisList shouldContainExactly testList.toMutableList().apply { removeIf { it == element } }
    }

    @Test
    fun removeLast() {
        val redisList = StringRedisList(jedis, key)
        redisList.removeLast()
        redisList shouldContainExactly testList.toMutableList().apply { removeLast() }
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun indexOf(element: String) {
        StringRedisList(jedis, key).indexOf(element) shouldBe testList.indexOf(element)
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun contains(element: String) {
        StringRedisList(jedis, key) shouldContain element
    }

    @Test
    fun containsAll() {
        StringRedisList(jedis, key).containsAll(testList) shouldBe true
    }

    @ParameterizedTest
    @FieldSource("testList")
    fun lastIndexOf(element: String) {
        val key = Uuid.random().toString()
        val list = testList + element
        jedis.rpush(key, *list.toTypedArray())
        StringRedisList(jedis, key).lastIndexOf(element) shouldBe list.lastIndexOf(element)
    }

    @Test
    fun clear() {
        val redisList = StringRedisList(jedis, key)
        redisList.clear()

        redisList.shouldBeEmpty()
    }

    @Test
    fun equals() {
        val firstList = StringRedisList(jedis, key)

        val secondListKey = Uuid.random().toString()
        jedis.rpush(secondListKey, *firstList.toTypedArray())
        val secondList = StringRedisList(jedis, secondListKey)

        (firstList == secondList) shouldBe true
    }

    @Test
    fun forEach() {
        val firstList = StringRedisList(jedis, key)

        val secondListKey = Uuid.random().toString()
        jedis.rpush(secondListKey, *firstList.toTypedArray())
        val secondList = StringRedisList(jedis, secondListKey)

        firstList.forEachIndexed { index, element ->
            element shouldBeEqual secondList[index]
        }
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

    companion object {
        @JvmStatic
        @Container
        private val redisContainer = RedisContainer(DockerImageName.parse("redis:6.2.6"))

        private lateinit var jedis: Jedis

        @JvmStatic
        private val testList = listOf("Cat", "Dog", "Home", "Garage", "Lawn")

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