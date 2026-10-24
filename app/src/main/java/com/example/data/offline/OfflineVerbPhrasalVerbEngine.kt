package com.example.data.offline

import com.example.data.model.VerbPhrasalVerbStudyTerm
import com.example.data.model.VerbTenseQuestionExample
import com.example.util.TextFormatUtils
import java.util.Locale

/**
 * Motor offline de alta precisão para varredura e extração de Verbos e Phrasal Verbs
 * a partir de qualquer texto de página de livro ou dicionário.
 *
 * Repara palavras quebradas por hífens ou artefatos de OCR/PDF, identifica verbos
 * simples e compostos/preposicionados presentes no texto, e gera RIGOROSAMENTE
 * 16 TEMPOS VERBAIS COM QUESTION WORDS E TRADUÇÕES EM PORTUGUÊS.
 */
object OfflineVerbPhrasalVerbEngine {

    data class VerbDictionaryEntry(
        val verbOrPhrase: String,
        val isPhrasal: Boolean,
        val prep: String,
        val base: String,
        val pt: String = "",
        val ptMeaning: String = pt,
        val defEn: String = "",
        val ipa: String = "",
        val languageCode: String = "en",
        val collocation: String = "",
        val sampleTemplate: ((tenseIdx: Int, tenseName: String, qWord: String) -> Pair<String, String>)? = null
    ) {
        val resolvedPortugueseMeaning: String
            get() = if (pt.isNotBlank()) pt else ptMeaning
    }

