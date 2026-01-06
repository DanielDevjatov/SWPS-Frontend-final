@file:JsModule("logplease")
@file:JsNonModule

package logplease

/**
 * External function of logplease package to create a logger.
 *
 * @param name    name of the logger
 * @param options js object of logger options; see logplease documentation for options
 */
external fun create(name: String, options: dynamic): dynamic

/**
 * External function of logplease package to set global loglevel vor logplease logger.
 *
 * @param level name of the loglevel; see logplease documentation for options
 */
external fun setLogLevel(level: String)
