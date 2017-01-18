package geowarin.bootwebpack.config;

/**
 * For some reason, I couldn't make StringToEnumIgnoringCaseConverterFactory to work
 * so this is a lower-case enum.
 * It allows users to write lowercase properties in their configuration file (this
 * is how Intellij autocompletes)
 */
public enum RunMode {
    production,
    development,
    auto
}