    // Catálogo extenso de verbos e phrasal verbs frequentemente encontrados em livros
    private val KNOWN_ENTRIES: List<VerbDictionaryEntry> = listOf(
        // Exemplo explícito do usuário: "eat", "get by", "get across"
        VerbDictionaryEntry(
            verbOrPhrase = "eat",
            isPhrasal = false,
            prep = "",
            base = "eat",
            pt = "Comer, alimentar-se, consumir alimentos.",
            defEn = "To put food into the mouth, chew and swallow it.",
            ipa = "/iːt/",
            collocation = "eat + meals / healthy food"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "get by",
            isPhrasal = true,
            prep = "by",
            base = "get",
            pt = "Sobreviver, virar-se com recursos limitados, passar sem luxos.",
            defEn = "To manage to live or do something with difficulty or limited financial resources.",
            ipa = "/ɡɛt baɪ/",
            collocation = "get by + on a low budget"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "get across",
            isPhrasal = true,
            prep = "across",
            base = "get",
            pt = "Transmitir com clareza, fazer-se entender, comunicar uma mensagem com sucesso.",
            defEn = "To successfully communicate an idea or message so that it is understood.",
            ipa = "/ɡɛt əˈkrɒs/",
            collocation = "get across + the core message"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "look for",
            isPhrasal = true,
            prep = "for",
            base = "look",
            pt = "Procurar, buscar, tentar encontrar algo ou alguém.",
            defEn = "To try to find something that is lost, or someone who is not present.",
            ipa = "/lʊk fɔːr/",
            collocation = "look for + solutions"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "give up",
            isPhrasal = true,
            prep = "up",
            base = "give",
            pt = "Desistir, renunciar a um hábito, abrir mão de um esforço.",
            defEn = "To stop doing or having something; to surrender or abandon an effort.",
            ipa = "/ɡɪv ʌp/",
            collocation = "give up + bad habits"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "break down",
            isPhrasal = true,
            prep = "down",
            base = "break",
            pt = "Quebrar (máquina), parar de funcionar ou desmoronar emocionalmente.",
            defEn = "To cease to function; to disintegrate or collapse.",
            ipa = "/breɪk daʊn/",
            collocation = "break down + barriers"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "achieve",
            isPhrasal = false,
            prep = "",
            base = "achieve",
            pt = "Alcançar, atingir, conquistar uma meta ou objetivo.",
            defEn = "To successfully bring about or reach a desired objective by effort or skill.",
            ipa = "/əˈtʃiːv/",
            collocation = "achieve + milestones"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "develop",
            isPhrasal = false,
            prep = "",
            base = "develop",
            pt = "Desenvolver, expandir, aprimorar habilidades ou sistemas.",
            defEn = "To grow or cause to grow and become more mature, advanced, or elaborate.",
            ipa = "/dɪˈvɛləp/",
            collocation = "develop + strategies"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "understand",
            isPhrasal = false,
            prep = "",
            base = "understand",
            pt = "Compreender, entender o significado e alcance de algo.",
            defEn = "To perceive the intended meaning of words, a language, or a concept.",
            ipa = "/ˌʌndərˈstænd/",
            collocation = "understand + principles"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "carry on",
            isPhrasal = true,
            prep = "on",
            base = "carry",
            pt = "Continuar, prosseguir apesar das dificuldades.",
            defEn = "To continue doing something, especially through adversity.",
            ipa = "/ˈkæri ɒn/",
            collocation = "carry on + research"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "turn down",
            isPhrasal = true,
            prep = "down",
            base = "turn",
            pt = "Recusar, rejeitar uma proposta ou diminuir volume/intensidade.",
            defEn = "To reject an offer or proposal; to reduce volume or intensity.",
            ipa = "/tɜːn daʊn/",
            collocation = "turn down + offers"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "run out of",
            isPhrasal = true,
            prep = "out of",
            base = "run",
            pt = "Esgotar, ficar sem suprimentos ou recursos essenciais.",
            defEn = "To use all of something so that there is none left.",
            ipa = "/rʌn aʊt əv/",
            collocation = "run out of + options"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "figure out",
            isPhrasal = true,
            prep = "out",
            base = "figure",
            pt = "Decifrar, descobrir a solução para um problema complexo.",
            defEn = "To discover or determine something; to solve a problem through reasoning.",
            ipa = "/ˈfɪɡjər aʊt/",
            collocation = "figure out + the puzzle"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "come across",
            isPhrasal = true,
            prep = "across",
            base = "come",
            pt = "Deparar-se com algo por acaso; encontrar casualmente.",
            defEn = "To meet or find someone or something by chance.",
            ipa = "/kʌm əˈkrɒs/",
            collocation = "come across + evidence"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "put off",
            isPhrasal = true,
            prep = "off",
            base = "put",
            pt = "Adiar, postergar uma tarefa ou evento marcado.",
            defEn = "To postpone or delay an activity or meeting.",
            ipa = "/pʊt ɒf/",
            collocation = "put off + decisions"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "set up",
            isPhrasal = true,
            prep = "up",
            base = "set",
            pt = "Estabelecer, fundar, montar ou configurar um sistema/empresa.",
            defEn = "To establish or organize something; to place in position.",
            ipa = "/sɛt ʌp/",
            collocation = "set up + operations"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "take on",
            isPhrasal = true,
            prep = "on",
            base = "take",
            pt = "Assumir responsabilidade; enfrentar um desafio ou contratar.",
            defEn = "To undertake a task or responsibility; to confront a challenge.",
            ipa = "/teɪk ɒn/",
            collocation = "take on + challenges"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "bring about",
            isPhrasal = true,
            prep = "about",
            base = "bring",
            pt = "Provocar, causar uma mudança profunda ou transformação.",
            defEn = "To make something happen; to cause a transformation.",
            ipa = "/brɪŋ əˈbaʊt/",
            collocation = "bring about + change"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "hold on",
            isPhrasal = true,
            prep = "on",
            base = "hold",
            pt = "Aguardar um momento; segurar firme com determinação.",
            defEn = "To wait for a short period; to endure difficult conditions.",
            ipa = "/həʊld ɒn/",
            collocation = "hold on + tight"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "keep up",
            isPhrasal = true,
            prep = "up",
            base = "keep",
            pt = "Acompanhar o ritmo, manter o mesmo nível de desempenho.",
            defEn = "To move or progress at the same rate as someone or something else.",
            ipa = "/kiːp ʌp/",
            collocation = "keep up + with pace"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "show up",
            isPhrasal = true,
            prep = "up",
            base = "show",
            pt = "Comparecer, aparecer, chegar a um local esperado.",
            defEn = "To arrive or appear at an appointed place or event.",
            ipa = "/ʃəʊ ʌp/",
            collocation = "show up + on time"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "stand out",
            isPhrasal = true,
            prep = "out",
            base = "stand",
            pt = "Destacar-se, sobressair-se perante os demais.",
            defEn = "To be clearly noticeable or significantly better than peers.",
            ipa = "/stænd aʊt/",
            collocation = "stand out + from crowd"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "call off",
            isPhrasal = true,
            prep = "off",
            base = "call",
            pt = "Cancelar um evento, suspender definitivamente.",
            defEn = "To cancel an event or an agreement that had been scheduled.",
            ipa = "/kɔːl ɒf/",
            collocation = "call off + the meeting"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "watch out",
            isPhrasal = true,
            prep = "out",
            base = "watch",
            pt = "Ficar atento, prestar atenção a perigos iminentes.",
            defEn = "To be alert and vigilant against potential dangers.",
            ipa = "/wɒtʃ aʊt/",
            collocation = "watch out + for pitfalls"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "look forward to",
            isPhrasal = true,
            prep = "to",
            base = "look",
            pt = "Aguardar com grande expectativa positiva e entusiasmo.",
            defEn = "To await something in the future with pleasure and excitement.",
            ipa = "/lʊk ˈfɔːwəd tuː/",
            collocation = "look forward to + results"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "consider",
            isPhrasal = false,
            prep = "",
            base = "consider",
            pt = "Considerar, ponderar e examinar com prudência antes de decidir.",
            defEn = "To think carefully about something, typically before making a decision.",
            ipa = "/kənˈsɪdər/",
            collocation = "consider + alternatives"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "maintain",
            isPhrasal = false,
            prep = "",
            base = "maintain",
            pt = "Manter, preservar a continuidade e a estabilidade de algo.",
            defEn = "To cause or enable a condition or state of affairs to continue.",
            ipa = "/meɪnˈteɪn/",
            collocation = "maintain + standards"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "identify",
            isPhrasal = false,
            prep = "",
            base = "identify",
            pt = "Identificar, reconhecer e apontar as características essenciais.",
            defEn = "To establish or indicate who or what someone or something is.",
            ipa = "/aɪˈdɛntɪfaɪ/",
            collocation = "identify + root causes"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "explain",
            isPhrasal = false,
            prep = "",
            base = "explain",
            pt = "Explicar, elucidar e tornar claro um conceito complexo.",
            defEn = "To make an idea or situation clear to someone by describing it in more detail.",
            ipa = "/ɪkˈspleɪn/",
            collocation = "explain + reasoning"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "provide",
            isPhrasal = false,
            prep = "",
            base = "provide",
            pt = "Fornecer, disponibilizar subsídios, recursos ou informações.",
            defEn = "To make available for use; to supply necessary materials.",
            ipa = "/prəˈvaɪd/",
            collocation = "provide + insights"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "produce",
            isPhrasal = false,
            prep = "",
            base = "produce",
            pt = "Produzir, gerar resultados ou criar obras de valor.",
            defEn = "To make or manufacture from components or raw materials; to yield results.",
            ipa = "/prəˈdjuːs/",
            collocation = "produce + evidence"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "write",
            isPhrasal = false,
            prep = "",
            base = "write",
            pt = "Escrever, redigir textos e expressar ideias formalmente.",
            defEn = "To mark letters, words, or other symbols on a surface; to author texts.",
            ipa = "/raɪt/",
            collocation = "write + essays"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "speak",
            isPhrasal = false,
            prep = "",
            base = "speak",
            pt = "Falar, expressar pensamentos verbalmente com clareza.",
            defEn = "To say words aloud in order to convey information, opinion, or feeling.",
            ipa = "/spiːk/",
            collocation = "speak + clearly"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "analyze",
            isPhrasal = false,
            prep = "",
            base = "analyze",
            pt = "Analisar, examinar minuciosamente a constituição de algo.",
            defEn = "To examine methodically and in detail the constitution or structure of something.",
            ipa = "/ˈænəlaɪz/",
            collocation = "analyze + data"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "decide",
            isPhrasal = false,
            prep = "",
            base = "decide",
            pt = "Decidir, deliberar e chegar a uma resolução fundamentada.",
            defEn = "To make a choice from a number of alternatives; to reach a resolution.",
            ipa = "/dɪˈsaɪd/",
            collocation = "decide + next steps"
        ),
        // Espanhol
        VerbDictionaryEntry(
            verbOrPhrase = "llevar a cabo",
            isPhrasal = true,
            prep = "a cabo",
            base = "llevar",
            pt = "Realizar, executar, concluir com êxito um projeto ou plano.",
            defEn = "Llevar a efecto o término algo; ejecutar un plan.",
            ipa = "/ʝeˈβaɾ a ˈkaβo/",
            languageCode = "es",
            collocation = "llevar a cabo + reformas"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "dar por sentado",
            isPhrasal = true,
            prep = "por sentado",
            base = "dar",
            pt = "Presumir, tomar como certo sem prévia verificação.",
            defEn = "Asumir o considerar seguro algo sin necesidad de comprobación.",
            ipa = "/daɾ poɾ senˈtaðo/",
            languageCode = "es",
            collocation = "dar por sentado + hechos"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "lograr",
            isPhrasal = false,
            prep = "",
            base = "lograr",
            pt = "Conquistar, obter com êxito, alcançar um objetivo almejado.",
            defEn = "Conseguir o alcanzar lo que se intenta o desea.",
            ipa = "/loˈɣɾaɾ/",
            languageCode = "es",
            collocation = "lograr + metas"
        ),
        // Francês
        VerbDictionaryEntry(
            verbOrPhrase = "se rendre compte",
            isPhrasal = true,
            prep = "compte",
            base = "rendre",
            pt = "Perceber, dar-se conta de uma realidade.",
            defEn = "Prendre conscience de quelque chose; comprendre pleinement.",
            ipa = "/sə ʁɑ̃dʁ kɔ̃t/",
            languageCode = "fr",
            collocation = "se rendre compte + de la vérité"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "avoir besoin",
            isPhrasal = true,
            prep = "besoin",
            base = "avoir",
            pt = "Precisar, necessitar de auxílio ou recursos.",
            defEn = "Éprouver la nécessité de quelque chose ou de quelqu'un.",
            ipa = "/a.vwaʁ bə.zwɛ̃/",
            languageCode = "fr",
            collocation = "avoir besoin + de soutien"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "réussir",
            isPhrasal = false,
            prep = "",
            base = "réussir",
            pt = "Ter sucesso, obter êxito, alcançar um resultado positivo.",
            defEn = "Avoir un résultat heureux; parvenir au but qu'on s'était fixé.",
            ipa = "/ʁe.y.siʁ/",
            languageCode = "fr",
            collocation = "réussir + le concours"
        ),
        // Alemão
        VerbDictionaryEntry(
            verbOrPhrase = "aufgeben",
            isPhrasal = true,
            prep = "auf",
            base = "geben",
            pt = "Desistir de uma luta, abandonar um esforço ou postar correspondência.",
            defEn = "Etwas beenden, woran man arbeitet; kapitulieren.",
            ipa = "/ˈaʊ̯fˌɡeːbn̩/",
            languageCode = "de",
            collocation = "aufgeben + den Kampf"
        ),
        VerbDictionaryEntry(
            verbOrPhrase = "erreichen",
            isPhrasal = false,
            prep = "",
            base = "erreichen",
            pt = "Alcançar uma meta, atingir um objetivo ou contatar alguém.",
            defEn = "Ein Ziel oder einen Ort durch Bemühung gelangen.",
            ipa = "/ɛɐ̯ˈʁaɪ̯çn̩/",
            languageCode = "de",
            collocation = "erreichen + das Ziel"
        )
    )

