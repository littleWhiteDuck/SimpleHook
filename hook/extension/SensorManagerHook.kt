package me.simpleHook.platform.hook.extension

import android.hardware.Sensor
import android.hardware.SensorManager
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import me.simpleHook.data.ExtensionConfig

object SensorManagerHook : BaseHook() {
    private val sensorTypes =
        arrayOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_LINEAR_ACCELERATION)
    private val sportSensorTypes = arrayOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_STEP_COUNTER
    )
    // Sensor.TYPE_ACCELEROMETER_UNCALIBRATED

    override fun startHook(extensionConfig: ExtensionConfig) {
        if (extensionConfig.sensorConfig.disableAG || extensionConfig.sensorConfig.disableSport) {
            findAllMethods(SensorManager::class.java) {
                name == "getSensorList" || name == "getDynamicSensorList"
            }.hookAfter {
                val type = it.args[0] as Int
                val disableSensorTypes =
                    if (extensionConfig.sensorConfig.disableSport) sportSensorTypes else sensorTypes
                if (type in disableSensorTypes) {
                    it.result = emptyList<Sensor>()
                } else if (type == Sensor.TYPE_ALL) {
                    val sensors = (it.result as? List<*>)?.filterIsInstance<Sensor>().orEmpty()
                    it.result = sensors.filter { sensor ->
                        sensor.type !in disableSensorTypes
                    }
                }
            }
        }
        /* findMethod(SensorManager::class.java) {
             name == "registerListener"
         }.hookReturnConstant(false)*/
    }
}
