package ru.ibakaidov.distypepro.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import java.io.File
import kotlinx.coroutines.launch
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityDialogBinding
import ru.ibakaidov.distypepro.shared.SharedSdkProvider
import ru.ibakaidov.distypepro.shared.model.DialogChat
import ru.ibakaidov.distypepro.shared.model.DialogMessage

class DialogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDialogBinding
    private val sdk by lazy { SharedSdkProvider.get(this) }
    private val adapter = DialogMessageAdapter()

    private var chats: List<DialogChat> = emptyList()
    private var activeChatId: String? = null

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dialogToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.dialogToolbar.setNavigationOnClickListener { finish() }

        binding.messagesRecycler.layoutManager = LinearLayoutManager(this)
        binding.messagesRecycler.adapter = adapter

        binding.sendButton.setOnClickListener { sendMessage() }
        binding.recordButton.setOnClickListener { toggleRecording() }

        binding.chatSpinner.onItemSelectedListener = SimpleItemSelectedListener { position ->
            val chat = chats.getOrNull(position)
            if (chat?.id != null) {
                activeChatId = chat.id
                loadMessages(chat.id)
            }
        }

        loadChats()
    }

    private fun loadChats() {
        lifecycleScope.launch {
            val list = runCatching { sdk.dialogRepository.listChats() }.getOrElse { emptyList() }
            chats = if (list.isEmpty()) {
                listOf(runCatching { sdk.dialogRepository.createChat(null) }.getOrNull() ?: return@launch)
            } else {
                list
            }
            val labels = chats.map { it.title ?: getString(R.string.dialog_untitled) }
            val spinnerAdapter = ArrayAdapter(this@DialogActivity, android.R.layout.simple_spinner_item, labels)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.chatSpinner.adapter = spinnerAdapter
            activeChatId = chats.firstOrNull()?.id
            activeChatId?.let { loadMessages(it) }
        }
    }

    private fun loadMessages(chatId: String) {
        lifecycleScope.launch {
            val list = runCatching { sdk.dialogRepository.listMessages(chatId, 200, null) }.getOrElse { emptyList() }
            adapter.submit(list)
        }
    }

    private fun sendMessage() {
        val chatId = activeChatId ?: return
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        binding.messageInput.setText("")
        lifecycleScope.launch {
            val result = runCatching {
                sdk.dialogRepository.sendMessage(
                    chatId = chatId,
                    role = "disabled_person",
                    content = text,
                    source = "typed",
                    created = System.currentTimeMillis(),
                    includeSuggestions = true,
                )
            }.getOrNull()

            result?.let { adapter.add(it.message) }
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecordingAndSend()
        } else {
            if (ensureRecordPermission()) {
                startRecording()
            }
        }
    }

    private fun ensureRecordPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
        return granted
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        }
    }

    private fun startRecording() {
        val file = File(cacheDir, "dialog_record.m4a")
        runCatching {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.onSuccess {
            recordingFile = file
            isRecording = true
            Snackbar.make(binding.root, R.string.dialog_recording, Snackbar.LENGTH_SHORT).show()
        }.onFailure {
            recorder?.release()
            recorder = null
            Snackbar.make(binding.root, R.string.dialog_send_error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun stopRecordingAndSend() {
        val chatId = activeChatId ?: return
        val file = recordingFile ?: return
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        isRecording = false

        lifecycleScope.launch {
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@launch
            if (bytes.size > MAX_AUDIO_BYTES) {
                Snackbar.make(binding.root, R.string.dialog_audio_too_large, Snackbar.LENGTH_LONG).show()
                return@launch
            }
            runCatching {
                sdk.dialogRepository.sendAudioMessage(
                    chatId = chatId,
                    role = "disabled_person",
                    audioBytes = bytes,
                    mimeType = "audio/mp4",
                    filename = file.name,
                    created = System.currentTimeMillis(),
                    source = "audio",
                    includeSuggestions = true,
                )
            }.onFailure {
                Snackbar.make(binding.root, R.string.dialog_send_error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private companion object {
        private const val REQUEST_RECORD_AUDIO = 2001
        private const val MAX_AUDIO_BYTES = 8 * 1024 * 1024
    }
}
