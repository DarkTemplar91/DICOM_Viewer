package hu.bme.szasz.temalab.dicom_viewer.ui.home

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.imebra.*
import hu.bme.szasz.temalab.dicom_viewer.RotationGestureDetector
import hu.bme.szasz.temalab.dicom_viewer.databinding.FragmentHomeBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer


class HomeFragment : Fragment(), RotationGestureDetector.OnRotationGestureListener{

    private var _binding: FragmentHomeBinding? = null
    private var imageView: ImageView? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var rotationDetector: RotationGestureDetector
    private var scaleFactor = 1.0

    private val binding get() = _binding!!

    private var dicomPath : String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        scaleGestureDetector = ScaleGestureDetector(binding.root.context, ScaleListener())
        rotationDetector = RotationGestureDetector(this)

        System.loadLibrary("imebra_lib")

        imageView = binding.imageView
        dicomPath = arguments?.getString("uri")

        binding.root.setOnTouchListener { p0, p1 ->
            scaleGestureDetector.onTouchEvent(p1)
            rotationDetector.onTouchEvent(p1)
            true
        }

        binding.orientateButton.setOnTouchListener { p0, p1 ->
            imageView?.rotation = 0.0f
            true
        }
        binding.sizeButton.setOnTouchListener{p0,p1->
            imageView?.scaleX = 1.0f
            imageView?.scaleY = 1.0f
            scaleFactor = 1.0
            true
        }


        if(dicomPath == null){
            //TODO: Give feedback
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

            val chain = TransformsChain()

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

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            imageView?.scaleX = scaleFactor.toFloat()
            imageView?.scaleY = scaleFactor.toFloat()
            return true
        }
    }

    override fun onRotation(rotationDetector: RotationGestureDetector?) {
        val angle = rotationDetector?.angle
        imageView?.rotation=angle!!
    }

}