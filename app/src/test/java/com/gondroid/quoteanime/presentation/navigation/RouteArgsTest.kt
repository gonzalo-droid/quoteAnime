package com.gondroid.quoteanime.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLDecoder

/**
 * Route arguments are percent-encoded so a free-text id can't break the route.
 *
 * Scenarios covered:
 *  - Characters with meaning in a route (`&`, `?`, `=`, `#`, `/`, `%`, space) are escaped
 *  - Plain ids (the RTDB's numeric keys) are unchanged, so existing routes don't move
 *  - Non-ASCII text is escaped as UTF-8, like `Uri.encode`
 *  - Decoding the result gives the original back (what Navigation does when matching)
 *  - Home and Catalog build their routes with it
 */
class RouteArgsTest {

    @Test
    fun `given an id with route delimiters, when encoded, then every delimiter is escaped`() {
        assertEquals("a%26b%3Fc%3Dd%23e%2Ff%25g%20h", encodeRouteArg("a&b?c=d#e/f%g h"))
    }

    @Test
    fun `given a plain id, when encoded, then it is unchanged`() {
        assertEquals("45", encodeRouteArg("45"))
        assertEquals("-Nx_3.a~b", encodeRouteArg("-Nx_3.a~b"))
    }

    @Test
    fun `given non-ASCII text, when encoded, then it is escaped as UTF-8 like Uri encode`() {
        assertEquals("motivaci%C3%B3n", encodeRouteArg("motivación"))
        assertEquals("One%20Piece", encodeRouteArg("One Piece"))
    }

    @Test
    fun `given any id, when encoded and decoded, then the original comes back`() {
        listOf("45", "a&b", "¿qué?", "100%", "One Piece", "x+y", "a/b#c").forEach { id ->
            assertEquals(id, URLDecoder.decode(encodeRouteArg(id), "UTF-8"))
        }
    }

    @Test
    fun `given an id with an ampersand, when building the Home route, then it stays one argument`() {
        assertEquals("home?quoteId=a%26b", Screen.Home.createRoute("a&b"))
        assertEquals("home", Screen.Home.createRoute(null))
    }

    @Test
    fun `given an anime name, when building the Catalog route, then it is encoded`() {
        assertEquals("catalog?categoryId=Re%3AZero%20%26%20Co", Screen.Catalog.createRoute("Re:Zero & Co"))
        assertEquals("catalog", Screen.Catalog.createRoute(null))
    }
}
