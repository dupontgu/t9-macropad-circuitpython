package com.dupontgu.t9.web

import LibraryResult
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
import serializeLibraryFile
import java.util.*
import kotlin.io.path.createTempFile

private const val LIB_FILE_NAME = "library.t9l"

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

        post("/upload") { _ ->
            val multipart = call.receiveMultipart()
            var responded = false
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val tempFile = createTempFile(prefix = UUID.randomUUID().toString()).toFile()
                    val result = serializeLibraryFile(part.streamProvider(), tempFile.outputStream())
                    if (result is LibraryResult.Error) {
                        call.respond(HttpStatusCode.UnprocessableEntity, result.message)
                    } else {
                        call.response.header("Content-Disposition", "attachment; filename=\"$LIB_FILE_NAME\"")
                        call.respondFile(tempFile)
                    }
                    responded = true
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
        +"Read the usage instructions "; a("/usage") { +"here." }; br()
        +"Generate your own custom T9 library "; a("/library") { +"here." }; br()
        +"Purchase "; a("https://www.etsy.com/shop/EsotericGadgetsByGuy?ele=shop_open") { +"here!!" }; +" (limited supply)"; br();
        +"Follow Guy on Twitter "; a("https://twitter.com/gvy_dvpont") { +"here." }; br()
        +"Email Guy at "; a("mailto:gvy.dvpont@gmail.com") { +"gvy.dvpont@gmail.com." }; br()
        +"Watch the YouTube video "; a("https://youtu.be/6cbBSEbwLUI") { +"here." }; br()
        +"See the Hackaday.io project page "; a("https://hackaday.io/project/179977-standalone-t9-predictive-keyboard") { +"here." }; br()
        +"Find the source code "; a("https://github.com/dupontgu/t9-macropad-circuitpython") { +"here." }; br()
    }
}

fun HTML.renderUsageInstructionsPage() {
    head {
        title { +"T9 Keypad Usage Instructions" }
    }
    body {
        h1 { +"Usage Instructions" }
        h3 { a("../") { +"(Project Root)" } }
        div {
            style {
                unsafe {
                    raw("""
                        div { max-width: 800px; }
                    """)
                }
            }
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
        h3 { a("../") { +"(Project Root)" } }
        +"This page allows you to customize the built-in word library on your "
        a("https://hackaday.io/project/179977-standalone-t9-predictive-keyboard") {
            +"Standalone T9 Predictive Keyboard."
        }; br()
        +"Use the form below to upload a text file containing the words you'd like available on your keyboard."; br()
        +"The file should be a plain .txt file, and each line should contain exactly one word."; br()
        +"Currently, words can only contain the letters A-Z (upper or lowercase), and can only be 25 characters long."; br()
        a("https://raw.githubusercontent.com/dupontgu/t9-macropad-circuitpython/main/library_generator/src/main/resources/dict.txt") { +"Here" }
        +" you can find my default list of words. Feel free to download this file (File -> Save As) and add or remove words as you see fit!"
        br(); br()
        +"When you successfully upload a word library file using the \"Browse\" button below, you will be given the option to download a new file, named "
        b { +LIB_FILE_NAME }; +"."; br()
        +"Drag and drop this .t9l file on to your T9 keyboard drive (likely named CIRCUITPY) and replace the existing file with the same name."; br()
        +"Back up the old file first if you want to save it! You can name it anything, but the keyboard will only use the file named $LIB_FILE_NAME as it's current library. "; br()
        +"Once the new file has loaded (it may take a minute or so), the keyboard will reboot and your new library will be loaded!"
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
