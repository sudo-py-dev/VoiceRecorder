package com.voicerecorder.data.util

import java.io.RandomAccessFile

/**
 * Utility to write WAV header for PCM data.
 * WAV Header structure:
 * - ChunkID (4 bytes): "RIFF"
 * - ChunkSize (4 bytes): 36 + SubChunk2Size
 * - Format (4 bytes): "WAVE"
 * - SubChunk1ID (4 bytes): "fmt "
 * - SubChunk1Size (4 bytes): 16 (for PCM)
 * - AudioFormat (2 bytes): 1 (for PCM)
 * - NumChannels (2 bytes): 1 (Mono)
 * - SampleRate (4 bytes): e.g. 44100
 * - ByteRate (4 bytes): SampleRate * NumChannels * BitsPerSample/8
 * - BlockAlign (2 bytes): NumChannels * BitsPerSample/8
 * - BitsPerSample (2 bytes): 16
 * - SubChunk2ID (4 bytes): "data"
 * - SubChunk2Size (4 bytes): numSamples * NumChannels * BitsPerSample/8
 */
object WavHeaderWriter {

    fun writeHeader(
        file: RandomAccessFile,
        sampleRate: Int,
        numChannels: Int,
        bitsPerSample: Int,
        dataLength: Long
    ) {
        file.seek(0)
        
        val byteRate = (sampleRate * numChannels * bitsPerSample / 8).toLong()
        val blockAlign = (numChannels * bitsPerSample / 8).toShort()

        // RIFF header
        file.writeBytes("RIFF")
        file.writeInt(Integer.reverseBytes((36 + dataLength).toInt())) // ChunkSize
        file.writeBytes("WAVE")

        // fmt subchunk
        file.writeBytes("fmt ")
        file.writeInt(Integer.reverseBytes(16)) // SubChunk1Size
        file.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat (PCM = 1)
        file.writeShort(java.lang.Short.reverseBytes(numChannels.toShort()).toInt())
        file.writeInt(Integer.reverseBytes(sampleRate))
        file.writeInt(Integer.reverseBytes(byteRate.toInt()))
        file.writeShort(java.lang.Short.reverseBytes(blockAlign).toInt())
        file.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())

        // data subchunk
        file.writeBytes("data")
        file.writeInt(Integer.reverseBytes(dataLength.toInt())) // SubChunk2Size
    }
}
