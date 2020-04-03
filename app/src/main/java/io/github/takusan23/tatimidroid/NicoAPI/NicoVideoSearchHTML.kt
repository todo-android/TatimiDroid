package io.github.takusan23.tatimidroid.NicoAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.text.SimpleDateFormat

/**
 * ニコ動の検索結果をスクレイピングする
 * */
class NicoVideoSearchHTML {

    /**
     * 検索結果のHTMLを返す。コルーチン
     * @param searchText 検索する文字列
     * @param userSession ユーザーセッション
     * @param tagOrSearch "tag" か "search" のどちらか。"tag"はタグ検索。"search"はキーワード検索。
     * @param order "d" か "a" のどちらか。 "d"は新しい順。 "a"は古い順。
     * @param page ページ数
     * @param sort ↓これ見て
     *              h 人気の高い順
     *              f 投稿日時
     *              v 再生回数
     *              m マイリスト数
     *              n コメント数
     * */
    fun getHTML(userSession: String, searchText: String, tagOrSearch: String, sort: String, order: String, page: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://www.nicovideo.jp/$tagOrSearch/$searchText?page=$page&sort=$sort&order=$order")
                addHeader("Cookie", "user_session=$userSession")
                addHeader("User-Agent", "TatimiDroid;@takusan_23")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * HTMLをパースする
     * */
    fun parseHTML(html: String?): ArrayList<NicoVideoData> {
        val list = arrayListOf<NicoVideoData>()
        val document = Jsoup.parse(html)
        val li = document.getElementsByTag("li")
        li.forEach {
            // ニコニ広告はのせない？
            if (it.attr("data-video-id").isNotEmpty()) {
                val title = it.getElementsByClass("itemTitle")[0].text()
                val videoId = it.attr("data-video-id")
                val thum = it.getElementsByTag("img")[0].attr("src")
                val date = toUnixTime(it.getElementsByClass("time")[0].text())
                val viewCount =
                    it.getElementsByClass("count view")[0].getElementsByClass("value")[0].text()
                val commentCount =
                    it.getElementsByClass("count comment")[0].getElementsByClass("value")[0].text()
                val mylistCount =
                    it.getElementsByClass("count mylist")[0].getElementsByClass("value")[0].text()
                val isCache = false
                val data = NicoVideoData(isCache, title, videoId, thum, date, viewCount, commentCount, mylistCount)
                list.add(data)
            }
        }
        return list
    }

    // 投稿時間をUnixTimeへ変換
    fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
        return simpleDateFormat.parse(time).time
    }

}