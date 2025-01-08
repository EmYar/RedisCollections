package me.emyar.rediscollection

import org.intellij.lang.annotations.Language

object InsertElementsToListInPositionScript {
    @Language("Lua")
    val text = """
        local listKey = KEYS[1]
        local index = tonumber(table.remove(ARGV, 1))

        local listLength = redis.call("LLEN", listKey)
        if index < 1 then
            return "${Result.INDEX_OUT_OF_BOUNDS}"
        elseif index > listLength then
            return "${Result.INDEX_OUT_OF_BOUNDS}"
        end

        local tempKey = listKey .. ":temp"

        if index < (listLength / 2) then
            for i = 1, index - 1 do
                local element = redis.call("LPOP", listKey)
                redis.call("RPUSH", tempKey, element)
            end

            for i = #ARGV, 1, -1 do
                redis.call("LPUSH", listKey, ARGV[i])
            end

            for i = 1, redis.call("LLEN", tempKey) do
                local element = redis.call("RPOP", tempKey)
                redis.call("LPUSH", listKey, element)
            end
        else
            for i = 1, listLength - index + 1 do
                local element = redis.call("RPOP", listKey)
                redis.call("LPUSH", tempKey, element)
            end

            for i = 1, #ARGV do
                redis.call("RPUSH", listKey, ARGV[i])
            end

            for i = 1, redis.call("LLEN", tempKey) do
                local element = redis.call("LPOP", tempKey)
                redis.call("RPUSH", listKey, element)
            end
        end

        redis.call("DEL", tempKey)

        return "${Result.SUCCESS}"
    """.trimIndent()

    enum class Result { SUCCESS, INDEX_OUT_OF_BOUNDS }
}