    private val COMMON_VERBS = mapOf(
        "eat" to "Comer, alimentar-se",
        "drink" to "Beber, ingerir líquidos",
        "speak" to "Falar, conversar",
        "talk" to "Conversar, dialogar",
        "say" to "Dizer, pronunciar",
        "tell" to "Contar, relatar",
        "read" to "Ler, interpretar texto",
        "write" to "Escrever, redigir",
        "learn" to "Aprender, assimilar",
        "teach" to "Ensinar, instruir",
        "study" to "Estudar, analisar",
        "think" to "Pensar, raciocinar",
        "know" to "Saber, conhecer",
        "understand" to "Entender, compreender",
        "remember" to "Lembrar, recordar",
        "forget" to "Esquecer, omitir",
        "feel" to "Sentir, pressentir",
        "see" to "Ver, enxergar",
        "look" to "Olhar, fitar, examinar",
        "watch" to "Assistir, observar",
        "hear" to "Ouvir, escutar",
        "listen" to "Escutar atentamente",
        "make" to "Fazer, construir, produzir",
        "do" to "Fazer, executar",
        "create" to "Criar, conceber",
        "build" to "Construir, edificar",
        "break" to "Quebrar, romper",
        "give" to "Dar, conceder",
        "take" to "Tomar, pegar, levar",
        "bring" to "Trazer, transportar",
        "carry" to "Carregar, transportar",
        "hold" to "Segurar, reter",
        "get" to "Obter, conseguir, receber",
        "find" to "Encontrar, achar",
        "lose" to "Perder, extraviar",
        "keep" to "Manter, guardar",
        "leave" to "Deixar, sair, abandonar",
        "stay" to "Ficar, permanecer",
        "come" to "Vir, aproximar-se",
        "go" to "Ir, partir",
        "walk" to "Caminhar, andar",
        "run" to "Correr, disputar",
        "drive" to "Dirigir, conduzir",
        "fly" to "Voar, pilotar",
        "swim" to "Nadar",
        "fall" to "Cair, tombar",
        "grow" to "Crescer, cultivar",
        "change" to "Mudar, alterar",
        "become" to "Tornar-se, vir a ser",
        "happen" to "Acontecer, ocorrer",
        "seem" to "Parecer, aparentar",
        "appear" to "Aparecer, surgir",
        "show" to "Mostrar, evidenciar",
        "hide" to "Esconder, ocultar",
        "open" to "Abrir, inaugurar",
        "close" to "Fechar, encerrar",
        "start" to "Começar, iniciar",
        "begin" to "Começar, principiar",
        "stop" to "Parar, cessar",
        "finish" to "Terminar, concluir",
        "wait" to "Esperar, aguardar",
        "hope" to "Esperar, ter esperança",
        "want" to "Querer, desejar",
        "need" to "Precisar, necessitar",
        "like" to "Gostar, simpatizar",
        "love" to "Amar, adorar",
        "choose" to "Escolher, eleger",
        "decide" to "Decidir, deliberar",
        "try" to "Tentar, experimentar",
        "help" to "Ajudar, socorrer",
        "allow" to "Permitir, conceder",
        "ask" to "Perguntar, solicitar",
        "answer" to "Responder, contestar",
        "call" to "Chamar, telefonar",
        "meet" to "Encontrar, reunir-se",
        "live" to "Viver, habitar",
        "work" to "Trabalhar, funcionar",
        "play" to "Jogar, tocar, brincar",
        "sleep" to "Dormir, repousar",
        "stand" to "Ficar de pé, suportar",
        "sit" to "Sentar-se",
        "put" to "Colocar, pôr",
        "set" to "Definir, ajustar",
        "send" to "Enviar, remeter",
        "receive" to "Receber, acolher",
        "buy" to "Comprar, adquirir",
        "sell" to "Vender, comercializar",
        "pay" to "Pagar, remunerar",
        "spend" to "Gastar, despender",
        "win" to "Ganhar, vencer",
        "achieve" to "Conquistar, alcançar",
        "investigate" to "Investigar, averiguar",
        "analyze" to "Analisar, dissecar",
        "explain" to "Explicar, esclarecer",
        "develop" to "Desenvolver, progredir",
        "support" to "Apoiar, sustentar",
        "consider" to "Considerar, ponderar",
        "include" to "Incluir, conter",
        "provide" to "Prover, disponibilizar",
        "suggest" to "Sugerir, propor",
        "require" to "Exigir, demandar",
        "expect" to "Esperar, supor",
        "manage" to "Gerenciar, conseguir"
    )

