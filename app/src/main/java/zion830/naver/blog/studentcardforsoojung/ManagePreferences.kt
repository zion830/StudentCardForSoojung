package zion830.naver.blog.studentcardforsoojung

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap

class ManagePreferences(context: Context) {
    private lateinit var instance: ManagePreferences
    private val pref: SharedPreferences = context.getSharedPreferences("pref", Context.MODE_PRIVATE)

    fun getInstance() = instance

    fun savePreferences(isChecked: Boolean) {
        val editor = pref.edit()
        editor.putString("state", isChecked.toString())
        editor.apply()
    }

    fun saveImgPreferences(bitmap: Bitmap) {
        val editor = pref.edit()
        editor.putString("img", TransBitmap.bitmapToString(bitmap))

        editor.apply()
    }

    fun getStateStr(): String = pref.getString("state", "true")

    fun getBmp(): String = pref.getString("img", "none")
}