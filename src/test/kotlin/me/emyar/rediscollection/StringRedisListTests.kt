package me.emyar.rediscollection

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.*
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import redis.clients.jedis.UnifiedJedis
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    @ParameterizedTest
    @FieldSource("testList")
    fun addAllAtPosition(element: String) {
        val position = testList.indexOf(element)
        val redisList = StringRedisList(jedis, key)
        val newElements = listOf("newElement", "anotherElement")
        redisList.addAll(position, newElements)

        redisList shouldContainExactly testList.toMutableList().apply { addAll(position, newElements) }
    }

    @ParameterizedTest
    @FieldSource("indexesOutOfBounds")
    fun addAllAtPositionIOB(position: Int) {
        shouldThrow<IndexOutOfBoundsException> {
            StringRedisList(jedis, key).add(position, "newElement")
        }
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
    fun notContains() {
        StringRedisList(jedis, key) shouldNotContain null
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
        private val jedis = UnifiedJedis("redis://${redisContainer.redisHost}:${redisContainer.redisPort}")

        @JvmStatic
        private val testList = listOf("Cat", "Dog", "Home", "Garage", "Lawn", "Fence", "Roof")

        @JvmStatic
        private val indexesOutOfBounds = listOf(-1, testList.size)

        @JvmStatic
        @AfterAll
        fun afterAll() {
            jedis.close()
        }
    }
}