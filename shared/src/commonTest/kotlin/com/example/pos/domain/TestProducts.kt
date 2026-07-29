package com.example.pos.domain

/** Builds a catalog product for tests. Defaults are deliberately boring so each test can vary one thing. */
internal fun product(
    id: String = "p-1",
    priceCents: Long = 1_000,
    stock: Int = 10,
    taxable: Boolean = true,
    name: String = "Product $id",
    category: String = "Test",
): Product =
    Product(
        id = id,
        name = name,
        category = category,
        priceCents = priceCents,
        stock = stock,
        taxable = taxable,
    )
