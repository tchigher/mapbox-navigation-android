package com.mapbox.navigation.examples.core

import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.constants.MapboxConstants
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
import kotlinx.android.synthetic.main.activity_junction_snapshot.*
import timber.log.Timber

class JunctionSnapshotActivity : AppCompatActivity() {

    private lateinit var mapboxMap: MapboxMap
    private val junctionsProvider = JunctionsProvider()
    private var currentJunctionData: JunctionData? = null

    private lateinit var cameraPosition: CameraPosition
    private var mapSnapshotter: MapSnapshotter? = null

    private val mapRouteLayerProvider = MapRouteLayerProvider()

    companion object {
        private const val CAMERA_DEFAULT_TILT = MapboxConstants.MAXIMUM_TILT
        private const val CAMERA_DEFAULT_ZOOM = 17.0
        private const val POINT_ID = "point"
        const val TAG = "JunctionSnapshotActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_junction_snapshot)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            mapboxMap = map
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                initSpinner()
                initSettingViews()
            }
        }
    }

    private fun initSettingViews() {
        zoomLevelSb.max = MapboxConstants.MAXIMUM_ZOOM.toInt()
        zoomLevelSb.progress = CAMERA_DEFAULT_ZOOM.toInt()
        zoomLabel.text = "${zoomLabel.text}(0..${zoomLevelSb.max})"
        zoomLevelSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cameraPosition = CameraPosition.Builder(cameraPosition)
                    .zoom(progress.toDouble())
                    .build()
                currentJunctionData?.let { prepareAndMakeSnapshot(it) }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        tiltLevelSb.max = MapboxConstants.MAXIMUM_TILT.toInt()
        tiltLevelSb.progress = CAMERA_DEFAULT_TILT.toInt()
        tiltLabel.text = "${tiltLabel.text}(0..${tiltLevelSb.max})"
        tiltLevelSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cameraPosition = CameraPosition.Builder(cameraPosition)
                    .tilt(progress.toDouble())
                    // .padding(0.0, 0.0, 0.0, progress.toDouble())
                    .build()
                currentJunctionData?.let { prepareAndMakeSnapshot(it) }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        cameraOffsetSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cameraPosition = CameraPosition.Builder(cameraPosition)
                    .padding(0.0, 0.0, 0.0, progress.toDouble())
                    .build()
                currentJunctionData?.let { prepareAndMakeSnapshot(it) }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
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
        mapSnapshotter?.cancel()
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
        currentJunctionData = junctionData

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
            .zoom(CAMERA_DEFAULT_ZOOM)
            .bearing(bearing)
            .tilt(CAMERA_DEFAULT_TILT)
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
            .withPixelRatio(resources.displayMetrics.density)
            // .withPixelRatio(1.0f)
            .withCameraPosition(cameraPosition)
            .withStyleBuilder(
                Style.Builder()
                    .fromUri(Style.MAPBOX_STREETS)
                    .withSources(
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

        mapSnapshotter?.cancel()
        mapSnapshotter = MapSnapshotter(this, options)
        mapSnapshotter?.setObserver(object : MapSnapshotter.Observer {
            override fun onStyleImageMissing(imageName: String?) {
                Timber.d("MapSnapshotter onStyleImageMissing, imageName: $imageName")
            }

            override fun onDidFinishLoadingStyle() {
                Timber.d("onDidFinishLoadingStyle")
            }
        })
        Timber.d("MapSnapshotter is starting")
        mapSnapshotter?.start({ snapshot ->
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