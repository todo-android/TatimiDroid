package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.NicoLogin
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.dialog_watchmode_layout.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class BottomSheetDialogWatchMode : BottomSheetDialogFragment() {

    /*
    * findViewById卒業できない
    * */
    lateinit var commentViewerModeButton: Button
    lateinit var commentPostModeButton: Button
    lateinit var nicocasModeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_watchmode_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentViewerModeButton =
            view.findViewById(R.id.dialog_watchmode_comment_viewer_mode_button)
        commentPostModeButton =
            view.findViewById(R.id.dialog_watchmode_comment_post_mode_button)
        nicocasModeButton =
            view.findViewById(R.id.dialog_watchmode_nicocas_comment_mode_button)

        //ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        dialog_watchmode_parent_linearlayout.background =
            ColorDrawable(darkModeSupport.getThemeColor())

        getProgram()

    }

    fun getProgram() {
        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref_setting.edit()
        //LiveID
        val liveId = arguments?.getString("liveId")


        //番組が終わってる場合は落ちちゃうので修正。
        val programInfo = "https://live2.nicovideo.jp/watch/${liveId}";
        val request = Request.Builder()
            .url(programInfo)
            .header("Cookie", "user_session=${pref_setting.getString("user_session", "")}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //エラーなので閉じる
                activity?.runOnUiThread {
                    dismiss()
                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {

                    val html = Jsoup.parse(response.body?.string())
                    if (html.getElementById("embedded-data") != null) {
                        val json = html.getElementById("embedded-data").attr("data-props")
                        val jsonObject = JSONObject(json)
                        //現在放送中か？
                        val status = jsonObject.getJSONObject("program").getString("status")
                        //公式番組かどうか
                        val type = jsonObject.getJSONObject("program").getString("providerType")
                        var isOfficial = false
                        if (type.contains("official")) {
                            isOfficial = true
                        }
                        if (status == "ON_AIR") {
                            //生放送中！
                            //コメントビューワーモード
                            //コメント投稿機能、視聴継続メッセージ送信機能なし
                            commentViewerModeButton.setOnClickListener {
                                //設定変更
                                editor.putBoolean("setting_watching_mode", false)
                                editor.putBoolean("setting_nicocas_mode", false)
                                editor.apply()
                                //画面移動
                                val intent = Intent(context, CommentActivity::class.java)
                                intent.putExtra("liveId", liveId)
                                intent.putExtra("watch_mode", "comment_viewer")
                                intent.putExtra("isOfficial", isOfficial)
                                startActivity(intent)
                                this@BottomSheetDialogWatchMode.dismiss()
                            }

                            //コメント投稿モード
                            //書き込める
                            commentPostModeButton.setOnClickListener {
                                //設定変更
                                editor.putBoolean("setting_watching_mode", true)
                                editor.putBoolean("setting_nicocas_mode", false)
                                editor.apply()
                                //画面移動
                                val intent = Intent(context, CommentActivity::class.java)
                                intent.putExtra("liveId", liveId)
                                intent.putExtra("watch_mode", "comment_post")
                                intent.putExtra("isOfficial", isOfficial)
                                startActivity(intent)
                                this@BottomSheetDialogWatchMode.dismiss()
                            }

                            //nicocas式コメント投稿モード
                            //nicocasのAPIでコメント投稿を行う
                            nicocasModeButton.setOnClickListener {
                                //設定変更
                                editor.putBoolean("setting_watching_mode", false)
                                editor.putBoolean("setting_nicocas_mode", true)
                                editor.apply()
                                //画面移動
                                val intent = Intent(context, CommentActivity::class.java)
                                intent.putExtra("liveId", liveId)
                                intent.putExtra("watch_mode", "nicocas")
                                intent.putExtra("isOfficial", isOfficial)
                                startActivity(intent)
                                this@BottomSheetDialogWatchMode.dismiss()
                            }
                        } else {
                            activity?.runOnUiThread {
                                dismiss()
                                Toast.makeText(
                                    context,
                                    getString(R.string.program_end),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                } else {
                    //ニコニコにログインをし直す
                    if (context != null) {
                        //４０１エラーのときはuser_sessionが切れた
                        if (response.code == 401) {
                            val nicoLogin = NicoLogin(context!!, liveId!!)
                            //こるーちん？
                            //再ログインする
                            activity?.runOnUiThread {
                                dismiss()
                                Toast.makeText(
                                    context,
                                    getString(R.string.re_login),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                            //ログインしてから再度取得すr
                            GlobalScope.launch {
                                nicoLogin.getUserSession()
                                getProgram()
                            }
                        } else {
                            //エラーなので閉じる
                            activity?.runOnUiThread {
                                dismiss()
                                Toast.makeText(
                                    context,
                                    getString(R.string.error),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }
                }
            }
        })
    }
}