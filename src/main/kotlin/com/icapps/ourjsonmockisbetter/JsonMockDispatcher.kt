package com.icapps.ourjsonmockisbetter

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.io.File
import java.nio.charset.Charset
import java.util.logging.Logger
import java.util.regex.Pattern

class JsonMockDispatcher : Dispatcher() {

    companion object {
        const val RESPONSES_DIRECTORY = "responses/"
    }

    private val responseFiles = ResourceUtils.getResourceListing(javaClass, RESPONSES_DIRECTORY)
            .sortedBy { it.path.count { it == '*' } } // Least amount of wildcards go first

    override fun dispatch(request: RecordedRequest): MockResponse {
        val fileName = getFilenameForRequest(request)
        val matchedResponse = findResponseFileForFilename(fileName)

        return if (matchedResponse == null) {
            MockResponse()
                    .setResponseCode(404)
        } else {
            Logger.getLogger(JsonMockDispatcher::class.java.simpleName).info("Matched response ${matchedResponse.name}")

            MockResponse()
                    .setResponseCode(200)
                    .setBody(matchedResponse.readText(Charset.forName("UTF-8")))
        }
    }

    private fun getFilenameForRequest(request: RecordedRequest): String {
        val resultFilename = request.path.replaceLast("/", "/${request.method}_")
        return "$resultFilename.json"
    }

    private fun findResponseFileForFilename(uri: String): File? {
        return responseFiles.firstOrNull {
            val fileName = it.path.substringAfter(RESPONSES_DIRECTORY)
            val pattern = "(.*)${fileName.escapeForRegex()}".toRegex()
            uri.matches(pattern)
        }
    }

    private fun String.escapeForRegex(): String {
        return this.split("*").joinToString(separator = "(.*)") { Pattern.quote(it) }
    }

    private fun String.replaceLast(last: String, replacement: String): String {
        val indexOfLast = lastIndexOf(last)
        if (indexOfLast < 0) return this
        return replaceRange(indexOfLast, indexOfLast + last.length, replacement)
    }

}