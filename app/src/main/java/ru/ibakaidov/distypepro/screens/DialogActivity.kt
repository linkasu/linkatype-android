package ru.ibakaidov.distypepro.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ibakaidov.distypepro.R
import ru.ibakaidov.distypepro.databinding.ActivityDialogBinding
import ru.ibakaidov.distypepro.dialogs.ConfirmDialog
import ru.ibakaidov.distypepro.screens.dialog.DialogError
import ru.ibakaidov.distypepro.screens.dialog.DialogEvent
import ru.ibakaidov.distypepro.screens.dialog.DialogViewModel
import ru.ibakaidov.distypepro.shared.model.DialogChat
import ru.ibakaidov.distypepro.utils.Callback

@AndroidEntryPoint
class DialogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDialogBinding
    private val viewModel: DialogViewModel by viewModels()
    private val messageAdapter = DialogMessageAdapter()
    private val chatAdapter = DialogChatAdapter(
        onSelect = { chat -> viewModel.selectChat(chat) },
        onDelete = { chat -> confirmDeleteChat(chat) },
    )

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var recordingFile: File? = null
    private var isRecording = false

    override fun onDestroy() {
        super.onDestroy()
        recordingJob?.cancel()
        audioRecord?.let { record ->
            runCatching { record.stop() }
            record.release()
        }
        audioRecord = null
        recordingFile = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dialogToolbar.setNavigationOnClickListener {
            binding.dialogDrawer.openDrawer(GravityCompat.START)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (binding.dialogDrawer.isDrawerOpen(GravityCompat.START)) {
                binding.dialogDrawer.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        binding.messagesRecycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messagesRecycler.adapter = messageAdapter

        binding.chatList.layoutManager = LinearLayoutManager(this)
        binding.chatList.adapter = chatAdapter

        binding.sendButton.setOnClickListener { sendMessage() }
        binding.recordButton.setOnClickListener { toggleRecording() }
        binding.clearButton.setOnClickListener { clearInput() }
        binding.newChatButton.setOnClickListener { viewModel.createChat() }

        updateRecordingUi(false)
        applyWindowInsets()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        messageAdapter.submit(state.messages)
                        chatAdapter.submit(state.chats, state.activeChatId)
                        binding.emptyMessages.isVisible = state.isMessagesEmpty
                        binding.dialogToolbar.subtitle = state.activeChat?.title ?: getString(R.string.dialog_untitled)
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is DialogEvent.ScrollToBottom -> scrollToBottom()
                            is DialogEvent.CloseDrawer -> binding.dialogDrawer.closeDrawer(GravityCompat.START)
                            is DialogEvent.ClearInput -> binding.messageInput.setText("")
                            is DialogEvent.ShowError -> showError(event.error)
                        }
                    }
                }
            }
        }
    }

    private fun showError(error: DialogError) {
        val messageRes = when (error) {
            DialogError.SendFailed -> R.string.dialog_send_error
            DialogError.AudioTooLarge -> R.string.dialog_audio_too_large
        }
        Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
    }

    private fun applyWindowInsets() {
        val toolbarPaddingTop = binding.dialogToolbar.paddingTop
        val messagesPaddingLeft = binding.messagesRecycler.paddingLeft
        val messagesPaddingRight = binding.messagesRecycler.paddingRight
        val messagesPaddingBottom = binding.messagesRecycler.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.dialogToolbar) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = toolbarPaddingTop + statusBars.top)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.messagesRecycler) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = messagesPaddingLeft + systemBars.left,
                right = messagesPaddingRight + systemBars.right,
                bottom = messagesPaddingBottom,
            )
            insets
        }

        val inputLayoutParams = binding.inputCard.layoutParams as ViewGroup.MarginLayoutParams
        val inputMarginLeft = inputLayoutParams.leftMargin
        val inputMarginRight = inputLayoutParams.rightMargin
        val inputMarginBottom = inputLayoutParams.bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.inputCard) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(systemBars.bottom, ime.bottom)
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.leftMargin = inputMarginLeft + systemBars.left
            params.rightMargin = inputMarginRight + systemBars.right
            params.bottomMargin = inputMarginBottom + bottomInset
            view.layoutParams = params
            insets
        }

        val drawerPaddingTop = binding.dialogDrawerContent.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.dialogDrawerContent) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = drawerPaddingTop + statusBars.top)
            insets
        }

        ViewCompat.requestApplyInsets(binding.dialogRoot)
    }

    private fun confirmDeleteChat(chat: DialogChat) {
        ConfirmDialog.showConfirmDialog(this, R.string.dialog_delete_chat_confirm, object : Callback<Unit> {
            override fun onDone(result: Unit) {
                viewModel.deleteChat(chat)
            }
        })
    }

    private fun sendMessage() {
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        viewModel.sendTextMessage(text)
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
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
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
        val bufferSize = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Snackbar.make(binding.root, R.string.dialog_send_error, Snackbar.LENGTH_LONG).show()
            return
        }
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            Snackbar.make(binding.root, R.string.dialog_send_error, Snackbar.LENGTH_LONG).show()
            return
        }

        val file = File(cacheDir, "dialog_record.wav")
        audioRecord = record
        recordingFile = file
        isRecording = true
        updateRecordingUi(true)

        recordingJob = lifecycleScope.launch(Dispatchers.IO) {
            var totalBytes = 0
            val buffer = ByteArray(bufferSize)
            try {
                FileOutputStream(file).use { out ->
                    out.write(buildWavHeader(0, AUDIO_SAMPLE_RATE, AUDIO_CHANNELS, AUDIO_BITS_PER_SAMPLE))
                    if (!isRecording) return@launch
                    record.startRecording()
                    while (isRecording && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            out.write(buffer, 0, read)
                            totalBytes += read
                        } else if (read < 0) {
                            break
                        }
                    }
                }
                if (totalBytes > 0) {
                    updateWavHeader(file, totalBytes, AUDIO_SAMPLE_RATE, AUDIO_CHANNELS, AUDIO_BITS_PER_SAMPLE)
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, R.string.dialog_send_error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun stopRecordingAndSend() {
        val file = recordingFile ?: return

        val record = audioRecord
        audioRecord = null
        isRecording = false
        updateRecordingUi(false)
        runCatching { record?.stop() }

        lifecycleScope.launch {
            recordingJob?.join()
            record?.release()
            recordingJob = null
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@launch
            viewModel.sendAudioMessage(bytes, file.name)
        }
    }

    private fun updateRecordingUi(recording: Boolean) {
        binding.dialogStatus.isVisible = recording
        if (recording) {
            binding.dialogStatus.setText(R.string.dialog_recording)
            binding.recordButton.setText(R.string.dialog_stop_record)
            binding.recordButton.setIconResource(R.drawable.ic_baseline_stop_24)
            binding.recordButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorError))
            binding.recordButton.setTextColor(ContextCompat.getColor(this, R.color.colorOnError))
            binding.recordButton.iconTint = ContextCompat.getColorStateList(this, R.color.colorOnError)
        } else {
            binding.recordButton.setText(R.string.dialog_record)
            binding.recordButton.setIconResource(R.drawable.ic_baseline_mic_24)
            binding.recordButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorSecondary))
            binding.recordButton.setTextColor(ContextCompat.getColor(this, R.color.colorOnSecondary))
            binding.recordButton.iconTint = ContextCompat.getColorStateList(this, R.color.colorOnSecondary)
        }
    }

    private fun clearInput() {
        binding.messageInput.setText("")
    }

    private fun scrollToBottom() {
        val count = messageAdapter.itemCount
        if (count > 0) {
            binding.messagesRecycler.scrollToPosition(count - 1)
        }
    }

    private fun buildWavHeader(
        dataLength: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = dataLength + 36
        return ByteArray(WAV_HEADER_SIZE).apply {
            this[0] = 'R'.code.toByte()
            this[1] = 'I'.code.toByte()
            this[2] = 'F'.code.toByte()
            this[3] = 'F'.code.toByte()
            writeInt32LE(this, 4, totalDataLen)
            this[8] = 'W'.code.toByte()
            this[9] = 'A'.code.toByte()
            this[10] = 'V'.code.toByte()
            this[11] = 'E'.code.toByte()
            this[12] = 'f'.code.toByte()
            this[13] = 'm'.code.toByte()
            this[14] = 't'.code.toByte()
            this[15] = ' '.code.toByte()
            writeInt32LE(this, 16, 16)
            writeInt16LE(this, 20, 1)
            writeInt16LE(this, 22, channels.toShort())
            writeInt32LE(this, 24, sampleRate)
            writeInt32LE(this, 28, byteRate)
            writeInt16LE(this, 32, (channels * bitsPerSample / 8).toShort())
            writeInt16LE(this, 34, bitsPerSample.toShort())
            this[36] = 'd'.code.toByte()
            this[37] = 'a'.code.toByte()
            this[38] = 't'.code.toByte()
            this[39] = 'a'.code.toByte()
            writeInt32LE(this, 40, dataLength)
        }
    }

    private fun updateWavHeader(
        file: File,
        dataLength: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ) {
        runCatching {
            RandomAccessFile(file, "rw").use { raf ->
                val header = buildWavHeader(dataLength, sampleRate, channels, bitsPerSample)
                raf.seek(0)
                raf.write(header)
            }
        }
    }

    private fun writeInt16LE(target: ByteArray, offset: Int, value: Short) {
        target[offset] = (value.toInt() and 0xff).toByte()
        target[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }

    private fun writeInt32LE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xff).toByte()
        target[offset + 1] = ((value shr 8) and 0xff).toByte()
        target[offset + 2] = ((value shr 16) and 0xff).toByte()
        target[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private companion object {
        private const val REQUEST_RECORD_AUDIO = 2001
        private const val AUDIO_SAMPLE_RATE = 16_000
        private const val AUDIO_CHANNELS = 1
        private const val AUDIO_BITS_PER_SAMPLE = 16
        private const val WAV_HEADER_SIZE = 44
    }
}
