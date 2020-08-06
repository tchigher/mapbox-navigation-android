package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.junctonssupport.JunctionData
import com.mapbox.navigation.examples.junctonssupport.JunctionsProvider
import com.mapbox.navigation.examples.utils.extensions.toLatLng
import com.mapbox.navigation.ui.internal.route.MapRouteLayerProvider
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.turf.TurfClassification
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_guidance_view.snapshotImage
import kotlinx.android.synthetic.main.activity_junction_snapshot.*
import timber.log.Timber

@SuppressLint("LogNotTimber")
class JunctionSnapshotActivity : AppCompatActivity() {

    private lateinit var mapboxMap: MapboxMap
    private val junctionsProvider = JunctionsProvider()
    // private lateinit var junctionData: JunctionData

    // private lateinit var cameraPoint: Point
    private lateinit var cameraPosition: CameraPosition

    private val pointOfCamera1 = Point.fromLngLat(-1.8611406499303198, 52.50886354508242)

    private val cameraPosition1 = CameraPosition.Builder()
        .target(pointOfCamera1.toLatLng())
        .zoom(17.0)
        // .bearing(304.0)
        // .bearing(297.0)
        .tilt(60.0)
        .build()

    // private lateinit var snapshotterOptions: MapSnapshotter.Options

    private val options by lazy {
        MapSnapshotter.Options(
            mapView.measuredWidth,
            mapView.measuredHeight
        )
            // .withPixelRatio(resources.displayMetrics.density)
            .withPixelRatio(1.0f)
            .withCameraPosition(cameraPosition1)
            .withStyleBuilder(
                Style.Builder()
                    .fromUri(Style.MAPBOX_STREETS)
                    .withSource(
                        GeoJsonSource(
                            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                            LineString.fromJson(
                                junctionsProvider.listOfJunctions.first().readLineString()
                            )
                        )
                    )
                    .withLayers(
                        mapRouteLayerProvider.initializePrimaryRouteLayer(
                            mapboxMap.style,
                            true,
                            1.0f,
                            Color.BLUE
                        )
                    )
            )
    }

    // private val mapSnapshotter: MapSnapshotter by lazy {
    //     MapSnapshotter(this, options)
    // }

    private val mapRouteLayerProvider = MapRouteLayerProvider()

