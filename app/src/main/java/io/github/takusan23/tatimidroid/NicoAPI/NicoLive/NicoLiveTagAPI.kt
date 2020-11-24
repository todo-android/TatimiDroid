package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * ニコ生のタグを取得する関数
 * コルーチンで呼んでね
 * */
class NicoLiveTagAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

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
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * getTags()のレスポンスをパースする関数
     * @param responseString getTags()のレスポンス
     * @return NicoLiveTagItemDataの配列
     * */
    suspend fun parseTags(responseString: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoLiveTagDataClass>()
        val tags = JSONObject(responseString).getJSONObject("data").getJSONArray("tags")
        for (i in 0 until tags.length()) {
            val tagObject = tags.getJSONObject(i)
            val title = tagObject.getString("tag")
            val isLocked = tagObject.getBoolean("isLocked")
            val type = tagObject.getString("type")
            val isDeletable = tagObject.getBoolean("isDeletable")
            val hasNicoPedia = tagObject.getBoolean("hasNicopedia")
            val nicoPediaUrl = tagObject.getString("nicopediaUrl")
            val data = NicoLiveTagDataClass(title, isLocked, type, isDeletable, hasNicoPedia, nicoPediaUrl)
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
        // なんかAPI変わってトークンが要らなくなってJSONで送信するように
        val sendJSON = JSONObject().apply {
            put("tag", tagName)
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/unama/api/v2/programs/$liveId/livetags")
            header("Cookie", "user_session=$userSession")
            // なんかログイン情報を渡す方法もCookieからヘッダーに付ける方式に変わってる
            header("X-niconico-session", userSession)
            header("User-Agent", "TatimiDroid;@takusan_23")
            put(sendJSON) // 追加はput
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * タグを削除する。
     * @param liveId 番組ID
     * @param tagName 削除するタグ名
     * @param userSession ユーザーセッション
     * */
    suspend fun deleteTag(liveId: String, userSession: String, tagName: String) = withContext(Dispatchers.IO) {
        // なんかAPI変わってトークンが要らなくなってJSONで送信するように
        val sendJSON = JSONObject().apply {
            put("tag", tagName)
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/unama/api/v2/programs/$liveId/livetags")
            header("Cookie", "user_session=$userSession")
            // なんかログイン情報を渡す方法もCookieからヘッダーに付ける方式に変わってる
            header("X-niconico-session", userSession)
            header("User-Agent", "TatimiDroid;@takusan_23")
            delete(sendJSON) // 削除はdelete
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

}