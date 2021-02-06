package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R

/**
 * 統計情報（来場者、コメント投稿数、アクティブ人数）を表示するためのUI
 *
 * @param allViewer 累計来場者
 * @param allCommentCount コメント数
 * @param activeCountText 一分間のコメント数
 * @param onClickRoomChange 部屋別表示に切り替え押した時
 * @param onClickActiveCalc アクティブ人数計算ボタン押した時
 * */
@Composable
fun NicoLiveStatisticsUI(
    allViewer: Int,
    allCommentCount: Int,
    activeCountText: String,
    onClickRoomChange: () -> Unit,
    onClickActiveCalc: () -> Unit,
) {
    Row {
        // 来場者
        Row(
            modifier = Modifier
                .padding(5.dp, 0.dp, 5.dp, 0.dp)
                .align(Alignment.CenterVertically)
                .weight(1f)
        ) {
            Icon(
                imageVector = vectorResource(id = R.drawable.ic_outline_people_outline_24px),
                contentDescription = "累計来場者",
            )
            Text(
                text = allViewer.toString(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // コメント数
        Row(
            modifier = Modifier
                .padding(5.dp, 0.dp, 5.dp, 0.dp)
                .align(Alignment.CenterVertically)
                .weight(1f)
        ) {
            Icon(
                imageVector = vectorResource(id = R.drawable.ic_outline_comment_24px),
                contentDescription = stringResource(id = R.string.comment_count),
            )
            Text(
                text = allCommentCount.toString(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // アクティブ人数
        Row(
            modifier = Modifier
                .padding(5.dp, 0.dp, 5.dp, 0.dp)
                .align(Alignment.CenterVertically)
                .weight(1f)
        ) {
            Icon(
                imageVector = vectorResource(id = R.drawable.ic_active_icon),
                contentDescription = stringResource(id = R.string.active),
            )
            Text(
                text = activeCountText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // アクティブ人数計算ボタン
        IconButton(onClick = { onClickActiveCalc() }) {
            Icon(
                imageVector = vectorResource(id = R.drawable.ic_timeline_black_24dp),
                contentDescription = "アクティブ人数計算"
            )
        }
        // 部屋切り替えボタン
        IconButton(onClick = { onClickRoomChange() }) {
            Icon(
                imageVector = vectorResource(id = R.drawable.ic_outline_meeting_room_24px),
                stringResource(id = R.string.room_comment)
            )
        }
    }
}