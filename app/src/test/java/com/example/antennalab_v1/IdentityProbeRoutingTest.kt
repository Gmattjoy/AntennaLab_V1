package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.IdentityProbeStrategy
import com.example.antennalab_v1.domain.testing.resolveIdentityProbeStrategy
import com.example.antennalab_v1.model.DriverProtocolType
import org.junit.Assert.assertEquals
import org.junit.Test

/*
Pure coverage for the identity-probe routing decision (doc §9b).

The full before/after matrix from the plan. The load-bearing rows are named for what
they protect: the LiteVNA regression guard and the H4 fix.
*/
class IdentityProbeRoutingTest {

    private fun strategy(protocol: DriverProtocolType?, isCdc: Boolean) =
        resolveIdentityProbeStrategy(protocolType = protocol, isCdcTransport = isCdc)

    // REGRESSION GUARD: a real LiteVNA is always CDC. Old condition (isCdc) was true;
    // new condition must also resolve to the binary path, reaching the same unmodified
    // branch. This is the "provably unaffected" requirement.
    @Test
    fun liteVnaOverCdc_stillTakesBinaryPath() {
        assertEquals(
            IdentityProbeStrategy.LITEVNA_BINARY,
            strategy(DriverProtocolType.LITE_VNA_V2_STYLE, isCdc = true)
        )
    }

    // THE FIX: the H4 is a CDC device speaking ASCII shell. Old code fed it binary
    // registers and identity failed; now it takes the ASCII path.
    @Test
    fun nanoShellOverCdc_takesAsciiPath() {
        assertEquals(
            IdentityProbeStrategy.ASCII_SHELL,
            strategy(DriverProtocolType.NANO_SHELL, isCdc = true)
        )
    }

    // Unknown device (no profile selected) must NOT drift onto ASCII — it keeps the
    // exact transport-based behaviour that shipped.
    @Test
    fun nullProfileOverCdc_keepsLegacyBinaryBehaviour() {
        assertEquals(
            IdentityProbeStrategy.LITEVNA_BINARY,
            strategy(null, isCdc = true)
        )
    }

    @Test
    fun nullProfileOverNonCdc_keepsLegacyAsciiBehaviour() {
        assertEquals(
            IdentityProbeStrategy.ASCII_SHELL,
            strategy(null, isCdc = false)
        )
    }

    // A NanoVNA over a non-CDC transport was already ASCII and stays ASCII.
    @Test
    fun nanoShellOverNonCdc_takesAsciiPath() {
        assertEquals(
            IdentityProbeStrategy.ASCII_SHELL,
            strategy(DriverProtocolType.NANO_SHELL, isCdc = false)
        )
    }

    // Protocol wins over transport: a LiteVNA profile always gets the binary probe,
    // even on a (non-functional) non-CDC transport - it fails either way, but now with
    // the correct protocol attempted.
    @Test
    fun liteVnaOverNonCdc_takesBinaryPathOnProtocol() {
        assertEquals(
            IdentityProbeStrategy.LITEVNA_BINARY,
            strategy(DriverProtocolType.LITE_VNA_V2_STYLE, isCdc = false)
        )
    }

    // A profile explicitly named ASCII serial must not receive binary register probes,
    // regardless of transport. (No hardware has exercised this path.)
    @Test
    fun experimentalAsciiSerial_alwaysTakesAsciiPath() {
        assertEquals(
            IdentityProbeStrategy.ASCII_SHELL,
            strategy(DriverProtocolType.EXPERIMENTAL_ASCII_SERIAL, isCdc = true)
        )
        assertEquals(
            IdentityProbeStrategy.ASCII_SHELL,
            strategy(DriverProtocolType.EXPERIMENTAL_ASCII_SERIAL, isCdc = false)
        )
    }

    // Transport is irrelevant whenever a protocol IS known - the whole point of the fix.
    @Test
    fun knownProtocol_ignoresTransport() {
        assertEquals(
            strategy(DriverProtocolType.NANO_SHELL, isCdc = true),
            strategy(DriverProtocolType.NANO_SHELL, isCdc = false)
        )
        assertEquals(
            strategy(DriverProtocolType.LITE_VNA_V2_STYLE, isCdc = true),
            strategy(DriverProtocolType.LITE_VNA_V2_STYLE, isCdc = false)
        )
    }
}
