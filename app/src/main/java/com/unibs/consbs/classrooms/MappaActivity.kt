package com.unibs.consbs.classrooms

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.mapbox.geojson.Point
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.unibs.consbs.R
import com.unibs.consbs.data.Segnaposto
import com.unibs.consbs.databinding.MapBinding
import com.unibs.consbs.profile.ProfiloActivity
import com.unibs.consbs.utils.Utils

class MappaActivity: AppCompatActivity() {

    private lateinit var binding: MapBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    lateinit var mapView: MapView
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var location: Location
    private lateinit var mPointsList: ArrayList<Segnaposto>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.setCurrentActivty("MappaActivity")

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        mapView = binding.mapView
        mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS)

        val btnBack = binding.arrowBack
        btnBack.setOnClickListener{
            val intent = Intent(this, AuleActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    //val location: Location?= task.result
                    location = task.result
                    if (location == null) {
                        Toast.makeText(this, "Ricevuto valore nullo", Toast.LENGTH_SHORT).show()
                    } else {
                        //latitude.text = ""+location.latitude
                        //longitude.text = ""+location.longitude
                        var cameraPosition = CameraOptions.Builder().center(Point.fromLngLat(location.longitude, location.latitude)).build()
                        mapView.getMapboxMap().setCamera(cameraPosition)
                        addAnnotationToMap()
                    }
                }


            } else {
                Toast.makeText(this, "turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermission()
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            return false
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_LOCATION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permesso", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Negato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private fun addAnnotationToMap() {
        // Create an instance of the Annotation API and get the PointAnnotationManager.
        bitmapFromDrawableRes(
            this@MappaActivity,
            R.drawable.red_marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
            // Set options for the resulting symbol layer.
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                // Define a geographic coordinate.
                .withPoint(Point.fromLngLat(location.longitude, location.latitude))
                // Specify the bitmap you assigned to the point annotation
                // The bitmap will be added to map style automatically.
                .withIconImage(it)
                .withIconAnchor(IconAnchor.BOTTOM)
            // Add the resulting pointAnnotation to the map.
            pointAnnotationManager?.create(pointAnnotationOptions)
        }

        bitmapFromDrawableRes(
            this@MappaActivity,
            R.drawable.blue_marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager()

            mPointsList = PuntiSede.getPoints()

            mPointsList.forEach { points ->
                val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(points.lon, points.lat))
                    .withIconImage(it)
                    .withIconAnchor(IconAnchor.BOTTOM)
                val pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)

                val viewAnnotationManager = mapView.viewAnnotationManager
                val viewAnnotation = viewAnnotationManager.addViewAnnotation(
                    resId = R.layout.item_callout_view,
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(points.lon, points.lat))
                        associatedFeatureId(pointAnnotation?.featureIdentifier)
                        anchor(ViewAnnotationAnchor.BOTTOM)
                        offsetY((pointAnnotation?.iconImageBitmap?.height!!).toInt())
                    }
                )

                viewAnnotation.visibility = View.GONE

                viewAnnotation.findViewById<TextView>(R.id.annotation).setText("${points.name}")

                viewAnnotation.findViewById<TextView>(R.id.annotation).setOnClickListener {
                    viewAnnotation.visibility = View.GONE
                }

                pointAnnotationManager?.apply {
                    addClickListener(
                        OnPointAnnotationClickListener {clickedAnnotation ->
                            if (pointAnnotation == clickedAnnotation) {
                                Log.i("babs", "cliccato era visibile ora è gone")
                                if (viewAnnotation.visibility == View.VISIBLE) {
                                    viewAnnotation.visibility = View.GONE
                                }
                                else {
                                    Log.i("babs", "cliccato era gone ora è visibile")
                                    viewAnnotation.visibility = View.VISIBLE
                                    Toast.makeText(this@MappaActivity, "test2", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewAnnotation.visibility = View.GONE
                                Log.i("babs", "elemento non cliccato che diventa gone")
                            }
                            false
                        }
                    )
                }
            }
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            // copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

}