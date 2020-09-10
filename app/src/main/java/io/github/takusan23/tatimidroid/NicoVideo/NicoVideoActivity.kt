package io.github.takusan23.tatimidroid.NicoVideo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import kotlinx.android.synthetic.main.activity_nicovideo.*

/**
 * ニコ動再生Activity
 * 入れて欲しいもの↓
 * id       |   動画ID
 * cache    |   キャッシュ再生ならtrue
 * eco      |   エコノミー再生するなら（?eco=1）true
 * */
class NicoVideoActivity : AppCompatActivity() {

    lateinit var darkModeSupport: DarkModeSupport
    lateinit var prefSetting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_nicovideo)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        val id = intent.getStringExtra("id")
        val isCache = intent?.getBooleanExtra("cache", false) ?: false
        val isEconomy = intent?.getBooleanExtra("eco", false) ?: false

        // 画面回転復帰時はFragmentを置かない（savedInstanceStateがnullのときだけ生成する）
        if (savedInstanceState == null) {
            // 初回
            val nicoVideoFragment = NicoVideoFragment()
            val bundle = Bundle()
            bundle.putString("id", id)
            bundle.putBoolean("cache", isCache)
            bundle.putBoolean("eco", isEconomy)
            nicoVideoFragment.arguments = bundle
            //あとから探せるように第三引数にID入れる
            supportFragmentManager.beginTransaction()
                .replace(activity_nicovideo_parent_linearlayout.id, nicoVideoFragment, id)
                .commit()
        }
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}