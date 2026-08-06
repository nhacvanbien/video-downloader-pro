import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vimalcvs.materialrating.DialogManager
import com.vimalcvs.materialrating.FeedbackMediaAdapter
import com.vimalcvs.materialrating.R
import kotlin.collections.filter
import kotlin.collections.isNotEmpty

class MaterialFeedback(var email: String?) : BottomSheetDialogFragment() {
    private var theDialogView: View? = null
    private var rating: Float = 0f
    private var deviceInfo: String? = null

    private lateinit var checkBox1: AppCompatCheckBox
    private lateinit var checkBox2: AppCompatCheckBox
    private lateinit var checkBox3: AppCompatCheckBox
    private lateinit var checkBox4: AppCompatCheckBox
    private lateinit var checkBox5: AppCompatCheckBox
    private lateinit var checkBox6: AppCompatCheckBox
    private lateinit var etEmail: AppCompatEditText

    private lateinit var mediaAdapter: FeedbackMediaAdapter
    private lateinit var rcvMedia: RecyclerView
    private lateinit var layoutEmpty: LinearLayoutCompat
    private lateinit var buttonAddImage: AppCompatImageView

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                val filteredUris = uris.filter { uri ->
                    val mimeType = requireActivity().contentResolver.getType(uri)
                    mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
                }
                filteredUris.take(5 - mediaAdapter.itemCount).forEach { uri ->
                    mediaAdapter.addMedia(uri)
                }
                updateMediaVisibility()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val behavior =
                BottomSheetBehavior.from(it.findViewById(com.google.android.material.R.id.design_bottom_sheet))
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun getView(): View? = theDialogView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.lib_material_fragment_feedback, container, false)
        if (savedInstanceState != null) {
            rating = savedInstanceState.getFloat(RATING_KEY)
        }

        checkBox1 = view.findViewById(R.id.checkbox_reason1)
        checkBox2 = view.findViewById(R.id.checkbox_reason2)
        checkBox3 = view.findViewById(R.id.checkbox_reason3)
        checkBox4 = view.findViewById(R.id.checkbox_reason4)
        checkBox5 = view.findViewById(R.id.checkbox_reason5)
        checkBox6 = view.findViewById(R.id.checkbox_reason6)

        val btMaybeLater = view.findViewById<AppCompatButton>(R.id.bt_maybeLater)
        btMaybeLater.setOnClickListener { dismiss() }

        deviceInfo = buildDeviceInfo()
        val etFeedback = view.findViewById<AppCompatEditText>(R.id.et_feedback)
        val btFeedbackSend = view.findViewById<AppCompatButton>(R.id.bt_feedbackSend)

        btFeedbackSend.setOnClickListener {
            val feedback = etFeedback.text?.toString().orEmpty()
            sendFeedbackEmail(feedback)
            DialogManager.showFeedbackAppreciate(requireActivity())
            dismiss()
        }

        setupMediaSelection(view)
        theDialogView = view
        return view
    }

    private fun setupMediaSelection(view: View) {
        rcvMedia = view.findViewById(R.id.rcv_media)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        buttonAddImage = view.findViewById(R.id.button_add_image)

        mediaAdapter = FeedbackMediaAdapter { position ->
            mediaAdapter.removeMedia(position)
            updateMediaVisibility()
        }
        rcvMedia.adapter = mediaAdapter

        buttonAddImage.setOnClickListener {
            getContent.launch("*/*")
        }
    }

    private fun updateMediaVisibility() {
        if (mediaAdapter.itemCount > 0) {
            layoutEmpty.visibility = View.GONE
            rcvMedia.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.VISIBLE
            rcvMedia.visibility = View.GONE
        }
    }

    private fun buildDeviceInfo(): String = """
        Device Info:
        OS Version: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})
        OS API Level: ${Build.VERSION.SDK_INT}
        Device: ${Build.DEVICE}
        Model (and Product): ${Build.MODEL} (${Build.PRODUCT})
    """.trimIndent()

    private fun sendFeedbackEmail(feedback: String) {
        val appName =
            requireActivity().applicationInfo.loadLabel(requireActivity().packageManager).toString()
        var subject = "$appName - "

        // ✅ Gom các checkbox được chọn
        val selectedReasons = listOf(
            checkBox1, checkBox2, checkBox3, checkBox4, checkBox5, checkBox6
        ).filter { it.isChecked }.joinToString(" ") { it.text.toString() }

        if (selectedReasons.isNotBlank()) {
            subject += selectedReasons
        }

        val ratingText = if (rating > 0.0f) "Stars: $rating\n\n" else ""

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "$ratingText Feedback: $feedback\n\n$deviceInfo")
            val mediaUris = ArrayList<Uri>(mediaAdapter.getMediaList())
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, mediaUris)
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Send email..."))
        } catch (e: Exception) {
            if (requireActivity().packageManager.resolveActivity(shareIntent, 0) != null) {
                startActivity(shareIntent)
            }
        }
    }

    fun setRating(rating: Float) {
        this.rating = rating
    }

    companion object {
        private const val RATING_KEY = "rating"
    }
}
