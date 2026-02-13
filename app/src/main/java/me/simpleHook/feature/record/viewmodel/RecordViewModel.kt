package me.simpleHook.feature.record.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.sqlite.db.SimpleSQLiteQuery
import me.simpleHook.core.GlobalValue
import me.simpleHook.R
import me.simpleHook.data.RecordShowItem
import me.simpleHook.data.RecordShowPack
import me.simpleHook.data.RecordShowType
import me.simpleHook.data.record.Base64Operation
import me.simpleHook.data.record.Record
import me.simpleHook.data.record.RecordApplication
import me.simpleHook.data.record.RecordBase64
import me.simpleHook.data.record.RecordCipher
import me.simpleHook.data.record.RecordClickEvent
import me.simpleHook.data.record.RecordClipboard
import me.simpleHook.data.record.RecordCrash
import me.simpleHook.data.record.RecordDialog
import me.simpleHook.data.record.RecordError
import me.simpleHook.data.record.RecordExit
import me.simpleHook.data.record.RecordField
import me.simpleHook.data.record.RecordFileOperation
import me.simpleHook.data.record.RecordHmac
import me.simpleHook.data.record.RecordIntent
import me.simpleHook.data.record.RecordJson
import me.simpleHook.data.record.RecordMac
import me.simpleHook.data.record.RecordParam
import me.simpleHook.data.record.RecordParamReturn
import me.simpleHook.data.record.RecordPopupWindow
import me.simpleHook.data.record.RecordReturn
import me.simpleHook.data.record.RecordSignature
import me.simpleHook.data.record.RecordToast
import me.simpleHook.data.record.RecordType
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.data.record.RecordWebLoadUrl
import me.simpleHook.data.record.SmallRecordEntity
import me.simpleHook.data.local.db.RecordDatabase
import me.simpleHook.data.local.db.entity.RecordEntity
import me.simpleHook.data.record.RecordDetailItem as RDItem

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val recordDao = RecordDatabase.getDatabase(application).recordDao()

    private var _recordShowItems = MutableLiveData<List<RecordShowItem>>()
    val recordShowItems: LiveData<List<RecordShowItem>> get() = _recordShowItems
    val queryPattern = MutableStateFlow("")
    val fastSearchEnabled = MutableStateFlow(true)

    private val _recordDetail = MutableStateFlow(emptyList<String>())
    val recordDetail = _recordDetail

    private val _recordDetailItems = MutableLiveData<List<RDItem>>(emptyList())
    val recordDetailItems: LiveData<List<RDItem>> = _recordDetailItems

    private val pagingConfig =
        PagingConfig(pageSize = 30, prefetchDistance = 3, enablePlaceholders = true, maxSize = 200)
    private val ftsTokenRegex = Regex("""[\p{L}\p{N}_]+""")
    private val ftsEligibleRegex = Regex("""^[\p{L}\p{N}_\s]+$""")


    fun getRecordEntity(
        typeOrPack: String, isType: Boolean
    ): Flow<PagingData<SmallRecordEntity>> {
        val pattern = queryPattern.value.trim()
        val jsonPattern = pattern.toJsonStringContent()
        val ftsQuery = jsonPattern.toFtsQueryOrNull().takeIf { fastSearchEnabled.value }
        return Pager(config = pagingConfig) {
            if (isType) {
                if (pattern.isEmpty()) {
                    recordDao.getRecordByTypeNoPattern(typeOrPack)
                } else if (ftsQuery != null) {
                    recordDao.getRecordByTypeFts(buildTypeFtsQuery(typeOrPack, ftsQuery))
                } else {
                    recordDao.getRecordByType(typeOrPack, jsonPattern, pattern)
                }
            } else {
                if (pattern.isEmpty()) {
                    recordDao.getRecordByPackNoPattern(typeOrPack)
                } else if (ftsQuery != null) {
                    recordDao.getRecordByPackFts(buildPackFtsQuery(typeOrPack, ftsQuery))
                } else {
                    recordDao.getRecordByPack(typeOrPack, jsonPattern, pattern)
                }
            }
        }.flow.cachedIn(viewModelScope)
    }


    fun getMarkedRecordByType(type: String) = recordDao.getMarkedRecordByType(type)

    fun getMarkedRecordByPack(packageName: String) = recordDao.getMarkedRecordByPack(packageName)

    fun getRecordByID(id: Int) = recordDao.getRecordById(id)

    fun fetchRecordShowItems() = viewModelScope.launch(Dispatchers.IO) {
        val showByType = GlobalValue.sp.showByType

        val showItems = if (showByType) {
            recordDao.getRecordCountByType().mapNotNull { row ->
                runCatching { RecordType.valueOf(row.type) }.getOrNull()?.let { type ->
                    RecordShowType(type = type, subType = "", count = row.count)
                }
            }
        } else {
            recordDao.getRecordCountByPack().map { row ->
                RecordShowPack(packageName = row.packageName, count = row.count)
            }
        }

        _recordShowItems.postValue(showItems)
    }


    fun updateRecord(recordEntity: RecordEntity) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.updateRecords(recordEntity)
    }

    fun updateRecordReadById(id: Int, isRead: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.updateRecordReadById(id = id, isRead = isRead)
    }

    fun updateRecordMarkById(id: Int, isMark: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.updateRecordMarkById(id = id, isMark = isMark)
    }

    fun insertRecords(vararg recordEntity: RecordEntity) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.insertRecords(*recordEntity)
    }


    fun deleteAllRecords() = viewModelScope.launch {
        recordDao.deleteAllRecords()
    }


    fun deleteRecordByTimeRange(start: String, end: String) =
        viewModelScope.launch(Dispatchers.IO) {
            recordDao.deleteRecordByTimeRange(start = start, end = end)
        }

    fun deleteRecordById(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.deleteRecordById(id)
    }

    fun deleteRecordByType(type: String) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.deleteRecordByType(type)
    }

    fun deleteRecordByPack(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.deleteRecordByPack(packageName)
    }

    fun deleteRecordByRead(read: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.deleteReadRecord(isRead = read)
    }

    fun deleteReadRecordByPack(read: Boolean, packageName: String) = viewModelScope.launch(
        Dispatchers.IO
    ) {
        recordDao.deleteReadRecordByPack(read, packageName)
    }

    fun deleteReadRecordByType(read: Boolean, type: String) =
        viewModelScope.launch(Dispatchers.IO) {
            recordDao.deleteReadRecordByType(read, type)
        }

    fun deleteMarkedRecordByPack(isMark: Boolean, packageName: String) = viewModelScope.launch(
        Dispatchers.IO
    ) {
        recordDao.deleteMarkedRecordByPack(isMark, packageName)
    }

    fun deleteMarkedRecordByType(isMark: Boolean, type: String) = viewModelScope.launch(
        Dispatchers.IO
    ) {
        recordDao.deleteMarkedRecordByType(isMark, type)
    }


    private fun Int.string() = application.getString(this)
    private fun Int.string(vararg formatArgs: Any) = application.getString(this, *formatArgs)

    fun fetchRecordDetail(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        val recordEntity = getRecordByID(id)
        val record = Json.decodeFromString<Record>(recordEntity.record)
        fetchCodeStyleDetail(record)
        fetchCardStyleDetail(record)
    }

    private fun fetchCardStyleDetail(record: Record) {
        val list = mutableListOf<RDItem>()
        with(list) {
            val labelType = R.string.record_label_type.string()
            val labelEntryName = R.string.record_label_entry_name.string()
            val labelEncodeDecode = R.string.record_label_encode_decode.string()
            val labelEncryptDecrypt = R.string.record_label_encrypt_decrypt.string()
            val labelViewType = R.string.record_label_view_type.string()
            val labelCallbackClass = R.string.record_label_callback_class.string()
            val labelId = R.string.record_label_id.string()
            val labelTextContent = R.string.record_label_text_content.string()
            val labelWriteRead = R.string.record_label_write_read.string()
            val labelContent = R.string.record_label_content.string()
            val labelThreadName = R.string.record_label_thread_name.string()
            val labelConfigInfo = R.string.record_label_config_info.string()
            val labelSupplementInfo = R.string.record_label_supplement_info.string()
            val labelExitType = R.string.record_label_exit_type.string()
            val labelRecordFieldValue = R.string.record_label_record_field_value.string()
            val labelClassName = R.string.record_label_class_name.string()
            val labelMethodWithParams = R.string.record_label_method_name_params.string()
            val labelFieldClassName = R.string.record_label_field_owner_class_name.string()
            val labelPath = R.string.record_label_path.string()
            val labelOperationContent = R.string.record_label_operation_content.string()
            val labelPackageName = R.string.record_label_package_name.string()
            val labelAction = R.string.record_label_action.string()
            val labelData = R.string.record_label_data.string()
            val labelUrl = R.string.record_label_url.string()
            val noId = R.string.record_no_id.string()

            when (record) {
                is RecordApplication -> {
                    add(RDItem(title = labelType, content = "Application"))
                    add(RDItem(title = labelEntryName, content = record.name))
                }

                is RecordBase64 -> {
                    val codeType = when (record.operation) {
                        Base64Operation.Encode -> R.string.record_base64_encode
                        Base64Operation.Decode -> R.string.record_base64_decode
                    }.string()
                    add(RDItem(title = labelType, content = "Base64"))
                    add(RDItem(title = labelEncodeDecode, content = codeType))
                    record.rawData.forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            codeType,
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))

                }

                is RecordCipher -> {
                    add(RDItem(title = labelType, content = record.algorithm))
                    add(RDItem(title = labelEncryptDecrypt, content = record.cryptType.displayId.string()))
                    record.key?.forEach {
                        val title = R.string.record_key_format.string(it.key.displayName, "")
                        add(RDItem(title = title, content = it.value))
                    }
                    record.iv?.forEach {
                        val title = R.string.record_iv_format.string(it.key.displayName, "")
                        add(RDItem(title = title, content = it.value))
                    }
                    record.rawData.forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            "",
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordClickEvent -> {
                    add(RDItem(title = labelType, content = R.string.record_type_click_event.string()))
                    add(RDItem(title = labelViewType, content = record.viewType))
                    add(RDItem(title = labelCallbackClass, content = record.callbackType))
                    add(RDItem(title = labelId, content = record.viewId ?: noId))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordClipboard -> {
                    add(RDItem(title = labelType, content = R.string.record_type_clipboard.string()))
                    val typeID = R.string.record_read_clipboard.takeIf { record.isRead }
                        ?: R.string.record_write_clipboard
                    add(RDItem(labelWriteRead, content = typeID.string()))
                    add(RDItem(labelContent, content = record.info))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordCrash -> {
                    add(RDItem(title = labelType, content = R.string.record_type_crash.string()))
                    add(RDItem(title = labelThreadName, content = record.threadName))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordDialog -> {
                    add(RDItem(title = labelType, content = record.dialogType.displayId.string()))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordError -> {
                    add(RDItem(title = labelType, content = record.errorType.displayId.string()))
                    record.hookConfig?.let {
                        add(RDItem(title = labelConfigInfo, content = Json.encodeToString(it)))
                    }
                    record.supplement?.let {
                        add(RDItem(title = labelSupplementInfo, content = it))
                    }
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordExit -> {
                    add(RDItem(title = labelType, content = R.string.record_type_exit.string()))
                    add(RDItem(title = labelExitType, content = record.exitType))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordField -> {
                    add(
                        RDItem(
                            title = labelRecordFieldValue,
                            content = R.string.record_type_field_value.string()
                        )
                    )
                    with(record) {
                        className?.let {
                            add(RDItem(title = labelClassName, it))
                        }
                        methodName?.let {
                            val methodSign =
                                "${record.methodName}(${record.params.joinToString(",")})"
                            add(RDItem(title = labelMethodWithParams, methodSign))
                        }
                        fieldClassName?.let {
                            add(RDItem(title = labelFieldClassName, content = it))
                        }
                        addAll(filedValue.map {
                            RDItem(
                                title = R.string.record_label_field_value_format.string(it.key.displayName),
                                content = it.value
                            )
                        })
                    }
                }

                is RecordFileOperation -> {
                    add(RDItem(title = labelType, content = record.operation.displayId.string()))
                    add(RDItem(title = labelPath, content = record.path))
                    record.partData?.let {
                        add(RDItem(title = labelOperationContent, content = it))
                    }
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordHmac -> {
                    add(RDItem(title = labelType, content = record.algorithm))
                    record.key?.forEach {
                        val title = R.string.record_key_format.string(it.key.displayName, "")
                        add(RDItem(title = title, content = it.value))
                    }
                    record.rawData.forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            "",
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordIntent -> {
                    add(RDItem(title = labelType, content = "Intent"))
                    add(RDItem(title = labelPackageName, content = record.packageName))
                    add(RDItem(title = labelClassName, content = record.className))
                    add(RDItem(title = labelAction, content = record.action))
                    add(RDItem(title = labelData, content = record.data))
                    record.extras.forEachIndexed { index, extra ->
                        val extraValues = extra.value.entries.joinToString("\n") {
                            R.string.record_label_extra_value_format.string(it.key.displayName, it.value)
                        }
                        add(
                            RDItem(
                                R.string.record_label_extra_format.string(index + 1), content = """
                            ${R.string.record_label_extra_type_format.string(extra.intentType)}
                            ${R.string.record_label_extra_key_format.string(extra.key)}
                            $extraValues
                        """.trimIndent()
                            )
                        )
                    }
                }

                is RecordJson -> {
                    add(RDItem(title = labelType, content = record.jsonType.displayId.string()))
                    addAll(record.values.map {
                        RDItem(title = it.key, content = it.value)
                    })
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordMac -> {
                    add(RDItem(title = labelType, content = record.algorithm))
                    record.rawData.forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            "",
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordParam -> {
                    add(RDItem(title = labelType, content = R.string.record_type_param_value.string()))
                    add(RDItem(title = labelClassName, content = record.className))
                    val methodSign =
                        "${record.methodName}(${record.params.joinToString(",")})"
                    add(RDItem(title = labelMethodWithParams, methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(map.map {
                            RDItem(
                                title = R.string.record_label_param_value_format.string(
                                    index + 1,
                                    it.key.displayName
                                ),
                                content = it.value
                            )
                        })
                    }
                    add(
                        RDItem(
                            title = R.string.record_call_stack.string(),
                            record.callStack.joinToString("\n")
                        )
                    )
                }

                is RecordParamReturn -> {
                    add(RDItem(title = labelType, content = R.string.record_type_param_value.string()))
                    add(RDItem(title = labelClassName, content = record.className))
                    val methodSign =
                        "${record.methodName}(${record.params.joinToString(",")})"
                    add(RDItem(title = labelMethodWithParams, methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(map.map {
                            RDItem(
                                title = R.string.record_label_param_value_format.string(
                                    index + 1,
                                    it.key.displayName
                                ),
                                content = it.value
                            )
                        })
                    }
                    addAll(record.returnValue.map {
                        RDItem(
                            title = R.string.record_label_return_value_format.string(it.key.displayName),
                            content = it.value
                        )
                    })
                    add(
                        RDItem(
                            title = R.string.record_call_stack.string(),
                            record.callStack.joinToString("\n")
                        )
                    )
                }

                is RecordPopupWindow -> {
                    add(RDItem(title = labelType, content = record.popupType.displayId.string()))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordReturn -> {
                    add(RDItem(title = labelType, content = R.string.record_type_param_value.string()))
                    add(RDItem(title = labelClassName, content = record.className))
                    val methodSign =
                        "${record.methodName}(${record.params.joinToString(",")})"
                    add(RDItem(title = labelMethodWithParams, methodSign))
                    addAll(record.returnValue.map {
                        RDItem(
                            title = R.string.record_label_return_value_format.string(it.key.displayName),
                            content = it.value
                        )
                    })
                    add(
                        RDItem(
                            title = R.string.record_call_stack.string(),
                            record.callStack.joinToString("\n")
                        )
                    )
                }

                is RecordSignature -> {
                    add(RDItem(title = labelType, content = R.string.record_type_signature.string()))
                    add(RDItem(title = R.string.record_label_md5.string(), content = record.md5))
                    add(RDItem(title = R.string.record_label_sha1.string(), content = record.sha1))
                    add(RDItem(title = R.string.record_label_sha256.string(), content = record.sha256))
                    add(RDItem(title = R.string.record_label_char_string.string(), content = record.charStr))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))
                }

                is RecordToast -> {
                    add(RDItem(title = labelClassName, content = R.string.record_type_toast.string()))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    add(RDItem(title = R.string.record_call_stack.string(), record.stackDetail))

                }

                is RecordWebLoadUrl -> {
                    add(RDItem(title = labelClassName, content = R.string.record_type_web_url.string()))
                    add(RDItem(title = labelUrl, content = record.url))
                    addAll(record.headers.map {
                        RDItem(title = it.key, content = it.value)
                    })
                }
            }
        }

        _recordDetailItems.postValue(list)
    }

    private fun fetchCodeStyleDetail(record: Record) {
        val items = mutableListOf<String>()

        with(items) {
            when (record) {
                is RecordApplication -> {
                    add(R.string.record_type_format.string("Application"))
                    add(R.string.record_application_name_format.string(record.name))
                }

                is RecordBase64 -> {
                    val type = when (record.operation) {
                        Base64Operation.Encode -> R.string.record_base64_encode
                        Base64Operation.Decode -> R.string.record_base64_decode
                    }.string()

                    add(R.string.record_type_format.string("Base64"))
                    add(R.string.record_base64_code_type_format.string(type))
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(type = type))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordCipher -> {
                    add(R.string.record_type_format.string(record.algorithm))
                    add(R.string.record_cipher_crypt_type_format.string(record.cryptType.displayId.string()))
                    record.key?.let {
                        addAll(it.toKeyList())
                    }
                    record.iv?.let {
                        addAll(it.toIvList())
                    }
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(""))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordClickEvent -> {
                    add(R.string.record_type_format.string(R.string.record_type_click_event.string()))
                    add(R.string.record_view_type_format.string(record.viewType))
                    add(R.string.record_callback_class_format.string(record.callbackType))
                    add(R.string.record_id_format.string(record.viewId ?: R.string.record_no_id.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordClipboard -> {
                    add(R.string.record_type_format.string(R.string.record_type_clipboard.string()))
                    val typeID = R.string.record_read_clipboard.takeIf { record.isRead }
                        ?: R.string.record_write_clipboard
                    add(R.string.record_clipboard_read_or_write_format.string(typeID.string()))
                    add(R.string.record_clipboard_content_format.string(record.info))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordCrash -> {
                    add(R.string.record_type_format.string(R.string.record_type_crash.string()))
                    add(R.string.record_crash_thread_name_format.string(record.threadName))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordDialog -> {
                    add(R.string.record_type_format.string(record.dialogType.displayId.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordError -> {
                    add(R.string.record_error_type_format.string(record.errorType.displayId.string()))
                    record.hookConfig?.let {
                        add(R.string.record_error_config_info.string())
                        add(Json.encodeToString(it))
                    }
                    record.supplement?.let {
                        add(R.string.record_error_supplement_info.string())
                        add(it)
                    }
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordExit -> {
                    add(R.string.record_type_format.string(R.string.record_type_exit.string()))
                    add(R.string.record_exit_type_format.string(record.exitType))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordField -> {
                    add(R.string.record_type_format.string(R.string.record_type_field_value.string()))
                    with(record) {
                        className?.let {
                            add(R.string.record_class_name_format.string(it))
                        }
                        methodName?.let {
                            val methodSign =
                                "${record.methodName}(${record.params.joinToString(",")})"
                            add(R.string.record_method_sign_format.string(methodSign))
                        }
                        fieldClassName?.let {
                            add(R.string.record_field_class_name_format.string(it))
                        }
                        addAll(filedValue.map {
                            R.string.record_field_value_format.string(it.key.displayName, it.value)
                        })
                    }
                }

                is RecordFileOperation -> {
                    add(R.string.record_type_format.string(record.operation.displayId.string()))
                    add(R.string.record_file_path_format.string(record.path))
                    record.partData?.let {
                        add(R.string.record_file_op_content.string())
                        add(it)
                    }
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordMac -> {
                    add(R.string.record_type_format.string(record.algorithm))
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(""))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordJson -> {
                    add(R.string.record_type_format.string(record.jsonType.displayId.string()))
                    addAll(
                        record.values.map {
                            R.string.record_label_name_value_format.string(it.key, it.value)
                        }
                    )
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordHmac -> {
                    add(R.string.record_type_format.string(record.algorithm))
                    record.key?.let {
                        addAll(it.toKeyList())
                    }
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(""))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordParam -> {
                    add(R.string.record_type_format.string(R.string.record_type_param_value.string()))
                    add(R.string.record_class_name_format.string(record.className))
                    val methodSign = "${record.methodName}(${record.params.joinToString(",")})"
                    add(R.string.record_method_sign_format.string(methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(
                            map.map {
                                R.string.record_param_value_format.string(
                                    index + 1,
                                    it.key,
                                    it.value
                                )
                            }
                        )
                    }
                    add(R.string.record_call_stack.string())
                    addAll(record.callStack)
                }

                is RecordParamReturn -> {
                    val methodSign = "${record.methodName}(${record.params.joinToString(",")})"

                    add(R.string.record_type_format.string(R.string.record_type_return_value.string()))
                    add(R.string.record_class_name_format.string(record.className))
                    add(R.string.record_method_sign_format.string(methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(
                            map.map {
                                R.string.record_param_value_format.string(
                                    index + 1,
                                    it.key,
                                    it.value
                                )
                            }
                        )
                    }
                    addAll(record.returnValue.map {
                        R.string.record_return_value_format.string(it.key.displayName, it.value)
                    })
                    add(R.string.record_call_stack.string())
                    addAll(record.callStack)
                }

                is RecordPopupWindow -> {
                    add(R.string.record_type_format.string(record.popupType.displayId.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordReturn -> {
                    val methodSign = "${record.methodName}(${record.params.joinToString(",")})"
                    add(R.string.record_type_format.string(R.string.record_type_return_value.string()))
                    add(R.string.record_class_name_format.string(record.className))
                    add(R.string.record_method_sign_format.string(methodSign))
                    addAll(record.returnValue.map {
                        R.string.record_return_value_format.string(it.key.displayName, it.value)
                    })
                    add(R.string.record_call_stack.string())
                    addAll(record.callStack)
                }

                is RecordSignature -> {
                    add(R.string.record_type_format.string(R.string.record_type_signature.string()))
                    add(R.string.record_signature_type_format.string("MD5", record.md5))
                    add(R.string.record_signature_type_format.string("SHA-1", record.sha1))
                    add(R.string.record_signature_type_format.string("SHA-256", record.sha256))
                    add(R.string.record_signature_type_format.string("CharString", record.charStr))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordToast -> {
                    add(R.string.record_type_format.string(R.string.record_type_toast.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordWebLoadUrl -> {
                    add(R.string.record_type_format.string(R.string.record_type_web_url.string()))
                    add(R.string.record_url_format.string(record.url))
                    add(R.string.record_label_headers.string())
                    addAll(
                        record.headers.map {
                            R.string.record_label_name_value_format.string(it.key, it.value)
                        }
                    )
                }

                is RecordIntent -> {
                    add(R.string.record_type_format.string("Intent"))
                    add(R.string.record_intent_package_name_format.string(record.packageName))
                    add(R.string.record_intent_class_name_format.string(record.className))
                    add(R.string.record_intent_action_format.string(record.action))
                    add(R.string.record_intent_data_format.string(record.data))
                    record.extras.forEach {
                        add(R.string.record_intent_type_format.string(it.intentType))
                        add(R.string.record_intent_key_format.string(it.key))
                        it.value.forEach { mapEntry ->
                            add(R.string.record_intent_value_to_string_format.string(mapEntry.value))
                        }
                        add(R.string.record_intent_separator.string())
                    }
                }
            }
        }

        _recordDetail.value = items
    }

    private fun Map<RecordValueType, String>.toRawList(): List<String> {
        return map {
            R.string.record_raw_data_format_type_value.string(it.key.displayName, it.value)
        }
    }

    private fun Map<RecordValueType, String>.toResultList(type: String): List<String> {
        return map {
            R.string.record_result_data_format_type2_value.string(
                type,
                it.key.displayName,
                it.value
            )
        }
    }

    private fun Map<RecordValueType, String>.toKeyList(): List<String> {
        return map {
            R.string.record_key_format.string(it.key.displayName, it.value)
        }
    }

    private fun Map<RecordValueType, String>.toIvList(): List<String> {
        return map {
            R.string.record_iv_format.string(it.key.displayName, it.value)

        }
    }

    private fun String.toFtsQueryOrNull(): String? {
        if (!ftsEligibleRegex.matches(this)) return null
        val tokens = ftsTokenRegex.findAll(this).map { it.value }.toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { "\"$it\"*" }
    }

    private fun buildPackFtsQuery(
        packageName: String,
        ftsQuery: String
    ): SimpleSQLiteQuery {
        val sql = """
            SELECT r.id,r.type,r.subType,r.packageName,r.isRead,r.isMark,r.time
            FROM RecordEntity r
            JOIN RecordEntityFts ON RecordEntityFts.rowid = r.id
            WHERE r.packageName = ?
              AND RecordEntityFts MATCH ?
            ORDER BY r.time DESC
        """.trimIndent()
        return SimpleSQLiteQuery(sql, arrayOf(packageName, ftsQuery))
    }

    private fun buildTypeFtsQuery(type: String, ftsQuery: String): SimpleSQLiteQuery {
        val sql = """
            SELECT r.id,r.type,r.subType,r.packageName,r.isRead,r.isMark,r.time
            FROM RecordEntity r
            JOIN RecordEntityFts ON RecordEntityFts.rowid = r.id
            WHERE r.type = ?
              AND RecordEntityFts MATCH ?
            ORDER BY r.time DESC
        """.trimIndent()
        return SimpleSQLiteQuery(sql, arrayOf(type, ftsQuery))
    }

    private fun String.toJsonStringContent(): String {
        return Json.encodeToString(this).removePrefix("\"").removeSuffix("\"")
    }
}
