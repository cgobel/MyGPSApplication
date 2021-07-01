package com.example.mytestapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

@RequiresApi(Build.VERSION_CODES.N)
class MainActivity : AppCompatActivity(), LocationListener, OnNmeaMessageListener{
    val REQUESTCODE : Int = 12345
    lateinit var textView : TextView
    lateinit var locationManager : LocationManager
    lateinit var locationHandlerThread : HandlerThread
    lateinit var locationHandler : Handler
    var granted : Boolean = false
    var registered : Boolean = false
    var permissionRequestEnd = false
    var starttime : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById<View>(R.id.textView) as TextView
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onResume() {
        super.onResume()
        startLocationListener()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()
        locationManager.removeNmeaListener(this)
        locationHandlerThread.quitSafely()
        locationManager.removeUpdates(this)
        registered = false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTCODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                granted = true
            }
        }
        permissionRequestEnd = true
    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun onButtonClick(view : View){
        if(!registered){
            startLocationListener()
        } else {
            locationManager.removeNmeaListener(this)
            locationHandlerThread.quitSafely()
            locationManager.removeUpdates(this)
            registered = false
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun startLocationListener() {
        //check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            var requestedPermission : Array<String> = Array(1) {Manifest.permission.ACCESS_FINE_LOCATION}
            if(ActivityCompat.shouldShowRequestPermissionRationale( this, Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, requestedPermission, REQUESTCODE)
            } else {
                return
            }
        } else {
            granted = true
        }
        if (!granted) {
            textView.text = "location permission not granted"
            return
        }
        //request location updates
        locationHandlerThread = HandlerThread("locationHandlerThread")
        locationHandlerThread.start()
        val locationHandlerLooper = locationHandlerThread.looper
        locationHandler = object : Handler(locationHandlerLooper) { //needed to get nmea updates on looper of handlerthread
            override fun handleMessage(msg : Message) {
                handleNmeaMessage(msg)
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, this, locationHandlerLooper);
        locationManager.addNmeaListener(this, locationHandler)
        registered = true
        starttime = System.currentTimeMillis()
    }

    override fun onLocationChanged(location: Location) {
        //not used because we get nmea message instead
    }

    override fun onNmeaMessage(message: String?, timestamp: Long) {
        val msg = Message()
        msg.obj = message
        locationHandler.sendMessage(msg)
    }

    fun handleNmeaMessage(message: Message){
        val msg = message.obj as String
        textView.post{
            textView.text = msg
        }
        val messageType = msg.subSequence(3, 6)
        if(messageType == "VTG"){
            val fields = msg.split(",")
            if (fields != null) {
                for(i in 0 until fields.size-1){
                    if(fields[i] == "K"){
                        textView.post{
                            textView.text = (fields[i-1]+" km/h")
                        }
                    }
                }
            }
        }
    }

}