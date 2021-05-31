package com.hera.bangkit.ui.main.post.report

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.hera.bangkit.R
import com.hera.bangkit.data.response.ReportEntity
import com.hera.bangkit.databinding.ActivityReportBinding
import com.hera.bangkit.tflite.Classifier
import com.hera.bangkit.ui.main.MainActivity
import com.hera.bangkit.utils.DateHelper
import com.hera.bangkit.utils.DummyUser
import com.share424.sastrawi.Stemmer.StemmerFactory
import com.share424.sastrawi.StopWordRemover.StopWordRemoverFactory
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

@AndroidEntryPoint
class PostReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val viewModel: ReportViewModel by viewModels()

    private val modelAssetPath = "modellaporan.tflite"

    private var tfLiteInterpreter: Interpreter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }


        //Tensorflow
        val stemmer = StemmerFactory(applicationContext).create()
        val stopwordRemover = StopWordRemoverFactory(applicationContext).create()
        val classifier = Classifier(this, "word_dict_laporan.json")
        val options = Interpreter.Options()
        tfLiteInterpreter = Interpreter(loadModelFile(), options)
        classifier.processVocab()

        with(binding) {
            btnPost.setOnClickListener {

                val desc = etDesc.editText?.text.toString()
                Toast.makeText(this@PostReportActivity, "Isi desc $desc", Toast.LENGTH_LONG).show()

                if (desc.isEmpty()) {
                    etDesc.error = "Kronologi Tidak boleh kosong"
                } else {
                    val stemmed = stemmer.stem(
                        etDesc.editText?.text.toString().toLowerCase(Locale.ROOT).trim()
                    )
                    val stopword = stopwordRemover.remove(stemmed)
                    if (stopword.isNotEmpty()) {
                        val tokenizedMessage = classifier.tokenize(stopword)
                        val paddedMessage = classifier.padSequence(tokenizedMessage)
                        val results = classifySequence(paddedMessage)

                        val highest = results.maxOrNull()
                        val idxLabel = results.indexOfFirst { it == highest!! }
                        val category = findLabel(idxLabel)
                        postData(desc, category)
                        Toast.makeText(
                            this@PostReportActivity,
                            "Isi Label $category",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }
            }
        }

    }

    private fun postData(desc: String, category: String) {
        val dummyUser = DummyUser.generateUser()
        val reportItem = ReportEntity(
            dummyUser.address,
            category,
            desc,
            DummyUser.generateUser().Fullname,
            dummyUser.NIK,
            dummyUser.DateOfBirth,
            dummyUser.PhoneNumber,
            DateHelper.getCurrentDate()
        )


        viewModel.insertReport(reportItem)
    }
    //  <!--------------Tensor Flow Function---------->


    private fun findLabel(idx: Int): String {
        var label = ""
        when (idx) {
            0 -> label = "Eksploitasi"
            1 -> label = "Eksploitasi Seksual"
            2 -> label = "Kekerasan Fisik"
            3 -> label = "Kekerasan Lainnya"
            4 -> label = "Kekerasan Psikis"
            5 -> label = "Kekerasan Seksual"
            6 -> label = "Penelantaran"
        }
        return label
    }


    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(modelAssetPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    private fun classifySequence(sequence: IntArray): FloatArray {
        val inputs: Array<FloatArray> = arrayOf(sequence.map { it.toFloat() }.toFloatArray())
        val outputs: Array<FloatArray> = arrayOf(FloatArray(7))
        tfLiteInterpreter?.run(inputs, outputs)
        return outputs[0]
    }
}