    companion object {
        const val TAG = "JunctionSnapshotActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_junction_snapshot)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            mapboxMap = map
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                // mapboxMap.cameraPosition = cameraPosition1
                // initSnapshotter()
                // makeSnapshot()
                initSpinner()
            }
            mapboxMap.addOnCameraMoveListener {
                // Log.d(TAG, mapboxMap.cameraPosition.toString())
            }
        }

        // options.withCameraPosition(CameraPosition.Builder(cameraPosition1).bearing(0.0).build())

        // val geometryCoordinates = LineString.fromJson(
        //     junctionsProvider.listOfJunctions.first().readLineString()
        // )
        //     .coordinates()
        //
        // val theNearest = geometryCoordinates.theNearestCoordinateTo(
        //     junctionsProvider.listOfJunctions.first().junctionEntry
        // )
        // Toast.makeText(this, "The nearest point = $theNearest", Toast.LENGTH_LONG).show()
        //
        // val indexOfTheNearestCoordinate = geometryCoordinates.indexOf(theNearest)
        //
        // Toast.makeText(
        //     this,
        //     "The nearest point index = $indexOfTheNearestCoordinate, of size = ${geometryCoordinates.size}",
        //     Toast.LENGTH_LONG
        // ).show()
        //
        // val bearing = TurfMeasurement.bearing(
        //     geometryCoordinates[indexOfTheNearestCoordinate],
        //     geometryCoordinates[indexOfTheNearestCoordinate - 1]
        // ) + 180.0
        // Toast.makeText(this, "The bearing = $bearing", Toast.LENGTH_SHORT).show()
    }

    private fun initSnapshotter() {
        // mapSnapshotter.setObserver(object : MapSnapshotter.Observer {
        //     override fun onStyleImageMissing(imageName: String?) {
        //         Timber.d(
        //             "MapSnapshotter.Observer onStyleImageMissing; $imageName"
        //         )
        //     }
        //
        //     override fun onDidFinishLoadingStyle() {
        //         Timber.d(
        //             "MapSnapshotter.Observer onDidFinishLoadingStyle"
        //         )
        //     }
        // })
    }

    private fun makeSnapshot() {
        // mapSnapshotter.start { snapshot ->
        //     snapshotImage.setImageBitmap(snapshot.bitmap)
        // }
    }

    private fun initSpinner() {
        junctionSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            junctionsProvider.listOfJunctions.map { it.junctionLocationName }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        junctionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                Toast.makeText(
                    this@JunctionSnapshotActivity,
                    "Item selected $selectedItem",
                    Toast.LENGTH_SHORT
                ).show()
                setupRouteFor(selectedItem)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }

    private fun setupRouteFor(itemName: String) {
        val junctionData =
            junctionsProvider.listOfJunctions.find { it.junctionLocationName == itemName } ?: let {
                Timber.w("Junction location name $itemName is not found")
                return
            }
        val geometryCoordinates = LineString.fromJson(
            junctionData.readLineString()
        ).coordinates()

        val theNearestPoint = geometryCoordinates.theNearestCoordinateTo(junctionData.junctionEntry)

        val indexOfNearestPoint = geometryCoordinates.indexOf(theNearestPoint)
        val points = when {
            geometryCoordinates.size <= 1 -> {
                Timber.e("Coordinates is less than 2, not possible to count bearing")
                return
            }
            indexOfNearestPoint == -1 -> {
                Timber.e("Nearest coordinate is not found")
                return
            }
            geometryCoordinates.getOrNull(indexOfNearestPoint - 1) != null -> {
                geometryCoordinates[indexOfNearestPoint] to geometryCoordinates[indexOfNearestPoint - 1]
            }
            geometryCoordinates.getOrNull(indexOfNearestPoint + 1) != null -> {
                geometryCoordinates[indexOfNearestPoint + 1] to geometryCoordinates[indexOfNearestPoint]
            }
            else -> let {
                Timber.w("Cannot find points to get bearing")
                return
            }
        }

        val bearing = TurfMeasurement.bearing(
            points.first,
            points.second
        ).plus(180.0)

        prepareCamera(junctionData.junctionEntry, bearing)
        moveMapToCameraPosition()
        prepareAndMakeSnapshot(junctionData)
    }

    private fun prepareCamera(target: Point, bearing: Double) {
        cameraPosition = CameraPosition.Builder()
            .target(target.toLatLng())
            .zoom(17.0)
            .bearing(bearing)
            .tilt(60.0)
            .build()
    }

    private fun moveMapToCameraPosition() {
        mapboxMap.cameraPosition = cameraPosition
    }

    private fun prepareAndMakeSnapshot(junctionData: JunctionData) {
        val options = MapSnapshotter.Options(
            mapView.measuredWidth,
            mapView.measuredHeight
        )
            // .withPixelRatio(resources.displayMetrics.density)
            .withPixelRatio(1.0f)
            .withCameraPosition(cameraPosition)
            .withStyleBuilder(
                Style.Builder()
                    .fromUri(Style.MAPBOX_STREETS)
                    .withSource(
                        GeoJsonSource(
                            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                            LineString.fromJson(junctionData.readLineString())
                        )
                    )
                    .withLayers(
                        mapRouteLayerProvider.initializePrimaryRouteLayer(
                            mapboxMap.style,
                            true,
                            1.0f,
                            Color.BLUE
                        )
                    )
            )

        val mapSnapshotter = MapSnapshotter(this, options)
        mapSnapshotter.setObserver(object : MapSnapshotter.Observer{
            override fun onStyleImageMissing(imageName: String?) {
                Timber.d("MapSnapshotter onStyleImageMissing, imageName: $imageName")
            }

            override fun onDidFinishLoadingStyle() {
                Timber.d("onDidFinishLoadingStyle")
            }
        })
            Timber.d("MapSnapshotter is starting")
            mapSnapshotter.start ({ snapshot ->
                Timber.d("MapSnapshotter has finished successful")
                snapshotImage.setImageBitmap(snapshot.bitmap)
            }, { error ->
                Timber.w("MapSnapshotter error: $error")
            })
    }

    private fun List<Point>.theNearestCoordinateTo(point: Point): Point {
        return TurfClassification.nearestPoint(
            point,
            this
        )
    }

    private fun JunctionData.readLineString(): String {
        return resources.openRawResource(lineString).bufferedReader().use { it.readText() }
    }
}