package me.simpleHook.hook.extension

import android.hardware.Sensor
import android.hardware.SensorManager
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import me.simpleHook.bean.ExtensionConfig

object SensorMangerHook : BaseHook() {
    private val sensorTypes =
        arrayOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_LINEAR_ACCELERATION)
    private val sportSensorTypes = arrayOf(Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_STEP_COUNTER)
    // Sensor.TYPE_ACCELEROMETER_UNCALIBRATED

    override fun startHook(configBean: ExtensionConfig) {
        if (configBean.disSensorAG || configBean.disSensorSport) {
            findAllMethods(SensorManager::class.java) {
                name == "getSensorList" || name == "getDynamicSensorList"
            }.hookAfter {
                val type = it.args[0] as Int
                val disableSensorTypes =
                    if (configBean.disSensorSport) sportSensorTypes else sensorTypes
                if (type in disableSensorTypes) {
                    it.result = null
                } else if (type == Sensor.TYPE_ALL) {
                    @Suppress("UNCHECKED_CAST")
                    val unmodifiableList = it.result as List<Sensor>
                    val sensors = ArrayList<Sensor>()
                    unmodifiableList.forEach { sensor ->
                        if (sensor.type !in disableSensorTypes) {
                            sensors.add(sensor)
                        }
                    }
                    it.result = sensors
                }
            }
        }
        /* findMethod(SensorManager::class.java) {
             name == "registerListener"
         }.hookReturnConstant(false)*/
    }
}