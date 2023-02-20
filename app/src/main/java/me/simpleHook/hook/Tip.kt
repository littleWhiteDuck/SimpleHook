package me.simpleHook.hook

import com.google.gson.Gson
import me.simpleHook.util.LanguageUtils

object Tip {
    private const val chineseTip =
        "{ \"text\": \"文本：\", \"button\": \"按钮：\", \"callbackType\": \"回调类名：\", \"viewType\": \"控件类型：\", \"encryptType\": \"类型：加密\", \"rawData\": \"原始数据：\", \"encryptResult\": \"加密结果：\", \"decryptResult\": \"解密结果：\", \"key\": \"密钥：\", \"keyAlgorithm\": \"密钥算法：\", \"encrypt\": \"加密\", \"decrypt\": \"解密\", \"isDecrypt\": \"加密/解密：解密\", \"isEncrypt\": \"加密/解密：加密\", \"className\": \"类名：\", \"methodName\": \"方法名：\", \"param\": \"参数\", \"returnValue\": \"返回值：\", \"startCustomHook\": \"开始自定义Hook\", \"startExtensionHook\": \"开始扩展Hook\", \"errorType\": \"错误类型：\", \"solution\": \"解决方案：\", \"filledClassName\": \"所填类名：\", \"filledMethodParams\": \"所填方法（参数）：\", \"filledMethodOrField\": \"所填方法（参数）|变量： \", \"detailReason\": \"具体原因：\", \"notFoundClass\": \"请确保填写的类名正确\", \"noSuchMethod\": \"请确保填写的方法名/参数等数据正确\", \"paramsNotEqualValues\": \"请查看修改值个数是否与参数个数相同\", \"useSmali2Config\": \"你的方法/参数/类名填写有问题，请使用【smali转配置】来降低出错的概率\", \"useNormalVersion\": \"请注意，你的机型并不适合使用ROOT版，请使用普通版\", \"notHaveParams\": \"参数：这个方法没有参数！\", \"unknownError\": \"未知错误\", \"encryptOrDecrypt\": \"加密/解密：\", \"result\": \"结果：\", \"setClipboard\": \"写入剪贴板\", \"getClipboard\": \"读取剪贴板\", \"clipboardInfo\": \"信息：\", \"fieldName\": \"变量名：\", \"fieldValue\": \"变量值：\", \"applicationName\": \"Application入口名：\", \"createFile\": \"创建文件\", \"deleteFile\": \"删除文件\", \"readFile\": \"读取文件\", \"writeFile\": \"写出文件\", \"readAssets\": \"读取Assets文件\", \"path\": \"路径：\", \"info\": \"信息（仅显示缓存大小的部分）：\", \"notSetCacheSize\": \"没有设置缓存大小\" }"
    private const val englishTip =
        "{ \"text\": \"Text: \", \"button\": \"Button: \", \"callbackType\": \"callbackType: \", \"viewType\": \"viewType: \", \"encryptType\": \"Type: encrypt\", \"rawData\": \"Raw Data: \", \"encryptResult\": \"Encrypt result: \", \"decryptResult\": \"Decrypt result: \", \"key\": \"Key: \", \"keyAlgorithm\": \"Key algorithm: \", \"encrypt\": \"encrypt\", \"decrypt\": \"decrypt\", \"isDecrypt\": \"Encrypt/Decrypt: decrypt\", \"isEncrypt\": \"Encrypt/Decrypt: encrypt\", \"className\": \"Class name: \", \"methodName\": \"Method name: \", \"param\": \"Param\", \"returnValue\": \"Return value: \", \"startCustomHook\": \"Start custom hook\", \"startExtensionHook\": \"start extension hook\", \"errorType\": \"Error type: \", \"solution\": \"Solution: \", \"filledClassName\": \"Filled class name: \", \"filledMethodParams\": \"Filled method (parameters): \", \"filledMethodOrField\": \"Filled method(parameters)|Field: \", \"detailReason\": \"Detail reason: \", \"notFoundClass\": \"Please make sure the class name is correct\", \"noSuchMethod\": \"Please make sure that the method name/parameters and other data filled in are correct\", \"paramsNotEqualValues\": \"Please check whether the number of modified values is the same as the number of parameters\", \"useSmali2Config\": \"There is a problem with filling in your method/parameter/class name, please use [smali to config] to reduce the probability of errors\", \"useNormalVersion\": \"Please note that your model is not suitable for the ROOT version, please use the normal version\", \"notHaveParams\": \"Parameters: This method has no parameters!\", \"unknownError\": \"Unknown error\", \"encryptOrDecrypt\": \"Encrypt/Decrypt: \", \"result\": \"result: \", \"setClipboard\": \"Write clipboard\", \"getClipboard\": \"Read clipboard\", \"clipboardInfo\": \"Info: \", \"fieldName\": \"Field name: \", \"fieldValue\": \"Field value: \", \"applicationName\": \"Application name：\", \"createFile\": \"Create file\", \"deleteFile\": \"Delete file\", \"readFile\": \"Read file\", \"writeFile\": \"Write file\", \"readAssets\": \"Read Assets file\", \"path\": \"Path: \", \"info\": \"Info(Show only the cache size): \", \"notSetCacheSize\": \"No cache size set\" }"
    private val tipMap = Gson().fromJson<Map<String, String>>(chineseTip, Map::class.java)
    private val tipEnglishMap = Gson().fromJson<Map<String, String>>(englishTip, Map::class.java)
    private val isNotChinese = LanguageUtils.isNotChinese()

    fun getTip(key: String): String {
        return if (isNotChinese) {
            tipEnglishMap[key] ?: key
        } else {
            tipMap[key] ?: key
        }
    }
}

