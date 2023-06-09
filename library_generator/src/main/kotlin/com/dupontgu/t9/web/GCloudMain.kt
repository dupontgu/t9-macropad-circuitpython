package com.dupontgu.t9.web

import KEY_MAP_FILE
import com.dupontgu.t9.*
import com.dupontgu.t9.web.DevBoard.*
import com.dupontgu.t9.writeKeyMapFile
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.apache.log4j.BasicConfigurator
import org.zeroturnaround.zip.ZipUtil
import java.lang.IllegalArgumentException
import kotlin.io.path.createTempFile

private const val ARCHIVE_NAME = "t9_library.zip"
private const val LIB_FILE_NAME = "library.t9l2"
private const val VERSION_FILE_NAME = "version.txt"

private enum class DevBoard(
    val displayName: String
) {
    QTPY_2040("Adafruit QT Py RP2040"),
    TINY_2040("Pimoroni Tiny 2040")
}

fun Application.main() {
    BasicConfigurator.configure()
    // This adds Date and Server headers to each response, and allows custom additional headers
    install(DefaultHeaders)
    // This uses use the logger to log every call (request/response)
    install(CallLogging)
    // Registers routes
    routing {

        get("/") { call.respondHtml(HttpStatusCode.OK, HTML::renderRootPage) }

        // Page that allows users to create their own T9 library from a text file
        get("/library") { call.respondHtml(HttpStatusCode.OK, HTML::renderLibraryGeneratorPage) }
        // Usage Instructions
        get("/usage") { call.respondHtml(HttpStatusCode.OK, HTML::renderUsageInstructionsPage) }

        get("/board") {
            val destination = call.request.queryParameters["destination"] ?: "assembly"
            call.respondHtml { renderBoardPickerPage(destination) }
        }

        fun ApplicationCall.parseDevBoard(): DevBoard? {
            val boardParam = parameters["board"]
            return try {
                valueOf(boardParam ?: QTPY_2040.name)
            } catch (e: IllegalArgumentException) {
                println("ERROR: Invalid board specified: $boardParam")
                null
            }
        }

        get("/assembly/{board}") {
            val devBoard = call.parseDevBoard() ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondHtml(HttpStatusCode.OK) { renderKitInstructionsPage(devBoard) }
        }

        get("/firmware/{board}") {
            val devBoard = call.parseDevBoard() ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondHtml(HttpStatusCode.OK) { renderFirmwarePage(devBoard) }
        }

        get("/fw-update/{board}") {
            val devBoard = call.parseDevBoard() ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondHtml(HttpStatusCode.OK) { renderFirmwareUpdatePage(devBoard) }
        }

        val keyMapPrefix = "keyMap"
        val libraryPrefix = "library"
        post("/upload") { _ ->
            val multipart = call.receiveMultipart()
            var responded = false
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val libraryFile = createTempFile(prefix = libraryPrefix).toFile()
                    val result = serializeLibraryFile(part.streamProvider(), libraryFile.outputStream())
                    if (result is LibraryResult.Error) {
                        call.respond(HttpStatusCode.UnprocessableEntity, result.message)
                        responded = true
                    } else if (result is LibraryResult.Success) {
                        val keyMapFile = createTempFile(prefix = keyMapPrefix).toFile()
                        val archive = createTempFile(prefix = "archive").toFile()
                        writeKeyMapFile(result, keyMapFile)
                        ZipUtil.packEntries(arrayOf(libraryFile, keyMapFile), archive) {
                            when {
                                it.startsWith(keyMapPrefix) -> KEY_MAP_FILE
                                it.startsWith(libraryPrefix) -> LIB_FILE_NAME
                                else -> it
                            }
                        }
                        call.response.header("Content-Disposition", "attachment; filename=\"$ARCHIVE_NAME\"")
                        call.respondFile(archive)
                        responded = true
                    }
                }
                part.dispose()
            }
            if (!responded) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Something went wrong. Did you actually submit a file?"
                )
            }
        }
    }
}

