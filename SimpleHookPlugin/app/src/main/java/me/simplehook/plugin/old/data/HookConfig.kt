package me.simplehook.plugin.old.data

/**
 * 配置数据类
 * @param mode hook模式：0: 返回值、1：参数值、2：中断执行、3:静态变量值
 * @param className 类名
 * @param methodName 方法名
 * @param params 参数
 * @param fieldName 变量名
 * @param fieldClassName 变量所在类
 * @param resultValues 返回值/参数值/变量值
 * @param hookPoint before/after
 */
@kotlinx.serialization.Serializable
data class HookConfig(
    val mode: Int = 0,
    val className: String = "",
    val methodName: String = "",
    val params: String = "",
    val fieldName: String = "",
    val fieldClassName: String = "",
    val resultValues: String = "",
    val hookPoint: String = "",
    val returnClassName: String = "",
    val fieldType: String = "",
    var desc: String = "",
    var enable: Boolean = true
)
