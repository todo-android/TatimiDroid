package io.github.takusan23.tatimidroid.Adapter

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import io.github.takusan23.tatimidroid.Fragment.CommentMenuBottomFragment
import io.github.takusan23.tatimidroid.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * CommentJSONParse配列を使う
 * */
class CommentRecyclerViewAdapter(val commentList: ArrayList<CommentJSONParse>) :
    RecyclerView.Adapter<CommentRecyclerViewAdapter.ViewHolder>() {

    //UserIDの配列。初コメを太字表示する
    val userList = arrayListOf<String>()
    lateinit var pref_setting: SharedPreferences

    lateinit var appCompatActivity: AppCompatActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_comment_layout, parent, false)
        return ViewHolder(view)
    }

    fun setActivity(appCompatActivity: AppCompatActivity) {
        this.appCompatActivity = appCompatActivity
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val commentJSONParse = commentList[position]
        val context = holder.cardView.context
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //NGチェック
        var isNGUser = false
        var isNGComment = false
        var userId: String = commentJSONParse.userId

        //CommentFragment取得

        //ロックオンだけcontextからActivityが取れないので。。
        //共通化する
        if (context is AppCompatActivity) {
            this.appCompatActivity = context
        }

        val commentFragment =
            this.appCompatActivity.supportFragmentManager.findFragmentByTag(commentJSONParse.videoOrLiveId) as CommentFragment

        //NG配列
        val userNGList = commentFragment.userNGList
        val commentNGList = commentFragment.commentNGList
        //コテハンMAP
        val kotehanMap = commentFragment.kotehanMap
        //NGコメント、ユーザーか
        if (userNGList.indexOf(commentJSONParse.userId) != -1) {
            isNGUser = true
        }
        if (commentNGList.indexOf(commentJSONParse.comment) != -1) {
            isNGComment = true
        }
        //コテハン
        if (kotehanMap.containsKey(commentJSONParse.userId)) {
            userId = kotehanMap.get(commentJSONParse.userId) ?: ""
        } else {
            userId = commentJSONParse.userId
        }

        //絶対時刻か相対時刻か
        var time = ""
        if (pref_setting.getBoolean("setting_zettai_zikoku_hyouzi", true)) {
            if (commentJSONParse.date.isNotEmpty()) {
                //相対時刻（25:25）など
                val programStartTime = commentFragment.nicoLiveHTML.programStartTime
                val commentUnixTime = commentJSONParse.date.toLong()
                val calc = (commentUnixTime - programStartTime)
                time = DateUtils.formatElapsedTime(calc) // 時：分：秒　っていい感じにしてくれる
            } else {
                time = "0"
            }
        } else {
            //絶対時刻（12:13:00）など
            //UnixTime -> Minute
            if (commentJSONParse.date.isNotEmpty()) {
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = commentJSONParse.date.toLong() * 1000L
                val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
                time = simpleDateFormat.format(commentJSONParse.date.toLong() * 1000L)
            }
        }

        var info = "${commentJSONParse.roomName} | $time | ${userId}"
        var comment: String

        //公式番組のコメントはコメント番号存在しない
        if (commentJSONParse.commentNo.isEmpty()) {
            if (commentJSONParse.uneiComment.isNotEmpty()) {
                comment = commentJSONParse.uneiComment // 運営コメントをきれいにしたやつ
            } else {
                comment = commentJSONParse.comment
            }
        } else {
            if (commentJSONParse.uneiComment.isNotEmpty()) {
                comment =
                    "${commentJSONParse.commentNo} : ${commentJSONParse.uneiComment}" // 運営コメントをきれいにしたやつ
            } else {
                comment = "${commentJSONParse.commentNo} : ${commentJSONParse.comment}"
            }
        }

        //NGスコア表示するか
        if (pref_setting.getBoolean("setting_show_ng", false)) {
            if (commentJSONParse.score.isNotEmpty()) {
                info = "$info | ${commentJSONParse.score}"
            } else {
                info = info
            }
        } else {
            info = info
        }

        //プレ垢
        if (commentJSONParse.premium.isNotEmpty()) {
            info = "$info | ${commentJSONParse.premium}"
        } else {
            info = info
        }

        //NGの場合は置き換える
        if (isNGUser) {
            info = ""
            comment = context.getString(R.string.ng_user)
        }
        if (isNGComment) {
            info = ""
            comment = context.getString(R.string.ng_comment)
        }

        //UserIDの配列になければ配列に入れる。初コメ
        if (userList.indexOf(commentJSONParse.userId) == -1) {
            userList.add(commentJSONParse.userId)
            //初コメは太字にする
            holder.commentTextView.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.commentTextView.typeface = Typeface.DEFAULT
        }

        holder.commentTextView.text = comment
        holder.roomNameTextView.text = info


        //詳細画面出す
        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("comment", commentJSONParse.comment)
            bundle.putString("user_id", commentJSONParse.userId)
            bundle.putString("liveId", commentJSONParse.videoOrLiveId)
            val commentMenuBottomFragment = CommentMenuBottomFragment()
            commentMenuBottomFragment.arguments = bundle
            if (context is AppCompatActivity) {
                commentMenuBottomFragment.show(
                    context.supportFragmentManager,
                    "comment_menu"
                )
            }
        }

        //部屋の色
        if (pref_setting.getBoolean("setting_room_color", true)) {
            holder.roomNameTextView.setTextColor(getRoomColor(commentJSONParse.roomName, context))
        }

        //ID非表示
        if (pref_setting.getBoolean("setting_id_hidden", false)) {
            //非表示
            holder.roomNameTextView.visibility = View.GONE
            //部屋の色をつける設定有効時はコメントのTextViewに色を付ける
            if (pref_setting.getBoolean("setting_room_color", true)) {
                holder.commentTextView.setTextColor(getRoomColor(commentJSONParse.roomName, context))
            }
        }
        //一行表示とか
        if (pref_setting.getBoolean("setting_one_line", false)) {
            holder.commentTextView.maxLines = 1
        }

        // ユーザーの設定したフォントサイズ
        commentFragment.customFont.apply {
            holder.commentTextView.textSize = commentFontSize
            holder.roomNameTextView.textSize = userIdFontSize
        }

        // ユーザーの設定したフォント
        commentFragment.customFont.setTextViewFont(holder.commentTextView)
        commentFragment.customFont.setTextViewFont(holder.roomNameTextView)

    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var commentTextView: TextView
        var roomNameTextView: TextView
        var cardView: CardView

        init {
            commentTextView = itemView.findViewById(R.id.adapter_comment_textview)
            roomNameTextView = itemView.findViewById(R.id.adapter_room_name_textview)
            cardView = itemView.findViewById(R.id.adapter_room_name_cardview)
        }
    }

    //コメビュの部屋の色。NCVに追従する
    fun getRoomColor(room: String, context: Context): Int {
        when (room) {
            context.getString(R.string.official_program) -> {
                return Color.argb(255, 0, 153, 229)
            }
            context.getString(R.string.arena) -> {
                return Color.argb(255, 0, 153, 229)
            }
            "立ち見1" -> {
                return Color.argb(255, 234, 90, 61)
            }
            "立ち見2" -> {
                return Color.argb(255, 172, 209, 94)
            }
            "立ち見3" -> {
                return Color.argb(255, 0, 217, 181)
            }
            "立ち見4" -> {
                return Color.argb(255, 229, 191, 0)
            }
            "立ち見5" -> {
                return Color.argb(255, 235, 103, 169)
            }
            "立ち見6" -> {
                return Color.argb(255, 181, 89, 217)
            }
            "立ち見7" -> {
                return Color.argb(255, 20, 109, 199)
            }
            "立ち見8" -> {
                return Color.argb(255, 226, 64, 33)
            }
            "立ち見9" -> {
                return Color.argb(255, 142, 193, 51)
            }
            "立ち見10" -> {
                return Color.argb(255, 0, 189, 120)
            }
        }
        return Color.argb(255, 0, 0, 0)
    }

}