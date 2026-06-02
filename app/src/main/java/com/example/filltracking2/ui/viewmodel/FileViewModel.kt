package com.example.filltracking2.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filltracking2.data.FileRecord
import com.example.filltracking2.data.FileRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRecordRepository(application)
    val records: StateFlow<List<FileRecord>> = FileRecordRepository.recordsFlow
    val savedSources: StateFlow<List<com.example.filltracking2.data.SavedSource>> = FileRecordRepository.sourcesFlow
    
    val savedSourcesFromRoom: StateFlow<List<com.example.filltracking2.data.SavedSourceEntity>> = 
        repository.savedSourceDao.getAllSources().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Error state for UI to observe
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    // Image Viewer state
    private val _viewerImages = MutableStateFlow<List<String>>(emptyList())
    val viewerImages = _viewerImages.asStateFlow()

    private val _viewerInitialIndex = MutableStateFlow(0)
    val viewerInitialIndex = _viewerInitialIndex.asStateFlow()

    fun openImageViewer(images: List<String>, index: Int) {
        _viewerImages.value = images
        _viewerInitialIndex.value = index
    }

    // Export state
    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class Success(val uri: Uri) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun exportToExcel(uri: Uri) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            val success = withContext(Dispatchers.IO) {
                com.example.filltracking2.util.ExcelExporter.exportToExcel(
                    getApplication(),
                    uri,
                    records.value
                )
            }
            if (success) {
                _exportState.value = ExportState.Success(uri)
            } else {
                _exportState.value = ExportState.Error("فشل تصدير البيانات")
            }
        }
    }

    init {
        loadRecords()
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = repository.loadSources()
            if (saved.isEmpty()) {
                val mockSources = listOf(
                    "وزارة التربية الوطنية",
                    "الأكاديمية الجهوية",
                    "المديرية الإقليمية",
                    "عمالة الإقليم",
                    "رئاسة الحكومة",
                    "وزارة المالية",
                    "الخزينة العامة للمملكة",
                    "المندوبية السامية للتخطيط",
                    "المجلس الأعلى للتعليم",
                    "مؤسسة محمد السادس"
                ).map { name ->
                    com.example.filltracking2.data.SavedSource(
                        sourceName = name,
                        lastUsedAt = System.currentTimeMillis(),
                        useCount = 1
                    )
                }
                mockSources.forEach { repository.saveSource(it) }
            }
        }
    }

    fun handleSourceSaving(sourceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingSources = repository.loadSources()
            val existing = existingSources.find { it.sourceName.equals(sourceName, ignoreCase = true) }
            
            if (existing != null) {
                repository.saveSource(existing.copy(
                    lastUsedAt = System.currentTimeMillis(),
                    useCount = existing.useCount + 1
                ))
            } else {
                repository.saveSource(com.example.filltracking2.data.SavedSource(
                    sourceName = sourceName,
                    lastUsedAt = System.currentTimeMillis(),
                    useCount = 1
                ))
            }
        }
    }

    fun handleSourceSavingToRoom(sourceName: String) {
        if (sourceName.length < 2) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.savedSourceDao.insertOrUpdate(sourceName)
        }
    }

    /**
     * Load records from persistent storage.
     * Prevents data loss by NOT overwriting if load fails partially.
     */
    private fun loadRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saved = repository.loadRecords()
                if (saved.isEmpty()) {
                    // Seed 30 mock records to demonstrate all features
                    val mockData = listOf(
                        FileRecord(
                            id = "mock_1",
                            subject = "مراجعة الميزانية السنوية",
                            sectors = listOf("Finance"),
                            urgency = "Urgent",
                            status = "Processed",
                            dateRegistered = "2026-04-01",
                            dateReceivedGov = "2026-04-01",
                            dateDeliveredToDomain = "2026-04-01",
                            source = "الأكاديمية",
                            originalSerial = "27/1001",
                            internalSerial = "27/0001",
                            recipientName = "محمد أمين",
                            attachments = listOf(com.example.filltracking2.data.Attachment("budget.pdf", "application/pdf", 1024, "mock_path_1"))
                        ),
                        FileRecord(
                            id = "mock_2",
                            subject = "تقرير التخطيط الفصلي",
                            sectors = listOf("Planning"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-04-02",
                            dateReceivedGov = "2026-04-02",
                            dateDeliveredToDomain = "2026-04-02",
                            source = "المديرية",
                            originalSerial = "27/1002",
                            internalSerial = "27/0002",
                            recipientName = "فاطمة الزهراء"
                        ),
                        FileRecord(
                            id = "mock_3",
                            subject = "عقد توريد معدات",
                            sectors = listOf("Legal Affairs"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-04-03",
                            dateReceivedGov = "2026-04-03",
                            dateDeliveredToDomain = "2026-04-03",
                            source = "الوزارة",
                            originalSerial = "27/1003",
                            internalSerial = "27/0003",
                            recipientName = "يوسف بنعلي"
                        ),
                        FileRecord(
                            id = "mock_4",
                            subject = "طلب توظيف إداري",
                            sectors = listOf("HR Management"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-04-05",
                            dateReceivedGov = "2026-04-05",
                            dateDeliveredToDomain = "2026-04-05",
                            source = "الأكاديمية",
                            originalSerial = "27/1004",
                            internalSerial = "27/0004",
                            recipientName = "سلمى العمراني"
                        ),
                        FileRecord(
                            id = "mock_5",
                            subject = "صيانة الشبكة الداخلية",
                            sectors = listOf("Technical"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-04-07",
                            dateReceivedGov = "2026-04-07",
                            dateDeliveredToDomain = "2026-04-07",
                            source = "المديرية",
                            originalSerial = "27/1005",
                            internalSerial = "27/0005",
                            recipientName = "رشيد التازي"
                        ),
                        FileRecord(
                            id = "mock_6",
                            subject = "تقرير الامتحانات الجهوية",
                            sectors = listOf("Educational Affairs"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-04-08",
                            dateReceivedGov = "2026-04-08",
                            dateDeliveredToDomain = "2026-04-08",
                            source = "الوزارة",
                            originalSerial = "27/1006",
                            internalSerial = "27/0006",
                            recipientName = "نادية بوشتى",
                            attachments = listOf(com.example.filltracking2.data.Attachment("exams.jpg", "image/jpeg", 2048, "mock_path_6"))
                        ),
                        FileRecord(
                            id = "mock_7",
                            subject = "مراسلة أمنية طارئة",
                            sectors = listOf("Security"),
                            urgency = "Urgent",
                            status = "Processed",
                            dateRegistered = "2026-04-10",
                            dateReceivedGov = "2026-04-10",
                            dateDeliveredToDomain = "2026-04-10",
                            source = "العمالة",
                            originalSerial = "27/1007",
                            internalSerial = "27/0007",
                            recipientName = "كريم الإدريسي"
                        ),
                        FileRecord(
                            id = "mock_8",
                            subject = "تحديث السجلات الإدارية",
                            sectors = listOf("Admin"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-04-12",
                            dateReceivedGov = "2026-04-12",
                            dateDeliveredToDomain = "2026-04-12",
                            source = "الأكاديمية",
                            originalSerial = "27/1008",
                            internalSerial = "27/0008",
                            recipientName = "لطيفة موساوي"
                        ),
                        FileRecord(
                            id = "mock_9",
                            subject = "دورة تكوينية للأساتذة",
                            sectors = listOf("Educational Affairs"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-04-14",
                            dateReceivedGov = "2026-04-14",
                            dateDeliveredToDomain = "2026-04-14",
                            source = "المديرية",
                            originalSerial = "27/1009",
                            internalSerial = "27/0009",
                            recipientName = "محمد الغازي"
                        ),
                        FileRecord(
                            id = "mock_10",
                            subject = "فاتورة مستلزمات مكتبية",
                            sectors = listOf("Finance"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-04-15",
                            dateReceivedGov = "2026-04-15",
                            dateDeliveredToDomain = "2026-04-15",
                            source = "الوزارة",
                            originalSerial = "27/1010",
                            internalSerial = "27/0010",
                            recipientName = "أسماء قاسمي",
                            attachments = listOf(com.example.filltracking2.data.Attachment("invoice.png", "image/png", 512, "mock_path_10"))
                        ),
                        FileRecord(
                            id = "mock_11",
                            subject = "مراجعة عقود الموردين",
                            sectors = listOf("Legal Affairs"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-04-17",
                            dateReceivedGov = "2026-04-17",
                            dateDeliveredToDomain = "2026-04-17",
                            source = "العمالة",
                            originalSerial = "27/1011",
                            internalSerial = "27/0011",
                            recipientName = "هشام الناصري"
                        ),
                        FileRecord(
                            id = "mock_12",
                            subject = "خطة التنمية المؤسسية",
                            sectors = listOf("Planning"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-04-20",
                            dateReceivedGov = "2026-04-20",
                            dateDeliveredToDomain = "2026-04-20",
                            source = "الأكاديمية",
                            originalSerial = "27/1012",
                            internalSerial = "27/0012",
                            recipientName = "زينب بلقاسم"
                        ),
                        FileRecord(
                            id = "mock_13",
                            subject = "تقرير عمليات الصيانة",
                            sectors = listOf("Operations"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-04-22",
                            dateReceivedGov = "2026-04-22",
                            dateDeliveredToDomain = "2026-04-22",
                            source = "المديرية",
                            originalSerial = "27/1013",
                            internalSerial = "27/0013",
                            recipientName = "عمر الفاسي"
                        ),
                        FileRecord(
                            id = "mock_14",
                            subject = "تقرير إحصائي عام",
                            sectors = listOf("General"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-04-25",
                            dateReceivedGov = "2026-04-25",
                            dateDeliveredToDomain = "2026-04-25",
                            source = "الوزارة",
                            originalSerial = "27/1014",
                            internalSerial = "27/0014",
                            recipientName = "حنان المرابط"
                        ),
                        FileRecord(
                            id = "mock_15",
                            subject = "طلب ترقية موظف",
                            sectors = listOf("HR Management"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-04-28",
                            dateReceivedGov = "2026-04-28",
                            dateDeliveredToDomain = "2026-04-28",
                            source = "العمالة",
                            originalSerial = "27/1015",
                            internalSerial = "27/0015",
                            recipientName = "سعيد بنيوسف"
                        ),
                        FileRecord(
                            id = "mock_16",
                            subject = "مذكرة وزارية بخصوص التعليم",
                            sectors = listOf("Educational Affairs"),
                            urgency = "Urgent",
                            status = "Processed",
                            dateRegistered = "2026-05-01",
                            dateReceivedGov = "2026-05-01",
                            dateDeliveredToDomain = "2026-05-02",
                            source = "الوزارة",
                            originalSerial = "27/1016",
                            internalSerial = "27/0016",
                            recipientName = "يوسف العالمي"
                        ),
                        FileRecord(
                            id = "mock_17",
                            subject = "توزيع الكتب المدرسية الجديدة",
                            sectors = listOf("Planning"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-05-02",
                            dateReceivedGov = "2026-05-02",
                            dateDeliveredToDomain = "2026-05-03",
                            source = "الأكاديمية",
                            originalSerial = "27/1017",
                            internalSerial = "27/0017",
                            recipientName = "مريم بناني"
                        ),
                        FileRecord(
                            id = "mock_18",
                            subject = "إصلاح المرافق الصحية",
                            sectors = listOf("Buildings"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-05-03",
                            dateReceivedGov = "2026-05-03",
                            dateDeliveredToDomain = "2026-05-04",
                            source = "المديرية",
                            originalSerial = "27/1018",
                            internalSerial = "27/0018",
                            recipientName = "خالد مرزوق"
                        ),
                        FileRecord(
                            id = "mock_19",
                            subject = "ملف منحة التعليم الثانوي",
                            sectors = listOf("Finance"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-05-04",
                            dateReceivedGov = "2026-05-04",
                            dateDeliveredToDomain = "2026-05-05",
                            source = "الوزارة",
                            originalSerial = "27/1019",
                            internalSerial = "27/0019",
                            recipientName = "ليلى الفهري"
                        ),
                        FileRecord(
                            id = "mock_20",
                            subject = "تكوين مستمر في الرقمنة",
                            sectors = listOf("HR Management"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-05-05",
                            dateReceivedGov = "2026-05-05",
                            dateDeliveredToDomain = "2026-05-06",
                            source = "الأكاديمية",
                            originalSerial = "27/1020",
                            internalSerial = "27/0020",
                            recipientName = "أمين القادري"
                        ),
                        FileRecord(
                            id = "mock_21",
                            subject = "مراسلة واردة من عمالة الإقليم",
                            sectors = listOf("Mail Writing"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-05-06",
                            dateReceivedGov = "2026-05-06",
                            dateDeliveredToDomain = "2026-05-07",
                            source = "العمالة",
                            originalSerial = "27/1021",
                            internalSerial = "27/0021",
                            recipientName = "نور الدين"
                        ),
                        FileRecord(
                            id = "mock_22",
                            subject = "تحديث بوابة المسار",
                            sectors = listOf("Information System"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-05-07",
                            dateReceivedGov = "2026-05-07",
                            dateDeliveredToDomain = "2026-05-08",
                            source = "الوزارة",
                            originalSerial = "27/1022",
                            internalSerial = "27/0022",
                            recipientName = "عادل الصقلي"
                        ),
                        FileRecord(
                            id = "mock_23",
                            subject = "محضر مداولات الامتحانات",
                            sectors = listOf("Exams"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-05-08",
                            dateReceivedGov = "2026-05-08",
                            dateDeliveredToDomain = "2026-05-09",
                            source = "المديرية",
                            originalSerial = "27/1023",
                            internalSerial = "27/0023",
                            recipientName = "سارة التازي"
                        ),
                        FileRecord(
                            id = "mock_24",
                            subject = "اتفاقية شراكة مع جمعية",
                            sectors = listOf("Legal Affairs"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-05-09",
                            dateReceivedGov = "2026-05-09",
                            dateDeliveredToDomain = "2026-05-10",
                            source = "الوزارة",
                            originalSerial = "27/1024",
                            internalSerial = "27/0024",
                            recipientName = "ياسين بنسودة"
                        ),
                        FileRecord(
                            id = "mock_25",
                            subject = "تقرير تفتيش دوري",
                            sectors = listOf("Inspection"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-05-10",
                            dateReceivedGov = "2026-05-10",
                            dateDeliveredToDomain = "2026-05-11",
                            source = "المفتشية",
                            originalSerial = "27/1025",
                            internalSerial = "27/0025",
                            recipientName = "جمال الإدريسي"
                        ),
                        FileRecord(
                            id = "mock_26",
                            subject = "تركيب كاميرات مراقبة",
                            sectors = listOf("Security"),
                            urgency = "Urgent",
                            status = "Received",
                            dateRegistered = "2026-05-11",
                            dateReceivedGov = "2026-05-11",
                            dateDeliveredToDomain = "2026-05-12",
                            source = "العمالة",
                            originalSerial = "27/1026",
                            internalSerial = "27/0026",
                            recipientName = "مراد الصبري"
                        ),
                        FileRecord(
                            id = "mock_27",
                            subject = "جرد الأرشيف السنوي",
                            sectors = listOf("Admin"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-05-12",
                            dateReceivedGov = "2026-05-12",
                            dateDeliveredToDomain = "2026-05-13",
                            source = "المديرية",
                            originalSerial = "27/1027",
                            internalSerial = "27/0027",
                            recipientName = "سناء الشافعي"
                        ),
                        FileRecord(
                            id = "mock_28",
                            subject = "طلب قطع غيار للمكيفات",
                            sectors = listOf("Technical"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-05-13",
                            dateReceivedGov = "2026-05-13",
                            dateDeliveredToDomain = "2026-05-14",
                            source = "الأكاديمية",
                            originalSerial = "27/1028",
                            internalSerial = "27/0028",
                            recipientName = "فؤاد الرازي"
                        ),
                        FileRecord(
                            id = "mock_29",
                            subject = "تموين المطعم المدرسي",
                            sectors = listOf("Operations"),
                            urgency = "Normal",
                            status = "Received",
                            dateRegistered = "2026-05-14",
                            dateReceivedGov = "2026-05-14",
                            dateDeliveredToDomain = "2026-05-15",
                            source = "الوزارة",
                            originalSerial = "27/1029",
                            internalSerial = "27/0029",
                            recipientName = "دنيا العمراني"
                        ),
                        FileRecord(
                            id = "mock_30",
                            subject = "التقرير الإحصائي العام",
                            sectors = listOf("General"),
                            urgency = "Normal",
                            status = "Processed",
                            dateRegistered = "2026-05-15",
                            dateReceivedGov = "2026-05-15",
                            dateDeliveredToDomain = "2026-05-16",
                            source = "المديرية",
                            originalSerial = "27/1030",
                            internalSerial = "27/0030",
                            recipientName = "حمزة الوزاني"
                        )
                    )
                    FileRecordRepository.updateRecords(mockData)
                    repository.saveRecords(mockData)
                } else {
                    FileRecordRepository.updateRecords(saved)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _saveError.value = "Error loading history: ${e.localizedMessage}"
                }
            }
        }
    }

    /**
     * Refresh records from the in-memory repository.
     * (Deprecated: recordsFlow is now automatic)
     */
    fun refreshRecords() {
        // No longer needed but kept for compatibility
    }

    /**
     * Add a new record and persist immediately.
     * Persistence happens on a background thread to prevent UI lag or crash.
     */
    fun addRecord(record: FileRecord) {
        // Update in-memory repository (and UI via Flow)
        FileRecordRepository.addRecordAtStart(record)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save ONLY the new record file (efficient storage like IndexedDB)
                val success = repository.saveRecord(record)
                if (!success) {
                    withContext(Dispatchers.Main) {
                        _saveError.value = "Storage is full or inaccessible. Document saved in memory only."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _saveError.value = "Critical save error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun updateRecord(record: FileRecord) {
        val previousRecord = records.value.find { it.id == record.id }
        FileRecordRepository.updateRecordInList(record)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveRecord(record)
                val currentAttachmentPaths = record.attachments.map { it.path }.toSet()
                previousRecord?.attachments
                    ?.asSequence()
                    ?.map { it.path }
                    ?.filterNot { it in currentAttachmentPaths }
                    ?.forEach { com.example.filltracking2.data.AttachmentStorage.deleteAttachment(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRecord(record: FileRecord) {
        FileRecordRepository.removeRecordFromList(record.id)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete JSON record
                repository.deleteRecord(record.id)
                // Delete associated attachment files
                record.attachments.forEach { attachment ->
                    com.example.filltracking2.data.AttachmentStorage.deleteAttachment(attachment.path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun wipeAllData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Clear in-memory list
                FileRecordRepository.updateRecords(emptyList())

                // 2. Delete all JSON files and attachments
                repository.wipeAllData()

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getRecordBySerial(serial: String): FileRecord? {
        return records.value.find { it.internalSerial == serial || it.originalSerial == serial }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    companion object {
        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
            return sdf.format(Date())
        }
    }
}
