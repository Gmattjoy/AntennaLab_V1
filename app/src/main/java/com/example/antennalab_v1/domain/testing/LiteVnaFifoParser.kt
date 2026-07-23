package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: LiteVnaFifoParser.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / LiteVNA FIFO record decode

SYSTEM ROLE
Pure, device-free decode of the LiteVNA valuesFIFO byte stream into
records, plus the record-selection and frequency-index analysis used by
LiteVnaSweepProtocol. Extracted so the raw-bytes → SweepPoints path can be
reproduced and asserted in plain JVM tests (feed a captured FIFO payload,
observe how many of N requested points survive and WHY).

Record format (32 bytes, little-endian) — NanoVNA-V2 / LiteVNA valuesFIFO:
  fwd0Re i32 @0x00  fwd0Im i32 @0x04
  rev0Re i32 @0x08  rev0Im i32 @0x0C
  rev1Re i32 @0x10  rev1Im i32 @0x14
  freqIndex u16 @0x18   (bytes 0x1A..0x1F reserved)

The blocking USB IO that produces these bytes stays in UsbVnaCommandChannel
/ UsbCdcSerialChannel; only this decode/selection/analysis is here.
########################################################################
*/

internal const val LITEVNA_FIFO_RECORD_SIZE = 32

internal data class LiteVnaFifoRecord(
    val fwd0Re: Int,
    val fwd0Im: Int,
    val rev0Re: Int,
    val rev0Im: Int,
    val rev1Re: Int,
    val rev1Im: Int,
    val freqIndex: Int
)

/*
Diagnostic breakdown of the freqIndex field across the decoded records — the
"exact reason" the 101→N drop happens. duplicateInRangeCount + outOfRangeCount is
the number of records the direct-index selection discards.
*/
internal data class FreqIndexAnalysis(
    val decodedCount: Int,
    val inRangeCount: Int,
    val outOfRangeCount: Int,
    val distinctInRangeCount: Int,
    val duplicateInRangeCount: Int,
    val minIndex: Int?,
    val maxIndex: Int?,
    val sequencePreview: String
)

internal fun parseLiteVnaFifoRecords(rawBytes: ByteArray): List<LiteVnaFifoRecord> {
    val completeRecordCount = rawBytes.size / LITEVNA_FIFO_RECORD_SIZE
    if (completeRecordCount <= 0) {
        return emptyList()
    }

    return buildList {
        for (recordIndex in 0 until completeRecordCount) {
            val start = recordIndex * LITEVNA_FIFO_RECORD_SIZE
            add(
                LiteVnaFifoRecord(
                    fwd0Re = decodeLittleEndianInt32(rawBytes, start + 0x00),
                    fwd0Im = decodeLittleEndianInt32(rawBytes, start + 0x04),
                    rev0Re = decodeLittleEndianInt32(rawBytes, start + 0x08),
                    rev0Im = decodeLittleEndianInt32(rawBytes, start + 0x0C),
                    rev1Re = decodeLittleEndianInt32(rawBytes, start + 0x10),
                    rev1Im = decodeLittleEndianInt32(rawBytes, start + 0x14),
                    freqIndex = decodeLittleEndianUInt16(rawBytes, start + 0x18)
                )
            )
        }
    }
}

// The current selection: in-range freqIndex, deduped by freqIndex, ascending.
internal fun selectDirectRecords(
    records: List<LiteVnaFifoRecord>,
    requestedPointCount: Int
): List<LiteVnaFifoRecord> {
    return records
        .filter { it.freqIndex in 0 until requestedPointCount }
        .distinctBy { it.freqIndex }
        .sortedBy { it.freqIndex }
}

internal fun analyzeFreqIndices(
    records: List<LiteVnaFifoRecord>,
    requestedPointCount: Int
): FreqIndexAnalysis {
    val indices = records.map { it.freqIndex }
    val inRange = indices.filter { it in 0 until requestedPointCount }
    val distinctInRange = inRange.distinct()

    return FreqIndexAnalysis(
        decodedCount = records.size,
        inRangeCount = inRange.size,
        outOfRangeCount = indices.size - inRange.size,
        distinctInRangeCount = distinctInRange.size,
        duplicateInRangeCount = inRange.size - distinctInRange.size,
        minIndex = indices.minOrNull(),
        maxIndex = indices.maxOrNull(),
        sequencePreview = indices.take(32).joinToString(prefix = "[", postfix = "]")
    )
}

/*
Accumulate/dedup/complete state for the sweep read. The LiteVNA v0.3.3 ignores the
USB sweepPoints register and free-runs ~201 points, so a single drain gets only a
scattered subset of the target 0..(pointCount-1) indices. We therefore reconstruct
the sweep by collecting DISTINCT in-range freqIndex across many small reads: the read
loop keeps going until every 0..(pointCount-1) index has been seen (complete) or the
wall-clock expires (honest partial — report the actual count, never claim complete).

Pure and deterministic: the read cadence/jitter and wall-clock live in the caller;
this only tracks which target indices have been observed.
*/
internal class DistinctInRangeAccumulator(
    val pointCount: Int
) {
    private val seenInRange = HashSet<Int>()

    fun addFreqIndex(freqIndex: Int) {
        if (freqIndex in 0 until pointCount) {
            seenInRange.add(freqIndex)
        }
    }

    fun addRecords(records: List<LiteVnaFifoRecord>) {
        records.forEach { addFreqIndex(it.freqIndex) }
    }

    val distinctInRangeCount: Int
        get() = seenInRange.size

    val isComplete: Boolean
        get() = seenInRange.size >= pointCount

    // Target indices not yet observed (the "starved" indices when a sweep times out).
    fun missingIndices(): List<Int> = (0 until pointCount).filter { it !in seenInRange }
}

internal fun decodeLittleEndianInt32(bytes: ByteArray, startIndex: Int): Int {
    if (startIndex + 3 >= bytes.size) return 0
    val b0 = bytes[startIndex].toUByte().toInt()
    val b1 = bytes[startIndex + 1].toUByte().toInt()
    val b2 = bytes[startIndex + 2].toUByte().toInt()
    val b3 = bytes[startIndex + 3].toUByte().toInt()
    return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}

internal fun decodeLittleEndianUInt16(bytes: ByteArray, startIndex: Int): Int {
    if (startIndex + 1 >= bytes.size) return 0
    val b0 = bytes[startIndex].toUByte().toInt()
    val b1 = bytes[startIndex + 1].toUByte().toInt()
    return b0 or (b1 shl 8)
}
