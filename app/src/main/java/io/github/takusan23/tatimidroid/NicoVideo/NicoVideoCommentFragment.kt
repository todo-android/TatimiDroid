package io.github.takusan23.tatimidroid.NicoVideo

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.adapter_auto_admission_layout.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class NicoVideoCommentFragment : Fragment() {

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nicoVideoAdapter: NicoVideoAdapter

    lateinit var pref_setting: SharedPreferences

    var usersession = ""

    var id = "sm157"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //動画ID受け取る（sm9とかsm157とか）
        id = arguments?.getString("id") ?: "sm157"

        usersession = pref_setting.getString("user_session", "") ?: ""

        //RecyclerView初期化
        initRecyclerView()

        // スクレイピングしてコメント取得に必要な情報を取得する
        getNicoVideoWebPage()

    }

    private fun initRecyclerView() {
        activity_nicovideo_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        activity_nicovideo_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        nicoVideoAdapter = NicoVideoAdapter(recyclerViewList)
        activity_nicovideo_recyclerview.adapter = nicoVideoAdapter
    }

    /*
    *
    * https://nmsg.nicovideo.jp/api.json/ を叩く時に必要な情報をスクレイピングして取得
    * これで動くと思ってたんだけど「dmcInfo」がない動画が存在するんだけどどういう事？。
    *     */
    fun getNicoVideoWebPage() {
        //番組ID
        //視聴モード（ユーザーセッション付き）
        val request = Request.Builder()
            .url("https://www.nicovideo.jp/watch/${id}")
            .header("Cookie", "user_session=${usersession}")
            .header("User-Agent", "TatimiDroid;@takusan_23")
            .get()
            .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response_string = response.body?.string()
                    //HTML解析
                    val html = Jsoup.parse(response_string)
                    //謎のJSON取得
                    //この部分長すぎてChromeだとうまくコピーできないんだけど、Edgeだと完璧にコピーできたぞ！
                    if (html.getElementById("js-initial-watch-data") != null) {
                        val data_api_data = html.getElementById("js-initial-watch-data")
                        val json = JSONObject(data_api_data.attr("data-api-data"))
                        //userkey
                        val userkey = json.getJSONObject("context").getString("userkey")
                        //user_id
                        val user_id = json.getJSONObject("video").getJSONObject("dmcInfo")
                            .getJSONObject("user").getString("user_id")
                        //length(再生時間
                        val length = json.getJSONObject("video").getJSONObject("dmcInfo")
                            .getJSONObject("video").getString("length_seconds").toInt()
                        //必要なのは「分」なので割る
                        //そして分に+1している模様
                        //一時間超えでも分を使う模様？66みたいに
                        val minute = (length / 60) + 1
                        //contentつくる。1000が限界？
                        val content = "0-${minute}:100,1000,nicoru:100"

                        /*
                        * JSONの構成を指示してくれるJSONArray
                        * threads[]の中になんのJSONを作ればいいかが書いてある。
                        * */
                        val commentComposite =
                            json.getJSONObject("commentComposite").getJSONArray("threads")
                        //投げるJSON
                        val postJSONArray = JSONArray()
                        for (i in 0 until commentComposite.length()) {
                            val thread = commentComposite.getJSONObject(i)
                            val thread_id = thread.getString("id")  //thread まじでなんでこの管理方法にしたんだ運営・・
                            val fork = thread.getInt("fork")    //わからん。
                            val isOwnerThread = thread.getBoolean("isOwnerThread")   //
                            //threads[]のJSON配列の中に「isActive」がtrueなら次のJSONを作成します
                            if (thread.getBoolean("isActive")) {
                                val jsonObject = JSONObject().apply {
                                    //投稿者コメントも見れるように。「isOwnerThread」がtrueの場合は「version」を20061206にする？
                                    if (isOwnerThread) {
                                        put("version", "20061206")
                                        put("res_from", -1000)
                                    } else {
                                        put("version", "20090904")
                                    }
                                    put("thread", thread_id)
                                    put("fork", fork)
                                    put("language", 0)
                                    put("user_id", user_id)
                                    put("with_global", 1)
                                    put("score", 1)
                                    put("nicoru", 3)
                                    put("userkey", userkey)
                                }
                                val post_thread = JSONObject().apply {
                                    put("thread", jsonObject)
                                }
                                postJSONArray.put(post_thread)
                            }
                            //threads[]のJSON配列の中に「isLeafRequired」がtrueなら次のJSONを作成します
                            if (thread.getBoolean("isLeafRequired")) {
                                val jsonObject = JSONObject().apply {
                                    put("thread", thread_id)
                                    put("language", 0)
                                    put("user_id", user_id)
                                    put("content", content)
                                    put("scores", 0)
                                    put("nicoru", 3)
                                    put("userkey", userkey)
                                }
                                val thread_leaves = JSONObject().apply {
                                    put("thread_leaves", jsonObject)
                                }
                                postJSONArray.put(thread_leaves)
                            }
                        }
                        //POST実行
                        // println(postJSONArray)
                        postNicoVideoCommentAPI(postJSONArray)
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    fun postNicoVideoCommentAPI(jsonArray: JSONArray) {
        val request = Request.Builder()
            .url("https://nmsg.nicovideo.jp/api.json/")
            .header("Cookie", "user_session=${usersession}")
            .post(jsonArray.toString().toRequestBody())
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                val response_string = response.body?.string()
                val jsonArray = JSONArray(response_string)

                //RecyclerViewに入れる配列には並び替えをしてから入れるのでそれまで一時的に入れておく配列
                val commentListList = arrayListOf<ArrayList<String>>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    if (jsonObject.has("chat")) {
                        val chat = jsonObject.getJSONObject("chat")
                        if (chat.has("content")) {
                            val comment = chat.getString("content")
                            val user_id = ""
                            val date = chat.getString("date")
                            val vpos = chat.getString("vpos")
                            val item = arrayListOf<String>()
                            item.add("")
                            item.add(user_id)
                            item.add(comment)
                            item.add(date)
                            item.add(vpos)
                            commentListList.add(item)
                        }
                    }
                }

                /*
                * コメントを動画再生と同じ用に並び替える。vposを若い順にする。
                * vposだけの配列だと「.sorted()」が使えるんだけど、今回は配列に配列があるので無理です。
                * 数字だけ（vposだけ）だと[0,10,30,20] -> sorted()後 [0,10,20,30]
                *
                * でも今回は配列の中のにある配列の4番目の値で並び替えてほしいのです。
                * そこで「sortedBy{}」を使い、4番目の値で並び替えろと書いています。
                * arrayListは配列の中の配列がそうみたい。printlnして確認した。forEach的な
                *
                * */
                commentListList.sortedBy { arrayList -> arrayList[4].toInt() }
                    .forEach {
                        recyclerViewList.add(it)
                    }


                activity?.runOnUiThread {
                    nicoVideoAdapter.notifyDataSetChanged()
                    Snackbar.make(
                        activity_nicovideo_recyclerview,
                        "取得済みコメント：${recyclerViewList.size}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}