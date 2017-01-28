package geowarin.bootwebpack.webpack.errors

import geowarin.bootwebpack.webpack.WebpackError
import mu.KotlinLogging

enum class KnowErrors(val s: String) {
    SYNTAX_ERROR("Syntax Error"),
    MODULE_NOT_FOUND("Module not found"),
    ESLINT("Lint error"),
}

fun matchErrorName(errorName: String): KnowErrors? {
    return KnowErrors.values().find { it.s == errorName }
}

class ErrorLogger {
    private val logger = KotlinLogging.logger {}

    fun getErrorsLogs(errors: List<WebpackError>): List<String> {
        val maxSeverity = errors.maxBy { it.severity }?.severity ?: 0
        val maxSeverityErrors = errors.filter { it.severity == maxSeverity }
        return maxSeverityErrors.map { "ERROR in ${it.file}:\n${it.message}" }
    }

    fun displayErrors(errors: List<WebpackError>) {
        val errorsLogs = getErrorsLogs(errors)
        errorsLogs.forEach { log ->
            logger.error { log }
        }
    }
}