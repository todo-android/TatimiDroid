package io.github.takusan23.tatimidroid.NicoLive

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTagAPI
import io.github.takusan23.tatimidroid.NicoAPI.User.User
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveTagBottomFragment
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_program_info.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat

/**
 * 番組情報Fragment
 * */
class ProgramInfoFragment : Fragment() {

    var liveId = ""
    var usersession = ""
    lateinit var pref_setting: SharedPreferences

    // ユーザー、コミュID
    var userId = ""
    var communityId = ""

    // タグ変更に使うトークン
    var tagToken = ""

    // htmlの中にあるJSON
    lateinit var jsonObject: JSONObject

    // CommentFragmentとそれのViewModel
    val commentFragment by lazy { requireParentFragment() as CommentFragment }
    val viewModel by viewModels<NicoLiveViewModel>({ commentFragment })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_program_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //番組ID取得
        liveId = arguments?.getString("liveId") ?: ""
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        usersession = pref_setting.getString("user_session", "") ?: ""

        viewModel.nicoLiveJSON.observe(viewLifecycleOwner) { json ->
            setUIFromJSON(json)
        }

        // ユーザーフォロー
        fragment_program_info_broadcaster_follow_button.setOnClickListener {
            requestFollow(userId) {
                Toast.makeText(context, "ユーザーをフォローしました。", Toast.LENGTH_SHORT).show()
            }
        }

        // コミュニティフォロー
        fragment_program_info_community_follow_button.setOnClickListener {
            requestCommunityFollow(communityId) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "コミュニティをフォローしました。\n$communityId", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // 公式番組はユーザーフォローボタンない？
        if (commentFragment.isOfficial) {
            fragment_program_info_broadcaster_follow_button.isEnabled = false
        }

        // タグ編集
        fragment_program_info_tag_add_button.setOnClickListener {
            val nicoLiveTagBottomFragment = NicoLiveTagBottomFragment()
            val bundle = Bundle().apply {
                putString("liveId", liveId)
                putString("tagToken", tagToken)
            }
            nicoLiveTagBottomFragment.arguments = bundle
            nicoLiveTagBottomFragment.programFragment = this@ProgramInfoFragment
            nicoLiveTagBottomFragment.show(childFragmentManager, "bottom_tag")
        }

        // TS予約
        fragment_program_info_timeshift_button.setOnClickListener {
            registerTimeShift {
                if (it.isSuccessful) {
                    activity?.runOnUiThread {
                        // 成功したらTS予約リストへ飛ばす
                        Snackbar.make(
                            fragment_program_info_timeshift_button,
                            R.string.timeshift_reservation_successful,
                            Snackbar.LENGTH_LONG
                        ).setAction(R.string.timeshift_list) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, "https://live.nicovideo.jp/my".toUri())
                            startActivity(intent)
                        }.setAnchorView(commentFragment.getSnackbarAnchorView()).show()
                    }
                } else if (it.code == 500) {
                    // 予約済みの可能性。
                    // なお本家も多分一度登録APIを叩いて500エラーのとき登録済みって判断するっぽい？
                    Snackbar.make(fragment_program_info_timeshift_button, R.string.timeshift_reserved, Snackbar.LENGTH_LONG).setAction(R.string.timeshift_delete_reservation_button) {
                        deleteTimeShift {
                            println(it.body?.string())
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    R.string.timeshift_delete_reservation_successful,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }.setAnchorView(commentFragment.getSnackbarAnchorView()).show()
                }
            }
        }

