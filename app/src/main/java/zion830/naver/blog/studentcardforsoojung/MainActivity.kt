package zion830.naver.blog.studentcardforsoojung

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.soundcloud.android.crop.Crop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import zion830.naver.blog.studentcardforsoojung.TransBitmap.stringToBitMap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val requestExternalStorage: Int = 0
    private val aspectX: Int = 69
    private val aspectY: Int = 42

    private lateinit var params: WindowManager.LayoutParams
    private lateinit var pref: ManagePreferences
    private var brightness: Float = 0f
    private var isCroped: Boolean = false
    private var stateStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setTitle(R.string.toolbar_title)

        pref = ManagePreferences(this).getInstance()

        //화면 정보 가져오기
        val params: WindowManager.LayoutParams = window.attributes
        brightness = params.screenBrightness

        initView()

        if (android.os.Build.VERSION.SDK_INT >= 23)
            askForPermission()
    }

    private fun initView() {
        stateStr = pref.getStateStr()
        val bmp = pref.getBmp()

        switch_bright_onoff.isChecked = stateStr.toBoolean()

        if (switch_bright_onoff.isChecked) {
            params.screenBrightness = 1f
            window.attributes = params
        }

        if (bmp.equals("none"))
            iv_card.setImageResource(R.drawable.info_test)
        else
            iv_card.setImageBitmap(stringToBitMap(bmp))


        switch_bright_onoff.setOnCheckedChangeListener { buttonView, isChecked ->
            params.screenBrightness = 1f.takeIf { isChecked } ?: brightness
            window.attributes = params
            pref.savePreferences(switch_bright_onoff.isChecked)
        }
    }

    fun changePicBtnOnClicked(view: View) {
        val charSequences = arrayOf<CharSequence>(getString(R.string.main_btn_opt1), getString(R.string.main_btn_opt2))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.main_dlg_title))
                .setIcon(R.drawable.app_icon)
                .setItems(charSequences) { _, which ->
                    isCroped = (which != 0)
                    Crop.pickImage(this)
                }.setNegativeButton(getString(R.string.general_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    fun getCardBtnOnClicked(view: View) {
        val link = Intent(Intent.ACTION_VIEW, Uri.parse("http://smart.sungshin.ac.kr/user/main.jsp"))
        startActivity(link)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (isCroped) {
            if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK)
                beginCrop(data.data)
            else if (requestCode == Crop.REQUEST_CROP)
                handleCrop(resultCode, data)
        } else if (resultCode == -1 && requestCode == Crop.REQUEST_PICK) {
            iv_card.setImageURI(data.data)

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
                pref.saveImgPreferences(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, getString(R.string.main_img_error), Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun beginCrop(source: Uri?) {
        val destination = Uri.fromFile(File(cacheDir, "cropped"))
        Crop.of(source, destination).withAspect(aspectX, aspectY).start(this)
    }

    private fun handleCrop(resultCode: Int, result: Intent) {
        if (resultCode == RESULT_OK) {
            iv_card.setImageURI(Crop.getOutput(result))

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Crop.getOutput(result))
                pref.saveImgPreferences(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        //최대 밝기로 변경
        if (stateStr.toBoolean()) {
            params.screenBrightness = 1f
            window.attributes = params
        }
    }

    override fun onPause() {
        super.onPause()

        //기존 밝기로 변경
        params.screenBrightness = brightness
        window.attributes = params
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.main_info_title))
                    .setMessage(getString(R.string.main_info_msg))
                    .setNegativeButton(getString(R.string.general_ok)) { dialog, which -> }.show()
        }

        return super.onOptionsItemSelected(item)
    }

    //권한 요청
    private fun askForPermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, permissions, requestExternalStorage)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestExternalStorage) {
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.main_pms_error), Toast.LENGTH_LONG).show()
                    this.finish()
                }
            }
        }
    }
}