package io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass

/**
 * 投げ銭のギフト履歴APIのデータクラス
 *
 * @param advertiserName 投げた人の名前
 * @param userId 投げた人のユーザーID
 * @param adPoint 投げた額
 * @param itemName 投げたアイテムの名前
 * @param itemThumbUrl 投げたアイテムの画像
 * */
data class NicoLiveGiftHistoryData(
    val advertiserName: String,
    val userId: Int,
    val adPoint: Int,
    val itemName: String,
    val itemThumbUrl: String,
)