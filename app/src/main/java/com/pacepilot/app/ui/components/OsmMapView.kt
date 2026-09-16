package com.pacepilot.app.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.pacepilot.app.data.model.BikePoint
import com.pacepilot.app.data.model.RouteProfile
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    route: RouteProfile? = null,
    currentLocation: BikePoint? = null,
    onMapTap: ((BikePoint) -> Unit)? = null,
    enableLocationOverlay: Boolean = true,
    initialCenter: BikePoint = BikePoint(-6.2088, 106.8456),
    pendingWaypoints: List<BikePoint> = emptyList()
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
            controller.setCenter(initialCenter.toGeoPoint())
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            // 1. Event listener for tapping map to select destination or add waypoints
            if (onMapTap != null) {
                val eventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            onMapTap(BikePoint(it.latitude, it.longitude))
                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        return false
                    }
                }
                map.overlays.add(MapEventsOverlay(eventsReceiver))
            }

            // 2. Draw route polyline and all waypoint markers if available
            if (route != null && route.waypoints.isNotEmpty()) {
                val polyline = Polyline(map).apply {
                    val geoPoints = route.waypoints.map { it.toGeoPoint() }
                    setPoints(geoPoints)
                    outlinePaint.color = AndroidColor.parseColor("#00E5FF") // Bright Cyan
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                map.overlays.add(polyline)

                // Start marker
                val startMarker = Marker(map).apply {
                    position = route.waypoints.first().toGeoPoint()
                    title = "Mulai: ${route.startName}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(startMarker)

                // Intermediate waypoints markers
                route.userWaypoints.forEachIndexed { index, wp ->
                    val wpMarker = Marker(map).apply {
                        position = wp.toGeoPoint()
                        title = "Titik Jalur #${index + 1}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(wpMarker)
                }

                // Destination marker
                val destMarker = Marker(map).apply {
                    position = route.waypoints.last().toGeoPoint()
                    title = if (route.isRoundTrip) "Finish (Roundtrip Loop)" else "Tujuan: ${route.destinationName}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(destMarker)
            } else if (pendingWaypoints.isNotEmpty()) {
                // Tampilkan marker titik sementara sebelum rute selesai dihitung
                pendingWaypoints.forEachIndexed { idx, pt ->
                    val marker = Marker(map).apply {
                        position = pt.toGeoPoint()
                        title = if (idx == 0) "Mulai" else "Titik Jalur #$idx"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(marker)
                }
            }

            // 3. User current location marker/overlay
            if (currentLocation != null) {
                val currentGeoPoint = currentLocation.toGeoPoint()
                val userMarker = Marker(map).apply {
                    position = currentGeoPoint
                    title = "Posisi Anda"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(userMarker)
                map.controller.animateTo(currentGeoPoint)
            } else if (enableLocationOverlay) {
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map).apply {
                    enableMyLocation()
                    enableFollowLocation()
                }
                map.overlays.add(locationOverlay)
            }

            map.invalidate()
        }
    )
}
