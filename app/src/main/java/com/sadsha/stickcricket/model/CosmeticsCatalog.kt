package com.sadsha.stickcricket.model

object CosmeticsCatalog {
    val BATS = listOf(
        CosmeticItem(
            id = "bat_classic",
            name = "Willow Classic",
            category = CosmeticCategory.BAT,
            price = 0,
            primaryColorHex = 0xFFD2B48C, // tan wood
            secondaryColorHex = 0xFF8B4513, // leather handle
            accentColorHex = 0xFFFFFFFF,
            description = "Traditional English willow blade with balanced pickup.",
            isDefault = true
        ),
        CosmeticItem(
            id = "bat_golden",
            name = "Golden Blaster",
            category = CosmeticCategory.BAT,
            price = 500,
            primaryColorHex = 0xFFFFD700, // gold
            secondaryColorHex = 0xFF212121, // black grip
            accentColorHex = 0xFFFFA000,
            description = "Forged with gold sheen for dazzling boundaries."
        ),
        CosmeticItem(
            id = "bat_neon",
            name = "Neon Striker",
            category = CosmeticCategory.BAT,
            price = 850,
            primaryColorHex = 0xFF00E5FF, // neon cyan
            secondaryColorHex = 0xFF76FF03, // neon lime
            accentColorHex = 0xFF00B0FF,
            description = "Electric neon style that glows under stadium floodlights."
        ),
        CosmeticItem(
            id = "bat_carbon",
            name = "Carbon Dominator",
            category = CosmeticCategory.BAT,
            price = 1400,
            primaryColorHex = 0xFF263238, // carbon dark
            secondaryColorHex = 0xFFFF1744, // red grip
            accentColorHex = 0xFF78909C,
            description = "Ultra-modern carbon weave aesthetic for brutal power hitters."
        ),
        CosmeticItem(
            id = "bat_flame",
            name = "Flame Igniter",
            category = CosmeticCategory.BAT,
            price = 2200,
            primaryColorHex = 0xFFFF3D00, // flame orange
            secondaryColorHex = 0xFFFFD600, // flame yellow
            accentColorHex = 0xFFDD2C00,
            description = "Ignite every ball with smoking boundaries and towering sixes."
        )
    )

    val HELMETS = listOf(
        CosmeticItem(
            id = "helmet_navy",
            name = "Rookie Navy",
            category = CosmeticCategory.HELMET,
            price = 0,
            primaryColorHex = 0xFF1A237E,
            secondaryColorHex = 0xFFEEEEEE,
            accentColorHex = 0xFF3949AB,
            description = "Standard protective navy helmet with steel grille.",
            isDefault = true
        ),
        CosmeticItem(
            id = "helmet_white",
            name = "White Knight",
            category = CosmeticCategory.HELMET,
            price = 400,
            primaryColorHex = 0xFFECEFF1,
            secondaryColorHex = 0xFF37474F,
            accentColorHex = 0xFFCFD8DC,
            description = "Clean pearl-white shell designed for calm composure."
        ),
        CosmeticItem(
            id = "helmet_crimson",
            name = "Crimson Fury",
            category = CosmeticCategory.HELMET,
            price = 750,
            primaryColorHex = 0xFFC62828,
            secondaryColorHex = 0xFF212121,
            accentColorHex = 0xFFEF5350,
            description = "Aggressive crimson dome showing fearless intent."
        ),
        CosmeticItem(
            id = "helmet_gold",
            name = "Spartan Gold",
            category = CosmeticCategory.HELMET,
            price = 1200,
            primaryColorHex = 0xFFFFB300,
            secondaryColorHex = 0xFF212121,
            accentColorHex = 0xFFFFE082,
            description = "Golden warrior crown reserved for match winners."
        ),
        CosmeticItem(
            id = "helmet_cyber",
            name = "Cyber Visor",
            category = CosmeticCategory.HELMET,
            price = 1800,
            primaryColorHex = 0xFF00E676,
            secondaryColorHex = 0xFF121212,
            accentColorHex = 0xFF00B0FF,
            description = "Futuristic aerodynamic visor with augmented HUD optics."
        )
    )

