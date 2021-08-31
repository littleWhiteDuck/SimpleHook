package me.simpleHook.bean

/**
 * 配置数据类
 * @param mode hook模式：0: 返回值、1：参数值、2：中断执行、3:静态变量值
 * @param className 类名
 * @param methodName 方法名
 * @param params 参数
 * @param fieldName 变量名
 * @param fieldType 变量类型
 * @param resultValues 返回值/参数值
 */
data class ConfigBean(
    val mode:Int,
    val className:String,
    val methodName:String = "",
    val params:String = "",
    val fieldName:String = "",
    val fieldType: String = "",
    val resultValues:String = ""
){
    override fun toString(): String {
        return "{\"mode\":$mode,\"className\":\"$className\",\"methodName\":\"$methodName\",\"params\":\"$params\"," +
                "\"fieldName\":\"$fieldName\",\"fieldType\":\"$fieldType\",\"resultValues\":\"$resultValues\"}"
    }
}
