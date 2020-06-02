/* Licensed under MIT */
package dev.bloodstone.invisiframes.utils

import java.util.function.Consumer
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta

fun ItemStack.amount(amount: Int): ItemStack {
    setAmount(amount)
    return this
}

var ItemStack.safeMeta: ItemMeta
    get() {
        return itemMeta ?: throw NullPointerException("$type doesn't have metadata")
    }
    set(im: ItemMeta) {
        itemMeta = im
    }

fun ItemStack.name(name: String): ItemStack {
    val meta = safeMeta
    meta.setDisplayName(name.colorized)
    safeMeta = meta
    return this
}

fun ItemStack.lore(text: String): ItemStack {
    val meta = safeMeta
    val lore: MutableList<String> = meta.lore ?: ArrayList()
    lore.add(text)
    meta.lore = lore.colorized
    safeMeta = meta
    return this
}

fun ItemStack.lore(vararg text: String): ItemStack {
    text.forEach { this.lore(it) }
    return this
}

fun ItemStack.lore(text: List<String>): ItemStack {
    text.forEach { this.lore(it) }
    return this
}

fun ItemStack.enchantment(enchantment: Enchantment, level: Int): ItemStack {
    addUnsafeEnchantment(enchantment, level)
    return this
}

fun ItemStack.enchantment(enchantment: Enchantment): ItemStack {
    addUnsafeEnchantment(enchantment, 1)
    return this
}

fun ItemStack.type(material: Material): ItemStack {
    type = material
    return this
}

fun ItemStack.clearLore(): ItemStack {
    val meta = safeMeta
    meta.lore = ArrayList()
    safeMeta = meta
    return this
}

fun ItemStack.clearEnchantments(): ItemStack {
    enchantments.keys.forEach(Consumer<Enchantment> { this.removeEnchantment(it) })
    return this
}

fun ItemStack.color(color: Color): ItemStack {
    if (type == Material.LEATHER_BOOTS ||
        type == Material.LEATHER_CHESTPLATE ||
        type == Material.LEATHER_HELMET ||
        type == Material.LEATHER_LEGGINGS) {

        val meta = safeMeta as LeatherArmorMeta
        meta.setColor(color)
        safeMeta = meta
        return this
    } else {
        throw IllegalArgumentException("Colors only applicable for leather armor!")
    }
}

fun ItemStack.flag(vararg flag: ItemFlag): ItemStack {
    val meta = safeMeta
    meta.addItemFlags(*flag)
    safeMeta = meta
    return this
}

private val String.colorized: String
    get() {
        return ChatColor.translateAlternateColorCodes('&', this)
    }

private val List<String>.colorized: List<String>
    get() = this.map { it.colorized }
