package com.example.esdl_term_project

data class GameStatus(
    val score: Int,
    val time: Int
)

object MessageParser {
    /**
     * Parses a STATUS message from the STM32.
     * Expected format: "STATUS,score=20,time=3"
     * Returns null if format is invalid or not a STATUS message.
     */
    fun parseStatus(message: String): GameStatus? {
        if (!message.startsWith("STATUS")) return null

        try {
            // Example: STATUS,score=20,time=3
            val parts = message.split(",")
            // parts[0] = "STATUS"
            // parts[1] = "score=20"
            // parts[2] = "time=3"

            var score = 0
            var time = 0

            for (part in parts) {
                if (part.startsWith("score=")) {
                    score = part.substringAfter("score=").toInt()
                } else if (part.startsWith("time=")) {
                    time = part.substringAfter("time=").toInt()
                }
            }

            return GameStatus(score, time)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
