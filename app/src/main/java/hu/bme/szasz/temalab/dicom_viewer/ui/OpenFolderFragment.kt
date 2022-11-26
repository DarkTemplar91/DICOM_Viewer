package hu.bme.szasz.temalab.dicom_viewer.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.bme.szasz.temalab.dicom_viewer.R
import hu.bme.szasz.temalab.dicom_viewer.databinding.FragmentOpenFileBinding


class OpenFolderFragment : Fragment() {
    private var _binding: FragmentOpenFileBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        resultLauncher.launch(intent)

        _binding = FragmentOpenFileBinding.inflate(inflater,container,false)
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
            bundle.putBoolean("folder",true)

            findNavController().navigate(R.id.action_nav_open_folder_to_nav_home,bundle)
        }
        else{
            findNavController().navigate(R.id.action_nav_open_folder_to_nav_home)
        }
    }


}