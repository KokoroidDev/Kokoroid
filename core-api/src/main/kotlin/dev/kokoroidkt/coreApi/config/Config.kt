package dev.kokoroidkt.coreApi.config

import java.nio.file.Path

val kokoroidConfigRoot: Path = Path.of("kokoroid", "config")

inline fun <reified T : Any> encodeDataToString(data: T): String = encodeHoconToString<T>(data)

/**
 * 在/config目录下生成配置文件
 * 不建议直接使用，建议使用对应拓展的拓展方法
 *
 * @param T
 * @param data
 * @param path
 */
inline fun <reified T : Any> encodeDataToPath(
    data: T,
    path: Path,
) {
    val target = kokoroidConfigRoot.resolve(path).normalize().toFile()
    val hoconStr = encodeHoconToString<T>(data)
    target.parentFile?.mkdirs()
    target.writeText(hoconStr)
}

/**
 * 从指定文件读取配置文件
 * 不建议直接使用，建议使用对应拓展的拓展方法
 *
 * @param T
 * @param path
 * @return
 */
inline fun <reified T : Any> decodeDataFromPath(path: Path): T = decodeHoconFile<T>(kokoroidConfigRoot.resolve(path).normalize().toFile())
