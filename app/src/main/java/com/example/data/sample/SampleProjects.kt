package com.example.data.sample

import com.example.data.model.Folder
import com.example.data.model.Project

object SampleProjects {

    val defaultFolders = listOf(
        Folder(id = 1, name = "Aulas de Inglês", colorHex = "#3F51B5"),
        Folder(id = 2, name = "Podcasts & Entrevistas", colorHex = "#E91E63"),
        Folder(id = 3, name = "Vídeos Curto & Reels", colorHex = "#009688")
    )

    val defaultProjects = listOf(
        Project(
            id = 1,
            title = "Aula de Inglês - Diálogo no Aeroporto",
            folderId = 1,
            mediaType = "AUDIO",
            srtContent = """
1
00:00:00,500 --> 00:00:03,200
Olá! Bem-vindo ao curso de inglês prático.

2
00:00:03,500 --> 00:00:06,800
Hoje vamos aprender frases úteis para usar no aeroporto.

3
00:00:07,100 --> 00:00:10,500
Primeiro: "Where is the check-in desk?" significa onde fica o balcão.

4
00:00:10,900 --> 00:00:14,200
Repita comigo com muita calma: Where is the check-in desk?

5
00:00:14,600 --> 00:00:18,000
Muito bem! Agora pratique com o áudio e acompanhe o texto.
            """.trimIndent(),
            timeOffsetMs = 0L,
            fontSizeSp = 24f,
            isFavorite = true
        ),
        Project(
            id = 2,
            title = "Podcast Tech - Inteligência Artificial",
            folderId = 2,
            mediaType = "AUDIO",
            srtContent = """
1
00:00:00,800 --> 00:00:04,100
Sejam bem-vindos ao episódio semanal sobre tecnologia e inovação.

2
00:00:04,500 --> 00:00:08,200
O avanço do áudio e da sincronização de legendas revoluciona a educação.

3
00:00:08,600 --> 00:00:12,400
Com este aplicativo, cada palavra falada é destacada em tempo real.

4
00:00:12,800 --> 00:00:16,500
Você pode ajustar milissegundos com precisão absoluta.
            """.trimIndent(),
            timeOffsetMs = 100L,
            fontSizeSp = 28f,
            isFavorite = false
        ),
        Project(
            id = 3,
            title = "Vídeo Curto - Apresentação do App",
            folderId = 3,
            mediaType = "VIDEO",
            srtContent = """
1
00:00:00,200 --> 00:00:03,500
Este é o Sincronizador de Legendas SRT e Áudio!

2
00:00:03,800 --> 00:00:07,100
Edite seus projetos, organize em pastas por temas e exporte facilmente.

3
00:00:07,400 --> 00:00:10,800
Ative o modo tela cheia para apresentação sem distrações.
            """.trimIndent(),
            timeOffsetMs = -200L,
            fontSizeSp = 32f,
            isFavorite = true
        )
    )

