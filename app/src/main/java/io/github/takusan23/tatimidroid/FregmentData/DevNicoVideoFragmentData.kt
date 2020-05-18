package io.github.takusan23.tatimidroid.FregmentData

import io.github.takusan23.tatimidroid.Adapter.NicoHistoryAdapter
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import java.io.Serializable

/**
 * DevNicoVideoFragmentの画面回転でも保持してほしい値のデータクラス。
 * */
data class DevNicoVideoFragmentData(
    /** キャッシュ再生時はtrue */
    val isCachePlay: Boolean = false,
    /** 動画URLか動画Fileパス（キャッシュ再生時） */
    val contentUrl: String,
    /** nicoHistory。Smileサーバーの動画再生時にリクエストヘッダーにくっつける。いまほぼDMC鯖から配信されてるので多分使わなくなる（というかそもそも使ってない説有る） */
    val nicoHistory: String,
    /** コメントの配列 */
    val commentList: ArrayList<CommentJSONParse>,
    /** 再生時の位置。 */
    val currentPos: Long,
    /** data-api-dataの値。キャッシュ再生時でなお動画情報がないときはnullおけ */
    val dataApiData: String?,
    /** ハートビートPOSTのときに使うJSON文字列。キャッシュ再生時は無いのでnullおっけー */
    val sessionAPIJSONObject: String?,
    /** ニコるくん。キャッシュ再生時はnullでいいよ */
    val nicoruKey: String?,
    /** 関連動画。キャッシュ再生時はからの配列だと思う */
    val recommendList: ArrayList<NicoVideoData>
) : Serializable