package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Utilitário para converter equações em formato computacional (ASCII/código/LaTeX bruto)
 * para a notação tipográfica elegante idêntica àquela encontrada nos livros didáticos impressos.
 */
object TextbookMathFormatter {

    /**
     * Mapeamento de expoentes e sobrescritos Unicode
     */
    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ',
        'a' to 'ᵃ', 'b' to 'ᵇ', 't' to 'ᵗ'
    )

    /**
     * Mapeamento de índices e subscritos Unicode
     */
    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'h' to 'ₕ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'p' to 'ₚ',
        's' to 'ₛ', 't' to 'ₜ'
    )

    fun toSuperscript(str: String): String {
        return str.map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
    }

    fun toSubscript(str: String): String {
        return str.map { SUBSCRIPT_MAP[it] ?: it }.joinToString("")
    }

    /**
     * Transforma expressões de computador (*, ^, _, sqrt, frac, etc.)
     * na notação limpa e didática de livros físicos de matemática, química e física.
     */
    fun formatToTextbookNotation(rawFormula: String): String {
        if (rawFormula.isBlank()) return ""

        var formatted = rawFormula

        // 1. Limpeza de formatações Markdown residuais
        formatted = formatted
            .replace("```latex", "")
            .replace("```math", "")
            .replace("```", "")
            .replace("$", "")
            .trim()

        // 2. Substituição de LaTeX comum para Unicode didático
        formatted = formatted
            .replace("\\cdot", " · ")
            .replace("\\times", " × ")
            .replace("\\div", " ÷ ")
            .replace("\\pm", " ± ")
            .replace("\\mp", " ∓ ")
            .replace("\\neq", " ≠ ")
            .replace("\\leq", " ≤ ")
            .replace("\\geq", " ≥ ")
            .replace("\\approx", " ≈ ")
            .replace("\\equiv", " ≡ ")
            .replace("\\propto", " ∝ ")
            .replace("\\infty", "∞")
            .replace("\\partial", "∂")
            .replace("\\nabla", "∇")
            .replace("\\int", "∫")
            .replace("\\iint", "∬")
            .replace("\\iiint", "∭")
            .replace("\\oint", "∮")
            .replace("\\sum", "∑")
            .replace("\\prod", "∏")
            .replace("\\lim", "lim")
            .replace("\\to", " → ")
            .replace("\\rightarrow", " → ")
            .replace("\\leftrightarrow", " ↔ ")
            .replace("\\rightleftharpoons", " ⇌ ")
            .replace("\\leftrightharpoons", " ⇄ ")
            .replace("\\Delta", "Δ")
            .replace("\\Omega", "Ω")
            .replace("\\pi", "π")
            .replace("\\theta", "θ")
            .replace("\\lambda", "λ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\sigma", "σ")
            .replace("\\mu", "μ")
            .replace("\\epsilon", "ε")
            .replace("\\phi", "φ")
            .replace("\\rho", "ρ")
            .replace("\\omega", "ω")
            .replace("\\left(", "(")
            .replace("\\right)", ")")
            .replace("\\left[", "[")
            .replace("\\right]", "]")

        // 3. LaTeX \frac{a}{b} -> (a) / (b) para renderização
        formatted = formatted.replace(Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")) { match ->
            val num = match.groupValues[1]
            val den = match.groupValues[2]
            "($num) ÷ ($den)"
        }

        // 4. LaTeX \sqrt[n]{x} -> ⁿ√(x) e \sqrt{x} -> √(x)
        formatted = formatted.replace(Regex("""\\sqrt\[([0-9]+)\]\{([^{}]+)\}""")) { match ->
            val n = toSuperscript(match.groupValues[1])
            "${n}√(${match.groupValues[2]})"
        }
        formatted = formatted.replace(Regex("""\\sqrt\{([^{}]+)\}""")) { match ->
            "√(${match.groupValues[1]})"
        }

        // 5. Limpeza de comandos \text{} ou \mathrm{}
        formatted = formatted.replace(Regex("""\\(?:text|mathrm|mathbf|mathit)\{([^{}]+)\}""")) { match ->
            match.groupValues[1]
        }

        // 6. Substituição de operadores de computador para símbolos de livros didáticos
        // Multiplicação computacional: * ou **
        formatted = formatted.replace(Regex("""\s*\*\*\s*"""), "^")
        formatted = formatted.replace(Regex("""(?<=[0-9a-zA-Z\)])\s*\*\s*(?=[0-9a-zA-Z\(])"""), " · ")

        // Divisão computacional: / quando cercada de espaços
        formatted = formatted.replace(" != ", " ≠ ")
        formatted = formatted.replace(" == ", " = ")
        formatted = formatted.replace(" <= ", " ≤ ")
        formatted = formatted.replace(" >= ", " ≥ ")
        formatted = formatted.replace(" +/- ", " ± ")
        formatted = formatted.replace("+/-", "±")
        formatted = formatted.replace(" <-> ", " ⇌ ")
        formatted = formatted.replace(" <=> ", " ⇔ ")
        formatted = formatted.replace(" => ", " ⇒ ")
        formatted = formatted.replace(" -> ", " → ")

        // Raiz quadrada computacional: sqrt(...)
        formatted = formatted.replace(Regex("""\bsqrt\(([^)]+)\)""")) { match ->
            "√(${match.groupValues[1]})"
        }

        // 7. Conversão de circunflexo ^ para expoentes sobrescritos reais de livros físicos
        // Ex: x^2 -> x², e^-t -> e⁻ᵗ, 10^5 -> 10⁵, Fe^3+ -> Fe³⁺
        formatted = formatted.replace(Regex("""\^\{([^}]+)\}""")) { match ->
            toSuperscript(match.groupValues[1])
        }
        formatted = formatted.replace(Regex("""\^([0-9a-zA-Z\+\-]+)""")) { match ->
            toSuperscript(match.groupValues[1])
        }

        // 8. Conversão de subscritos computacionais com _
        // Ex: H_2_O -> H₂O, v_0 -> v₀, a_n -> aₙ
        formatted = formatted.replace(Regex("""_\{([^}]+)\}""")) { match ->
            toSubscript(match.groupValues[1])
        }
        formatted = formatted.replace(Regex("""_([0-9a-zA-Z\+\-]+)""")) { match ->
            toSubscript(match.groupValues[1])
        }

        // 9. Fórmulas químicas comuns que vieram em formato plano de computador
        formatted = formatChemicalFormulas(formatted)

        return formatted.trim()
    }

    /**
     * Ajusta fórmulas químicas para exibir subscritos naturais em átomos comuns
     * (Ex: H2O -> H₂O, CO2 -> CO₂, H2SO4 -> H₂SO₄, C6H12O6 -> C₆H₁₂O₆)
     */
    private fun formatChemicalFormulas(text: String): String {
        // Encontra padrões químicos como C6H12O6 ou H2O ou O2 ou CO2
        val chemRegex = Regex("""\b([A-Z][a-z]?)([0-9]+)\b""")
        return text.replace(chemRegex) { match ->
            val element = match.groupValues[1]
            val count = toSubscript(match.groupValues[2])
            "$element$count"
        }
    }

    /**
     * Tenta decompor uma equação que possui fração principal empilhada (numerador sobre denominador)
     * para desenhar a barra de fração de livro físico.
     */
    fun extractFractionParts(formula: String): Pair<String, String>? {
        // Se contiver ÷ com parênteses: (A) ÷ (B)
        val divParenRegex = Regex("""^\s*(.*?)=\s*\((.+?)\)\s*÷\s*\((.+?)\)\s*$""")
        val matchParen = divParenRegex.find(formula)
        if (matchParen != null) {
            val prefix = matchParen.groupValues[1].trim()
            val num = matchParen.groupValues[2].trim()
            val den = matchParen.groupValues[3].trim()
            return "$prefix = $num" to den
        }

        // Se contiver barra simples / com parênteses: prefix = (A) / (B)
        val slashParenRegex = Regex("""^\s*(.*?)=\s*\((.+?)\)\s*/\s*\((.+?)\)\s*$""")
        val matchSlash = slashParenRegex.find(formula)
        if (matchSlash != null) {
            val prefix = matchSlash.groupValues[1].trim()
            val num = matchSlash.groupValues[2].trim()
            val den = matchSlash.groupValues[3].trim()
            return "$prefix = $num" to den
        }

        return null
    }
}

