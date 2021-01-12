package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoLikeBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoAccountFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoMyListListFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSearchFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSeriesFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.NicoVideoDescriptionText
import io.github.takusan23.tatimidroid.Tool.isDarkMode

/**
 * 動画情報Fragment。Jetpack Composeでレイアウトを作っている。Jetpack Composeたのし～～～。なんで動いてんのかよく知らんけど
 *
 * [io.github.takusan23.tatimidroid.BottomSheetPlayerBehavior.isDraggableAreaPlayerOnly]がtrueじゃないとうまくスクロールできない。
 *
 * [JCNicoVideoFragment]のViewModelを利用している。
 * */
class JCNicoVideoInfoFragment : Fragment() {

    /** [JCNicoVideoFragment]のViewModelを取得する */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            // Jetpack Compose
            setContent {
                MaterialTheme(
                    // ダークモード。動的にテーマ変更できるようになるんか？
                    colors = if (isDarkMode(AmbientContext.current)) DarkColors else LightColors,
                ) {
                    Surface {

                        // Snackbar表示で使う
                        val state = rememberScaffoldState()
                        val scope = rememberCoroutineScope()

                        Scaffold(
                            scaffoldState = state,
                            floatingActionButton = {
/*
                                // コメント一覧表示Fab
                                NicoVideoCommentListFab(
                                    isShowCommentList = isShowCommentList.value,
                                    click = { viewModel.commentListShowLiveData.postValue(!isShowCommentList.value) }
                                )
*/
                            }
                        ) {

                            // LiveDataをJetpack Composeで利用できるように
                            val data = viewModel.nicoVideoData.observeAsState()
                            // いいね状態
                            val isLiked = viewModel.isLikedLiveData.observeAsState(initial = false)
                            // 動画情報
                            val descroption = viewModel.nicoVideoDescriptionLiveData.observeAsState(initial = "")
                            // 関連動画
                            val recommendList = viewModel.recommendList.observeAsState()
                            // ユーザー情報
                            val userData = viewModel.userDataLiveData.observeAsState()
                            // タグ一覧
                            val tagList = viewModel.tagListLiveData.observeAsState()
                            // シリーズ
                            val seriesLiveData = viewModel.seriesDataLiveData.observeAsState()

                            // 連続再生
                            val playlist = viewModel.playlistLiveData.observeAsState()
                            // 連続再生？
                            val isPlaylistMode = viewModel.isPlayListMode.observeAsState(initial = false)
                            // 連続再生で現在再生中
                            val playingVideoId = viewModel.playingVideoId.observeAsState(initial = "")
                            // 連続再生、シャッフル有効？
                            val isShuffleMode = viewModel.isShuffled.observeAsState(initial = false)
                            // 連続再生、逆順？
                            val isReverseMode = viewModel.isReversed.observeAsState(initial = false)

                            Column {

                                // 連続再生
                                val isPlaylistShow = remember { mutableStateOf(false) }
                                if (isPlaylistMode.value) {
                                    NicoVideoPlayList(
                                        isShowList = isPlaylistShow.value,
                                        playingVideoId = playingVideoId.value,
                                        videoList = playlist.value!!,
                                        showButtonClick = { isPlaylistShow.value = !isPlaylistShow.value },
                                        videoClick = { videoId -> viewModel.playlistGoto(videoId) },
                                        isReverse = isReverseMode.value,
                                        isShuffle = isShuffleMode.value,
                                        reverseClick = {
                                            viewModel.isReversed.postValue(!isReverseMode.value)
                                            viewModel.setPlaylistReverse()
                                        },
                                        shuffleClick = {
                                            viewModel.setPlaylistShuffle(viewModel.isShuffled.value!!)
                                            viewModel.isShuffled.postValue(!isShuffleMode.value)
                                        }
                                    )
                                }

                                // スクロールできるやつ
                                ScrollableColumn {
                                    if (data.value != null) {
                                        // 動画情報表示Card
                                        NicoVideoInfoCard(
                                            nicoVideoData = data.value,
                                            isLiked = isLiked.value,
                                            onLikeClick = {
                                                if (isLiked.value) {
                                                    // いいね解除
                                                    removeLike()
                                                    viewModel.removeLike()
                                                } else {
                                                    // いいね登録
                                                    NicoVideoLikeBottomFragment().show(parentFragmentManager, "like")
                                                }
                                            },
                                            isOffline = viewModel.isOfflinePlay.value ?: false,
                                            scaffoldState = state,
                                            description = descroption.value,
                                            descriptionClick = { link, type ->
                                                // 押した時
                                                descriptionClick(type, link)
                                            }
                                        )
                                    }

                                    // シリーズ
                                    if (seriesLiveData.value != null) {
                                        NicoVideoSeriesCard(
                                            nicoVideoSeriesData = seriesLiveData.value!!,
                                            startSeriesPlay = {
                                                // シリーズ連続再生押した時
                                                viewModel.addSeriesPlaylist(seriesId = seriesLiveData.value!!.seriesId)
                                            }
                                        )
                                    }

                                    // タグ
                                    if (tagList.value != null) {
                                        NicoVideoTagCard(
                                            tagDataList = tagList.value!!,
                                            onTagClick = { data ->
                                                // タグ押した時
                                                setTagSearchFragment(data.tagName)
                                            }
                                        )
                                    }
                                    // ユーザー情報
                                    if (userData.value != null) {
                                        NicoVideoUserCard(
                                            userData = userData.value!!,
                                            onUserOpenClick = {
                                                setAccountFragment(userData.value!!.userId.toString())
                                            }
                                        )
                                    }

                                    // メニューカード。長いのでまとめた
                                    NicoVideoMenuScreen(requireParentFragment())

                                    // 関連動画表示Card
                                    if (recommendList.value != null) {
                                        NicoVideoRecommendCard(recommendList.value!!)
                                    }
                                }
                            }

                        }

                    }
                }
            }
        }
    }

    private fun removeLike() {
        // どうにかしたい
        val view = (requireParentFragment() as? JCNicoVideoFragment)?.bottomComposeView
        if (view != null) {
            com.google.android.material.snackbar.Snackbar.make(view, getString(R.string.unlike), com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE).apply {
                setAction(getString(R.string.torikesu)) {
                    viewModel.removeLike()
                }
                anchorView = view
            }.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Snackbar表示。使い方合ってんのかはしらんけど
        viewModel.likeThanksMessageLiveData.observe(viewLifecycleOwner) {
            com.google.android.material.snackbar.Snackbar.make(view, it, com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE).apply {
                setAction(getString(R.string.close)) {
                    dismiss()
                }
                view.elevation = 30f
                anchorView = (requireParentFragment() as? JCNicoVideoFragment)?.bottomComposeView
            }.show()
        }
    }

    /**
     * 動画説明文押したときに呼ばれる
     *
     * @param link [io.github.takusan23.tatimidroid.Tool.NicoVideoDescriptionText.DESCRIPTION_TYPE_NICOVIDEO] 参照
     * @param type [io.github.takusan23.tatimidroid.Tool.NicoVideoDescriptionText.DESCRIPTION_TYPE_NICOVIDEO] など
     * */
    private fun descriptionClick(type: String, link: String) {
        when (type) {
            NicoVideoDescriptionText.DESCRIPTION_TYPE_URL -> {
                val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                startActivity(intent)
            }
            NicoVideoDescriptionText.DESCRIPTION_TYPE_SEEK -> {
                viewModel.playerSetSeekMs.postValue(link.toLong())
            }
            NicoVideoDescriptionText.DESCRIPTION_TYPE_SERIES -> {
                setSeriesFragment(link)
            }
            NicoVideoDescriptionText.DESCRIPTION_TYPE_MYLIST -> {
                setMylistFragment(link)
            }
            NicoVideoDescriptionText.DESCRIPTION_TYPE_NICOVIDEO -> {
                setNicoVideoFragment(link)
            }
        }
    }

    /**
     * 動画再生Fragment設置
     * @param videoId 動画ID
     * */
    private fun setNicoVideoFragment(videoId: String) {
        (requireActivity() as? MainActivity)?.setNicovideoFragment(videoId)
    }

    /**
     * マイリストFragmentを設置
     * @param mylistId マイリストID
     * */
    private fun setMylistFragment(mylistId: String) {
        val myListFragment = NicoVideoMyListListFragment().apply {
            arguments = Bundle().apply {
                putString("mylist_id", mylistId)
            }
        }
        setFragment(myListFragment, "mylist")
    }

    /**
     * シリーズFragmentを設置
     * @param seriesId
     * */
    private fun setSeriesFragment(seriesId: String) {
        val seriesFragment = NicoVideoSeriesFragment().apply {
            arguments = Bundle().apply {
                putString("series_id", seriesId)
            }
        }
        setFragment(seriesFragment, "series")
    }

    /**
     * アカウント情報Fragmentを表示
     * @param userId ゆーざーID
     * */
    private fun setAccountFragment(userId: String) {
        val accountFragment = NicoAccountFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
        setFragment(accountFragment, "account")
    }

    /**
     * タグの検索をするFragmentを表示する
     * */
    private fun setTagSearchFragment(tag: String) {
        val searchFragment = NicoVideoSearchFragment().apply {
            arguments = Bundle().apply {
                putString("search", tag)
            }
        }
        setFragment(searchFragment, "tag_search")
    }

    /**
     * Fragmentを置く関数
     *
     * @param fragment 置くFragment
     * @param backstack Fragmentを積み上げる場合は適当な値を入れて
     * */
    private fun setFragment(fragment: Fragment, backstack: String) {
        // Fragment設置
        (requireActivity() as MainActivity).setFragment(fragment, backstack, true)
        // ミニプレイヤー化
        viewModel.isMiniPlayerMode.postValue(true)
    }

}