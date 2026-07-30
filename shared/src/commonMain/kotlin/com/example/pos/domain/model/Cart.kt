package com.example.pos.domain.model

import com.example.pos.domain.CartCalculator

data class Cart(
    val lines: List<CartLine> = emptyList(),
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    val itemCount: Int get() = lines.sumOf { it.quantity }

    val totals: CartTotals get() = CartCalculator.totals(lines)

    fun quantityOf(productId: String): Int =
        lines.firstOrNull { it.product.id == productId }?.quantity ?: 0

    fun add(product: Product, quantity: Int = 1): Cart {
        if (!product.inStock || quantity <= 0) return this
        val target = (quantityOf(product.id) + quantity).coerceAtMost(product.stock)
        return withQuantity(product, target)
    }

    fun setQuantity(productId: String, quantity: Int): Cart {
        val line = lines.firstOrNull { it.product.id == productId } ?: return this
        val target = quantity.coerceIn(0, line.product.stock)
        return if (target == 0) remove(productId) else withQuantity(line.product, target)
    }

    fun remove(productId: String): Cart =
        copy(lines = lines.filterNot { it.product.id == productId })

    fun clear(): Cart = Cart()

    private fun withQuantity(product: Product, quantity: Int): Cart {
        val line = CartLine(product, quantity)
        val existing = lines.any { it.product.id == product.id }
        return copy(
            lines =
                if (existing) {
                    lines.map { if (it.product.id == product.id) line else it }
                } else {
                    lines + line
                },
        )
    }
}
