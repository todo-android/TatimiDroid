package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.Fragment.CommentLockonBottomFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.CustomFont
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動のコメント表示Adapter
 * @param devNicoVideoFragment ニコるくんとコテハンで使う。ニコるくん/コテハンがいらないならnullでおーけー
 * */
class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<CommentJSONParse>, private val devNicoVideoFragment: DevNicoVideoFragment? = null) : RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    lateinit var prefSetting: SharedPreferences
    lateinit var font: CustomFont

    var textColor = Color.parseColor("#000000")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val context = holder.commentTextView.context

        // しょっきかー
        if (!::font.isInitialized) {
            font = CustomFont(context)
            prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
            textColor = TextView(context).textColors.defaultColor
        }

        val item = arrayListArrayAdapter[position]
        val comment = item.comment
        val date = item.date
        val vpos = item.vpos
        val time = vpos.toFloat() / 100 //再生時間。100で割ればいいっぽい？
        // きれいな形へ
        val formattedTime = formatTime(time)
        val mail = item.mail

        holder.userNameTextView.maxLines = 1
        // たちみどろいど以外のキャッシュはCommentNoがないので？
        if (item.commentNo == "-1" || item.commentNo.isEmpty()) {
            holder.commentTextView.text = "$comment"
        } else {
            holder.commentTextView.text = "${item.commentNo}：$comment"
        }

        // ニコるくん表示
        val isShowNicoruButton = prefSetting.getBoolean("setting_nicovideo_nicoru_show", false)
        // mail（コマンド）がないときは表示しない
        val mailText = if (item.mail.isNotEmpty()) {
            "| $mail "
        } else {
            ""
        }

        // NGスコア表示するか
        val ngScore = if (prefSetting.getBoolean("setting_show_ng", false) && item.score.isNotEmpty()) {
            "| ${item.score} "
        } else {
            ""
        }

        // ユーザーの設定したフォントサイズ
        font.apply {
            holder.commentTextView.textSize = commentFontSize
            holder.userNameTextView.textSize = userIdFontSize
        }
        // フォント
        font.apply {
            setTextViewFont(holder.commentTextView)
            setTextViewFont(holder.userNameTextView)
        }

        // ニコるカウントに合わせて色つける
        if (prefSetting.getBoolean("setting_nicovideo_nicoru_color", false)) {
            holder.cardView.apply {
                strokeColor = getNicoruLevelColor(item.nicoru, context)
                strokeWidth = 2
                elevation = 0f
                setBackgroundColor(getThemeColor(context))
            }
        }

        // 一般会員にはニコる提供されてないのでニコる数だけ表示
        // あとDevNicoVideoFragmentはがめんスワイプしてたらなんか落ちたので
        val nicoruCount = if (!isShowNicoruButton && devNicoVideoFragment == null && item.nicoru > 0) {
            "| ニコる ${item.nicoru} "
        } else {
            ""
        }

        // 動画でコテハン。DevNicoVideoFragment（第二引数）がnullなら動きません。
        val kotehanOrUserId = devNicoVideoFragment?.kotehanMap?.get(item.userId) ?: item.userId

        holder.userNameTextView.text = "${setTimeFormat(date.toLong())} | $formattedTime $mailText$nicoruCount$ngScore| ${kotehanOrUserId}"
        holder.nicoruButton.text = item.nicoru.toString()

        // ロックオン芸（詳細画面表示）
        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("comment", item.comment)
            bundle.putString("user_id", item.userId)
            bundle.putString("liveId", item.videoOrLiveId)
            bundle.putString("label", holder.userNameTextView.text.toString())
            bundle.putLong("current_pos", item.vpos.toLong())
            val commentLockonBottomFragment = CommentLockonBottomFragment()
            commentLockonBottomFragment.arguments = bundle
            if (context is AppCompatActivity) {
                commentLockonBottomFragment.show(context.supportFragmentManager, "comment_menu")
            }
        }

        // DevNicoVideoFragmentに依存してる系。なにかに依存させるとかFragmentの意味ないやんけ！
        if (devNicoVideoFragment != null) {

            // オフライン再生かどうか
            val isOfflinePlay = devNicoVideoFragment.isCache

            // プレ垢
            val isPremium = if (!devNicoVideoFragment.isCache) {
                devNicoVideoFragment.nicoVideoHTML.isPremium(devNicoVideoFragment.jsonObject)
            } else {
                false
            }

            // ニコる押したとき
            holder.nicoruButton.setOnClickListener {
                postNicoru(context, holder, item)
            }

            // プレ垢はニコるくんつける
            if (isPremium && isShowNicoruButton && !isOfflinePlay) {
                holder.nicoruButton.isVisible = true
            }
        }
    }

    /** ニコるくんニコる関数 */
    private fun postNicoru(context: Context, holder: ViewHolder, item: CommentJSONParse) {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast(context, "${context.getString(R.string.error)}\n${throwable.message}")
        }
        GlobalScope.launch(errorHandler) {
            devNicoVideoFragment?.apply {
                if (nicoVideoHTML.isPremium(jsonObject)) {
                    val nicoruKey = nicoruAPI.nicoruKey
                    val responseNicoru = nicoruAPI.postNicoru(userSession, threadId, userId, item.commentNo, item.comment, "${item.date}.${item.dateUsec}", nicoruKey)
                    if (!responseNicoru.isSuccessful) {
                        // 失敗時
                        showToast(context, "${context?.getString(R.string.error)}\n${responseNicoru.code}")
                        return@launch
                    }
                    val responseString = responseNicoru.body?.string()
                    // 成功したか
                    val jsonObject = JSONArray(responseString).getJSONObject(0)
                    val status = nicoruAPI.nicoruResultStatus(jsonObject)
                    when (status) {
                        0 -> {
                            // ニコれた
                            item.nicoru = nicoruAPI.nicoruResultNicoruCount(jsonObject)
                            val nicoruId = nicoruAPI.nicoruResultId(jsonObject)
                            showSnackbar("${getString(R.string.nicoru_ok)}：${item.nicoru}\n${item.comment}", getString(R.string.nicoru_delete)) {
                                // 取り消しAPI叩く
                                GlobalScope.launch {
                                    val deleteResponse = nicoruAPI.deleteNicoru(userSession, nicoruId)
                                    if (deleteResponse.isSuccessful) {
                                        this@NicoVideoAdapter.showToast(context, getString(R.string.nicoru_delete_ok))
                                    } else {
                                        this@NicoVideoAdapter.showToast(context, "${getString(R.string.error)}${deleteResponse.code}")
                                    }
                                }
                            }
                            // ニコるボタンに再適用
                            holder.nicoruButton.post {
                                holder.nicoruButton.text = item.nicoru.toString()
                            }
                        }
                        2 -> {
                            // nicoruKey失効。
                            GlobalScope.launch {
                                // 再取得
                                devNicoVideoFragment.nicoruAPI.getNicoruKey(userSession, threadId)
                                postNicoru(context, holder, item)
                            }
                        }
                        else -> {
                            this@NicoVideoAdapter.showToast(context, getString(R.string.nicoru_error))
                        }
                    }
                } else {
                    showToast(context, "プレ垢限定です")
                }
            }
        }
    }

    /**
     * 時間表記をきれいにする関数
     * */
    private fun formatTime(time: Float): String {
        val minutes = time / 60
        val hour = (minutes / 60).toInt()
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "${hour}:${simpleDateFormat.format(time * 1000)}"
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var commentTextView: TextView = itemView.findViewById(R.id.adapter_nicovideo_comment_textview)
        var userNameTextView: TextView = itemView.findViewById(R.id.adapter_nicovideo_user_textview)
        var nicoruButton: MaterialButton = itemView.findViewById(R.id.adapter_nicovideo_comment_nicoru)
        var cardView: MaterialCardView = itemView.findViewById(R.id.adapter_nicovideo_comment_cardview)
    }

    fun setTimeFormat(date: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return simpleDateFormat.format(date * 1000)
    }

    fun showToast(context: Context, message: String) {
        (context as AppCompatActivity).runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ニコる色。
    private fun getNicoruLevelColor(nicoruCount: Int, context: Context) = when {
        nicoruCount >= 9 -> Color.rgb(252, 216, 66)
        nicoruCount >= 6 -> Color.rgb(253, 235, 160)
        nicoruCount >= 3 -> Color.rgb(254, 245, 207)
        else -> textColor
    }

}
