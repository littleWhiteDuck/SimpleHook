package littleWhiteDuck


import java.util.regex.Pattern.matches

object Type {
    private const val INT_PATTERN = """^-?[1-9]\d*$"""
    private const val LONG_PATTERN = """^-?[1-9]\d*[l|L]$"""
    private const val BOOLEAN_PATTERN = """(?i)true|false"""
    private const val STRING_PATTERN = """^-?0?[1-9]\d*[s|S]$"""
    private const val NULL_PATTERN = """(?i)null"""
    private const val STRING_EMPTY_PATTERN = """(?i)empty|空"""

    fun getDataType(value: String) = when{
        matches(BOOLEAN_PATTERN,value) -> value.toBoolean()
        matches(INT_PATTERN,value) -> value.toInt()
        matches(LONG_PATTERN,value) -> value.replace(Regex("""[l|L]"""),"").toLong()
        matches(STRING_PATTERN,value) -> value.replace(Regex("""[s|S]"""),"")
        matches(NULL_PATTERN,value) -> null
        matches(STRING_EMPTY_PATTERN,value) -> ""
        else -> value
    }
    fun getClassType(className:String) = when(className){
        "java.lang.Long" -> Long::class.java
        "java.lang.Boolean" -> Boolean::class.java
        "java.lang.Int" -> Int::class.java
        else -> null
    }
}