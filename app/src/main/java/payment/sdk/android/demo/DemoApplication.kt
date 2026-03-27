package payment.sdk.android.demo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import com.chuckerteam.chucker.api.Chucker
import kotlin.math.sqrt

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ShakeDetector", "DemoApplication onCreate, Chucker.isOp=${Chucker.isOp}")
        registerActivityLifecycleCallbacks(ShakeDetectorCallbacks())
    }
}

private class ShakeDetectorCallbacks : Application.ActivityLifecycleCallbacks, SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var currentActivity: Activity? = null
    private var lastShakeTime = 0L
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var shakeAcceleration = 0f

    companion object {
        private const val SHAKE_THRESHOLD = 12f
        private const val MIN_TIME_BETWEEN_SHAKES = 2000L
        private const val TAG = "ShakeDetector"
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        Log.d(TAG, "onActivityResumed: accelerometer sensor=$sensor")
        if (sensor != null) {
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        currentActivity = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        shakeAcceleration = shakeAcceleration * 0.9f + delta

        if (shakeAcceleration > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > MIN_TIME_BETWEEN_SHAKES) {
                lastShakeTime = now
                Log.d(TAG, "Shake detected! acceleration=$shakeAcceleration")
                try {
                    currentActivity?.let { activity ->
                        val intent = Chucker.getLaunchIntent(activity)
                        Log.d(TAG, "Launching Chucker intent: $intent")
                        activity.startActivity(intent)
                    } ?: Log.w(TAG, "currentActivity is null")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch Chucker", e)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