    private val IRREGULAR_TO_BASE = mapOf(
        "ate" to "eat", "eaten" to "eat",
        "got" to "get", "gotten" to "get",
        "looked" to "look",
        "gave" to "give", "given" to "give",
        "took" to "take", "taken" to "take",
        "made" to "make",
        "did" to "do", "done" to "do",
        "saw" to "see", "seen" to "see",
        "came" to "come",
        "went" to "go", "gone" to "go",
        "knew" to "know", "known" to "know",
        "thought" to "think",
        "found" to "find",
        "told" to "tell",
        "said" to "say",
        "asked" to "ask",
        "felt" to "feel",
        "tried" to "try",
        "left" to "leave",
        "called" to "call",
        "kept" to "keep",
        "held" to "hold",
        "turned" to "turn",
        "started" to "start",
        "showed" to "show", "shown" to "show",
        "heard" to "hear",
        "ran" to "run",
        "moved" to "move",
        "lived" to "live",
        "brought" to "bring",
        "happened" to "happen",
        "wrote" to "write", "written" to "write",
        "sat" to "sit",
        "stood" to "stand",
        "lost" to "lose",
        "paid" to "pay",
        "met" to "meet",
        "set" to "set",
        "learned" to "learn", "learnt" to "learn",
        "understood" to "understand",
        "watched" to "watch",
        "stopped" to "stop",
        "spoke" to "speak", "spoken" to "speak",
        "read" to "read",
        "spent" to "spend",
        "grew" to "grow", "grown" to "grow",
        "opened" to "open",
        "walked" to "walk",
        "won" to "win",
        "bought" to "buy",
        "waited" to "wait",
        "sent" to "send",
        "built" to "build",
        "fell" to "fall", "fallen" to "fall",
        "cut" to "cut",
        "broke" to "break", "broken" to "break"
    )

