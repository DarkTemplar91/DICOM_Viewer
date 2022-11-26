package hu.bme.szasz.temalab.dicom_viewer.ui

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import com.imebra.*
import hu.bme.szasz.temalab.dicom_viewer.R
import hu.bme.szasz.temalab.dicom_viewer.RotationGestureDetector
import hu.bme.szasz.temalab.dicom_viewer.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer


class HomeFragment : Fragment(), RotationGestureDetector.OnRotationGestureListener {

    private var _binding: FragmentHomeBinding? = null
    private var imageView: ImageView? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var rotationDetector: RotationGestureDetector
    private var scaleFactor = 1.0

    private val binding get() = _binding!!
    private var actionTypeIsDrag = true

    private var imageX: Float = 0.0f
    private var imageY: Float = 0.0f

    private var isFolder: Boolean = false

    private var menu: SubMenu? = null

    private var desciptorMap = LinkedHashMap<String, MutableList<Bitmap>>()
    private var currentKey: String? = null


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

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navView = activity?.findViewById<NavigationView>(R.id.nav_view)
        menu = navView?.menu?.getItem(3)?.subMenu

        val dicomPath = arguments?.getString("uri")
        isFolder = arguments?.getBoolean("folder")!!

        binding.root.setOnTouchListener { _, p1 ->
            scaleGestureDetector.onTouchEvent(p1)
            rotationDetector.onTouchEvent(p1)
            true
        }

        binding.orientateButton.setOnClickListener {
            imageView?.rotation = 0.0f
        }
        binding.sizeButton.setOnClickListener {
            imageView?.scaleX = 1.0f
            imageView?.scaleY = 1.0f
            scaleFactor = 1.0
        }
        binding.centerButton.setOnClickListener {
            imageView?.x = imageX
            imageView?.y = imageY
        }

