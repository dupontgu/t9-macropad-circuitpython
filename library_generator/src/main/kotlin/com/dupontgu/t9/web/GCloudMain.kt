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

        post("/upload") { _ ->
            val multipart = call.receiveMultipart()
            var responded = false
            multipart.forEachPart { part ->
                if(part is PartData.FileItem) {
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
                call.respond(HttpStatusCode.InternalServerError, "Something went wrong. Did you actually submit a file?")
            }
        }

    }
}

fun HTML.renderRootPage() {
    head {
        title { +"Standalone T9 Keypad" }
    }
    body {
        +"Generate your own custom T9 library "; a("/library") { +"here." }; br()
        +"Follow Guy on Twitter "; a("https://twitter.com/gvy_dvpont") { +"here." }; br()
        +"Watch the YouTube video "; a("https://youtu.be/6cbBSEbwLUI") { +"here." }; br()
        +"See the project page "; a("https://hackaday.io/project/179977-standalone-t9-predictive-keyboard") { +"here." }; br()
        +"Find the source code "; a("https://github.com/dupontgu/t9-macropad-circuitpython") { +"here." }; br()
    }
}

fun HTML.renderLibraryGeneratorPage() {
    head {
        title { +"T9 Library Generator" }
    }
    body {
        h1 { +"Guy's T9 Library Generator" }
        +"This page allows you to customize the built-in word library on your "
        a("https://hackaday.io/project/179977-standalone-t9-predictive-keyboard") {
            + "Standalone T9 Predictive Keyboard."
        }; br()
        +"Use the form below to upload a text file containing the words you'd like available on your keyboard."; br()
        +"The file should be a plain .txt file, and each line should contain exactly one word."; br()
        +"Currently, words can only contain the letters A-Z (upper or lowercase), and can only be 25 characters long."; br()
        a("https://raw.githubusercontent.com/dupontgu/t9-macropad-circuitpython/main/library_generator/src/main/resources/dict.txt") { +"Here" }
        +" you can find my default list of words. Feel free to download this file (File -> Save As) and add or remove words as you see fit!"
        br(); br()
        +"When you successfully upload a word library file using the \"Browse\" button below, you will be given the option to download a new file, named "
        b { +LIB_FILE_NAME }; +"."; br()
        + "Drag and drop this .t9l file on to your T9 keyboard drive (likely named CIRCUITPY) and replace the existing file with the same name."; br()
        + "Back up the old file first if you want to save it! You can name it anything, but the keyboard will only use the file named $LIB_FILE_NAME as it's current library. "; br()
        + "Once the new file has loaded (it may take a minute or so), the keyboard will reboot and your new library will be loaded!"
        br(); br()
        form(action = "upload", method = FormMethod.post) {
            input(InputType.file, formEncType = InputFormEncType.multipartFormData, name = "File here!")
            input(InputType.submit, formEncType = InputFormEncType.multipartFormData, name = "Submit Library File")
        }
        br(); br()
        a("mailto:gvy.dvpont@gmail.com?subject=T9%20Library%20Generator"){
            +"(Contact me if you have issues/questions)"
        }
    }
}