fun HTML.renderRootPage() {
    head {
        title { +"Standalone T9 Keypad" }
    }
    body {
        h1 { +"Standalone T9 Keypad" }
        h2 { +"By Guy Dupont" }
        h3 { +"If you already have a working T9 Keypad:" }
        ul {
            li { +"Read the usage instructions "; a("/usage") { +"here." }; }
            li { +"Generate your own custom T9 library "; a("/library") { +"here." }; }
            li { +"Update to the latest version of the firmware "; a("/board?destination=fw-update") { +"here." }; }
        }
        h3 { +"If you do not yet have a T9 Keypad:" }
        ul {
            li {
                +"Purchase "; a("https://www.etsy.com/shop/EsotericGadgetsByGuy?ele=shop_open") { +"here!!" }; +" (kits available as well)";
            }
        }
        h3 { +"If you have a kit, or are building on your own:" }
        ul {
            li { +"Assembly instructions are "; a("/board?destination=assembly") { +"here." }; }
            li { +"If you need to install the T9 firmware, check "; a("/board?destination=firmware") { +"here." }; }
        }

        h3 { +"If you want to get in touch with Guy:" }
        ul {
            li { +"Follow Guy on Twitter "; a("https://twitter.com/gvy_dvpont") { +"here." }; }
            li { +"Email Guy at "; a("mailto:gvy.dvpont@gmail.com") { +"gvy.dvpont@gmail.com." }; }
        }

        h3 { +"If want to learn more about the project:" }
        ul {
            li { +"Watch the YouTube video "; a("https://youtu.be/6cbBSEbwLUI") { +"here." }; }
            li { +"See the Hackaday.io project page "; a("https://hackaday.io/project/179977-standalone-t9-predictive-keyboard") { +"here." }; }
            li { +"Find the source code "; a("https://github.com/dupontgu/t9-macropad-circuitpython") { +"here." }; }
        }

    }
}

private fun BODY.narrowDiv(content: DIV.() -> Unit) {
    div {
        style {
            unsafe {
                raw(
                    """
                        div { max-width: 800px; }
                    """
                )
            }
        }
        content()
    }
}

private fun HTML.renderBoardPickerPage(destination: String) {
    head {
        title { +"Choose Your Dev Board" }
    }
    body {
        h1 { +"Which development board do you have?" }
        narrowDiv {
            ul {
                values().forEach {
                    li {
                        a("/$destination/${it.name}") { +it.displayName }
                    }
                }
            }
        }
    }
}

private fun HTML.renderKitInstructionsPage(devBoard: DevBoard) {
    head {
        title { +"T9 Keypad Assembly" }
        style {
            unsafe {
                raw(
                    """
                       img { max-width: 800px; }
                       """
                )
            }
        }
    }
    body {
        h1 { +"Assembling the T9 Macropad" }
        h3 { a("/") { +"(Project Root)" } }
        narrowDiv {
            b { +"A few things before you get started:" }
            ul {
                li { +"Confirm that you're using the ${devBoard.displayName} development board." }
                li { +"These instructions assume you have some soldering ability. This project is not too bad! But you might not want this to be the first thing you've ever soldered." }
                li {
                    +"This dev board has castellated pads, and will be soldered on to the surface of my PCB. If you've never done this kind of soldering, "
                    a("https://www.youtube.com/watch?v=rGvvwXrv310") { +"This tutorial is awesome." }
                }
            }
        }
        h3 { +"Step 1: Attach your dev board" }
        img(src = "/static/${devBoard.name}/pad_lineup.jpg")
        narrowDiv {
            +"""
                |Solder the castellated pins of your dev board on to the pads on the Macropad as shown. 
                |The two pins closest to the USB jack on your dev board should line up with the uppermost 
                |pads on the macropad. Solder all pins that come in contact with a pad, and note that some 
                |dev boards may extend beyond the pads.
                |""".trimMargin()
        }
        h3 { +"Step 2: Solder the diodes" }
        img(src = "/static/single_diode.jpg")
        narrowDiv {
            +"""
                |Line your 12 diodes up with white silkscreen outlines on the macropad - they're labeled D1 through D12.
                |Each diode should have a stripe or dark marking indicating which terminal is the cathode (-). 
                |Orient each diode such that it's cathode terminal is pushed through the hole on the right. 
                |Push each positive terminal through the corresponding left hole, and solder all terminals to the board. 
                |""".trimMargin()
        }
        h3 { +"Step 3: Attach your key switches" }
        img(src = "/static/key_sw_back.jpg"); br()
        img(src = "/static/key_sw_thru.jpg")
        narrowDiv {
            +"""
                |Push your key switches into each of the 12 slots, and solder the metal terminals to the macropad from the bottom.
                |""".trimMargin()
        }
        br(); br()
        +"That's it for physical assembly. Instructions for installing firmware on a fresh ${devBoard.displayName} are "
        a("/firmware/${devBoard.name}") { +"here" }
    }
}

private val DevBoard.circuitPythonLink: String
    get() = when (this) {
        QTPY_2040 -> "https://circuitpython.org/board/adafruit_qtpy_rp2040/"
        TINY_2040 -> "https://circuitpython.org/board/pimoroni_tiny2040/"
    }

private val DevBoard.fwZipName: String
    get() = when (this) {
        QTPY_2040 -> "qtpy2040"
        TINY_2040 -> "tiny2040"
    }

