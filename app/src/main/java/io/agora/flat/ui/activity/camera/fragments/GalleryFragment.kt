package io.agora.flat.ui.activity.camera.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import coil.load
import io.agora.flat.R
import io.agora.flat.databinding.FragmentGalleryBinding
import io.agora.flat.util.padWithDisplayCutout

/** Fragment used to present the user with a gallery of photos taken */
class GalleryFragment internal constructor() : Fragment() {
    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding get() = _fragmentGalleryBinding!!

    private val args: GalleryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding = FragmentGalleryBinding.inflate(inflater, container, false)
        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            fragmentGalleryBinding.cutoutSafeArea.padWithDisplayCutout()
        }

        fragmentGalleryBinding.back.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
        }

        fragmentGalleryBinding.send.setOnClickListener {
            activity?.setResult(RESULT_OK, Intent().apply {
                data = Uri.parse(args.uriString)
            })
            activity?.finish()
        }

        fragmentGalleryBinding.photoView.load(Uri.parse(args.uriString))
    }

    override fun onDestroyView() {
        _fragmentGalleryBinding = null
        super.onDestroyView()
    }
}
