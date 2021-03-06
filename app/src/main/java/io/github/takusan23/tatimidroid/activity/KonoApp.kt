package io.github.takusan23.tatimidroid.activity

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.adapter.MenuRecyclerAdapter
import io.github.takusan23.tatimidroid.adapter.MenuRecyclerAdapterDataClass
import io.github.takusan23.tatimidroid.databinding.ActivityKonoAppBinding
import io.github.takusan23.tatimidroid.tool.DarkModeSupport
import io.github.takusan23.tatimidroid.tool.LanguageTool
import io.github.takusan23.tatimidroid.tool.getThemeColor
import io.github.takusan23.tatimidroid.tool.isDarkMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * このアプリについて。
 * */
class KonoApp : AppCompatActivity() {

    /**
     * 作者のTwitter、Mastodonリンク
     * */
    val twitterLink = "https://twitter.com/takusan__23"
    val mastodonLink = "https://best-friends.chat/@takusan_23"
    val source = "https://github.com/takusan23/TatimiDroid"
    val privacy_policy = "https://github.com/takusan23/TatimiDroid/blob/master/privacy_policy.md"

    /**
     * バージョンとか
     * */
    val version = "\uD83C\uDF38 2021/04/26 \uD83C\uDF38 "
    val codeName1 = "（く）" // https://dic.nicovideo.jp/a/ニコニコ動画の変遷

    /** findViewById駆逐 */
    private val viewBinding by lazy { ActivityKonoAppBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)
        if (isDarkMode(this)) {
            // バーを暗くする
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(this)))
        }

        setContentView(viewBinding.root)

        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName

        title = getString(R.string.kono_app)

        viewBinding.activityKonoAppCodenameTextView.text = "$appVersion\n$version\n$codeName1"

        viewBinding.activityKonoAppCardView.setOnClickListener {
            Toast.makeText(this, "新生活頑張って", Toast.LENGTH_SHORT).show()
            runEasterEgg()
            runSakura()
        }

        // リンク集展開/非表示など
        viewBinding.activityKonoAppLinkShowButton.setOnClickListener { button ->
            viewBinding.konoAppRecyclerView.isVisible = !viewBinding.konoAppRecyclerView.isVisible
            // アイコン設定
            if (viewBinding.konoAppRecyclerView.isVisible) {
                // 格納
                (button as MaterialButton).apply {
                    text = getString(R.string.hide)
                    icon = getDrawable(R.drawable.ic_expand_more_24px)
                }
            } else {
                // 表示
                (button as MaterialButton).apply {
                    text = getString(R.string.show)
                    icon = getDrawable(R.drawable.ic_expand_less_black_24dp)
                }
            }
        }

        // リンク集初期化
        initRecyclerView()

    }

    private fun initRecyclerView() {
        viewBinding.konoAppRecyclerView.apply {
            val menuList = arrayListOf<MenuRecyclerAdapterDataClass>().apply {
                add(MenuRecyclerAdapterDataClass("Twitter", twitterLink, getDrawable(R.drawable.ic_outline_account_circle_24px), twitterLink))
                add(MenuRecyclerAdapterDataClass("Mastodon", mastodonLink, getDrawable(R.drawable.ic_outline_account_circle_24px), mastodonLink))
                add(MenuRecyclerAdapterDataClass(getString(R.string.sourcecode), source, getDrawable(R.drawable.ic_code_black_24dp), source))
                add(MenuRecyclerAdapterDataClass(getString(R.string.privacy_policy), privacy_policy, getDrawable(R.drawable.ic_policy_black), privacy_policy))
            }
            layoutManager = LinearLayoutManager(context)
            val menuAdapter = MenuRecyclerAdapter(menuList)
            adapter = menuAdapter
        }
    }

    /** 桜を流す */
    private fun runSakura() {
        val sakura = "\uD83C\uDF38"
        lifecycleScope.launch {
            repeat(20) {
                val drawText = sakura.repeat(Random.nextInt(1, 10))
                val size = "small"
                val commentJSON = CommentJSONParse("{}", "arena", "sm157")
                commentJSON.comment = drawText
                commentJSON.mail = size
                viewBinding.activtyKonoAppCommentCanvas.postComment(drawText, commentJSON)
                delay(100)
            }
        }
    }

    /**
     * これ使って作った。 → https://www.nicovideo.jp/watch/sm37001529
     * イースターエッグの割には何の対策もしない
     * */
    private fun runEasterEgg() {
        val aa = """
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████████████　　　　　　 
　　　　█████████████████████　　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　███　　　　██　　　　 
　　　██　　　　　　　　　　　　███　　　　██　　　　 
　　　██　　　　　　　　　　　　███　　　　██　　　　 
　　　██　　　███　　　　　　　　　　　　　██　　　　 
　　　██　　　███　　　　　　　　　　　　　██　　　　 
　　　██　　　███　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　██　　　　　　　　　　　　　　　　　　　██　　　　 
　　　　█████████████████████　　　　　 
　　　　　███████████████████　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████████　　　　　　　　　　 
　　　　　███████████████　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████　　　　　　　　　　　　　　 
　　　　　███████████　　　　　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 
　　　　　███████████████████　　　　　　 
　　　　　███████████████████　　　　　　 
　　　　　　　　　　　　　　　　　
            """

        // 色ランダム
        val color = arrayListOf("pink", "blue", "cyan", "orange", "purple").random()
        val size = arrayListOf("small", "medium", "big").random()
        val commentJSON = CommentJSONParse("{}", "arena", "sm157")
        commentJSON.comment = aa
        commentJSON.mail = "$color $size"
        viewBinding.activtyKonoAppCommentCanvasSecond.postCommentAsciiArt(aa.split("\n"), commentJSON)
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}
