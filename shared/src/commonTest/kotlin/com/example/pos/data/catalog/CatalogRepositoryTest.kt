package com.example.pos.data.catalog

import com.example.pos.data.clientRespondingWith
import com.example.pos.data.testClient
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class CatalogRepositoryTest {
    @Test
    fun aWellFormedResponseIsParsedAndMapped() = runTest {
        val repository =
            repositoryServing(
                """
                {
                  "products": [
                    { "id": "sku-1", "name": "Latte", "price_cents": 425, "taxable": false, "stock": 9 },
                    { "id": "sku-2", "name": "Tumbler", "price_cents": 2199, "taxable": true, "stock": 0 }
                  ]
                }
                """.trimIndent(),
            )

        val products = assertIs<CatalogResult.Success>(repository.loadProducts()).products

        assertEquals(2, products.size)
        assertEquals("sku-1", products[0].id)
        assertEquals("Latte", products[0].name)
        assertEquals(425, products[0].priceCents)
        assertEquals(9, products[0].stock)
        assertTrue(!products[0].taxable)
        assertTrue(products[0].inStock)

        assertEquals(2_199, products[1].priceCents)
        assertTrue(products[1].taxable)
        assertTrue(!products[1].inStock)
    }

    @Test
    fun unknownWireFieldsAreIgnored() = runTest {
        val repository =
            repositoryServing(
                """
                {
                  "products": [
                    { "id": "sku-1", "name": "Latte", "price_cents": 425, "taxable": false,
                      "stock": 9, "supplier": "unused", "shelf": 3 }
                  ],
                  "generated_at": "2026-07-29T10:00:00Z"
                }
                """.trimIndent(),
            )

        val products = assertIs<CatalogResult.Success>(repository.loadProducts()).products

        assertEquals(1, products.size)
        assertEquals("Latte", products[0].name)
    }

    @Test
    fun anEmptyCatalogIsASuccessNotAFailure() = runTest {
        val repository = repositoryServing("""{ "products": [] }""")

        val products = assertIs<CatalogResult.Success>(repository.loadProducts()).products

        assertTrue(products.isEmpty())
    }

    @Test
    fun aServerErrorBecomesAFailure() = runTest {
        val repository =
            repositoryServing(
                body = """{"error":"boom"}""",
                status = HttpStatusCode.InternalServerError,
            )

        val failure = assertIs<CatalogResult.Failure>(repository.loadProducts())

        assertTrue(failure.message.isNotBlank())
        assertNotNull(failure.cause)
    }

    @Test
    fun aDroppedConnectionBecomesAFailure() = runTest {
        val client = testClient { throw IllegalStateException("connection reset") }
        val repository = DefaultCatalogRepository(KtorCatalogApi(client), Dispatchers.Unconfined)

        val failure = assertIs<CatalogResult.Failure>(repository.loadProducts())

        assertEquals("connection reset", failure.cause?.message)
    }

    @Test
    fun malformedJsonBecomesAFailure() = runTest {
        val repository = repositoryServing("""{ "products": [ { "id": """)

        assertIs<CatalogResult.Failure>(repository.loadProducts())
    }

    @Test
    fun aMissingRequiredFieldBecomesAFailure() = runTest {
        val repository =
            repositoryServing("""{ "products": [ { "id": "sku-1", "name": "Latte" } ] }""")

        assertIs<CatalogResult.Failure>(repository.loadProducts())
    }

    @Test
    fun productValuesThatBreakDomainRulesBecomeAFailure() = runTest {
        val repository =
            repositoryServing(
                """
                {
                  "products": [
                    { "id": "sku-1", "name": "Latte", "price_cents": 425, "taxable": false, "stock": -4 }
                  ]
                }
                """.trimIndent(),
            )

        val failure = assertIs<CatalogResult.Failure>(repository.loadProducts())

        assertIs<IllegalArgumentException>(failure.cause)
    }

    @Test
    fun theFailureMessageIsSafeToShowToTheCashier() = runTest {
        val repository =
            repositoryServing("""{"error":"stack trace"}""", HttpStatusCode.ServiceUnavailable)

        val failure = assertIs<CatalogResult.Failure>(repository.loadProducts())

        assertEquals("Couldn't load the catalog. Check the connection and try again.", failure.message)
    }

    @Test
    fun cancellationIsNotSwallowedAsAFailure() = runTest {
        val cancellingApi =
            object : CatalogApi {
                override suspend fun fetchCatalog(): List<ProductDto> =
                    throw CancellationException("cart screen left")
            }
        val repository = DefaultCatalogRepository(cancellingApi, Dispatchers.Unconfined)

        assertFailsWith<CancellationException> { repository.loadProducts() }
    }

    private fun repositoryServing(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): CatalogRepository = repositoryUsing(clientRespondingWith(body, status))

    private fun repositoryUsing(client: HttpClient): CatalogRepository =
        DefaultCatalogRepository(KtorCatalogApi(client), Dispatchers.Unconfined)
}