/**
 * Componente que renderiza equações com a tipografia, proporção e elegância visual
 * idênticas à diagramação dos livros didáticos físicos impressos (ensino médio e superior),
 * substituindo o formato de código/computador por tipografia serifada matemática.
 */
@Composable
fun TextbookEquationView(
    formulaNotation: String,
    latexNotation: String = "",
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8)
) {
    var viewMode by remember { mutableStateOf(EquationDisplayMode.TEXTBOOK) }

    val formattedMath = remember(formulaNotation) {
        TextbookMathFormatter.formatToTextbookNotation(formulaNotation)
    }

    val fractionParts = remember(formattedMath) {
        TextbookMathFormatter.extractFractionParts(formattedMath)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.2.dp, Color(0xFF334155)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E293B).copy(alpha = 0.85f),
                            Color(0xFF0F172A).copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            // Barra superior de identificação do formato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Etiqueta: Formato Livro Didático
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (viewMode == EquationDisplayMode.TEXTBOOK) "LIVRO DIDÁTICO" else "NOTAÇÃO COMPUTADOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = if (viewMode == EquationDisplayMode.TEXTBOOK) accentColor else Color(0xFF94A3B8)
                    )
                }

                // Alternador entre Formato de Livro e Formato Computacional / Código
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (viewMode == EquationDisplayMode.TEXTBOOK) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.7.dp, if (viewMode == EquationDisplayMode.TEXTBOOK) accentColor else Color.Transparent),
                        modifier = Modifier
                            .clickable { viewMode = EquationDisplayMode.TEXTBOOK }
                            .padding(1.dp)
                    ) {
                        Text(
                            text = "📖 Livro Físico",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewMode == EquationDisplayMode.TEXTBOOK) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (viewMode == EquationDisplayMode.COMPUTER) Color(0xFF64748B).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.7.dp, if (viewMode == EquationDisplayMode.COMPUTER) Color(0xFF94A3B8) else Color.Transparent),
                        modifier = Modifier
                            .clickable { viewMode = EquationDisplayMode.COMPUTER }
                            .padding(1.dp)
                    ) {
                        Text(
                            text = "💻 Computador",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewMode == EquationDisplayMode.COMPUTER) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // QUADRO CENTRAL DA EQUAÇÃO
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (viewMode == EquationDisplayMode.TEXTBOOK) Color(0xFF0B132B) else Color(0xFF030712),
                border = BorderStroke(
                    1.dp,
                    if (viewMode == EquationDisplayMode.TEXTBOOK) accentColor.copy(alpha = 0.45f) else Color(0xFF1E293B)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SelectionContainer {
                        if (viewMode == EquationDisplayMode.COMPUTER) {
                            // Modo Computador: Monospace cru
                            Text(
                                text = formulaNotation,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Modo Livro Didático: Tipografia Editorial Serifada com Frações e Símbolos
                            if (fractionParts != null) {
                                // Renderização com fração empilhada vertical
                                val (lhsAndNum, den) = fractionParts
                                val parts = lhsAndNum.split("=")
                                if (parts.size >= 2) {
                                    val lhs = parts[0].trim()
                                    val num = parts.drop(1).joinToString("=").trim()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$lhs = ",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontStyle = FontStyle.Italic,
                                            fontFamily = FontFamily.Serif,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = num,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontStyle = FontStyle.Italic,
                                                fontFamily = FontFamily.Serif,
                                                color = Color(0xFF7DD3FC)
                                            )
                                            HorizontalDivider(
                                                color = Color(0xFF94A3B8),
                                                thickness = 1.6.dp,
                                                modifier = Modifier.widthIn(min = 60.dp, max = 220.dp)
                                            )
                                            Text(
                                                text = den,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontStyle = FontStyle.Italic,
                                                fontFamily = FontFamily.Serif,
                                                color = Color(0xFF7DD3FC)
                                            )
                                        }
                                    }
                                } else {
                                    TextbookSerifFormula(formattedMath)
                                }
                            } else {
                                TextbookSerifFormula(formattedMath)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextbookSerifFormula(text: String) {
    Text(
        text = text,
        fontSize = 16.5.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        color = Color(0xFFF8FAFC),
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
    )
}

enum class EquationDisplayMode {
    TEXTBOOK,
    COMPUTER
}