private fun HTML.renderFirmwarePage(devBoard: DevBoard) {
    head {
        title { +"T9 Firmware Instructions" }
    }
    body {
        h1 { +"Installing T9 CircuitPython Firmware" }
        h3 { a("/") { +"(Project Root)" } }
        narrowDiv {
            ol {
                li {
                    +"Dowload the latest version of CircuitPython for the "
                    a(devBoard.circuitPythonLink) { +"${devBoard.displayName}." }
                    +" As of April 2022, version 7.2.5 works great on all boards."
                }
                li {
                    +"On your development board, hold the button labeled 'boot', and plug it into your computer."
                }
                li {
                    +"""
                        After a few seconds, you'll see a new storage drive mounted to your computer. Release the button.
                        Drag the UF2 file downloaded in step 1 onto that drive. This will install CircuitPython.
                    """.trimIndent()
                }
                li {
                    +"""
                        When the transfer is complete, the dev board will reboot and the storage drive will remount.
                        Confirm that the drive is now named 'CIRCUITPY'. This indicates a successful CircuitPython installation.
                        We still have to install the T9 code!
                    """.trimIndent()
                }
                li {
                    +"Find the latest T9 firmware release on the "
                    a("https://github.com/dupontgu/t9-macropad-circuitpython/releases") { +"GitHub releases page. " }
                    +"""
                        Each release should have a list of .zip files associated with it. Find the one that matches your dev board.
                        This board's zip file should be called: ${devBoard.fwZipName}.zip. Download and unzip it.
                    """.trimIndent()
                }
                li {
                    +"""
                        Copy all of the files from the extracted ${devBoard.fwZipName} folder onto your dev board, 
                        which should still be mounted on your computer as 'CIRCUITPY'. Overwrite any conflicting files.
                        This may take a minute. 
                    """.trimIndent()
                }
                li {
                    +"""
                        Download the CircuitPython library bundle that matches the CircuitPython version that you installed.
                        You can find all versions of the bundle 
                    """.trimIndent()
                    a("https://circuitpython.org/libraries") { +"here." }
                }
                li {
                    +"""
                        From the downloaded ${devBoard.fwZipName} folder, open the 'README' file.
                        It will contain a list of CircuitPython libraries that need to be installed as a final step.
                        Find each of the listed libraries in the CircuitPython bundle that you downloaded in the previous step,
                        and copy them into a folder called 'lib' on your 'CIRCUITPY' drive.
                        Some library files - such as the 'adafruit_hid' library - need to be grouped into a subfolder within 'lib'.
                        The 'README' file should indicate when this is the case. Here is an example of a matching 'README' 
                        and matching directory structure. NOTE THAT THE LIBRARIES IN THIS IMAGE MAY NOT BE UP TO DATE!
                    """.trimIndent()
                    img(src = "/static/library_hierarchy.png")
                }
                li {
                    +"""
                        With CircuitPython, the T9 firmware, and the libraries installed, the board should reboot
                        and the light should turn blue after ~5 seconds. You're good to go! Check out the full usage instructions 
                    """.trimIndent()
                    a("/usage") { +"here!" }
                }
            }
        }
    }
}

private fun HTML.renderFirmwareUpdatePage(devBoard: DevBoard) {
    head {
        title { +"T9 Firmware Update Instructions" }
    }
    body {
        h1 { +"Updating T9 CircuitPython Firmware" }
        h2 { +"Note - only follow these steps if you already have a working T9 Keypad." }
        h3 { a("/") { +"(Project Root)" } }
        narrowDiv {
            ol {
                li {
                    +"""
                        Plug your T9 keypad into your computer. Your computer should recognize it as both a 
                        keyboard and a mass storage device. The storage device is likely named: 'CIRCUITPY'.
                    """.trimIndent()
                }
                li {
                    +"Find the latest T9 firmware release on the "
                    a("https://github.com/dupontgu/t9-macropad-circuitpython/releases") { +"GitHub releases page. " }
                    +"""
                        Each release should have a list of .zip files associated with it. Find the one that matches your dev board.
                        This board's zip file should be called: ${devBoard.fwZipName}.zip. Download and unzip it.
                    """.trimIndent()
                }
                li {
                    +"""
                        Copy all of the files from the extracted ${devBoard.fwZipName} folder onto your dev board, 
                        which should still be mounted on your computer as 'CIRCUITPY'. Overwrite any conflicting files.
                        This may take a minute. Do not delete any existing files!
                    """.trimIndent()
                }
                li {
                    +"""
                        The board should automatically reboot
                        and the light should turn blue after ~5 seconds. You're good to go! Check out the full usage instructions 
                    """.trimIndent()
                    a("/usage") { +"here!" }
                }
            }
        }
    }
}

