package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat

/**
 * マイリストAPIを叩く。
 * こっちはスマホWebブラウザ版のAPI。token取得が不要で便利そう（小並感
 * */
class NicoVideoSPMyListAPI {

    /**
     * マイリスト一覧のAPIを叩く関数
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getMyListList(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/mylists")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * マイリスト一覧のAPIのレスポンスをパースする関数
     * @param responseString getMyListList()のレスポンス
     *
     * */
    suspend fun parseMyListList(responseString: String?) = withContext(Dispatchers.Default) {
        val myListListDataList = arrayListOf<MyListData>()
        val jsonObject = JSONObject(responseString)
        val mylists = jsonObject.getJSONObject("data").getJSONArray("mylists")
        for (i in 0 until mylists.length()) {
            val list = mylists.getJSONObject(i)
            val title = list.getString("name")
            val id = list.getString("id")
            val itemsCount = list.getInt("itemsCount")
            val data = MyListData(title, id, itemsCount)
            myListListDataList.add(data)
        }
        myListListDataList
    }

    /**
     * マイリストの中身APIを叩く関数。
     * @param myListId マイリストのID。MyListData#idで取得して
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス。パースはparseMyListItems()でできると思う
     * */
    suspend fun getMyListItems(myListId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/mylists/$myListId/items?pageSize=500")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * とりあえずマイリストの中身取得APIを叩く関数。
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス。パースはparseMyListItems()でできると思う
     * */
    suspend fun getToriaezuMyListList(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/deflist/items?pageSize=500")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * 他の人のマイリストを取得する。コルーチンです。100件取れます。
     * 注意：パースにはparseOtherUserMyListJSON()関数を使ってください。中身のJSONが若干違うみたいです。
     * @param id mylist/数字 の数字の部分だけ。
     * @param userSession ユーザーセッション。多分なくても（空文字でも）叩けるけど一応。
     * @param pageSize 省略時100件取れます。
     * */
    suspend fun getOtherUserMyListItems(id: String, userSession: String = "", pageSize: Int = 100) = withContext(Dispatchers.IO) {
        //とりあえずマイリストと普通のマイリスト。
        val url = "https://nvapi.nicovideo.jp/v2/mylists/$id?pageSize=$pageSize"
        val request = Request.Builder().apply {
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6")
            header("User-Agent", "TatimiDroid;@takusan_23")
            url(url)
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * マイリストの中身APIをパースする関数
     * @param responseString getMyListItems()のレスポンス
     * @param myListId マイリストのID。削除する時に使う。空文字の場合はとりあえずマイリストとして扱います。
     * @return NicoVideoDataの配列
     * */
    suspend fun parseMyListItems(myListId: String, responseString: String?) = withContext(Dispatchers.Default) {
        val videoItems = arrayListOf<NicoVideoData>()
        val items = JSONObject(responseString).getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val itemObject = items.getJSONObject(i)
            // マイリスト操作系
            val myListItemId = itemObject.getString("itemId")
            val myListAddedDate = toUnixTime(itemObject.getString("addedAt"))
            if (!itemObject.isNull("video")) {
                val videoObject = itemObject.getJSONObject("video")
                // 動画情報
                val videoId = videoObject.getString("id")
                val title = videoObject.getString("title")
                val date = toUnixTime(videoObject.getString("registeredAt"))
                val duration = videoObject.getLong("duration")
                // 再生数等
                val countObject = videoObject.getJSONObject("count")
                val viewCount = countObject.getString("view")
                val commentCount = countObject.getString("comment")
                val mylistCount = countObject.getString("mylist")
                // さむね
                val thum = if (videoObject.getJSONObject("thumbnail").isNull("largeUrl")) {
                    videoObject.getJSONObject("thumbnail").getString("url")
                } else {
                    videoObject.getJSONObject("thumbnail").getString("largeUrl")
                }
                val data = NicoVideoData(
                    isCache = false,
                    isMylist = true,
                    title = title,
                    videoId = videoId,
                    thum = thum,
                    date = date,
                    viewCount = viewCount,
                    commentCount = commentCount,
                    mylistCount = mylistCount,
                    mylistItemId = myListItemId,
                    mylistAddedDate = myListAddedDate,
                    duration = duration,
                    cacheAddedDate = null,
                    uploaderName = null,
                    isToriaezuMylist = myListId.isEmpty(),
                    mylistId = myListId
                )
                videoItems.add(data)
            }
        }
        videoItems
    }

    /**
     * 他の人のマイリストのJSONをパースする。
     * @param json getOtherUserMylistItems()の戻り値
     * @return NicoVideoData配列
     * */
    suspend fun parseOtherUserMyListJSON(json: String?) = withContext(Dispatchers.Default) {
        val myListList = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(json)
        val myListItem =
            jsonObject.getJSONObject("data").getJSONObject("mylist").getJSONArray("items")
        for (i in 0 until myListItem.length()) {
            val video = myListItem.getJSONObject(i)
            val itemId = video.getString("itemId")
            val videoObject = video.getJSONObject("video")
            val title = videoObject.getString("title")
            val videoId = videoObject.getString("id")
            val thum = videoObject.getJSONObject("thumbnail").getString("url")
            val date = toUnixTime(videoObject.getString("registeredAt"))
            val countObject = videoObject.getJSONObject("count")
            val viewCount = countObject.getString("view")
            val commentCount = countObject.getString("comment")
            val mylistCount = countObject.getString("mylist")
            val addedAt = toUnixTime(video.getString("addedAt"))
            val duration = videoObject.getLong("duration")
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = title,
                videoId = videoId,
                thum = thum,
                date = date,
                viewCount = viewCount,
                commentCount = commentCount,
                mylistCount = mylistCount,
                mylistItemId = itemId,
                mylistAddedDate = addedAt,
                duration = duration,
                cacheAddedDate = null
            )
            myListList.add(data)
        }
        myListList
    }

    /**
     * マイリストから動画を削除するAPIを叩く関数。
     * @param myListId マイリストのID。
     * @param itemId アイテムID（NicoVideoData#mylistItemId）。動画IDではないので注意
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス
     * */
    suspend fun deleteMyListVideo(myListId: String, itemId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/mylists/$myListId/items?itemIds=$itemId")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            header("x-request-with", "nicovideo")
            delete()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * とりあえずマイリストから動画を削除するAPIを叩く関数
     * @param itemId itemId アイテムID（NicoVideoData#mylistItemId）。動画IDではないので注意
     * @param userSession ユーザーセッション
     * @return okHttpのレスポンス
     * */
    suspend fun deleteToriaezuMyListVideo(itemId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/deflist/items?itemIds=$itemId")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3")
            header("x-request-with", "nicovideo")
            delete()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

    data class MyListData(val title: String, val id: String, val itemsCount: Int) : Serializable

}