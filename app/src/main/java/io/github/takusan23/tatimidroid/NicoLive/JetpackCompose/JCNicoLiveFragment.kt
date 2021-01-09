package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.PlayerBaseFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startLivePlayService
import io.github.takusan23.tatimidroid.Tool.ContentShare
import io.github.takusan23.tatimidroid.Tool.CustomFont
import io.github.takusan23.tatimidroid.Tool.InternetConnectionCheck
import io.github.takusan23.tatimidroid.Tool.RotationSensor
import io.github.takusan23.tatimidroid.databinding.IncludeNicolivePlayerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 番組詳細をJetpack Composeで作る。新UI
 *
 * 一部の関数は[PlayerBaseFragment]に実装しています
 *
 * 入れてほしいもの
 *
 * liveId       | String | 番組ID
 * watch_mode   | String | 現状 comment_post のみ
 * */
class JCNicoLiveFragment : PlayerBaseFragment() {

    /** 保存するやつ */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    /** プレイヤー部分のUI */
    private val nicolivePlayerUIBinding by lazy { IncludeNicolivePlayerBinding.inflate(layoutInflater) }

    /** そうですね、やっぱり僕は、王道を征く、ExoPlayerですか */
    private val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    /** 共有 */
    val contentShare = ContentShare(this)

