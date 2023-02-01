package me.simpleHook.hook.extension

import android.hardware.Sensor
import android.hardware.SensorManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import me.simpleHook.bean.ExtensionConfigBean

object SensorMangerHook : BaseHook() {
    override fun startHook(configBean: ExtensionConfigBean) {
        if (configBean.disSensorAG) {
            findMethod(SensorManager::class.java) {
                name == "getSensorList"
            }.hookAfter {
                val type = it.args[0] as Int
                if (type == Sensor.TYPE_ACCELEROMETER || type == Sensor.TYPE_GYROSCOPE) {
                    it.result = null
                } else if (type == Sensor.TYPE_ALL) {
                    val sensors = it.result as ArrayList<Sensor>
                    val size = sensors.size
                    var count = 0
                    for (i in sensors.indices) {
                        if (sensors[i + sensors.size - size].type == Sensor.TYPE_ACCELEROMETER || sensors[i + sensors.size - size].type == Sensor.TYPE_GYROSCOPE) {
                            sensors.removeAt(i + sensors.size - size)
                            if (++count == 2) break
                        }
                    }
                    it.result = sensors
                }
            }
        } else if (configBean.disSensor) {
            findMethod(SensorManager::class.java) {
                name == "registerListener"
            }.hookReturnConstant(false)
        }
    }
}