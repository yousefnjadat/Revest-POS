package com.example.pos.domain

import com.example.pos.domain.model.Product
/** Builds a catalog product for tests. Defaults are deliberately boring so each test can vary one thing. */
internal fun product(
    id: String = "p-1",
    priceCents: Long = 1_000,
    stock: Int = 10,
    taxable: Boolean = true,
    name: String = "Product $id",
): Product =
    Product(
        id = id,
        name = name,
        priceCents = priceCents,
        stock = stock,
        taxable = taxable,
    )