    /**
     * Escaneia e extrai TODOS os verbos e phrasal verbs a partir de texto de página.
     * Retorna itens rigorosamente ordenados por ORDEM ALFABÉTICA (A -> Z).
     */
    fun extractVerbsAndPhrasalVerbs(
        bookId: Int,
        pageNumber: Int,
        bookTitle: String,
        sourceText: String,
        languageCode: String = "en"
    ): List<VerbPhrasalVerbStudyTerm> {
        val healed = TextFormatUtils.healBrokenWordsAndHyphens(sourceText)
        val lowerText = healed.lowercase(Locale.getDefault())

        val matchedEntries = mutableListOf<VerbDictionaryEntry>()
        val targetLangCode = languageCode.lowercase()

        // 1. Busca por termos conhecidos no catálogo
        val langEntries = KNOWN_ENTRIES.filter {
            if (targetLangCode == "en") it.languageCode == "en"
            else it.languageCode == targetLangCode || it.languageCode == "en"
        }

        langEntries.forEach { entry ->
            val phrase = entry.verbOrPhrase.lowercase(Locale.getDefault())
            // Verifica correspondência exata de limites de palavra (word boundaries)
            val regex = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(lowerText)) {
                matchedEntries.add(entry)
            } else if (entry.base.isNotBlank()) {
                val baseRegex = Regex("\\b${Regex.escape(entry.base)}[a-z]{0,4}\\b", RegexOption.IGNORE_CASE)
                if (baseRegex.containsMatchIn(lowerText)) {
                    matchedEntries.add(entry)
                }
            }
        }

        // 2. Extrai SEMPRE TODOS os verbos e phrasal verbs dinâmicos presentes na página
        val dynamicWords = extractDynamicVerbalCandidates(healed, targetLangCode)
        dynamicWords.forEach { candidate ->
            matchedEntries.add(candidate)
        }

        // Se ainda estiver vazio (página em branco ou texto curtíssimo), garante verbos fundamentais
        if (matchedEntries.isEmpty()) {
            matchedEntries.addAll(
                KNOWN_ENTRIES.filter { it.verbOrPhrase in listOf("eat", "get across", "get by", "look for", "achieve") }
            )
        }

