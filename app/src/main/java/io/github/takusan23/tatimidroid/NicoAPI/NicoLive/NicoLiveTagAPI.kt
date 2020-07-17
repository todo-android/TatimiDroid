package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

/**
 * ニコ生のタグを取得する関数
 * コルーチンで呼んでね
 * */
class NicoLiveTagAPI {

    /**
     * タグを返すAPIを叩く関数
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun getTags(liveId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://papi.live.nicovideo.jp/api/relive/livetag/$liveId")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * getTags()のレスポンスをパースする関数
     * @param responseString getTags()のレスポンス
     * @return NicoLiveTagItemDataの配列
     * */
    suspend fun parseTags(responseString: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoLiveTagItemData>()
        val tags = JSONObject(responseString).getJSONObject("data").getJSONArray("tags")
        for (i in 0 until tags.length()) {
            val tagObject = tags.getJSONObject(i)
            val title = tagObject.getString("tag")
            val isLocked = tagObject.getBoolean("isLocked")
            val isDeletable = tagObject.getBoolean("isDeletable")
            val hasNicoPedia = tagObject.getBoolean("hasNicopedia")
            val nicoPediaUrl = tagObject.getString("nicopediaUrl")
            val data = NicoLiveTagItemData(title, isLocked, isDeletable, hasNicoPedia, nicoPediaUrl)
            list.add(data)
        }
        list
    }

    /**
     * タグを追加する。コルーチン
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * @param token タグ操作トークン
     * @param tagName 追加するタグの名前
     * */
    suspend fun addTag(liveId: String, userSession: String, token: String, tagName: String) = withContext(Dispatchers.IO) {
        val sendData = FormBody.Builder().apply {
            add("tag", tagName)
            add("token", token)
        }.build()
        val request = Request.Builder().apply {
            url("https://papi.live.nicovideo.jp/api/relive/livetag/$liveId/?_method=PUT")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            post(sendData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    // タグのデータクラス
    data class NicoLiveTagItemData(
        val title: String,
        val isLocked: Boolean,
        val isDeletable: Boolean,
        val hasNicoPedia: Boolean,
        val nicoPediaUrl: String
    )

}