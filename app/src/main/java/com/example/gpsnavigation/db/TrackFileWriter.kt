package com.example.gpsnavigation.db

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class TrackFileWriter(private val file: File) {
    private var writer: BufferedWriter? = null

    fun open() {
        file.parentFile?.mkdirs()
        writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8))
    }

    fun appendPoint(timeMillis: Long, lat: Double, lng: Double, speedKmh: Float?) {
        val line = buildString {
            append("""{"t":$timeMillis,"lat":$lat,"lng":$lng""")
            if (speedKmh != null) append(""","spd":$speedKmh""")
            append("}\n")
        }
        writer?.write(line)
    }

    fun flush() = writer?.flush()
    fun close() = writer?.close()
}
