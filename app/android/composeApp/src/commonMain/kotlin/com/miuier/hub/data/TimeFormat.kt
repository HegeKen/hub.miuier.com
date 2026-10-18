package com.miuier.hub.data

/**
 * 把接口的生成时间转成 **GMT+8** 的展示文本。
 *
 * 导出脚本已统一输出北京时间：`2026-10-18T21:41:54.111+08:00`，直接取墙钟即可。
 * 旧数据可能是 UTC：`2026-09-20T14:41:54.111Z`，需要「加 8 小时 + 处理进位」。
 *
 * 这里只做这点算术，不引入 kotlinx-datetime：偏移是固定的（东八区没有夏令时），
 * 输入格式也由我们自己的导出脚本决定。解析不出来就原样返回，界面不会因为一个脏字段变成空白。
 */
fun utcToGmt8(iso: String): String {
    val text = iso.trim()
    // 已经是北京时间（+08:00），直接取墙钟
    BEIJING.matchEntire(text)?.let { m ->
        return "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]} " +
            "${m.groupValues[4]}:${m.groupValues[5]}:${m.groupValues[6]}"
    }
    val m = ISO_UTC.matchEntire(text) ?: return iso
    var year = m.groupValues[1].toIntOrNull() ?: return iso
    var month = m.groupValues[2].toIntOrNull() ?: return iso
    var day = m.groupValues[3].toIntOrNull() ?: return iso
    val hour = m.groupValues[4].toIntOrNull() ?: return iso
    val minute = m.groupValues[5]
    val second = m.groupValues[6]

    var shiftedHour = hour + 8
    if (shiftedHour >= 24) {
        // 跨日（跨月、跨年同理），日历只用到平年 / 闰年这一个规则
        shiftedHour -= 24
        day += 1
        if (day > daysInMonth(year, month)) {
            day = 1
            month += 1
            if (month > 12) {
                month = 1
                year += 1
            }
        }
    }

    return "${pad(year, 4)}-${pad(month, 2)}-${pad(day, 2)} " +
        "${pad(shiftedHour, 2)}:$minute:$second"
}

/** `2026-10-18T21:41:54.111+08:00`，已是北京时间，直接取墙钟 */
private val BEIJING = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?\+08:00$""")

/** `2026-09-20T14:41:54.111Z`，UTC，需要 +8 小时 */
private val ISO_UTC = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?Z$""")

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 31
}

/** 格里高利历：4 年一闰、100 年不闰、400 年再闰 */
private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

/** `String.format` 是 JVM 专有的，commonMain 里只能自己补零 */
private fun pad(value: Int, width: Int): String = value.toString().padStart(width, '0')
