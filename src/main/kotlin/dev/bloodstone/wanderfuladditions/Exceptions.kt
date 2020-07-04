/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

class PluginNotFoundException(private val msg: String) : Exception(msg)
class UnsupportedPluginException(private val msg: String, private val exception: Exception? = null) : Exception(msg, exception)