        // Converte cada entrada em VerbPhrasalVerbStudyTerm com 16 tempos verbais completos
        val results = matchedEntries.distinctBy { it.verbOrPhrase.lowercase(Locale.getDefault()).trim() }.map { entry ->
            val examples = generateSixteenTenseExamples(entry)
            VerbPhrasalVerbStudyTerm(
                bookId = bookId,
                pageNumber = pageNumber,
                bookTitle = bookTitle,
                verbOrPhrase = entry.verbOrPhrase,
                isPhrasalVerb = entry.isPhrasal,
                prepositionUsed = entry.prep,
                baseVerb = entry.base,
                grammaticalCategory = if (entry.isPhrasal) "Phrasal Verb" else "Verbo",
                languageCode = entry.languageCode,
                definitionTargetLang = entry.defEn,
                portugueseMeaning = entry.resolvedPortugueseMeaning,
                ipaTranscription = entry.ipa,
                examplesJson = VerbPhrasalVerbStudyTerm.buildJsonFromExamples(examples),
                createdAt = System.currentTimeMillis()
            )
        }

        // ORDENA RIGOROSAMENTE POR ORDEM ALFABÉTICA (A -> Z)
        // Ex: [eat], [get across], [get by], [look for], etc.
        return results.sortedBy { it.verbOrPhrase.lowercase(Locale.getDefault()).trim() }
    }

    private fun extractDynamicVerbalCandidates(text: String, lang: String): List<VerbDictionaryEntry> {
        val candidates = mutableListOf<VerbDictionaryEntry>()
        val words = text.split(Regex("[^a-zA-ZÀ-ÿ'\\-]+")).filter { it.length >= 2 }
        val englishPrepositions = setOf(
            "by", "across", "for", "up", "down", "in", "on", "out", "off",
            "away", "back", "over", "through", "into", "about", "with",
            "after", "to", "from", "along", "ahead", "around", "apart"
        )

        // 1. Varre pares consecutivos de palavras para detectar PHRASAL VERBS (ex: "get by", "get across", "look for")
        for (i in 0 until words.size - 1) {
            val rawW1 = words[i].lowercase(Locale.getDefault())
            val rawW2 = words[i + 1].lowercase(Locale.getDefault())

            val baseW1 = IRREGULAR_TO_BASE[rawW1] ?: rawW1

            if (englishPrepositions.contains(rawW2) && (COMMON_VERBS.containsKey(baseW1) || rawW1.length in 3..10)) {
                val phrase = "$baseW1 $rawW2"
                val meaningPt = COMMON_VERBS[baseW1]?.let { "$it (com preposição $rawW2)" }
                    ?: "Expressão verbal preposicionada contextualizada com '$rawW2'."
                candidates.add(
                    VerbDictionaryEntry(
                        verbOrPhrase = phrase,
                        isPhrasal = true,
                        prep = rawW2,
                        base = baseW1,
                        pt = meaningPt,
                        defEn = "Phrasal verb using preposition '$rawW2' in authentic context.",
                        ipa = "/$baseW1 $rawW2/",
                        languageCode = lang
                    )
                )
            }
        }

        // 2. Varre palavras individuais para detectar VERBOS SIMPLES presentes na página (ex: "eat", "drink", "speak", etc.)
        for (w in words) {
            val raw = w.lowercase(Locale.getDefault())
            val base = IRREGULAR_TO_BASE[raw] ?: raw
            val meaning = COMMON_VERBS[base]
            if (meaning != null && !englishPrepositions.contains(base)) {
                candidates.add(
                    VerbDictionaryEntry(
                        verbOrPhrase = base,
                        isPhrasal = false,
                        prep = "",
                        base = base,
                        pt = meaning,
                        defEn = "Standard verb in target language.",
                        ipa = "/$base/",
                        languageCode = lang
                    )
                )
            }
        }

        return candidates
    }

    /**
     * Gera rigorosamente 16 tempos verbais com Question Words obrigatórias e traduções para português.
     */
    fun generateSixteenTenseExamples(entry: VerbDictionaryEntry): List<VerbTenseQuestionExample> {
        val verb = entry.verbOrPhrase
        val base = entry.base.ifBlank { verb.substringBefore(" ") }
        val pt = entry.resolvedPortugueseMeaning.substringBefore(",").substringBefore(".")

        val isEnglish = entry.languageCode == "en"

        return if (isEnglish) {
            listOf(
                VerbTenseQuestionExample(
                    1, "1. Present Simple", "Why",
                    "Why do you always $verb in this situation?",
                    "Por que você sempre $pt nesta situação?",
                    "$verb + naturally"
                ),
                VerbTenseQuestionExample(
                    2, "2. Present Continuous", "Where",
                    "Where are they ${makeGerund(base)} ${verb.substringAfter(base, "")} right now?",
                    "Onde eles estão praticando a ação de $pt agora mesmo?",
                    "$verb + active context"
                ),
                VerbTenseQuestionExample(
                    3, "3. Present Perfect", "How",
                    "How has the team ${makePastParticiple(base)} ${verb.substringAfter(base, "")} recently?",
                    "Como a equipe tem realizado o ato de $pt recentemente?",
                    "$verb + recent accomplishments"
                ),
                VerbTenseQuestionExample(
                    4, "4. Present Perfect Continuous", "How long",
                    "How long have you been ${makeGerund(base)} ${verb.substringAfter(base, "")}?",
                    "Há quanto tempo você vem praticando a ação de $pt?",
                    "$verb + duration"
                ),
                VerbTenseQuestionExample(
                    5, "5. Past Simple", "When",
                    "When did the leader $verb yesterday?",
                    "Quando o líder realizou o ato de $pt ontem?",
                    "$verb + specific time"
                ),
                VerbTenseQuestionExample(
                    6, "6. Past Continuous", "Why",
                    "Why were the members ${makeGerund(base)} ${verb.substringAfter(base, "")} during the crisis?",
                    "Por que os membros estavam praticando a ação de $pt durante a crise?",
                    "$verb + historical context"
                ),
                VerbTenseQuestionExample(
                    7, "7. Past Perfect", "Where",
                    "Where had she ${makePastParticiple(base)} ${verb.substringAfter(base, "")} before the meeting?",
                    "Onde ela havia realizado a ação de $pt antes da reunião?",
                    "$verb + prior event"
                ),
                VerbTenseQuestionExample(
                    8, "8. Past Perfect Continuous", "How many hours",
                    "How many hours had they been ${makeGerund(base)} ${verb.substringAfter(base, "")} before taking a break?",
                    "Por quantas horas eles vinham praticando a ação de $pt antes de fazer uma pausa?",
                    "$verb + continuous past"
                ),
                VerbTenseQuestionExample(
                    9, "9. Future Simple (will)", "Who",
                    "Who will $verb tomorrow morning?",
                    "Quem vai praticar o ato de $pt amanhã de manhã?",
                    "$verb + future plan"
                ),
                VerbTenseQuestionExample(
                    10, "10. Future Continuous", "At what time",
                    "At what time will the specialists be ${makeGerund(base)} ${verb.substringAfter(base, "")}?",
                    "A que horas os especialistas estarão praticando a ação de $pt?",
                    "$verb + scheduled future"
                ),
                VerbTenseQuestionExample(
                    11, "11. Future Perfect", "By when",
                    "By when will the organization have ${makePastParticiple(base)} ${verb.substringAfter(base, "")}?",
                    "Até quando a organização terá completado o ato de $pt?",
                    "$verb + target deadline"
                ),
                VerbTenseQuestionExample(
                    12, "12. Future Perfect Continuous", "How many months",
                    "How many months will you have been ${makeGerund(base)} ${verb.substringAfter(base, "")} by December?",
                    "Por quantos meses você estará praticando a ação de $pt até dezembro?",
                    "$verb + extended future"
                ),
                VerbTenseQuestionExample(
                    13, "13. Conditional Simple (would)", "Why",
                    "Why would anyone $verb under these circumstances?",
                    "Por que alguém praticaria o ato de $pt sob estas circunstâncias?",
                    "$verb + hypothetical"
                ),
                VerbTenseQuestionExample(
                    14, "14. Conditional Continuous", "Where",
                    "Where would the experts be ${makeGerund(base)} ${verb.substringAfter(base, "")} if funding allowed?",
                    "Onde os especialistas estariam praticando a ação de $pt se o financiamento permitisse?",
                    "$verb + condition"
                ),
                VerbTenseQuestionExample(
                    15, "15. Conditional Perfect", "Which option",
                    "Which option would you have ${makePastParticiple(base)} ${verb.substringAfter(base, "")} if you were in charge?",
                    "Qual opção você teria escolhido para $pt se estivesse no comando?",
                    "$verb + past possibility"
                ),
                VerbTenseQuestionExample(
                    16, "16. Modal Perfect / Subjunctive", "Why should",
                    "Why should the community $verb when alternative methods exist?",
                    "Por que a comunidade deveria praticar a ação de $pt quando métodos alternativos existem?",
                    "$verb + moral/practical imperative"
                )
            )
        } else {
            // Suporte para outros idiomas com as 16 estruturas universais
            listOf(
                VerbTenseQuestionExample(1, "1. Presente", "¿Por qué?", "¿Por qué se utiliza '$verb' en este contexto?", "Por que se utiliza '$pt' neste contexto?", "$verb + contexto"),
                VerbTenseQuestionExample(2, "2. Presente Contínuo", "¿Dónde?", "¿Dónde están practicando '$verb' ahora mismo?", "Onde estão praticando '$pt' neste momento?", "$verb + agora"),
                VerbTenseQuestionExample(3, "3. Pretérito Perfeito", "¿Cómo?", "¿Cómo se ha logrado '$verb' en los últimos días?", "Como se alcançou '$pt' nos últimos dias?", "$verb + recente"),
                VerbTenseQuestionExample(4, "4. Pretérito Perfeito Contínuo", "¿Cuánto tiempo?", "¿Cuánto tiempo llevan practicando '$verb'?", "Há quanto tempo vêm praticando '$pt'?", "$verb + duração"),
                VerbTenseQuestionExample(5, "5. Passado Simples", "¿Cuándo?", "¿Cuándo se llevó a cabo '$verb' ayer?", "Quando se realizou '$pt' ontem?", "$verb + passado"),
                VerbTenseQuestionExample(6, "6. Passado Contínuo", "¿Por qué?", "¿Por qué estaban realizando '$verb' durante la sesión?", "Por que estavam realizando '$pt' durante a sessão?", "$verb + sessão"),
                VerbTenseQuestionExample(7, "7. Mais-que-Perfeito", "¿Dónde?", "¿Dónde se había manifestado '$verb' antes de la reunión?", "Onde havia se manifestado '$pt' antes da reunião?", "$verb + anterior"),
                VerbTenseQuestionExample(8, "8. Passado Contínuo Prévio", "¿Cuántas horas?", "¿Cuántas horas habían estado aplicando '$verb'?", "Por quantas horas vinham aplicando '$pt'?", "$verb + horas"),
                VerbTenseQuestionExample(9, "9. Futuro Simples", "¿Quién?", "¿Quién llevará adelante '$verb' mañana?", "Quem levará adiante '$pt' amanhã?", "$verb + amanhã"),
                VerbTenseQuestionExample(10, "10. Futuro Contínuo", "¿A qué hora?", "¿A qué hora estarán ejecutando '$verb'?", "A que horas estarão executando '$pt'?", "$verb + horário"),
                VerbTenseQuestionExample(11, "11. Futuro Perfeito", "¿Para cuándo?", "¿Para cuándo habrán completado '$verb'?", "Para quando terão completado '$pt'?", "$verb + conclusão"),
                VerbTenseQuestionExample(12, "12. Futuro Perfeito Contínuo", "¿Cuántos meses?", "¿Cuántos meses habrán estado aplicando '$verb' para fin de año?", "Por quantos meses terão estado aplicando '$pt' até o fim do ano?", "$verb + prazo"),
                VerbTenseQuestionExample(13, "13. Condicional Simples", "¿Por qué?", "¿Por qué se aplicaría '$verb' en este caso?", "Por que se aplicaria '$pt' neste caso?", "$verb + hipótese"),
                VerbTenseQuestionExample(14, "14. Condicional Contínuo", "¿Dónde?", "¿Dónde estarían aplicando '$verb' si hubiera recursos?", "Onde estariam aplicando '$pt' se houvesse recursos?", "$verb + recurso"),
                VerbTenseQuestionExample(15, "15. Condicional Perfeito", "¿Qué método?", "¿Qué método habrían elegido para '$verb'?", "Qual método teriam escolhido para '$pt'?", "$verb + escolha"),
                VerbTenseQuestionExample(16, "16. Modo Subjuntivo / Imperativo", "¿Por qué debería?", "¿Por qué debería la institución priorizar '$verb'?", "Por que a instituição deveria priorizar '$pt'?", "$verb + prioridade")
            )
        }
    }

    private fun makeGerund(base: String): String {
        val b = base.trim().lowercase()
        return when {
            b == "eat" -> "eating"
            b == "get" -> "getting"
            b == "run" -> "running"
            b == "set" -> "setting"
            b == "put" -> "putting"
            b.endsWith("ie") -> b.dropLast(2) + "ying"
            b.endsWith("e") && !b.endsWith("ee") -> b.dropLast(1) + "ing"
            else -> b + "ing"
        }
    }

    private fun makePastParticiple(base: String): String {
        val b = base.trim().lowercase()
        return when (b) {
            "eat" -> "eaten"
            "get" -> "gotten"
            "give" -> "given"
            "break" -> "broken"
            "take" -> "taken"
            "bring" -> "brought"
            "look" -> "looked"
            "achieve" -> "achieved"
            "develop" -> "developed"
            "turn" -> "turned"
            "run" -> "run"
            "figure" -> "figured"
            "come" -> "come"
            "put" -> "put"
            "set" -> "set"
            "hold" -> "held"
            "keep" -> "kept"
            "show" -> "shown"
            "stand" -> "stood"
            "call" -> "called"
            "watch" -> "watched"
            "write" -> "written"
            "speak" -> "spoken"
            "understand" -> "understood"
            else -> if (b.endsWith("e")) b + "d" else b + "ed"
        }
    }
}
