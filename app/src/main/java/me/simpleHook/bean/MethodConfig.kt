package me.simpleHook.bean

/**
 * 方法配置类
 * @param mode hook模式：0: 返回值、1：参数值、2：中断执行
 * @param className 类名
 * @param methodName 方法名
 * @param params 参数
 * @param resultValues 返回值/参数值
 */
data class MethodConfig(
    val mode:Int,
    val className:String,
    val methodName:String,
    val params:String,
    val resultValues:String
){
    override fun toString(): String {
        return "{\"mode\":$mode,\"className\":\"$className\",\"methodName\":\"$methodName\"," +
                "\"params\":\"$params\",\"resultValues\":\"$resultValues\"}"
    }
}