    val defaultPdfBooks = listOf(
        com.example.data.model.PdfBook(
            id = 1,
            title = "Dom Casmurro - Obra Clássica",
            author = "Machado de Assis",
            totalPages = 4,
            currentPage = 1,
            speechSpeed = 1.0f,
            readingTheme = "MONOCHROME_BW",
            category = "Fiction",
            subcategory = "Classics",
            categoryColor = "#9333EA",
            sampleContentJson = """[
                "Capítulo I - Do Título. Uma noite destas, vindo da cidade para o Engenho Novo, encontrei no trem da Central um rapaz aqui do bairro, o qual eu conhecia de vista e de chapéu. Cumprimentou-me, sentou-se ao pé de mim, falou da lua e dos ministros, e acabou recitando-me versos. A viagem era curta, e os versos pode ser que não fossem inteiramente maus. Sucedeu, porém, que, como eu estava cansado, fechei os olhos três ou quatro vezes; tanto bastou para que ele interrompesse a leitura e metesse os versos no bolso.",
                "Capítulo II - Do Casmurro. Quando abri os olhos, ele já não falava; estava amurrado. No dia seguinte entrou a dizer de mim nomes feios, e acabou alcunhando-me de Dom Casmurro. Os vizinhos, que não gostam dos meus hábitos reclusos e calados, deram curso à alcunha, que afinal pegou. Nem por isso me zanguei. Contei a anedota aos amigos da cidade, e eles, por graça, chamam-me assim, alguns em cartas: Dom Casmurro, domingo vou jantar com você.",
                "Capítulo III - A Ópera da Vida. Capitu era Capitu, isto é, uma criatura muito particular, mais mulher do que eu era homem. Se ainda não disse, aí fica. Se disse, fica também. Há conceitos que se devem incutir na alma do leitor, à força de repetição. Tinha olhos de cigana oblíqua e dissimulada. Eu não sabia o que era oblíqua, mas dissimulada sabia, e queria ver se se podiam chamar assim. Capitu deixou-se fitar e examinar.",
                "Capítulo IV - O Segredo de Bentinho. Olhos de ressaca? Vá, de ressaca. É o que me dá ideia daquela feição nova. Traziam não sei que fluido misterioso e enérgico, uma força que arrastava para dentro, como a vaga que se retira da praia nos dias de ressaca. Para não ser arrastado, agarrei-me às outras partes vizinhas, às orelhas, aos braços, aos cabelos espalhados pelos ombros; mas tão depressa entrava no círculo dos olhos, a vaga crescia e engolia-me."
            ]""".trimIndent()
        ),
        com.example.data.model.PdfBook(
            id = 2,
            title = "The Art of Mastery & Immersion",
            author = "Language & Mind Institute",
            totalPages = 3,
            currentPage = 1,
            speechSpeed = 1.15f,
            readingTheme = "PAPER_SEPIA",
            category = "Self-Help, Relationships & Lifestyle",
            subcategory = "Personal Growth & Inspiration",
            categoryColor = "#10B981",
            sampleContentJson = """[
                "Chapter 1 - The Power of Dual Immersion. Deep immersion and listening to high-quality neural audio while simultaneously following the printed text is one of the most scientifically proven cognitive methods for acquiring expansive vocabulary, flawless natural pronunciation, and deep grammatical intuition effortlessly.",
                "Chapter 2 - Neural Synapses and Retention. When you listen to voice synthesis synchronized line by line, your brain forms rapid auditory and visual neural pathways. Repeat every key phrase aloud, star your favorite paragraphs, and explore definitions and context instantly with Gemini AI.",
                "Chapter 3 - The Flow State in Deep Reading. Entering a flow state requires full presence without interruptions. By eliminating distractions and using high-contrast monochromatic typography, comprehension accelerates by over four hundred percent."
            ]""".trimIndent()
        ),
        com.example.data.model.PdfBook(
            id = 3,
            title = "Pequeno Manual da Sabedoria Estoica",
            author = "Sêneca & Epicteto",
            totalPages = 3,
            currentPage = 1,
            speechSpeed = 0.95f,
            readingTheme = "DARK_OLED",
            category = "Society, Politics & Philosophy",
            subcategory = "Ancient & Medieval Philosophy",
            categoryColor = "#854D0E",
            sampleContentJson = """[
                "Seção 1 - Sobre a Brevidade da Vida. Não é que tenhamos pouco tempo, mas é que desperdiçamos muito. A vida é suficientemente longa e uma generosa quantidade nos foi dada para a realização das maiores tarefas, se toda ela for bem investida.",
                "Seção 2 - O Domínio do Momento Presente. Quem sabe aproveitar cada hora, quem estabelece ordem em seu dia como se fosse o último, não anseia pelo amanhã nem teme o destino. Aprenda a ler cada linha com tranquilidade, serenidade e foco inabalável.",
                "Seção 3 - A Paz Interior e o Desapego. Nenhum bem aproveita a quem o possui se o ânimo não estiver preparado para a sua perda. Fortaleça sua mente na verdade e cultive a clareza em todas as decisões cotidianas."
            ]""".trimIndent()
        ),
        com.example.data.model.PdfBook(
            id = 4,
            title = "Python on Windows - Official Guide",
            author = "Python Documentation Team",
            totalPages = 15,
            currentPage = 1,
            speechSpeed = 1.0f,
            readingTheme = "MONOCHROME_BW",
            category = "Computers",
            subcategory = "Programming",
            categoryColor = "#06B6D4",
            sampleContentJson = """[
                "Table of Contents\n\n4. Using Python on Windows\n▪ 4.1. Python install manager\n  ▪ 4.1.1. Installation\n  ▪ 4.1.2. Basic use\n  ▪ 4.1.3. Command help\n  ▪ 4.1.4. Listing runtimes\n  ▪ 4.1.5. Installing runtimes\n  ▪ 4.1.6. Offline installs\n  ▪ 4.1.7. Uninstalling runtimes\n  ▪ 4.1.8. Configuration\n  ▪ 4.1.9. Shebang lines\n  ▪ 4.1.10. Advanced installation\n  ▪ 4.1.11. Administrative configuration\n  ▪ 4.1.12. Installing free-threaded binaries\n  ▪ 4.1.13. Index signatures\n  ▪ 4.1.14. Troubleshooting\n▪ 4.2. The embeddable package\n  ▪ 4.2.1. Python application\n  ▪ 4.2.2. Embedding Python\n▪ 4.3. The nuget.org packages\n  ▪ 4.3.1. Free-threaded packages\n▪ 4.4. Alternative bundles\n▪ 4.5. Supported Windows versions\n▪ 4.6. Removing the MAX_PATH limitation\n▪ 4.7. UTF-8 mode\n▪ 4.8. Finding modules\n▪ 4.9. Additional modules\n  ▪ 4.9.1. PyWin32\n  ▪ 4.9.2. cx_Freeze\n▪ 4.10. Compiling Python on Windows\n▪ 4.11. The full installer (deprecated)\n  ▪ 4.11.1. Installation steps\n  ▪ 4.11.2. Removing the MAX_PATH",
                "4. Using Python on Windows\n\nThis document aims to give an overview of Windows-specific behaviour you should know about when using Python on Microsoft Windows. Unlike most Unix systems, Windows does not include a system installation of Python by default. The official Windows releases are built as standard MSI or EXE installers with complete C runtime libraries and toolchains.",
                "4.1. Python install manager\n\nThe Python install manager (py.exe) is a utility that helps in locating and executing different Python versions on Windows. It allows scripts or the command line to indicate a preference for a specific Python version, and will locate and invoke that version.",
                "4.1.1. Installation\n\nTo install Python using the installer manager, download the recommended installer from python.org. Run the installer and check 'Add python.exe to PATH' to allow direct command line execution from CMD or PowerShell.",
                "4.1.2. Basic use\n\nInvoking py with script arguments executes Python. To launch the latest version of Python installed on your system, simply run 'py'. You can also specify an exact version such as 'py -3.12' to target that specific runtime.",
                "4.1.3. Command help\n\nRun 'py --help' to display the full list of options, including runtime selection, virtual environment detection, and shebang line handling.",
                "4.1.4. Listing runtimes\n\nThe command 'py --list' or 'py -0' will list all installed Python runtimes on your machine, distinguishing between 32-bit and 64-bit architectures.",
                "4.1.5. Installing runtimes\n\nNew runtimes can be installed automatically via winget or the official Python installer. The install manager immediately recognizes new registry entries.",
                "4.2. The embeddable package\n\nThe embedded distribution is a ZIP file containing a minimal Python environment. It is intended for acting as part of a larger application, rather than being directly accessed by end-users.",
                "4.2.1. Python application\n\nAn application written in Python does not necessarily require users to be aware of that fact. The embedded distribution may be used in this case to bundle Python directly.",
                "4.3. The nuget.org packages\n\nThe nuget.org package is a compact Python environment intended for use on continuous integration (CI) and build systems where Python is not installed globally.",
                "4.5. Supported Windows versions\n\nPython 3.12+ supports Windows 10 and newer, as well as Windows Server 2016 and newer. Support for Windows 7 and 8 was discontinued in Python 3.9.",
                "4.6. Removing the MAX_PATH limitation\n\nHistorically, Windows file paths have been limited to 260 characters. Long path support can be enabled in Windows 10 version 1607 and later via Group Policy or the Python installer option.",
                "4.9. Additional modules\n\nPython on Windows includes standard library modules for interacting with the operating system, including winreg, msvcrt, and ctypes for calling native Win32 APIs.",
                "4.10. Compiling Python on Windows\n\nIf you want to compile CPython from source, you will need Microsoft Visual Studio with C++ tools installed. Run the PCbuild/build.bat script to build all binaries.",
                "4.11. Diagnostics and Performance Tuning\n\nWhen running mission-critical Python workloads on Windows Server or Windows 11, configure environment variables such as PYTHONUTF8=1 and ensure appropriate thread stack allocations. Profiling can be conducted with standard tools to monitor execution speed and memory consumption."
            ]""".trimIndent()
        )
    )
}
