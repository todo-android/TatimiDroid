package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R

/**
 * ダークモードなど
 * */
class DarkModeSupport(val context: Context) {

    var nightMode: Int = Configuration.UI_MODE_NIGHT_NO

    init {
        //ダークモードかどうか
        val conf = context.resources.configuration
        nightMode = conf.uiMode and Configuration.UI_MODE_NIGHT_MASK
        //Oreo
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
            if (pref_setting.getBoolean("setting_darkmode", false)) {
                nightMode = Configuration.UI_MODE_NIGHT_YES
            } else {
                nightMode = Configuration.UI_MODE_NIGHT_NO
            }
        }
    }

    /**
     * ダークモードテーマ。setContentView()の前に書いてね
     * */
    fun setActivityTheme(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            //ダークモード
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(activity)
            if (pref_setting.getBoolean("setting_darkmode", false)) {
                activity.setTheme(R.style.OLEDTheme)
            } else {
                activity.setTheme(R.style.AppTheme)
            }
        } else {
            when (nightMode) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    activity.setTheme(R.style.AppTheme)
                }
                Configuration.UI_MODE_NIGHT_YES -> {
                    activity.setTheme(R.style.OLEDTheme)
                }
            }
        }
    }

    /**
     * MainActivity用。TitleBarが表示されていない。setContentView()の前に書いてね
     * */
    fun setMainActivityTheme(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            //ダークモード
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(activity)
            if (pref_setting.getBoolean("setting_darkmode", false)) {
                activity.setTheme(R.style.MainActivity_OLEDTheme)
            } else {
                activity.setTheme(R.style.MainActivity_AppTheme)
            }
        } else {
            when (nightMode) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    activity.setTheme(R.style.MainActivity_AppTheme)
                }
                Configuration.UI_MODE_NIGHT_YES -> {
                    activity.setTheme(R.style.MainActivity_OLEDTheme)
                }
            }
        }
    }


    /**
     * 二窓Activity用ダークモード切り替えるやつ。setContentView()の前に書いてね
     * */
    fun setNimadoActivityTheme(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            //ダークモード
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(activity)
            if (pref_setting.getBoolean("setting_darkmode", false)) {
                activity.setTheme(R.style.NimadoOLEDTheme)
            } else {
                activity.setTheme(R.style.NimadoTheme)
            }
        } else {
            when (nightMode) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    activity.setTheme(R.style.NimadoTheme)
                }
                Configuration.UI_MODE_NIGHT_YES -> {
                    activity.setTheme(R.style.NimadoOLEDTheme)
                }
            }
        }
    }

    /**
     * 色を返す。白か黒か
     * ダークモード->黒
     * それ以外->白
     * */
    fun getThemeColor(): Int {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            //ダークモード
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
            if (pref_setting.getBoolean("setting_darkmode", false)) {
                return Color.parseColor("#000000")
            } else {
                return Color.parseColor("#ffffff")
            }
        } else {
            when (nightMode) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    return Color.parseColor("#ffffff")
                }
                Configuration.UI_MODE_NIGHT_YES -> {
                    return Color.parseColor("#000000")
                }
            }
        }
        return Color.parseColor("#ffffff")
    }

}

/**
 * ダークモードかどうか。Oreo以前ではアプリの設定の値を返します。
 * @param context こんてきすと
 * @return ダークモードならtrue。そうじゃなければfalse。
 * */
internal fun isDarkMode(context: Context?): Boolean {
    if (context == null) return false
    //ダークモードかどうか
    val conf = context.resources.configuration
    var nightMode = conf.uiMode and Configuration.UI_MODE_NIGHT_MASK
    //Oreo
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        if (pref_setting.getBoolean("setting_darkmode", false)) {
            nightMode = Configuration.UI_MODE_NIGHT_YES
        } else {
            nightMode = Configuration.UI_MODE_NIGHT_NO
        }
    }
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}

/**
 * テーマに合わせた色を返す関数。
 * @param context こんてきすと
 * @return ダークモードなら黒、それ以外なら白です。
 * */
internal fun getThemeColor(context: Context?): Int = if (isDarkMode(context)) {
    Color.parseColor("#000000")
} else {
    Color.parseColor("#ffffff")
}