        // 引っ張ったらタグ更新
        fragment_program_info_swipe.setOnRefreshListener {
            coroutineGetTag()
        }

    }

    // nicoLiveHTMLtoJSONObject()のJSONの中身をUIに反映させる
    fun setUIFromJSON(jsonObject: JSONObject) {
        if (!isAdded) return
        lifecycleScope.launch(Dispatchers.Main) {
            // 番組情報取得
            val program = jsonObject.getJSONObject("program")
            val title = program.getString("title")
            val description = program.getString("description")
            val beginTime = program.getLong("beginTime")
            val endTime = program.getLong("endTime")
            // 放送者
            val supplier = program.getJSONObject("supplier")
            val name = supplier.getString("name")

            // 公式では使わない
            if (supplier.has("programProviderId")) {
                userId = supplier.getString("programProviderId")
                // ユーザー情報取得。フォロー中かどうか判断するため
                val userData = withContext(Dispatchers.IO) {
                    User().getUserCoroutine(userId, usersession)
                }
                // ユーザーフォロー中？
                if (userData?.isFollowing == true) {
                    fragment_program_info_broadcaster_follow_button.isEnabled = false
                    fragment_program_info_broadcaster_follow_button.text = getString(R.string.is_following)
                }
            }
            var level = 0
            // 公式番組対応版
            if (supplier.has("level")) {
                level = supplier.getInt("level")
            }
            // UnixTimeから変換
            val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            // フォーマット済み
            val formattedBeginTime = simpleDateFormat.format(beginTime * 1000)
            val formattedEndTime = simpleDateFormat.format(endTime * 1000)

            //UI
            fragment_program_info_broadcaster_name.text = "${getString(R.string.broadcaster)} : $name"
            fragment_program_info_broadcaster_level.text = "${getString(R.string.level)} : $level"
            fragment_program_info_time.text = "${getString(R.string.program_start)} : $formattedBeginTime / ${getString(R.string.end_of_program)} : $formattedEndTime"
            fragment_program_info_title.text = title
            fragment_program_info_description.text = HtmlCompat.fromHtml(description, FROM_HTML_MODE_COMPACT)

            // 公式番組にはユーザーアイコンない
            if (supplier.has("icons")) {
                val userIcon = supplier.getJSONObject("icons").getString("uri150x150")
                // ユーザーアイコン
                fragment_program_info_broadcaster_imageview.imageTintList = null
                Glide.with(fragment_program_info_broadcaster_imageview)
                    .load(userIcon)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                    .into(fragment_program_info_broadcaster_imageview)
            }

            // コミュ
            val community = jsonObject.getJSONObject("socialGroup")
            val communityName = community.getString("name")
            val communityThumb = community.getString("thumbnailImageUrl")
            var communityLevel = 0
            // 公式番組にはlevelない
            if (community.has("level")) {
                communityLevel = community.getInt("level")
            }
            fragment_program_info_community_name.text = "${getString(R.string.community_name)} : $communityName"
            fragment_program_info_community_level.text = "${getString(R.string.community_level)} : $communityLevel"
            // コミュアイコン
            fragment_program_info_community_imageview.imageTintList = null
            Glide.with(fragment_program_info_community_imageview)
                .load(communityThumb)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(fragment_program_info_community_imageview)

            // コミュフォロー中？
            val isCommunityFollow =
                jsonObject.getJSONObject("socialGroup").getBoolean("isFollowed")
            if (isCommunityFollow) {
                // 押せないように
                fragment_program_info_community_follow_button.isEnabled = false
                fragment_program_info_community_follow_button.text =
                    getString(R.string.followed)
            }

            //たぐ
            val tag = jsonObject.getJSONObject("program").getJSONObject("tag")
            val isTagNotEditable = tag.getBoolean("isLocked") // タグ編集が可能か？
            val tagsList = tag.getJSONArray("list")
            if (tagsList.length() != 0) {
                activity?.runOnUiThread {
                    fragment_program_info_tag_linearlayout.removeAllViews()
                    for (i in 0 until tagsList.length()) {
                        val tag = tagsList.getJSONObject(i)
                        val text = tag.getString("text")
                        val isNicopedia = tag.getBoolean("existsNicopediaArticle")
                        //ボタン作成
                        val button = MaterialButton(requireContext())
                        button.text = text
                        button.isAllCaps = false
                        val nicopediaUrl =
                            tag.getString("nicopediaArticlePageUrl")
                        button.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, nicopediaUrl.toUri())
                            startActivity(intent)
                        }
                        fragment_program_info_tag_linearlayout.addView(button)
                    }
                }
            }
            // タグの登録に必要なトークンを取得
            tagToken = tag.getString("apiToken")
            // タグが変更できない場合はボタンをグレーアウトする
            if (isTagNotEditable) {
                fragment_program_info_tag_add_button.apply {
                    activity?.runOnUiThread {
                        isEnabled = false
                        text = getString(R.string.not_tag_editable)
                    }
                }
            }

            // タイムシフト予約済みか
            val userProgramReservation = jsonObject.getJSONObject("userProgramReservation")
            val isReserved = userProgramReservation.getBoolean("isReserved")

            // タイムシフト機能が使えない場合
            // JSONに programTimeshift と programTimeshiftWatch が存在しない場合はTS予約が無効にされている？
            // 存在すればTS予約が利用できる
            val canTSReserve =
                jsonObject.has("programTimeshift") && jsonObject.has("programTimeshiftWatch")
            activity?.runOnUiThread {
                if (fragment_program_info_timeshift_button == null) {
                    return@runOnUiThread
                }
                fragment_program_info_timeshift_button.isEnabled = canTSReserve
                if (!canTSReserve) {
                    fragment_program_info_timeshift_button.text =
                        getString(R.string.disabled_ts_reservation)
                }
            }
        }
    }


    // タグを取得する関数
    fun coroutineGetTag() {
        activity?.runOnUiThread {
            fragment_program_info_swipe.isRefreshing = true
        }
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            val nicoLiveTagAPI = NicoLiveTagAPI()
            val response = nicoLiveTagAPI.getTags(liveId, usersession)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // パース
            val list = withContext(Dispatchers.Default) {
                nicoLiveTagAPI.parseTags(response.body?.string())
            }
            // タグをUIに反映させる
            // 全部消す
            fragment_program_info_tag_linearlayout.removeAllViews()
            list.forEach {
                val text = it.title
                val isNicopedia = it.hasNicoPedia
                //ボタン作成
                val button = MaterialButton(requireContext())
                button.text = text
                button.isAllCaps = false
                val nicopediaUrl = it.nicoPediaUrl
                button.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, nicopediaUrl.toUri())
                    startActivity(intent)
                }
                fragment_program_info_tag_linearlayout.addView(button)
            }
            fragment_program_info_swipe.isRefreshing = false
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ユーザーをフォローする関数。
     * @param userId ユーザーID
     * @param response 成功時呼ばれます。UIスレッドではない。
     * */
    fun requestFollow(userId: String, response: (Response) -> Unit) {
        val request = Request.Builder().apply {
            url("https://public.api.nicovideo.jp/v1/user/followees/niconico-users/${userId}.json")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            // これがないと 200 が帰ってこない
            header(
                "X-Request-With",
                "https://live2.nicovideo.jp/watch/$liveId?_topic=live_user_program_onairs"
            )
            post("{}".toRequestBody()) // 送る内容は｛｝ていいらしい。
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val status = JSONObject(response.body?.string()).getJSONObject("meta").getInt("status")
                    if (status == 200) {
                        // 高階関数をよぶ
                        response(response)
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /**
     * コミュニティをフォローする関数。
     * @param communityId コミュニティーID coから
     * @param response 成功したら呼ばれます。
     * */
    fun requestCommunityFollow(communityId: String, response: (Response) -> Unit) {
        val formData = FormBody.Builder().apply {
            add("mode", "commit")
            add("title", "フォローリクエスト")
            add("comment", "")
            add("notify", "")
        }.build()
        val request = Request.Builder().apply {
            url("https://com.nicovideo.jp/motion/${communityId}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            // Referer これないと200が帰ってくる。（ほしいのは302 Found）
            header("Referer", "https://com.nicovideo.jp/motion/$communityId")
            post(formData) // これ送って何にするの？
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 302 Foundのとき成功？
                    response(response)
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /**
     * タイムシフト登録する。 overwriteってなんだ？
     * @param レスポンスが帰ってくれば
     * */
    fun registerTimeShift(successful: (Response) -> Unit) {
        val postFormData = FormBody.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            add("vid", liveId.replace("lv", ""))
            add("overwrite", "0")
        }.build()
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/api/timeshift.reservations")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            post(postFormData)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                successful(response)
            }
        })
    }

    /**
     * タイムシフトを削除する
     * @param successful 成功したら呼ばれます。
     * */
    fun deleteTimeShift(successful: (Response) -> Unit) {
        val request = Request.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            url("https://live.nicovideo.jp/api/timeshift.reservations?vid=${liveId.replace("lv", "")}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            delete()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    successful(response)
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }
}