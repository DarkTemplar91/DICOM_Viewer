package hu.bme.szasz.temalab.dicom_viewer.ui.openfile

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import hu.bme.szasz.temalab.dicom_viewer.R
import hu.bme.szasz.temalab.dicom_viewer.databinding.FragmentOpenFileBinding

class OpenFileFragment : Fragment() {

    private var _binding: FragmentOpenFileBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentOpenFileBinding.inflate(inflater,container, false)

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"

            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "")
        }
        val mimetypes = arrayOf("application/dicom", "application/zip")
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimetypes)
        resultLauncher.launch(intent)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = Bundle()
            bundle.putString("uri",result.data?.data?.toString())

            findNavController().navigate(R.id.action_nav_open_file_to_nav_home,bundle)
        }
    }

}