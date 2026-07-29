package com.example.pos.data.catalog

import com.example.pos.data.remote.MockPosBackend
import com.example.pos.data.remote.POS_BASE_URL
import com.example.pos.domain.CartCalculator
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

/** Covers the catalog the app actually ships with, end to end through Ktor and serialization. */
class MockPosBackendTest {
    private val repository =
        DefaultCatalogRepository(
            api = KtorCatalogApi(MockPosBackend().createClient()),
            dispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun theMockCatalogParsesIntoEightProducts() = runTest {
        val result = repository.loadProducts()

        val products = (result as CatalogResult.Success).products
        assertEquals(8, products.size)
        assertEquals(8, products.map { it.id }.toSet().size, "product ids must be unique")
        assertTrue(products.all { it.name.isNotBlank() })
    }

    @Test
    fun wireFieldsLandOnTheRightDomainProperties() = runTest {
        val products = (repository.loadProducts() as CatalogResult.Success).products

        val mug = products.first { it.id == "sku-2001" }
        assertEquals("Ceramic Mug", mug.name)
        assertEquals(1_750, mug.priceCents)
        assertEquals(12, mug.stock)
        assertTrue(mug.taxable)

        val muffin = products.first { it.id == "sku-1003" }
        assertEquals(295, muffin.priceCents)
        assertEquals(6, muffin.stock)
        assertTrue(!muffin.taxable)
    }

    @Test
    fun theCatalogCoversTheCasesTheCartRulesNeed() = runTest {
        val products = (repository.loadProducts() as CatalogResult.Success).products

        assertTrue(products.any { it.taxable }, "needs a taxable product")
        assertTrue(products.any { !it.taxable }, "needs a tax-exempt product")
        assertEquals(1, products.count { !it.inStock }, "needs exactly one out-of-stock product")
        assertTrue(products.any { it.stock in 1..5 }, "needs a low-stock product")
        assertTrue(products.any { it.stock >= 100 }, "needs a high-stock product")

        // The mug and the flask together land on exactly the discount threshold.
        val mug = products.first { it.id == "sku-2001" }
        val flask = products.first { it.id == "sku-2002" }
        assertEquals(CartCalculator.DISCOUNT_THRESHOLD_CENTS, mug.priceCents + flask.priceCents)

        // And the grinder clears it on its own.
        val grinder = products.first { it.id == "sku-2003" }
        assertTrue(grinder.priceCents > CartCalculator.DISCOUNT_THRESHOLD_CENTS)
    }

    @Test
    fun unknownEndpointsReturnNotFound() = runTest {
        // The fake backend is route-aware rather than answering every request with the catalog.
        val failure =
            assertFailsWith<ClientRequestException> {
                MockPosBackend().createClient().get("$POS_BASE_URL/nope")
            }

        assertEquals(HttpStatusCode.NotFound, failure.response.status)
    }
}
