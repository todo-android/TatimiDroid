package io.github.takusan23.tatimidroid.NicoAPI

import java.io.Serializable
import java.time.Duration

/**
 * 動画タイトル、動画ID、サムネとか
 * */
data class NicoVideoData(
    val isCache: Boolean, // キャッシュならtrue
    val isMylist: Boolean, // マイリストならtrue
    val title: String,
    val videoId: String,
    val thum: String,
    val date: Long,
    val viewCount: String,
    val commentCount: String,
    val mylistCount: String,
    val mylistItemId: String,// マイリストのitem_idの値。マイリスト以外は空文字。
    val mylistAddedDate: Long?,// マイリストの追加日時。マイリスト以外はnull可能
    val duration: Long?,// 再生時間（秒）。
    val cacheAddedDate: Long?,// キャッシュ取得日時。キャッシュ以外ではnullいいよ
    val uploaderName: String? = null, // キャッシュ再生で使うからキャッシュ以外はnull
    val videoTag: ArrayList<String>? = arrayListOf() // キャッシュ再生で使うからキャッシュ以外は省略していいよ
) : Serializable