    val JERSEYS = listOf(
        CosmeticItem(
            id = "jersey_white",
            name = "Original Whites",
            category = CosmeticCategory.JERSEY,
            price = 0,
            primaryColorHex = 0xFFF5F5F5,
            secondaryColorHex = 0xFF1E88E5,
            accentColorHex = 0xFFE0E0E0,
            description = "Timeless cricket whites with subtle navy crest.",
            isDefault = true
        ),
        CosmeticItem(
            id = "jersey_midnight",
            name = "Midnight Thunder",
            category = CosmeticCategory.JERSEY,
            price = 450,
            primaryColorHex = 0xFF0D47A1,
            secondaryColorHex = 0xFFFFEA00,
            accentColorHex = 0xFF1565C0,
            description = "Deep night sky blue charged with electric lightning stripes."
        ),
        CosmeticItem(
            id = "jersey_emerald",
            name = "Emerald Strikers",
            category = CosmeticCategory.JERSEY,
            price = 700,
            primaryColorHex = 0xFF2E7D32,
            secondaryColorHex = 0xFFFFD700,
            accentColorHex = 0xFF43A047,
            description = "Lush emerald stadium green with championship gold accents."
        ),
        CosmeticItem(
            id = "jersey_solar",
            name = "Solar Flare",
            category = CosmeticCategory.JERSEY,
            price = 1100,
            primaryColorHex = 0xFFF4511E,
            secondaryColorHex = 0xFFFFD600,
            accentColorHex = 0xFFFF7043,
            description = "Warm energetic sunset orange bursting across the pitch."
        ),
        CosmeticItem(
            id = "jersey_stealth",
            name = "Shadow Stealth",
            category = CosmeticCategory.JERSEY,
            price = 1750,
            primaryColorHex = 0xFF212121,
            secondaryColorHex = 0xFF00E5FF,
            accentColorHex = 0xFF424242,
            description = "Matte midnight black with razor-sharp cyan piping."
        )
    )

    val BALLS = listOf(
        CosmeticItem(
            id = "ball_red",
            name = "Traditional Red",
            category = CosmeticCategory.BALL,
            price = 0,
            primaryColorHex = 0xFFB71C1C,
            secondaryColorHex = 0xFFFFFFFF, // white seam
            accentColorHex = 0xFFD32F2F,
            description = "Classic red four-piece leather ball with prominent white seam.",
            isDefault = true
        ),
        CosmeticItem(
            id = "ball_white",
            name = "White Kookaburra",
            category = CosmeticCategory.BALL,
            price = 400,
            primaryColorHex = 0xFFFAFAFA,
            secondaryColorHex = 0xFF212121,
            accentColorHex = 0xFFEEEEEE,
            description = "Modern limited-overs white ball engineered for high contrast."
        ),
        CosmeticItem(
            id = "ball_pink",
            name = "Pink Twilight",
            category = CosmeticCategory.BALL,
            price = 650,
            primaryColorHex = 0xFFE91E63,
            secondaryColorHex = 0xFFFFFFFF,
            accentColorHex = 0xFFF06292,
            description = "Day-night special pink ball with clear flight visibility."
        ),
        CosmeticItem(
            id = "ball_gold",
            name = "Golden Fireball",
            category = CosmeticCategory.BALL,
            price = 1200,
            primaryColorHex = 0xFFFFC107,
            secondaryColorHex = 0xFFFF5722,
            accentColorHex = 0xFFFFE082,
            description = "Radiant golden orb trailing embers as it speeds down pitch."
        ),
        CosmeticItem(
            id = "ball_neon",
            name = "Electric Lime",
            category = CosmeticCategory.BALL,
            price = 1600,
            primaryColorHex = 0xFF76FF03,
            secondaryColorHex = 0xFF00E5FF,
            accentColorHex = 0xFFB2FF59,
            description = "Hyper-optic neon lime ball impossible for fielders to lose."
        )
    )

    val ALL_COSMETICS: List<CosmeticItem> = BATS + HELMETS + JERSEYS + BALLS

    fun findById(id: String): CosmeticItem? = ALL_COSMETICS.find { it.id == id }

    fun getDefault(category: CosmeticCategory): CosmeticItem =
        ALL_COSMETICS.first { it.category == category && it.isDefault }
}
