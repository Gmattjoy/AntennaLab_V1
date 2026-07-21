package com.example.antennalab_v1.features.lab

data class LabTestTemplate(
    val id: String,
    val displayName: String,
    val bandLabel: String,
    val antennaTypeKey: String,
    val targetFrequencyMHz: Double,
    val summary: String
)

object LabTestTemplates {

    val knownAntennaTemplates: List<LabTestTemplate> = listOf(
        LabTestTemplate(
            id = "yagi_hf_20m",
            displayName = "Yagi HF",
            bandLabel = "20 m / 14.2 MHz",
            antennaTypeKey = "YAGI",
            targetFrequencyMHz = 14.2,
            summary = "Known-band Yagi test preset for HF work around 20 m."
        ),
        LabTestTemplate(
            id = "dipole_hf_40m",
            displayName = "Dipole HF",
            bandLabel = "40 m / 7.1 MHz",
            antennaTypeKey = "DIPOLE",
            targetFrequencyMHz = 7.1,
            summary = "Known-band Dipole test preset for HF work around 40 m."
        ),
        LabTestTemplate(
            id = "dipole_vhf_2m",
            displayName = "Dipole VHF",
            bandLabel = "2 m / 146.0 MHz",
            antennaTypeKey = "DIPOLE",
            targetFrequencyMHz = 146.0,
            summary = "Known-band Dipole test preset for VHF work around 2 m."
        ),
        LabTestTemplate(
            id = "yagi_uhf_70cm",
            displayName = "Yagi UHF",
            bandLabel = "70 cm / 433.0 MHz",
            antennaTypeKey = "YAGI",
            targetFrequencyMHz = 433.0,
            summary = "Known-band Yagi test preset for UHF work around 70 cm."
        )
    )

    fun getTemplateById(id: String): LabTestTemplate? {
        return knownAntennaTemplates.firstOrNull { it.id == id }
    }

    fun getDefaultTemplate(): LabTestTemplate {
        return knownAntennaTemplates.first()
    }
}