    /** ViewModel初期化。ネットワークとかUI関係ないやつはこっちに書いていきます。 */
    private val viewModel by lazy {
        val liveId = arguments?.getString("liveId")!!
        ViewModelProvider(this, NicoLiveViewModelFactory(requireActivity().application, liveId, true)).get(NicoLiveViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // プレイヤー追加など
        setPlayerUI()

        // フォント設定
        setFont()

        // LiveData監視
        setLiveData()

        // スリープにしない
        caffeine()
    }

    /** LiveData監視 */
    private fun setLiveData() {
        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // アイコン直す
            nicolivePlayerUIBinding.includeNicolivePlayerCloseImageView.setImageDrawable(getCurrentStateIcon())
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                toMiniPlayer() // これ直したい
            }
        }
        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                getString(R.string.encryption_video_not_play) -> finishFragment()
            }
        }
        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            showSnackBar(it, null, null)
        }
        // 番組情報
        viewModel.nicoLiveProgramData.observe(viewLifecycleOwner) { data ->
            setLiveInfo(data)
        }
        // 新ニコニコ実況の番組と発覚した場合
        viewModel.isNicoJKLiveData.observe(viewLifecycleOwner) { nicoJKId ->
            // バックグラウンド再生無いので非表示
            nicolivePlayerUIBinding.includeNicolivePlayerBackgroundImageView.isVisible = false
            // 映像を受信しない。
            showSnackBar(getString(R.string.nicolive_jk_not_live_receive), null, null)
            // 映像を受信しないモードをtrueへ
            viewModel.isNotReceiveLive.postValue(true)
        }
        // 映像を受信しないモード。映像なしだと3分で620KBぐらい？
        viewModel.isNotReceiveLive.observe(viewLifecycleOwner) { isNotReceiveLive ->
            if (isNotReceiveLive) {
                // 背景真っ暗へ
                nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = ColorDrawable(Color.BLACK)
                exoPlayer.release()
            } else {
                // 生放送再生
                viewModel.hlsAddressLiveData.value?.let { playExoPlayer(it) }
            }
        }
        // うんこめ
        viewModel.unneiCommentLiveData.observe(viewLifecycleOwner) { unnkome ->

        }
        // あんけーと
        viewModel.enquateLiveData.observe(viewLifecycleOwner) { enquateMessage ->

        }
        // 統計情報
        viewModel.statisticsLiveData.observe(viewLifecycleOwner) { statistics ->

        }
        // アクティブユーザー？
        viewModel.activeCommentPostUserLiveData.observe(viewLifecycleOwner) { active ->

        }
        // 経過時間
        viewModel.programTimeLiveData.observe(viewLifecycleOwner) { programTime ->
            nicolivePlayerUIBinding.includeNicolivePlayerCurrentTimeTextView.text = programTime
        }
        // 終了時刻
        viewModel.formattedProgramEndTime.observe(viewLifecycleOwner) { endTime ->
            nicolivePlayerUIBinding.includeNicolivePlayerDurationTextView.text = endTime
        }
        // HLSアドレス取得
        viewModel.hlsAddressLiveData.observe(viewLifecycleOwner) { address ->
            playExoPlayer(address)
        }
        // 画質変更
        viewModel.changeQualityLiveData.observe(viewLifecycleOwner) { quality ->
            showSnackBar("${getString(R.string.successful_quality)}\n→${quality}", null, null)
        }
        // コメントうけとる
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { commentJSONParse ->
            // 豆先輩とか
            if (!commentJSONParse.comment.contains("\n")) {
                nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.postComment(commentJSONParse.comment, commentJSONParse)
            } else {
                // https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
                // 豆先輩！！！！！！！！！！！！！！！！！！
                // 下固定コメントで複数行だとAA（アスキーアートの略 / CA(コメントアート)とも言う）がうまく動かない。配列の中身を逆にする必要がある
                // Kotlinのこの書き方ほんと好き
                val asciiArtComment = if (commentJSONParse.mail.contains("shita")) {
                    commentJSONParse.comment.split("\n").reversed() // 下コメントだけ逆順にする
                } else {
                    commentJSONParse.comment.split("\n")
                }
                // 複数行対応Var
                nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
            }
        }
        // コメント一覧Fragmentを表示するかどうかのやつ
        viewModel.commentListShowLiveData.observe(viewLifecycleOwner) { isShow ->
            fragmentCommentFab.setImageDrawable(
                if (isShow) {
                    requireContext().getDrawable(R.drawable.ic_outline_info_24px)
                } else {
                    requireContext().getDrawable(R.drawable.ic_outline_comment_24px)
                }
            )
            // アニメーション？自作ライブラリ
            val dropPopAlert = fragmentCommentHostFrameLayout.toDropPopAlert()
            if (isShow) {
                dropPopAlert.showAlert(DropPopAlert.ALERT_UP)
            } else {
                dropPopAlert.hideAlert(DropPopAlert.ALERT_UP)
            }
        }
    }

    override fun onBottomSheetProgress(progress: Float) {
        super.onBottomSheetProgress(progress)
        aspectRatioFix()
    }

    /** ExoPlayerで生放送を再生する */
    private fun playExoPlayer(address: String) {
        // ニコ生版ニコニコ実況の場合 と 映像を受信しないモードのとき は接続しないので即return
        if (viewModel.nicoLiveHTML.getNicoJKIdFromChannelId(viewModel.communityId) != null || viewModel.isNotReceiveLive.value == true) {
            return
        }
        // 音声のみの再生はその旨（むね）を表示して、SurfaceViewを暗黒へ。わーわー言うとりますが、お時間でーす
        if (viewModel.currentQuality == "audio_high") {
            nicolivePlayerUIBinding.includeNicolivePlayerAudioOnlyTextView.isVisible = true
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = ColorDrawable(Color.BLACK)
        } else {
            nicolivePlayerUIBinding.includeNicolivePlayerAudioOnlyTextView.isVisible = false
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = null
        }
        // アスペクト比治す
        aspectRatioFix()
        // HLS受け取り
        val mediaItem = MediaItem.fromUri(address.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        // SurfaceView
        exoPlayer.setVideoSurfaceView(nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView)
        // 再生
        exoPlayer.playWhenReady = true
        // ミニプレイヤーから通常画面へ遷移
        var isFirst = true
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 一度だけ
                if (isFirst) {
                    isFirst = false
                    // 通常画面へ。なおこいつのせいで画面回転前がミニプレイヤーでもミニプレイヤーにならない
                    toDefaultPlayer()
                    // コメント一覧も表示
                    lifecycleScope.launch {
                        delay(1000)
                        if (!viewModel.isFullScreenMode && !viewModel.isAutoCommentListShowOff) {
                            // フルスクリーン時 もしくは 自動で展開しない場合 は操作しない
                            viewModel.commentListShowLiveData.postValue(true)
                        }
                    }
                } else {
                    exoPlayer.removeListener(this)
                }
            }
        })

        // もしエラー出たら
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                error.printStackTrace()
                println("生放送の再生が止まりました。")
                //再接続する？
                //それからニコ生視聴セッションWebSocketが切断されてなければ
                if (!viewModel.nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                    println("再度再生準備を行います")
                    activity?.runOnUiThread {
                        //再生準備
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        //SurfaceViewセット
                        exoPlayer.setVideoSurfaceView(nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView)
                        //再生
                        exoPlayer.playWhenReady = true
                        // 再生が止まった時に低遅延が有効になっていればOFFにできるように。安定して見れない場合は低遅延が有効なのが原因
                        if (viewModel.nicoLiveHTML.isLowLatency) {
                            showSnackBar(getString(R.string.error_player), getString(R.string.low_latency_off)) {
                                // 低遅延OFFを送信
                                viewModel.nicoLiveHTML.sendLowLatency(!viewModel.nicoLiveHTML.isLowLatency)
                            }
                        } else {
                            showSnackBar(getString(R.string.error_player), null, null)
                        }
                    }
                }
            }
        })
    }

    /** アスペクト比を治す。サイズ変更の度によぶ必要あり。めんどいので16:9固定で */
    private fun aspectRatioFix() {
        if (!isAdded) return
        fragmentPlayerFrameLayout.doOnLayout {
            val playerHeight = fragmentPlayerFrameLayout.height
            val playerWidth = (playerHeight / 9) * 16
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.updateLayoutParams {
                width = playerWidth
                height = playerHeight
            }
        }
    }

    /** 番組情報をUIに反映させる */
    private fun setLiveInfo(data: NicoLiveProgramData) {
        nicolivePlayerUIBinding.apply {
            includeNicolivePlayerTitleTextView.text = data.title
            includeNicolivePlayerVideoIdTextView.text = data.programId
        }
    }

    /** フォント設定を適用 */
    private fun setFont() {
        val font = CustomFont(requireContext())
        if (font.isApplyFontFileToCommentCanvas) {
            // 適用する設定の場合
            nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.typeface = font.typeface
        }
    }

    /** プレイヤーのUIをFragmentに追加する */
    private fun setPlayerUI() {
        // ここは動画と一緒
        addPlayerFrameLayout(nicolivePlayerUIBinding.root)
        // プレイヤー部分の表示設定
        val hideJob = Job()
        nicolivePlayerUIBinding.root.setOnClickListener {
            hideJob.cancelChildren()
            // ConstraintLayoutのGroup機能でまとめてVisibility変更。
            nicolivePlayerUIBinding.includeNicolivePlayerControlGroup.visibility = if (nicolivePlayerUIBinding.includeNicolivePlayerControlGroup.visibility == View.VISIBLE) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            // ちょっと強引
            if (isMiniPlayerMode()) {
                // ConstraintLayoutのGroup機能でまとめてVisibility変更。
                nicolivePlayerUIBinding.includeNicolivePlayerMiniPlayerGroup.visibility = View.INVISIBLE
            }
            // 遅延させて
            lifecycleScope.launch(hideJob) {
                delay(3000)
                nicolivePlayerUIBinding.includeNicolivePlayerControlGroup.visibility = View.INVISIBLE
            }
        }
        // プレイヤー右上のアイコンにWi-Fiアイコンがあるけどあれ、どの方法で再生してるかだから。キャッシュならフォルダーになる
        val playingTypeDrawable = InternetConnectionCheck.getConnectionTypeDrawable(requireContext())
        nicolivePlayerUIBinding.includeNicolivePlayerNetworkImageView.setImageDrawable(playingTypeDrawable)
        // ミニプレイヤー切り替えボタン
        nicolivePlayerUIBinding.includeNicolivePlayerCloseImageView.setOnClickListener {
            if (isMiniPlayerMode()) {
                toDefaultPlayer()
            } else {
                toMiniPlayer()
            }
        }
        // コメントキャンバス非表示
        nicolivePlayerUIBinding.includeNicolivePlayerCommentHideImageView.setOnClickListener {
            nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.apply {
                isVisible = !isVisible
            }
        }
        // ポップアップ再生
        nicolivePlayerUIBinding.includeNicolivePlayerPopupImageView.setOnClickListener {
            if (viewModel.nicoLiveProgramData.value != null) {
                startLivePlayService(
                    context = requireContext(),
                    mode = "popup",
                    liveId = viewModel.nicoLiveProgramData.value!!.programId,
                    isCommentPost = true,
                    isNicocasMode = false,
                    isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                    startQuality = viewModel.currentQuality
                )
                finishFragment()
            }
        }
        // バックグラウンド再生
        nicolivePlayerUIBinding.includeNicolivePlayerBackgroundImageView.setOnClickListener {
            if (viewModel.nicoLiveProgramData.value != null) {
                startLivePlayService(
                    context = requireContext(),
                    mode = "background",
                    liveId = viewModel.nicoLiveProgramData.value!!.programId,
                    isCommentPost = true,
                    isNicocasMode = false,
                    isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                    startQuality = viewModel.currentQuality
                )
                finishFragment()
            }

        }
        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(requireActivity(), lifecycle)
        }
    }

    override fun onBottomSheetStateChane(state: Int, isMiniPlayer: Boolean) {
        super.onBottomSheetStateChane(state, isMiniPlayer)
        // 展開 or ミニプレイヤー のみ
        if (state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_EXPANDED) {
            (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
            // 一応UI表示
            nicolivePlayerUIBinding.root.performClick()
            // アイコン直す
            nicolivePlayerUIBinding.includeNicolivePlayerCloseImageView.setImageDrawable(getCurrentStateIcon())
            // ViewModelへ状態通知
            viewModel.isMiniPlayerMode.value = isMiniPlayerMode()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        caffeineUnlock()
    }
}