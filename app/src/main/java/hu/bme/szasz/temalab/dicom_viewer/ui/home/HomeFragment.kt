package hu.bme.szasz.temalab.dicom_viewer.ui.home


import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.imebra.*
import hu.bme.szasz.temalab.dicom_viewer.databinding.FragmentHomeBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var imageView: ImageView? = null

    private val binding get() = _binding!!

    private var dicomPath : String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        System.loadLibrary("imebra_lib")

        imageView = binding.imageView
        dicomPath = arguments?.getString("uri")

        if(dicomPath == null){
            //TODO: Handle
        }
        else{
            loadDicomImage()
        }


        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    fun loadDicomImage(){

        try {

            val stream: InputStream? =
                binding.root.context.contentResolver?.openInputStream(Uri.parse(dicomPath))

            val imebraPipe = PipeStream(32000)

            GlobalScope.launch{
                kotlin.runCatching {


                val pipeWriter = StreamWriter(imebraPipe.streamOutput)
                try {

                    // Buffer used to read from the stream
                    val buffer = ByteArray(128000)
                    val memory = MutableMemory()

                    // Read until we reach the end
                    var readBytes: Int = stream?.read(buffer)!!
                    while (readBytes >= 0) {


                        // Push the data to the Pipe
                        if (readBytes > 0) {
                            memory.assign(buffer)
                            memory.resize(readBytes.toLong())
                            pipeWriter.write(memory)
                        }
                        readBytes = stream.read(buffer)
                    }
                } catch (e: IOException) {
                } finally {
                    pipeWriter.delete()
                    imebraPipe.close(50000)
                }
                }
            }

            val loadDataSet = CodecFactory.load(StreamReader(imebraPipe.streamInput))
            val dicomImage: Image = loadDataSet.getImageApplyModalityTransform(0)

            var chain = TransformsChain()

            if (ColorTransformsFactory.isMonochrome(dicomImage.colorSpace)) {
                val voilut = VOILUT(
                    VOILUT.getOptimalVOI(
                        dicomImage,
                        0,
                        0,
                        dicomImage.width,
                        dicomImage.height
                    )
                )
                chain.addTransform(voilut)
            }
            val drawBitmap = DrawBitmap(chain)
            val memory = drawBitmap.getBitmap(dicomImage, drawBitmapType_t.drawBitmapRGBA, 4)

            val renderBitmap = Bitmap.createBitmap(
                dicomImage.width.toInt(),
                dicomImage.height.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val memoryByte = ByteArray(memory.size().toInt())
            memory.data(memoryByte)
            val byteBuffer: ByteBuffer = ByteBuffer.wrap(memoryByte)
            renderBitmap.copyPixelsFromBuffer(byteBuffer)

            imageView?.setImageBitmap(renderBitmap)
            imageView?.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        catch (e: IOException){
            val dlgAlert: AlertDialog.Builder = AlertDialog.Builder(binding.root.context)
            dlgAlert.setMessage(e.message)
            dlgAlert.setTitle("Error")
            dlgAlert.setPositiveButton("OK",
                DialogInterface.OnClickListener { _, _ ->
                    //dismiss the dialog
                })
            dlgAlert.setCancelable(true)
            dlgAlert.create().show()
        }
    }
}