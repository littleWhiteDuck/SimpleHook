package me.simpleHook.data.record

data class RecordListSummary(
    val title: String? = null,
    val subtitle: String? = null,
    val meta: String? = null
)

object RecordSummaryBuilder {
    const val META_PARAM = "param"
    const val META_RETURN = "return"
    const val META_PARAM_RETURN = "param_return"
    const val META_STATIC_FIELD = "static_field"
    const val META_INSTANCE_FIELD = "instance_field"
    const val META_FIELD = "field"

    fun build(record: Record, type: RecordType, fallbackSubtype: String? = null): RecordListSummary {
        return when (record) {
            is RecordParam -> methodSummary(
                className = record.className,
                methodName = record.methodName,
                params = record.params,
                meta = META_PARAM
            )

            is RecordReturn -> methodSummary(
                className = record.className,
                methodName = record.methodName,
                params = record.params,
                meta = META_RETURN
            )

            is RecordParamReturn -> methodSummary(
                className = record.className,
                methodName = record.methodName,
                params = record.params,
                meta = META_PARAM_RETURN
            )

            is RecordField -> fieldSummary(record)
            else -> RecordListSummary(title = fallbackSubtype?.takeIf { it.isNotBlank() } ?: type.name)
        }
    }

    private fun methodSummary(
        className: String,
        methodName: String,
        params: List<String>,
        meta: String
    ): RecordListSummary {
        val cleanClassName = className.takeIf { it.isNotBlank() }.orEmpty()
        val cleanMethodName = methodName.takeIf { it.isNotBlank() } ?: "<unknown>"
        val title = listOfNotNull(
            cleanClassName.simpleClassName().takeIf { it.isNotBlank() },
            cleanMethodName
        ).joinToString(".")
        val signature = buildMethodSignature(cleanClassName, cleanMethodName, params)
        return RecordListSummary(
            title = title.takeIf { it.isNotBlank() } ?: cleanMethodName,
            subtitle = signature,
            meta = meta
        )
    }

    private fun fieldSummary(record: RecordField): RecordListSummary {
        val fieldName = record.fieldName.takeIf { it.isNotBlank() } ?: "<unknown>"
        val ownerClass = record.fieldClassName?.takeIf { it.isNotBlank() }
        val callClass = record.className?.takeIf { it.isNotBlank() }
        val callMethod = record.methodName?.takeIf { it.isNotBlank() }
        val callSignature = if (callClass != null && callMethod != null) {
            buildMethodSignature(callClass, callMethod, record.params)
        } else {
            callClass
        }
        val meta = when {
            ownerClass != null -> META_STATIC_FIELD
            callSignature != null -> META_INSTANCE_FIELD
            else -> META_FIELD
        }
        return RecordListSummary(
            title = fieldName,
            subtitle = ownerClass ?: callSignature,
            meta = meta
        )
    }

    private fun buildMethodSignature(
        className: String,
        methodName: String,
        params: List<String>
    ): String {
        val paramText = params.joinToString(", ")
        return if (className.isBlank()) {
            "$methodName($paramText)"
        } else {
            "$className#$methodName($paramText)"
        }
    }

    private fun String.simpleClassName(): String {
        return substringAfterLast('.')
            .replace('$', '.')
    }
}
