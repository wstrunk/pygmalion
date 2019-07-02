package net.rudoll.pygmalion.util

import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors

object RandomizerUtil {
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun getRandomString(length: Long): String {
        return ThreadLocalRandom.current()
                .ints(length, 0, charPool.size)
                .mapToObj { charPool[it] }.collect(Collectors.toSet())
                .joinToString("")
    }
}