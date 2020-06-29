package dev.bloodstone.invisiframes

class PluginNotFoundException(private val msg: String): Exception(msg)
class UnsupportedPluginException(private val msg: String, private val exception: Exception? = null): Exception(msg, exception) {
}
