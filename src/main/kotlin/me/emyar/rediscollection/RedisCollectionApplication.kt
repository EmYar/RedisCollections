package me.emyar.rediscollection

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RedisCollectionApplication

fun main(args: Array<String>) {
    runApplication<RedisCollectionApplication>(*args)
}
