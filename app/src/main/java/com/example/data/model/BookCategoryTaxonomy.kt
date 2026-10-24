package com.example.data.model

import androidx.compose.ui.graphics.Color

data class BookClassificationResult(
    val category: String,
    val subcategory: String? = null,
    val colorHex: String = "#6366F1"
)

data class BookCategoryDefinition(
    val name: String,
    val subcategories: List<String>,
    val defaultColorHex: String,
    val keywords: List<String> = emptyList()
)

data class CategoryColorPalette(
    val accentColor: Color,
    val surfaceColor: Color,
    val textColor: Color = Color.White,
    val containerColor: Color = surfaceColor,
    val primaryColor: Color = accentColor,
    val headerBg: Color = surfaceColor,
    val cardBg: Color = surfaceColor,
    val badgeBg: Color = accentColor.copy(alpha = 0.25f),
    val glowColor: Color = accentColor.copy(alpha = 0.4f),
    val hexCode: String = "#6366F1",
    val activeSentenceBg: Color = accentColor.copy(alpha = 0.25f)
)

object BookCategoryTaxonomy {
    val CATEGORIES = listOf(
        BookCategoryDefinition(
            name = "Fiction",
            subcategories = listOf("Classics", "Contemporary", "Fantasy", "Science Fiction", "Mystery & Thriller", "Historical Fiction", "Romance", "Literary Fiction"),
            defaultColorHex = "#9333EA",
            keywords = listOf("romance", "novela", "conto", "história", "ficção", "casmurro", "machado", "personagem", "narrativa", "poesia")
        ),
        BookCategoryDefinition(
            name = "Self-Help, Relationships & Lifestyle",
            subcategories = listOf("Personal Growth & Inspiration", "Habits & Productivity", "Mindfulness & Meditation", "Relationships", "Emotional Intelligence"),
            defaultColorHex = "#10B981",
            keywords = listOf("hábito", "mindset", "sucesso", "produtividade", "mastery", "immersion", "mind", "autoajuda", "disciplina", "foco")
        ),
        BookCategoryDefinition(
            name = "Society, Politics & Philosophy",
            subcategories = listOf("Ancient & Medieval Philosophy", "Modern Philosophy", "Ethics & Morality", "Political Theory", "Sociology", "Stoicism"),
            defaultColorHex = "#854D0E",
            keywords = listOf("filosofia", "estoicismo", "sêneca", "epicteto", "ética", "sociedade", "política", "moral", "sabedoria", "vida")
        ),
        BookCategoryDefinition(
            name = "Computers",
            subcategories = listOf("Programming", "Artificial Intelligence", "Data Science", "Web Development", "Operating Systems", "Cloud & DevOps"),
            defaultColorHex = "#06B6D4",
            keywords = listOf("python", "programming", "code", "software", "windows", "linux", "algorithm", "javascript", "developer", "data")
        ),
        BookCategoryDefinition(
            name = "Business & Economics",
            subcategories = listOf("Management & Leadership", "Finance & Investment", "Marketing", "Entrepreneurship", "Economics"),
            defaultColorHex = "#2563EB",
            keywords = listOf("negócios", "economia", "finanças", "investimento", "empresa", "liderança", "mercado", "gestão")
        ),
        BookCategoryDefinition(
            name = "Science & Mathematics",
            subcategories = listOf("Physics", "Mathematics", "Biology", "Chemistry", "Astronomy", "Neuroscience"),
            defaultColorHex = "#0284C7",
            keywords = listOf("ciência", "física", "matemática", "biologia", "química", "cálculo", "teorema", "experimento")
        ),
        BookCategoryDefinition(
            name = "History",
            subcategories = listOf("World History", "Ancient Civilizations", "Military & Wars", "Biographies & Memoirs", "Latin American History"),
            defaultColorHex = "#D97706",
            keywords = listOf("história", "guerra", "século", "império", "civilização", "revolução", "brasil", "roma")
        ),
        BookCategoryDefinition(
            name = "Religion & Spirituality",
            subcategories = listOf("Christianity & Theology", "Eastern Religions", "Spirituality", "Comparative Religion", "Biblical Studies"),
            defaultColorHex = "#7C3AED",
            keywords = listOf("teologia", "religião", "deus", "bíblia", "fé", "espiritualidade", "cristianismo", "oração")
        ),
        BookCategoryDefinition(
            name = "Psychology",
            subcategories = listOf("Cognitive Psychology", "Behavioral Science", "Psychoanalysis", "Developmental Psychology"),
            defaultColorHex = "#EC4899",
            keywords = listOf("psicologia", "mente", "cérebro", "comportamento", "cognição", "freud", "jung", "emoção")
        ),
        BookCategoryDefinition(
            name = "Language & Linguistics",
            subcategories = listOf("Grammar & Linguistics", "Foreign Language Learning", "Translation Studies", "Etymology"),
            defaultColorHex = "#059669",
            keywords = listOf("linguística", "gramática", "idioma", "inglês", "tradução", "vocabulário", "língua", "language")
        ),
        BookCategoryDefinition(
            name = "Arts & Photography",
            subcategories = listOf("Art History", "Design", "Photography", "Music & Performing Arts", "Architecture"),
            defaultColorHex = "#F43F5E",
            keywords = listOf("arte", "música", "design", "fotografia", "pintura", "estética", "cinema", "arquitetura")
        ),
        BookCategoryDefinition(
            name = "Health & Medicine",
            subcategories = listOf("Medicine", "Nutrition", "Mental Health", "Fitness & Exercise", "Public Health"),
            defaultColorHex = "#14B8A6",
            keywords = listOf("saúde", "medicina", "nutrição", "corpo", "exercício", "dieta", "doença", "cura")
        ),
        BookCategoryDefinition(
            name = "Education & Teaching",
            subcategories = listOf("Pedagogy", "Educational Technology", "Curriculum & Instruction", "Higher Education"),
            defaultColorHex = "#6366F1",
            keywords = listOf("educação", "pedagogia", "ensino", "didática", "aprendizagem", "escola", "professor")
        ),
        BookCategoryDefinition(
            name = "Law & Criminology",
            subcategories = listOf("Constitutional Law", "Civil Law", "Criminal Law", "International Law", "Legal Theory"),
            defaultColorHex = "#475569",
            keywords = listOf("direito", "lei", "jurídico", "constituição", "justiça", "código", "processo", "advocacia")
        ),
        BookCategoryDefinition(
            name = "Engineering & Technology",
            subcategories = listOf("Electrical Engineering", "Mechanical Engineering", "Civil Engineering", "Robotics", "Nanotechnology"),
            defaultColorHex = "#0891B2",
            keywords = listOf("engenharia", "mecânica", "elétrica", "robótica", "tecnologia", "circuitos", "sistemas")
        ),
        BookCategoryDefinition(
            name = "Reference",
            subcategories = listOf("Encyclopedias", "Dictionaries", "Atlases", "Research Manuals"),
            defaultColorHex = "#64748B",
            keywords = listOf("dicionário", "enciclopédia", "manual", "guia", "referência", "catálogo", "índice")
        )
    )

