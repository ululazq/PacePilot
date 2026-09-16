package com.pacepilot.app

import com.pacepilot.app.data.api.RoutingService
import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.RouteProfile
import com.pacepilot.app.data.model.SavedRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiPointRoutingTest {

    @Test
    fun testHaversineDistanceCalculation() {
        // Monas Jakarta to Bundaran HI (~2.3 km)
        val monas = BikePoint(-6.1754, 106.8272)
        val bundaranHi = BikePoint(-6.1950, 106.8230)

        val distMeters = RoutingService.calculateHaversineDistance(monas, bundaranHi)
        assertTrue("Jarak harus sekitar 2.1 - 2.5 km", distMeters in 2100.0..2500.0)
    }

    @Test
    fun testRouteProfileWithMultiWaypointsAndRoundTrip() {
        val p1 = BikePoint(-6.1754, 106.8272) // Monas
        val p2 = BikePoint(-6.1950, 106.8230) // Bundaran HI
        val p3 = BikePoint(-6.2250, 106.8080) // Senayan

        val profile = RouteProfile(
            waypoints = listOf(p1, p2, p3, p1),
            totalDistanceMeters = 15000.0,
            estimatedDurationSeconds = 3600.0,
            startName = "Monas",
            destinationName = "Senayan Loop",
            userWaypoints = listOf(p2),
            isRoundTrip = true
        )

        assertEquals(15.0, profile.totalDistanceKm, 0.01)
        assertEquals(60, profile.estimatedDurationMinutes)
        assertEquals(1, profile.userWaypoints.size)
        assertTrue(profile.isRoundTrip)
    }

    @Test
    fun testSavedRouteRoundtripSerialization() {
        val p1 = BikePoint(-6.1754, 106.8272)
        val p2 = BikePoint(-6.2250, 106.8080)

        val original = SavedRoute(
            id = "test-route-123",
            title = "Rute Loop Jakarta",
            totalDistanceMeters = 22000.0,
            estimatedDurationSeconds = 4800.0,
            waypoints = listOf(p1, p2, p1),
            isRoundTrip = true
        )

        val json = original.toJson()
        assertTrue(json.getBoolean("isRoundTrip"))

        val parsed = SavedRoute.fromJson(json)
        assertEquals(original.id, parsed.id)
        assertEquals(original.title, parsed.title)
        assertEquals(original.totalDistanceMeters, parsed.totalDistanceMeters, 0.01)
        assertEquals(original.isRoundTrip, parsed.isRoundTrip)
    }
}
