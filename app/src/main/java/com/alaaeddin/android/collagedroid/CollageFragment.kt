package com.alaaeddin.android.collagedroid

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import android.view.Menu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import android.provider.MediaStore.Images.Media.MIME_TYPE
import android.provider.MediaStore.Images.Media.DATE_ADDED
import android.provider.MediaStore.Images.Media.DATE_TAKEN
import android.provider.MediaStore.Images.Media.DATA
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.contentValuesOf
import androidx.core.os.bundleOf
import androidx.core.view.drawToBitmap
import androidx.core.view.get
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.fragment.app.Fragment

class CollageFragment : Fragment(), View.OnClickListener {

    private lateinit var templateType: TemplateType
    private lateinit var photo1: ImageView
    private lateinit var photo2: ImageView
    private lateinit var photo3: ImageView
    private lateinit var collageContainer: View
    private lateinit var selectedPhoto: ImageView
    private lateinit var menuDone: MenuItem

    companion object {
        private const val ARG_TEMPLATE_TYPE = "ARG_TEMPLATE_TYPE"
        private const val CODE_FOR_PERMISSION_WRITE_EXTERNAL_STORAGE = 111

        fun newInstance(templateType: TemplateType): CollageFragment {
            val fragment = CollageFragment()
            // 1 Create a Bundle object and Add the values
            /** Here we use bundleOf() helper function instead of
             * Creating { Bundle() }
             * and { bundle.putString(ARG_TEMPLATE_TYPE, templateType.name) }
             * The Result is : The code is more concise and readable */
            val bundle = bundleOf(ARG_TEMPLATE_TYPE to templateType.name)
            // 2 Add the bundle to fragment arguments
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {

            templateType = TemplateType.valueOf(it.getString(ARG_TEMPLATE_TYPE) ?: "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(templateType.layout, container, false)

        bindUI(rootView)
        setHasOptionsMenu(true)
        return rootView
    }

    private fun bindUI(rootView: View) {
        photo1 = rootView.findViewById(R.id.photo_1)
        photo2 = rootView.findViewById(R.id.photo_2)
        photo3 = rootView.findViewById(R.id.photo_3)

        activity?.let {
            collageContainer = it.findViewById(R.id.collage_container)
        }
        photo1.setOnClickListener(this)
        photo2.setOnClickListener(this)
        photo3.setOnClickListener(this)

        photo1.tag = templateType.photo1
        photo2.tag = templateType.photo2
        photo3.tag = templateType.photo3
    }

    override fun onClick(view: View) {
        selectedPhoto = view as ImageView
        val photoInfo = selectedPhoto.tag as PhotoInfo
        showCropView(photoInfo)
    }

    private fun showCropView(photoInfo: PhotoInfo) {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(photoInfo.aspectRatioX, photoInfo.aspectRatioY)
                .start(context as Context, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri
                selectedPhoto.setImageURI(resultUri)
                showGenerateCollageMenuIfNeedIt()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
        hideMenuItemDone(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                generateCollage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CODE_FOR_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result array is empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    generateCollage()
                }
            }
        }
    }

    private fun generateCollage() {
        context?.let {

            if (!requestPermissionToSaveCollageIfNeeded(it)) return

            val uri = saveCollageToGallery(it)
            showCollageInGallery(uri, it)
        }
    }

    private fun saveCollageToGallery(it: Context): Uri? {
        val collageBitmap = viewToBitmap(collageContainer)
        return storeBitmap(it, collageBitmap)
    }

    private fun showCollageInGallery(uri: Uri?, it: Context) {
        val intent = Intent(ACTION_VIEW)
        intent.data = uri
        it.startActivity(intent)
    }

    private fun viewToBitmap(view: View): Bitmap {
        /** Here we replace the entire function body with one line
         * The previous code is:
        { val bitmap = Bitmap.createBitmap(view.width, view.height,
        Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas) }
         */
        return view.drawToBitmap()
    }

    private fun storeBitmap(context: Context, bitmap: Bitmap): Uri? {
        var collageUri: Uri? = null
        try {
            val storedImagePath = createImageFile(context)

            saveBitMapOnDisk(storedImagePath, bitmap)

            collageUri = addImageToGallery(context.contentResolver, "jpeg",
                    storedImagePath)
        } catch (e: IOException) {
            e.printStackTrace()

            Toast.makeText(context, R.string.error_message_unable_to_generate_collage, LENGTH_LONG).show()
        }
        return collageUri
    }

    private fun saveBitMapOnDisk(storedImagePath: File, bitmap: Bitmap) {
        val output = FileOutputStream(storedImagePath)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        output.close()
    }

    @Throws(IOException::class)
    private fun createImageFile(context: Context): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Save a file: path for use with ACTION_VIEW intents
        //  mCurrentPhotoPath = image.getAbsolutePath();
        return File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
    }

    private fun addImageToGallery(cr: ContentResolver, imgType: String, filepath: File): Uri? {
        val currentTime = System.currentTimeMillis()
        val fileString = filepath.toString()

        /** Here we replace the
         *  val values = ContentValues() with the below code.
         *  The Result is a Code, which is much more compact and easy to read.*/
        val values = contentValuesOf(
                MIME_TYPE to "image/$imgType",
                DATE_ADDED to currentTime,
                DATE_TAKEN to currentTime,
                DATA to fileString)

        return cr.insert(EXTERNAL_CONTENT_URI, values)
    }

    private fun isWriteExternalStoragePermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, WRITE_EXTERNAL_STORAGE)
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED
    }

    private fun requestPermissionToSaveCollageIfNeeded(context: Context): Boolean {
        var canProceed = true

        if (!isWriteExternalStoragePermissionGranted(context)) {
            canProceed = false
            if (shouldShowExplanationForWriteExternalStoragePermission()) {

                requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE),
                        CODE_FOR_PERMISSION_WRITE_EXTERNAL_STORAGE)
            } else {
                requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE),
                        CODE_FOR_PERMISSION_WRITE_EXTERNAL_STORAGE)
            }
        }
        return canProceed
    }

    private fun shouldShowExplanationForWriteExternalStoragePermission(): Boolean {
        return shouldShowExplanationForPermission(WRITE_EXTERNAL_STORAGE)
    }

    private fun shouldShowExplanationForPermission(permission: String): Boolean {
        var shouldShould = false
        activity?.let {
            shouldShould = ActivityCompat.shouldShowRequestPermissionRationale(it,
                    permission)
        }

        return shouldShould
    }

    private fun areAllImageFilled(): Boolean {
        return photo1.isNotEmpty() && photo2.isNotEmpty() && photo2.isNotEmpty()
    }

    private fun ImageView.isNotEmpty(): Boolean {
        return drawable != null
    }

    private fun hideMenuItemDone(menu: Menu) {
        /** Here Core KTX extended the 'get' operator with relay syntax access the menu elements.
         *  Replaced this code menu.getItem(0) with below line*/
        menuDone = menu[0]
        menuDone.isVisible = false
    }

    private fun showGenerateCollageMenuIfNeedIt() {
        if (areAllImageFilled())
            menuDone.isVisible = true
    }
}
