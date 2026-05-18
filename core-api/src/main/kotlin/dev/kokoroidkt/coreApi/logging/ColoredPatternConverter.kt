// SPDX-FileCopyrightText: 2026 Kokoroid Contributors

// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.coreApi.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter
import java.text.SimpleDateFormat
import java.util.*

class ColoredPatternConverter : CompositeConverter<ILoggingEvent>() {
    private val timeFormat = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")

    override fun transform(
        event: ILoggingEvent,
        input: String,
    ): String {
        val timeColor = "\u001B[0;37m" // 淡蓝色
        val messageColor = "\u001B[0;37m" // 白色
        val resetColor = "\u001B[0m"

        val levelColor =
            when (event.level.toInt()) {
                Level.DEBUG_INT -> "\u001B[0;92m"

                // 浅绿色
                Level.INFO_INT -> "\u001B[0;96m"

                // 淡蓝色
                Level.WARN_INT -> "\u001B[0;33m"

                // 橙色
                Level.ERROR_INT -> "\u001B[0;91m"

                // 红色
                else -> resetColor
            }

        val time = timeFormat.format(Date(event.timeStamp))
        val level = event.level.levelStr.padEnd(5, ' ')

        return "[$timeColor$time$resetColor][$levelColor$level$resetColor]: $messageColor${event.formattedMessage}$resetColor"
    }
}