    fun getCategoryByName(name: String): BookCategoryDefinition? {
        val clean = name.trim().lowercase()
        return CATEGORIES.firstOrNull { it.name.lowercase() == clean || clean.contains(it.name.lowercase()) }
    }

    fun getPaletteForCategory(category: String?, customHex: String? = null): CategoryColorPalette {
        val hex = if (!customHex.isNullOrBlank() && customHex.startsWith("#")) {
            customHex
        } else {
            val matched = if (!category.isNullOrBlank()) getCategoryByName(category) else null
            matched?.defaultColorHex ?: "#6366F1"
        }

        val parsedColor = try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }

        return CategoryColorPalette(
            accentColor = parsedColor,
            surfaceColor = Color(0xFF10121A),
            textColor = Color.White,
            containerColor = Color(0xFF1A1C28),
            primaryColor = parsedColor,
            headerBg = Color(0xFF0C0D14),
            cardBg = Color(0xFF151722),
            badgeBg = parsedColor.copy(alpha = 0.25f),
            glowColor = parsedColor.copy(alpha = 0.4f),
            hexCode = hex,
            activeSentenceBg = parsedColor.copy(alpha = 0.25f)
        )
    }

    fun classifyByHeuristics(
        bookTitle: String,
        bookAuthor: String,
        sampleText: String? = null
    ): BookClassificationResult {
        val combined = "$bookTitle $bookAuthor ${sampleText ?: ""}".lowercase()

        for (cat in CATEGORIES) {
            if (cat.keywords.any { keyword -> combined.contains(keyword) }) {
                return BookClassificationResult(
                    category = cat.name,
                    subcategory = cat.subcategories.firstOrNull(),
                    colorHex = cat.defaultColorHex
                )
            }
        }

        return BookClassificationResult(
            category = "Reference",
            subcategory = "General",
            colorHex = "#64748B"
        )
    }
}
