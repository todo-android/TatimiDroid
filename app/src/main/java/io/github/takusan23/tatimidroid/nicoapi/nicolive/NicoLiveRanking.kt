package io.github.takusan23.tatimidroid.nicoapi.nicolive

import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder

/**
 * ニコ生のランキングを取得する。最近までヘッダーにランキングがあることに気付かなかった。
 * なおPC版ではなくスマホ版のサイトから取得している。(スマホ版はJSONがHTMLの中にある。PC版は無いので)
 * */
class NicoLiveRanking {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ニコ生のランキングサイト（スマホ版）のHTMLを取得する関数
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getRankingHTML() = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://sp.live.nicovideo.jp/ranking")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getRankingHTML]のHTMLからJSONを取ってきてパースする関数。
     * @param html [getRankingHTML]のレスポンス。response#body#string()
     * @return ProgramDataの配列
     * */
    suspend fun parseJSON(html: String?) = withContext(Dispatchers.Default) {
        // ProgramDataの配列
        val dataList = arrayListOf<NicoLiveProgramData>()
        val document = Jsoup.parse(html)
        // JSONっぽいのがあるので取り出す
        val json = document.getElementsByTag("script")[5]
        var json_string = URLDecoder.decode(json.html(), "utf-8")
        // いらない部分消す
        json_string = json_string.replace("window.__initial_state__ = \"", "")
        json_string = json_string.replace("window.__public_path__ = \"https://nicolive.cdn.nimg.jp/relive/sp/\";", "")
        json_string = json_string.replace("\";", "")
        val jsonObject = JSONObject(json_string)
        // JSON解析
        val programs = jsonObject.getJSONObject("pageContents").getJSONObject("ranking")
            .getJSONObject("rankingPrograms")
            .getJSONArray("rankingPrograms")
        for (i in 0 until programs.length()) {
            val jsonObject = programs.getJSONObject(i)
            val programId = jsonObject.getString("id")
            val title = jsonObject.getString("title")
            val beginAt = jsonObject.getString("beginAt")
            // val endAt = jsonObject.getString("endAt")
            val communityName = jsonObject.getString("socialGroupName")
            val liveNow = jsonObject.getString("status") //放送中か？
            val rank = jsonObject.getString("rank")
            val thum = jsonObject.getString("thumbnailUrl")
            // データクラス
            val data =
                NicoLiveProgramData(title, communityName, beginAt, beginAt, programId, "", liveNow, thum)
            dataList.add(data)
        }
        dataList
    }

}