package me.simpleHook.viewmodel

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
import me.simpleHook.data.RecordPart
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
import me.simpleHook.data.record.RecordValueType
import me.simpleHook.data.record.RecordWebLoadUrl
import me.simpleHook.database.RecordDatabase
import me.simpleHook.database.entity.RecordEntity

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val recordDao = RecordDatabase.getDatabase(application).recordDao()

    private var _filterRecordPartPT = MutableLiveData<List<RecordPart>>()
    val filterRecordPartPT: LiveData<List<RecordPart>> get() = _filterRecordPartPT
    val queryPattern = MutableStateFlow("")

    private val _recordDetail = MutableStateFlow(emptyList<String>())
    val recordDetail = _recordDetail

    private val pagingConfig =
        PagingConfig(pageSize = 30, prefetchDistance = 3, enablePlaceholders = true, maxSize = 200)


    fun getRecordEntity(
        typeOrPack: String, isType: Boolean
    ): Flow<PagingData<RecordEntity>> {
        return Pager(config = pagingConfig) {
            if (isType) {
                recordDao.getRecordByType("%$typeOrPack%", "%${queryPattern.value}%")
            } else {
                recordDao.getRecordByPack("%$typeOrPack%", "%${queryPattern.value}%")
            }
        }.flow.cachedIn(viewModelScope)
    }


    fun getMarkedRecordByType(type: String) = recordDao.getMarkedRecordByType("%$type%")

    fun getMarkedRecordByPack(packageName: String) = recordDao.getMarkedRecordByPack(packageName)

    fun getRecordByID(id: Int) = recordDao.getRecordById(id)

    fun getAllRecord() = viewModelScope.launch(Dispatchers.IO) {
        _filterRecordPartPT.postValue(recordDao.getAllRecordPart())
    }


    fun updateRecord(recordEntity: RecordEntity) = viewModelScope.launch(Dispatchers.IO) {
        recordDao.updateRecords(recordEntity)
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
        recordDao.deleteRecordByType("%$type%")
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
            recordDao.deleteReadRecordByType(read, "%$type%")
        }

    fun deleteMarkedRecordByPack(isMark: Boolean, packageName: String) = viewModelScope.launch(
        Dispatchers.IO
    ) {
        recordDao.deleteMarkedRecordByPack(isMark, packageName)
    }

    fun deleteMarkedRecordByType(isMark: Boolean, type: String) = viewModelScope.launch(
        Dispatchers.IO
    ) {
        recordDao.deleteMarkedRecordByType(isMark, "%$type%")
    }


    private fun Int.string() = application.getString(this)
    private fun Int.string(vararg formatArgs: Any) = application.getString(this, *formatArgs)

    fun fetchRecordDetail(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        val recordEntity = getRecordByID(id)
        val record = Json.decodeFromString<Record>(recordEntity.record)
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
                    addAll(record.result.toResultList(type = type))
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
                    add(R.string.record_id_format.string(record.viewId ?: "NO_ID"))
                    addAll(record.textList.map { R.string.record_text_format.string(it) })
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordClipboard -> {
                    add(R.string.record_type_format.string(R.string.record_type_clipboard.string()))
                    val typeID = R.string.record_read_clipboard.takeIf { record.isRead }
                        ?: R.string.record_write_clipboard
                    add(R.string.record_clipboard_read_or_write_format.string(typeID.string()))
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
                        add(R.string.record_error_config_info.string())
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
                    add(R.string.record_type_format.string(R.string.record_type_field.string()))
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

                is RecordHmac -> {
                    add(R.string.record_type_format.string(record.algorithm))
                    addAll(record.rawData.toRawList())
                    addAll(record.resultData.toResultList(""))
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordJson -> {
                    add(R.string.record_type_format.string(record.jsonType.displayId.string()))
                    addAll(record.values.map { "name: ${it.key}, value: ${it.value}" })
                    add(R.string.record_call_stack.string())
                    addAll(record.stackDetail.split("\n"))
                }

                is RecordMac -> {
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
                    add(R.string.record_type_format.string(R.string.record_type_return_value.string()))
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
                    add("Headers: ")
                    addAll(record.headers.map { "name: ${it.key}, value: ${it.value}" })
                }

                is RecordIntent -> {
                    add(R.string.record_type_format.string("Intent"))
                    add("Package name: ${record.packageName}")
                    add("Class name: ${record.className}")
                    add("Action: ${record.action}")
                    add("Data: ${record.data}")
                    record.extras.forEach {
                        add("Type: ${it.intentType}")
                        add("Key: ${it.key}")
                        it.value.forEach { mapEntry ->
                            when (mapEntry.key) {
                                RecordValueType.ToString -> {
                                    add("Value(toString): ${mapEntry.value}")
                                }

                                RecordValueType.GsonToString -> {
                                    add("Value(Gson): ${mapEntry.value}")
                                }

                                else -> Unit
                            }
                        }
                        add("=============================")
                    }
                }
            }
        }

        _recordDetail.value = items

    }

    private fun Map<RecordValueType, String>.toRawList(): List<String> {
        return map {
            when (it.key) {
                RecordValueType.BytesToString ->
                    R.string.record_raw_data_str_format.string(it.value)

                RecordValueType.Base64 ->
                    R.string.record_raw_data_base64_format.string(it.value)

                RecordValueType.Hex ->
                    R.string.record_raw_data_hex_format.string(it.value)

                else -> ""
            }
        }
    }

    private fun Map<RecordValueType, String>.toResultList(type: String): List<String> {
        return map {
            when (it.key) {
                RecordValueType.BytesToString ->
                    R.string.record_result_data_str_format.string(type, it.value)

                RecordValueType.Base64 ->
                    R.string.record_result_data_base64_format.string(type, it.value)

                RecordValueType.Hex ->
                    R.string.record_result_data_hex_format.string(type, it.value)

                else -> ""
            }
        }
    }

    private fun Map<RecordValueType, String>.toKeyList(): List<String> {
        return map {
            when (it.key) {
                RecordValueType.BytesToString ->
                    R.string.record_key_str_format.string(it.value)

                RecordValueType.Base64 ->
                    R.string.record_key_base64_format.string(it.value)

                RecordValueType.Hex ->
                    R.string.record_key_hex_format.string(it.value)

                else -> ""
            }
        }
    }

    private fun Map<RecordValueType, String>.toIvList(): List<String> {
        return map {
            when (it.key) {
                RecordValueType.BytesToString ->
                    R.string.record_iv_str_format.string(it.value)

                RecordValueType.Base64 ->
                    R.string.record_iv_base64_format.string(it.value)

                RecordValueType.Hex ->
                    R.string.record_iv_hex_format.string(it.value)

                else -> ""
            }
        }
    }
}