        binding.navigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dragButton -> {
                    actionTypeIsDrag = true
                }
                R.id.rotateButton -> {
                    actionTypeIsDrag = false
                }
            }
            true
        }

        binding.imageView.setOnTouchListener(object : View.OnTouchListener {
            var downPoint = PointF()
            var startPoint = PointF()

            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {

                when (p1?.action) {
                    MotionEvent.ACTION_MOVE -> {
                        imageView?.x = startPoint.x + p1.x - downPoint.x
                        imageView?.y = startPoint.y + p1.y - downPoint.y
                        startPoint.x = imageView?.x!!
                        startPoint.y = imageView?.y!!

                    }
                    MotionEvent.ACTION_DOWN -> {
                        downPoint.set(p1.x, p1.y)
                        startPoint.set(imageView?.x!!, imageView?.y!!)
                    }
                    MotionEvent.ACTION_UP -> {
                    }

                }
                return actionTypeIsDrag
            }

        })

        if (dicomPath == null) {
            //Snackbar.make(binding.root,"No file or folder selected to load images from!",Snackbar.LENGTH_LONG).show()
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {

                    val uri = Uri.parse(dicomPath)
                    if (isFolder) {
                        activity?.runOnUiThread {
                            binding.progressBar.isVisible = true
                        }
                        processFolder(uri)
                        activity?.runOnUiThread {
                            binding.progressBar.isVisible = false
                            binding.backButton.isVisible=true
                            binding.forwardButton.isVisible=true
                            binding.backButton.isEnabled = false
                        }
                    } else {
                        loadDicomImage(uri)
                        var result = uri.path
                        val cut: Int = result?.lastIndexOf('/')!!
                        if (cut != -1) {
                            result = result.substring(cut + 1)
                        }
                        activity?.runOnUiThread {
                            menu?.add(R.id.nav_view, 123, Menu.NONE, result)
                            binding.descriptor.text = result
                        }
                    }

                    activity?.runOnUiThread {
                        if (desciptorMap.size > 0) {
                            val key = desciptorMap.keys.first()
                            val bitmapList = desciptorMap[key]!!
                            imageView?.setImageBitmap(bitmapList[0])
                            imageView?.scaleType = ImageView.ScaleType.FIT_CENTER

                            imageX = imageView?.x!!
                            imageX = imageView?.x!!

                            binding.slider.valueTo = bitmapList.count().toFloat() - 1f
                            if(isFolder)
                                binding.descriptor.text = key
                            currentKey = key
                        } else {
                            //TODO: Snackbar
                        }
                    }

                }
            }
        }

        binding.slider.addOnChangeListener(Slider.OnChangeListener { slider, _, _ ->
            val bitmapList = desciptorMap[currentKey]!!
            imageView?.setImageBitmap(bitmapList[slider.value.toInt()])
            imageView?.scaleType = ImageView.ScaleType.FIT_CENTER
        })

        binding.forwardButton.setOnClickListener{
            val keys = desciptorMap.keys.toList()
            val newIndex = keys.indexOf(currentKey) + 1
            if(newIndex < keys.size ){
                currentKey = desciptorMap.keys.toList()[newIndex]
                if(newIndex == keys.size-1)
                    binding.forwardButton.isEnabled = false
                if(newIndex > 0)
                    binding.backButton.isEnabled = true
            }
            else{
                return@setOnClickListener
            }
            val list = desciptorMap[currentKey]
            imageView?.setImageBitmap(list?.get(0))

            val newValue = (list?.size?.minus(1f))!!
            if(newValue>0f){
                binding.slider.valueTo = newValue
            }

            binding.descriptor.text = currentKey
            if(!binding.slider.isVisible && list.size > 1){
                binding.slider.isVisible = true
            }
            else if(list.size < 2){
                binding.slider.isVisible = false
            }
            binding.slider.value = 0.0f
        }

        binding.backButton.setOnClickListener{
            val keys = desciptorMap.keys.toList()
            val newIndex = keys.indexOf(currentKey) - 1
            if(newIndex >= 0 ){
                currentKey = desciptorMap.keys.toList()[newIndex]
                if(newIndex == 0)
                    binding.backButton.isEnabled = false
                if(newIndex < keys.size)
                    binding.forwardButton.isEnabled = true
            }
            else{
                return@setOnClickListener
            }

            val list = desciptorMap[currentKey]
            imageView?.setImageBitmap(list?.get(0))

            val newValue = (list?.size?.minus(1f))!!
            if(newValue>0f){
                binding.slider.valueTo = newValue
            }

            binding.descriptor.text = currentKey
            if(!binding.slider.isVisible && list.size > 1){
                binding.slider.isVisible = true
            }
            else if(list.size < 2){
                binding.slider.isVisible = false
            }
            binding.slider.value = 0.0f
        }

        binding.slider.isVisible = false
        binding.progressBar.isVisible = false
        binding.backButton.isVisible = false
        binding.forwardButton.isVisible = false

    }

    private fun processFolder(dicomPath: Uri) {
        val permissionGranted = isReadStoragePermissionGranted()
        if (!permissionGranted) return

        val documentTree = DocumentFile.fromTreeUri(binding.root.context, dicomPath) ?: return
        if (!documentTree.isDirectory) return

        for (listFile in documentTree.listFiles()) {
            loadDicomImage(listFile.uri)
        }

        activity?.runOnUiThread {
            binding.slider.isVisible = desciptorMap.isEmpty()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun loadDicomImage(dicomPath: Uri) {


        //DICOM tag, study description.
        //Used for differentiating between images
        val tagId = TagId(8, 4158)

        try {
            val stream: InputStream? =
                binding.root.context.contentResolver?.openInputStream(dicomPath)

            val streamCheck: InputStream? =
                binding.root.context.contentResolver?.openInputStream(dicomPath)
            if (!isDicomFile(streamCheck)) {
                streamCheck?.close()
                return
            }

            val imebraPipe = PipeStream(32000)

            GlobalScope.launch {
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

            //Descriptor
            val studyDescriptor = loadDataSet.getString(tagId, 0)
            var value = desciptorMap[studyDescriptor]
            if (value == null) {
                value = mutableListOf()
                desciptorMap[studyDescriptor] = value
            }
            value.add(renderBitmap)
        } catch (e: IOException) {
            activity?.runOnUiThread {
                val dlgAlert: AlertDialog.Builder = AlertDialog.Builder(binding.root.context)
                dlgAlert.setMessage(e.message)
                dlgAlert.setTitle("Error")
                dlgAlert.setPositiveButton(
                    "OK"
                ) { _, _ ->
                }
                dlgAlert.setCancelable(true)
                dlgAlert.create().show()
            }
        }
    }

    private fun isDicomFile(stream: InputStream?): Boolean {
        //Look for magic bits
        val array = byteArrayOf(0, 0, 0, 0)
        stream?.skip(128)
        stream?.read(array, 0, 4)
        return array.contentEquals(byteArrayOf(68, 73, 67, 77))
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
        imageView?.rotation = angle!!
    }


    private fun isReadStoragePermissionGranted(): Boolean {
        return if (checkSelfPermission(
                binding.root.context,
                READ_EXTERNAL_STORAGE
            ) == PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(READ_EXTERNAL_STORAGE), 1)
            false
        }
    }

    private inner class LastOpened(uri: Uri, name: String) {
        val uri: Uri
        val name: String

        init {
            this.uri = uri
            this.name = name
        }
    }

}