fun HTML.renderUsageInstructionsPage() {
    head {
        title { +"T9 Keypad Usage Instructions" }
    }
    body {
        h1 { +"Usage Instructions" }
        h3 { a("/") { +"(Project Root)" } }
        unsafe {
            +"<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/ESmvoqq3m-g\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>"
        }; br(); br()
        narrowDiv {
            ol {
                li { +"In general, you just need to tap the number key (2-9) containing the letter you’re trying to type. If you get to the end of the word, and there’s a word there that’s not the one you want, just hit the # button. That will cycle through all valid words that match the key pattern you’ve entered. For example, ‘gate’ and ‘have’ would both be available after typing 4-2-8-3." }
                li { +"Hit ‘0’ to enter a space." }
                li { +"Hit ‘1’ for backspace." }
                li { +"Hold ‘#’ to hit enter." }
                li { +"‘*’ is the modifier key. It changes the behavior of all other keys while it is being held. To access punctuation, press the ‘#’ key while holding ‘*’. Pressing repeatedly will cycle through punctuation marks. Hitting the other number keys while holding ‘*’ will simulate multi-tap typing. That’s when you click each number repeatedly to cycle through letters. For example, holding ‘*’ and pressing ‘3’ twice quickly will enter the letter ‘E’." }
                li { +"If you want to use this keyboard in normal numeric mode, hold the 1 button while cycling the power or hitting the ‘Reset’ button. Hold for ~5 seconds. The LED should flash red twice, and then stay that way until you reboot again." }
                li { +"Similarly, you can use this keyboard in “macro” mode, where each of the buttons (‘1’->’#’) will map to the standard function keys (F1->F12). To use this, hold the ‘2’ button while power-cycling the keyboard. Hold for ~5 seconds. The LED should flash green twice, and then stay that way until you reboot again." }
                li { +"You’ll notice that the device shows up as both a keyboard and a flash drive on your PC. If you’re unfamiliar with CircuitPython, those files are the actual firmware for this device. Changing them can break things! If you’re comfortable with Python, feel free to mess around." }
                li { +"There is a file on the flash drive called “priority_words.txt”. This is the list of words that you’d like to show up first, given a matching key sequence. If you find that there’s a certain key sequence that defaults to a word you don’t care about, you can specify the word you’d like to show up here. It will only work for words that are included in the word library." }
                li {
                    a("../library") { +"Click here" }
                    +" if you want to upload your own word library to the device! You can download and edit the default one from that web page if you want a good starting point."
                }
            }
        }
    }
}

fun HTML.renderLibraryGeneratorPage() {
    head {
        title { +"T9 Library Generator" }
    }
    body {
        h1 { +"Guy's T9 Library Generator" }
        h2 { +"IMPORTANT! Library files generated by this page will only work with keypads running (at least) version 2 of the firmware." }
        narrowDiv {
            +"""
                If you are unsure which version of the firmware you are running, plug your keypad into a computer. 
                Open the CIRCUITPY drive in your file explorer.
                Look for a file named $VERSION_FILE_NAME. If the file does not exist, you are running version 1 and need to update.
                Otherwise, the file will tell you which version is currently running.
                If you need to update, follow the instructions 
            """.trimIndent()
            a("/board?destination=fw-update") { +"here." };
        }
        h3 { a("/") { +"(Project Root)" } }
        unsafe {
            +"<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/l2_QFRZjvGE\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>"
        }; br(); br()
        +"This page allows you to customize the built-in word library on your "
        a("https://hackaday.io/project/179977-standalone-t9-predictive-keyboard") {
            +"Standalone T9 Predictive Keyboard."
        }; br()
        +"Use the form below to upload a text file containing the words you'd like available on your keyboard."; br()
        +"The file should be a plain .txt file, and each line should contain exactly one word."; br()
        +"The T9 algorithm is language agnostic, but only the following characters are currently supported:"; br()
        +"${charMap.keys}"; br()
        a("https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/english.txt") { +"Here" }
        +" you can find a default list of English words. Feel free to download this file (File -> Save As) and add or remove words as you see fit!"; br()
        +"For other languages, check "
        a("https://github.com/oprogramador/most-common-words-by-language/tree/master/src/resources") { +"here." }
        br(); br()
        +"When you successfully upload a word library file using the \"Browse\" button below, you will be given the option to download a new file, named "
        b { +ARCHIVE_NAME }; +"."; br()
        +"Extract all files from this archive, them drag-and-drop them on to your T9 keyboard drive (likely named CIRCUITPY). Replace the existing files with the same name."; br()
        b { +"Back up the old files first if you want to save them!"; br() }
        +"Once the new files have loaded (it may take a minute or so), the keyboard will reboot and your new library will be loaded!"
        br(); br()
        form(action = "upload", method = FormMethod.post) {
            input(InputType.file, formEncType = InputFormEncType.multipartFormData, name = "File here!")
            input(InputType.submit, formEncType = InputFormEncType.multipartFormData, name = "Submit Library File")
        }
        br(); br()
        a("mailto:gvy.dvpont@gmail.com?subject=T9%20Library%20Generator") {
            +"(Contact me if you have issues/questions)"
        }
    }
}

