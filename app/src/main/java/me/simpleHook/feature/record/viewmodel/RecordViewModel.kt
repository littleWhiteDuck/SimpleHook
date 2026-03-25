package me.simpleHook.feature.record.viewmodel

import android.app.Application
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
import me.simpleHook.R
import me.simpleHook.core.GlobalValue
import me.simpleHook.data.RecordShowItem
import me.simpleHook.data.RecordShowPack
import me.simpleHook.data.RecordShowType
import me.simpleHook.data.local.db.RecordDatabase
import me.simpleHook.data.local.db.entity.RecordEntity
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
import me.simpleHook.data.record.RecordIntentExtra
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
import me.simpleHook.data.record.RecordDetailItem as RDItem

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val CARD_CONTENT_PREVIEW_LIMIT = 600
    }


    private val recordDao = RecordDatabase.getDatabase(application).recordDao()

    private var _recordShowItems = MutableLiveData<List<RecordShowItem>>()
    val recordShowItems: LiveData<List<RecordShowItem>> get() = _recordShowItems
    val queryPattern = MutableStateFlow("")

    private val _recordDetail = MutableStateFlow(emptyList<String>())
    val recordDetail = _recordDetail

    private val _recordEntity = MutableLiveData<RecordEntity?>()
    val recordEntity: LiveData<RecordEntity?> = _recordEntity

    private val _recordDetailItems = MutableLiveData<List<RDItem>>(emptyList())
    val recordDetailItems: LiveData<List<RDItem>> = _recordDetailItems

    private val pagingConfig =
        PagingConfig(pageSize = 30, prefetchDistance = 3, enablePlaceholders = true, maxSize = 200)


    fun getRecordEntity(
        typeOrPack: String, isType: Boolean
    ): Flow<PagingData<SmallRecordEntity>> {
        val pattern = queryPattern.value.trim()
        val jsonPattern = pattern.toJsonStringContent()
        return Pager(config = pagingConfig) {
            if (isType) {
                if (pattern.isEmpty()) {
                    recordDao.getRecordByTypeNoPattern(typeOrPack)
                } else {
                    recordDao.getRecordByType(typeOrPack, jsonPattern, pattern)
                }
            } else {
                if (pattern.isEmpty()) {
                    recordDao.getRecordByPackNoPattern(typeOrPack)
                } else {
                    recordDao.getRecordByPack(typeOrPack, jsonPattern, pattern)
                }
            }
        }.flow.cachedIn(viewModelScope)
    }


    fun getMarkedRecordByType(type: String) = recordDao.getMarkedRecordByType(type)

    fun getMarkedRecordByPack(packageName: String) = recordDao.getMarkedRecordByPack(packageName)

    fun getRecordByID(id: Int) = recordDao.getRecordById(id)

    suspend fun refreshRecordShowItemsNow() {
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

    suspend fun insertRecordsNow(recordEntities: List<RecordEntity>) {
        if (recordEntities.isEmpty()) return
        recordDao.insertRecords(*recordEntities.toTypedArray())
    }


    fun deleteAllRecords() = viewModelScope.launch {
        recordDao.deleteAllRecords()
    }

    suspend fun deleteAllRecordsNow() {
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

    suspend fun deleteRecordByTypeNow(type: String) {
        recordDao.deleteRecordByType(type)
    }

    fun deleteRecordByPack(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.deleteRecordByPack(packageName)
    }

    suspend fun deleteRecordByPackNow(packageName: String) {
        recordDao.deleteRecordByPack(packageName)
    }

    fun deleteRecordByRead(read: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.deleteReadRecord(isRead = read)
    }

    suspend fun deleteRecordByReadNow(read: Boolean) {
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
        _recordEntity.postValue(recordEntity)
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
                    record.rawData.displayEntries().forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.displayEntries().forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            codeType,
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    addStackDetailIfPresent(record.stackDetail)

                }

                is RecordCipher -> {
                    add(RDItem(title = labelType, content = record.algorithm))
                    add(RDItem(title = labelEncryptDecrypt, content = record.cryptType.displayId.string()))
                    record.key?.displayEntries()?.forEach {
                        val title = R.string.record_key_format.string(it.key.displayName, "")
                        add(RDItem(title = title, content = it.value))
                    }
                    record.iv?.displayEntries()?.forEach {
                        val title = R.string.record_iv_format.string(it.key.displayName, "")
                        add(RDItem(title = title, content = it.value))
                    }
                    record.rawData.displayEntries().forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.displayEntries().forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            "",
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordClickEvent -> {
                    add(RDItem(title = labelType, content = R.string.record_type_click_event.string()))
                    add(RDItem(title = labelViewType, content = record.viewType))
                    add(RDItem(title = labelCallbackClass, content = record.callbackType))
                    add(RDItem(title = labelId, content = record.viewId ?: noId))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordClipboard -> {
                    add(RDItem(title = labelType, content = R.string.record_type_clipboard.string()))
                    val typeID = R.string.record_read_clipboard.takeIf { record.isRead }
                        ?: R.string.record_write_clipboard
                    add(RDItem(labelWriteRead, content = typeID.string()))
                    add(RDItem(labelContent, content = record.info))
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordCrash -> {
                    add(RDItem(title = labelType, content = R.string.record_type_crash.string()))
                    add(RDItem(title = labelThreadName, content = record.threadName))
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordDialog -> {
                    add(RDItem(title = labelType, content = record.dialogType.displayId.string()))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordError -> {
                    add(RDItem(title = labelType, content = record.errorType.displayId.string()))
                    record.hookConfig?.let {
                        add(RDItem(title = labelConfigInfo, content = Json.encodeToString(it)))
                    }
                    record.supplement?.let {
                        add(RDItem(title = labelSupplementInfo, content = it))
                    }
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordExit -> {
                    add(RDItem(title = labelType, content = R.string.record_type_exit.string()))
                    add(RDItem(title = labelExitType, content = record.exitType))
                    addStackDetailIfPresent(record.stackDetail)
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
                        addAll(filedValue.displayEntries().map {
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
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordHmac -> {
                    add(RDItem(title = labelType, content = record.algorithm))
                    record.key?.displayEntries()?.forEach {
                        val title = R.string.record_key_format.string(it.key.displayName, "")
                        add(RDItem(title = title, content = it.value))
                    }
                    record.rawData.displayEntries().forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.displayEntries().forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            "",
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordIntent -> {
                    add(RDItem(title = labelType, content = "Intent"))
                    add(RDItem(title = labelPackageName, content = record.packageName))
                    add(RDItem(title = labelClassName, content = record.className))
                    add(RDItem(title = labelAction, content = record.action))
                    add(RDItem(title = labelData, content = record.data))
                    record.extras.forEachIndexed { index, extra ->
                        add(
                            RDItem(
                                R.string.record_label_extra_format.string(index + 1),
                                content = extra.toCardStyleContent()
                            )
                        )
                    }
                }

                is RecordJson -> {
                    add(RDItem(title = labelType, content = record.jsonType.displayId.string()))
                    addAll(record.values.map {
                        RDItem(title = it.key, content = it.value)
                    })
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordMac -> {
                    add(RDItem(title = labelType, content = record.algorithm))
                    record.rawData.displayEntries().forEach {
                        val title = R.string.record_raw_data_format_type.string(it.key.displayName)
                        add(RDItem(title = title, content = it.value))
                    }
                    record.resultData.displayEntries().forEach {
                        val title = R.string.record_result_data_format_type2.string(
                            "",
                            it.key.displayName
                        )
                        add(RDItem(title = title, content = it.value))
                    }
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordParam -> {
                    add(RDItem(title = labelType, content = R.string.record_type_param_value.string()))
                    add(RDItem(title = labelClassName, content = record.className))
                    val methodSign =
                        "${record.methodName}(${record.params.joinToString(",")})"
                    add(RDItem(title = labelMethodWithParams, methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(map.displayEntries().map {
                            RDItem(
                                title = R.string.record_label_param_value_format.string(
                                    index + 1,
                                    it.key.displayName
                                ),
                                content = it.value
                            )
                        })
                    }
                    addCallStackIfPresent(record.callStack)
                }

                is RecordParamReturn -> {
                    add(RDItem(title = labelType, content = R.string.record_type_param_value.string()))
                    add(RDItem(title = labelClassName, content = record.className))
                    val methodSign =
                        "${record.methodName}(${record.params.joinToString(",")})"
                    add(RDItem(title = labelMethodWithParams, methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(map.displayEntries().map {
                            RDItem(
                                title = R.string.record_label_param_value_format.string(
                                    index + 1,
                                    it.key.displayName
                                ),
                                content = it.value
                            )
                        })
                    }
                    addAll(record.returnValue.displayEntries().map {
                        RDItem(
                            title = R.string.record_label_return_value_format.string(it.key.displayName),
                            content = it.value
                        )
                    })
                    addCallStackIfPresent(record.callStack)
                }

                is RecordPopupWindow -> {
                    add(RDItem(title = labelType, content = record.popupType.displayId.string()))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordReturn -> {
                    add(RDItem(title = labelType, content = R.string.record_type_param_value.string()))
                    add(RDItem(title = labelClassName, content = record.className))
                    val methodSign =
                        "${record.methodName}(${record.params.joinToString(",")})"
                    add(RDItem(title = labelMethodWithParams, methodSign))
                    addAll(record.returnValue.displayEntries().map {
                        RDItem(
                            title = R.string.record_label_return_value_format.string(it.key.displayName),
                            content = it.value
                        )
                    })
                    addCallStackIfPresent(record.callStack)
                }

                is RecordSignature -> {
                    add(RDItem(title = labelType, content = R.string.record_type_signature.string()))
                    record.md5.takeIf { it.hasDisplayValue() }?.let {
                        add(RDItem(title = R.string.record_label_md5.string(), content = it))
                    }
                    record.sha1.takeIf { it.hasDisplayValue() }?.let {
                        add(RDItem(title = R.string.record_label_sha1.string(), content = it))
                    }
                    record.sha256.takeIf { it.hasDisplayValue() }?.let {
                        add(RDItem(title = R.string.record_label_sha256.string(), content = it))
                    }
                    record.charStr.takeIf { it.hasDisplayValue() }?.let {
                        add(RDItem(title = R.string.record_label_char_string.string(), content = it))
                    }
                    addStackDetailIfPresent(record.stackDetail)
                }

                is RecordToast -> {
                    add(RDItem(title = labelClassName, content = R.string.record_type_toast.string()))
                    add(RDItem(title = labelTextContent, content = record.textList.joinToString("\n")))
                    addStackDetailIfPresent(record.stackDetail)

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

        _recordDetailItems.postValue(
            list.map { it.toCardPreview(maxLength = CARD_CONTENT_PREVIEW_LIMIT) }
        )
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
                    addStackLinesIfPresent(record.stackDetail)
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
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordClickEvent -> {
                    add(R.string.record_type_format.string(R.string.record_type_click_event.string()))
                    add(R.string.record_view_type_format.string(record.viewType))
                    add(R.string.record_callback_class_format.string(record.callbackType))
                    add(R.string.record_id_format.string(record.viewId ?: R.string.record_no_id.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordClipboard -> {
                    add(R.string.record_type_format.string(R.string.record_type_clipboard.string()))
                    val typeID = R.string.record_read_clipboard.takeIf { record.isRead }
                        ?: R.string.record_write_clipboard
                    add(R.string.record_clipboard_read_or_write_format.string(typeID.string()))
                    add(R.string.record_clipboard_content_format.string(record.info))
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordCrash -> {
                    add(R.string.record_type_format.string(R.string.record_type_crash.string()))
                    add(R.string.record_crash_thread_name_format.string(record.threadName))
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordDialog -> {
                    add(R.string.record_type_format.string(record.dialogType.displayId.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    addStackLinesIfPresent(record.stackDetail)
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
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordExit -> {
                    add(R.string.record_type_format.string(R.string.record_type_exit.string()))
                    add(R.string.record_exit_type_format.string(record.exitType))
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
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
                        addAll(filedValue.displayEntries().map {
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
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordMac -> {
                    add(R.string.record_type_format.string(record.algorithm))
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(""))
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordJson -> {
                    add(R.string.record_type_format.string(record.jsonType.displayId.string()))
                    addAll(
                        record.values.map {
                            R.string.record_label_name_value_format.string(it.key, it.value)
                        }
                    )
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordHmac -> {
                    add(R.string.record_type_format.string(record.algorithm))
                    record.key?.let {
                        addAll(it.toKeyList())
                    }
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(""))
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordParam -> {
                    add(R.string.record_type_format.string(R.string.record_type_param_value.string()))
                    add(R.string.record_class_name_format.string(record.className))
                    val methodSign = "${record.methodName}(${record.params.joinToString(",")})"
                    add(R.string.record_method_sign_format.string(methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(
                            map.displayEntries().map {
                                R.string.record_param_value_format.string(
                                    index + 1,
                                    it.key,
                                    it.value
                                )
                            }
                        )
                    }
                    add(R.string.record_call_stack.string())
                    addCallStackLinesIfPresent(record.callStack)
                }

                is RecordParamReturn -> {
                    val methodSign = "${record.methodName}(${record.params.joinToString(",")})"

                    add(R.string.record_type_format.string(R.string.record_type_return_value.string()))
                    add(R.string.record_class_name_format.string(record.className))
                    add(R.string.record_method_sign_format.string(methodSign))
                    record.paramValues.forEachIndexed { index, map ->
                        addAll(
                            map.displayEntries().map {
                                R.string.record_param_value_format.string(
                                    index + 1,
                                    it.key,
                                    it.value
                                )
                            }
                        )
                    }
                    addAll(record.returnValue.displayEntries().map {
                        R.string.record_return_value_format.string(it.key.displayName, it.value)
                    })
                    add(R.string.record_call_stack.string())
                    addCallStackLinesIfPresent(record.callStack)
                }

                is RecordPopupWindow -> {
                    add(R.string.record_type_format.string(record.popupType.displayId.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordReturn -> {
                    val methodSign = "${record.methodName}(${record.params.joinToString(",")})"
                    add(R.string.record_type_format.string(R.string.record_type_return_value.string()))
                    add(R.string.record_class_name_format.string(record.className))
                    add(R.string.record_method_sign_format.string(methodSign))
                    addAll(record.returnValue.displayEntries().map {
                        R.string.record_return_value_format.string(it.key.displayName, it.value)
                    })
                    add(R.string.record_call_stack.string())
                    addCallStackLinesIfPresent(record.callStack)
                }

                is RecordSignature -> {
                    add(R.string.record_type_format.string(R.string.record_type_signature.string()))
                    record.md5.takeIf { it.hasDisplayValue() }?.let {
                        add(R.string.record_signature_type_format.string("MD5", it))
                    }
                    record.sha1.takeIf { it.hasDisplayValue() }?.let {
                        add(R.string.record_signature_type_format.string("SHA-1", it))
                    }
                    record.sha256.takeIf { it.hasDisplayValue() }?.let {
                        add(R.string.record_signature_type_format.string("SHA-256", it))
                    }
                    record.charStr.takeIf { it.hasDisplayValue() }?.let {
                        add(R.string.record_signature_type_format.string("CharString", it))
                    }
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
                }

                is RecordToast -> {
                    add(R.string.record_type_format.string(R.string.record_type_toast.string()))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    add(R.string.record_call_stack.string())
                    addStackLinesIfPresent(record.stackDetail)
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
                    record.extras.forEachIndexed { index, extra ->
                        addAll(extra.toCodeStyleLines())
                        if (index != record.extras.lastIndex) {
                            add(R.string.record_intent_separator.string())
                        }
                    }
                }
            }
        }

        _recordDetail.value = items
    }

    private fun MutableList<RDItem>.addStackDetailIfPresent(stackDetail: String) {
        if (stackDetail.hasDisplayValue()) {
            add(RDItem(title = R.string.record_call_stack.string(), content = stackDetail))
        }
    }

    private fun MutableList<RDItem>.addCallStackIfPresent(callStack: List<String>) {
        val stackText = callStack.filter { it.hasDisplayValue() }.joinToString("\n")
        if (stackText.hasDisplayValue()) {
            add(RDItem(title = R.string.record_call_stack.string(), content = stackText))
        }
    }

    private fun MutableList<String>.addStackLinesIfPresent(stackDetail: String) {
        val stackLines = stackDetail.split("\n").filter { it.hasDisplayValue() }
        if (stackLines.isEmpty()) {
            if (isNotEmpty() && last() == R.string.record_call_stack.string()) {
                removeAt(lastIndex)
            }
            return
        }
        addAll(stackLines)
    }

    private fun MutableList<String>.addCallStackLinesIfPresent(callStack: List<String>) {
        val stackLines = callStack.filter { it.hasDisplayValue() }
        if (stackLines.isEmpty()) {
            if (isNotEmpty() && last() == R.string.record_call_stack.string()) {
                removeAt(lastIndex)
            }
            return
        }
        addAll(stackLines)
    }

    private fun RecordIntentExtra.toCardStyleContent(): String {
        return buildList {
            add(R.string.record_label_extra_type_format.string(intentType))
            add(R.string.record_label_extra_key_format.string(key))
            addAll(
                value.displayEntries().map {
                    R.string.record_label_extra_value_format.string(it.key.displayName, it.value)
                }
            )
        }.joinToString("\n")
    }

    private fun RecordIntentExtra.toCodeStyleLines(): List<String> {
        return buildList {
            add(R.string.record_intent_type_format.string(intentType))
            add(R.string.record_intent_key_format.string(key))
            addAll(
                value.displayEntries().map {
                    R.string.record_intent_value_to_string_format.string(it.value)
                }
            )
        }
    }

    private fun Map<RecordValueType, String>.toRawList(): List<String> {
        return displayEntries().map {
            R.string.record_raw_data_format_type_value.string(it.key.displayName, it.value)
        }
    }

    private fun Map<RecordValueType, String>.toResultList(type: String): List<String> {
        return displayEntries().map {
            R.string.record_result_data_format_type2_value.string(
                type,
                it.key.displayName,
                it.value
            )
        }
    }

    private fun Map<RecordValueType, String>.toKeyList(): List<String> {
        return displayEntries().map {
            R.string.record_key_format.string(it.key.displayName, it.value)
        }
    }

    private fun Map<RecordValueType, String>.toIvList(): List<String> {
        return displayEntries().map {
            R.string.record_iv_format.string(it.key.displayName, it.value)

        }
    }

    private fun Map<RecordValueType, String>.displayEntries(): List<Map.Entry<RecordValueType, String>> {
        return entries.filterNot { entry ->
            (entry.key == RecordValueType.Base64 || entry.key == RecordValueType.Hex) &&
                    !entry.value.hasDisplayValue()
        }
    }

    private fun String?.hasDisplayValue(): Boolean {
        return !this.isNullOrBlank() && !this.equals("null", ignoreCase = true)
    }

    private fun RDItem.toCardPreview(maxLength: Int): RDItem {
        val source = fullContent
        if (source.length <= maxLength) {
            return copy(content = source, fullContent = source, isTruncated = false)
        }
        return copy(
            content = source.take(maxLength) + "...",
            fullContent = source,
            isTruncated = true
        )
    }

    private fun String.toJsonStringContent(): String {
        return Json.encodeToString(this).removePrefix("\"").removeSuffix("\"")
    }
}
