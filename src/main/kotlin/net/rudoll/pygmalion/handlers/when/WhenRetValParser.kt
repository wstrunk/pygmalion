package net.rudoll.pygmalion.handlers.`when`

import net.rudoll.pygmalion.handlers.`when`.dynamicretval.DynamicRetVal
import net.rudoll.pygmalion.handlers.`when`.dynamicretval.EmptyRetVal
import net.rudoll.pygmalion.handlers.`when`.dynamicretval.FileRetVal
import net.rudoll.pygmalion.handlers.`when`.dynamicretval.PatternRetVal
import net.rudoll.pygmalion.model.Input
import java.io.File

object WhenRetValParser {
    fun parseRetVal(input: Input): DynamicRetVal {
        return when (input.first()) {
            "status" -> getEmptyRetValWithStatus(input)
            "from" -> getRetValFromFile(input)
            else -> getRetValFromPattern(input)
        }
    }

    private fun getEmptyRetValWithStatus(input: Input): DynamicRetVal {
        val status = input.second()
        input.consume(2)
        return EmptyRetVal(status.toInt())
    }

    private fun getRetValFromPattern(input: Input): DynamicRetVal {
        val retVal = input.first()
        input.consume(1)
        return PatternRetVal(retVal)
    }

    private fun getRetValFromFile(input: Input): DynamicRetVal {
        val file = File(input.second())
        input.consume(2)
        return FileRetVal(file)
    }
}