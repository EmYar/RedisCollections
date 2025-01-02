package me.emyar.rediscollection

import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName

val redisContainer = RedisContainer(DockerImageName.parse("redis:6.2.6"))
    .apply { start() }