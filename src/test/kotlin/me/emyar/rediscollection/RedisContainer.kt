package me.emyar.rediscollection

import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName

val redisContainer = RedisContainer(DockerImageName.parse("redis:7.4.2"))
    .apply { start() }