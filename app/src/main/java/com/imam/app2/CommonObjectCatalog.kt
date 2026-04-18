package com.imam.app2

data class CommonObjectProfile(
    val label: String,
    val typicalSizeCm: Double,
    val minSizeCm: Double,
    val maxSizeCm: Double,
    val note: String
)

object CommonObjectCatalog {
    val items = listOf(
        CommonObjectProfile(
            label = "Tinggi manusia dewasa",
            typicalSizeCm = 167.0,
            minSizeCm = 150.0,
            maxSizeCm = 185.0,
            note = "perkiraan tinggi orang dewasa berdiri"
        ),
        CommonObjectProfile(
            label = "Tinggi mobil penumpang",
            typicalSizeCm = 148.0,
            minSizeCm = 140.0,
            maxSizeCm = 160.0,
            note = "sedan / hatchback / SUV rendah"
        ),
        CommonObjectProfile(
            label = "Panjang ruas ujung jari",
            typicalSizeCm = 2.7,
            minSizeCm = 2.2,
            maxSizeCm = 3.2,
            note = "perkiraan panjang ruas distal jari tangan"
        ),
        CommonObjectProfile(
            label = "Tinggi mini van",
            typicalSizeCm = 185.0,
            minSizeCm = 175.0,
            maxSizeCm = 198.0,
            note = "kelas MPV / minivan tinggi"
        ),
        CommonObjectProfile(
            label = "Tinggi bus",
            typicalSizeCm = 320.0,
            minSizeCm = 300.0,
            maxSizeCm = 340.0,
            note = "bus sedang sampai bus besar"
        )
    )
}
