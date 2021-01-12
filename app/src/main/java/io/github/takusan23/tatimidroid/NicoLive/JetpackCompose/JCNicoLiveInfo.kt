package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.CommunityOrChannelData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.getBitmapCompose
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.parentCardElevation
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.parentCardModifier
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.parentCardShape
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.toFormatTime

/**
 * 番組情報を表示するCard
 * @param nicoLiveProgramData 番組情報データクラス
 * @param programDescription 番組説明文
 * */
@Composable
fun NicoLiveInfoCard(
    nicoLiveProgramData: NicoLiveProgramData,
    programDescription: String
) {
    // 動画説明文表示状態
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            // 番組開始、終了時刻
            Row {
                Icon(
                    imageVector = Icons.Outlined.MeetingRoom
                )
                Text(
                    text = "${stringResource(id = R.string.nicolive_begin_time)}：${toFormatTime(nicoLiveProgramData.beginAt.toLong() * 1000)}",
                )
            }
            Row {
                Icon(
                    imageVector = Icons.Outlined.NoMeetingRoom
                )
                Text(
                    text = "${stringResource(id = R.string.nicolive_end_time)}：${toFormatTime(nicoLiveProgramData.endAt.toLong() * 1000)}",
                )
            }
            // 区切り線
            Divider(modifier = Modifier.padding(5.dp))
            // 真ん中にする
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    // タイトル
                    Text(
                        text = nicoLiveProgramData.title,
                        style = TextStyle(fontSize = 18.sp),
                        maxLines = 2,
                    )
                    // 生放送ID
                    Text(
                        text = nicoLiveProgramData.programId,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
                // 展開ボタン。動画説明文の表示を切り替える
                IconButton(onClick = { expanded = !expanded }) {
                    // アイコンコード一行で召喚できる。Node.jsのnpmのmdiみたいだな！
                    Icon(imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore)
                }
            }
            // 詳細表示
            if (expanded) {
                Column {
                    // 区切り線
                    Divider(modifier = Modifier.padding(5.dp))
                    /** 多分HTMLを表示する機能はないので従来のTextView登場 */
                    AndroidView(viewBlock = { context ->
                        TextView(context).apply {
                            // リンク押せるように
                            text = HtmlCompat.fromHtml(programDescription, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        }
                    })
                }
            }
        }
    }
}

/**
 * コミュニティー情報表示Card
 *
 * @param communityOrChannelData コミュ、番組情報
 * @param onCommunityOpenClick コミュ情報押した時に呼ばれる
 * @param isFollow フォロー中かどうか
 * @param onFollowClick フォロー押した時
 * */
@Composable
fun NicoLiveCommunityCard(
    communityOrChannelData: CommunityOrChannelData,
    onCommunityOpenClick: () -> Unit,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.SupervisorAccount)
                Text(text = stringResource(id = R.string.community_name))
            }
            Divider(modifier = Modifier.padding(5.dp))
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bitmap = getBitmapCompose(url = communityOrChannelData.icon)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    )
                }
                Text(
                    text = communityOrChannelData.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)
                )
            }
            Divider(modifier = Modifier.padding(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // コミュだけフォローボタンを出す
                if (!communityOrChannelData.isChannel) {
                    TextButton(modifier = Modifier.padding(3.dp), onClick = { onFollowClick() }) {
                        if (isFollow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.PersonRemove)
                                Text(text = stringResource(id = R.string.nicovideo_account_remove_follow))
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.StarBorder)
                                Text(text = stringResource(id = R.string.community_follow))
                            }
                        }
                    }
                }
                TextButton(modifier = Modifier.padding(3.dp), onClick = { onCommunityOpenClick() }) {
                    Icon(imageVector = Icons.Outlined.OpenInBrowser)
                }
            }
        }
    }
}

/**
 * タグ表示Card。動画とは互換性がない（データクラスが違うの）
 * @param list [NicoLiveTagDataClass]の配列
 * @param onTagClick タグを押した時
 * @param
 * */
@Composable
fun NicoLiveTagCard(
    list: ArrayList<NicoLiveTagDataClass>,
    onTagClick: (NicoLiveTagDataClass) -> Unit,
    isEditable: Boolean,
    onEditClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(
            modifier = parentCardModifier.weight(1f),
            shape = parentCardShape,
            elevation = parentCardElevation,
        ) {
            // 横方向スクロール。LazyRowでRecyclerViewみたいに画面外は描画しない
            LazyRow(
                modifier = Modifier.padding(3.dp),
                content = {
                    this.item {
                        // 編集ボタン
                        if (isEditable) {
                            Button(
                                modifier = Modifier.padding(3.dp),
                                onClick = { onEditClick() }
                            ) {
                                Icon(imageVector = Icons.Outlined.Edit)
                                Text(text = stringResource(id = R.string.tag_edit))
                            }
                        }
                    }
                    this.items(list) { data ->
                        OutlinedButton(
                            modifier = Modifier.padding(3.dp),
                            onClick = {
                                onTagClick(data)
                            },
                        ) {
                            Icon(imageVector = Icons.Outlined.LocalOffer)
                            Text(text = data.tagName)
                        }
                    }
                }
            )
        }
    }
}
