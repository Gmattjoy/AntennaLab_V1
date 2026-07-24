package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: IdentityProbeRouting.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Identity Probe

SYSTEM ROLE
Pure decision for WHICH identity-probe wire protocol to speak to the
attached analyzer.

WHY THIS EXISTS
queryAnalyzerIdentity used to branch on TRANSPORT (isUsingCdcSerialTransport),
running the LiteVNA V2 binary handshake + register reads for any CDC device.
The NanoVNA-H4 is also a CDC device but speaks an ASCII shell protocol, so it
was interrogated with binary registers it does not implement: identity failed,
the driver never resolved, and the H4 fell back to a simulated sweep
(bench 2026-07-24, doc §9b). Transport was a proxy for protocol; the selected
DriverProfile.protocolType is the real determinant of the wire format.

PURITY
Parameters in, enum out. No Compose, no Android, no session-singleton
reach-through. The blocking USB IO that consumes this decision stays in
UsbVnaCommandChannel.
########################################################################
*/

import com.example.antennalab_v1.model.DriverProtocolType

internal enum class IdentityProbeStrategy {
    /** LiteVNA V2-style binary handshake + 0xF0/0xF1 register reads. */
    LITEVNA_BINARY,

    /** NanoVNA/generic ASCII shell: "version" / "info" / "v". */
    ASCII_SHELL
}

/*
Choose the identity-probe strategy from the selected driver profile's protocol.

The `when` over DriverProtocolType is deliberately EXHAUSTIVE with no `else`: adding a
fourth protocol later becomes a compile error here, not a silent ASCII fallthrough. Only
the `protocolType == null` case (no profile selected yet) is unresolved, and it preserves
the EXISTING transport-based behaviour exactly — an unknown CDC device keeps getting the
binary probe rather than drifting silently onto the ASCII path.
*/
internal fun resolveIdentityProbeStrategy(
    protocolType: DriverProtocolType?,
    isCdcTransport: Boolean
): IdentityProbeStrategy {
    return when (protocolType) {
        DriverProtocolType.LITE_VNA_V2_STYLE ->
            IdentityProbeStrategy.LITEVNA_BINARY

        DriverProtocolType.NANO_SHELL,
        DriverProtocolType.EXPERIMENTAL_ASCII_SERIAL ->
            IdentityProbeStrategy.ASCII_SHELL

        null ->
            if (isCdcTransport) {
                IdentityProbeStrategy.LITEVNA_BINARY
            } else {
                IdentityProbeStrategy.ASCII_SHELL
            }
    }
}
