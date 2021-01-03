package io.github.takusan23.tatimidroid.NicoVideo

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.BottomSheetPlayerBehavior
import io.github.takusan23.tatimidroid.MainActivityPlayerFragmentInterface
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.SystemBarVisibility
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentPlayerBaseBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 動画、生放送のFragmentのベースになるFragment。これを継承して作っていきたい。
 *
 * プレイヤーに関しては[BottomSheetPlayerBehavior]も参照
 * */
open class PlayerBaseFragment : Fragment(), MainActivityPlayerFragmentInterface {

    /** BottomSheetとプレイヤーFrameLayoutとFragment置くFrameLayoutがあるだけ */
    private val viewBinding by lazy { FragmentPlayerBaseBinding.inflate(layoutInflater) }

    /** BottomSheetをコード上で操作するために */
    val bottomSheetPlayerBehavior by lazy { BottomSheetPlayerBehavior.from(viewBinding.fragmentPlayerBaseFragmentParentLinearLayout) }

    /** Fragmentを置くFrameLayout */
    val fragmentHostFrameLayout by lazy { viewBinding.fragmentPlayerBaseFragmentFrameLayout }

    /** プレイヤー部分におくFrameLayout。背景暗黒にしてます。 */
    val fragmentPlayerFrameLayout by lazy { viewBinding.fragmentPlayerBasePlayerFrameLayout }

    /** コメントFragmentを置くためのFrameLayout */
    val fragmentCommentHostFrameLayout by lazy { viewBinding.fragmentPlayerCommentFragmentFrameLayout }

    /** コメント一覧表示用Fab */
    val fragmentCommentFab by lazy { viewBinding.fragmentPlayerCommentShowFab }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    /** View表示時 */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // BottomSheet初期化
        bottomSheetPlayerBehavior.init(450, viewBinding.fragmentPlayerBaseFragmentParentLinearLayout, viewBinding.fragmentPlayerBasePlayerFrameLayout)
        bottomSheetPlayerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        // ダークモード対策
        viewBinding.fragmentPlayerBaseFragmentParentLinearLayout.background = ColorDrawable(getThemeColor(context))
        // コールバック
        bottomSheetPlayerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                onBottomSheetStateChane(newState, isMiniPlayerMode())
                // 終了の時
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finishFragment()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                onBottomSheetProgress(slideOffset)
            }
        })
        // プレイヤー以外で動かさないように
        setDraggableAreaPlayerOnly(true)

        // 遅延で表示。ぴょこって
        lifecycleScope.launch {
            delay(100)
            bottomSheetPlayerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // バックキーのイベント
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!isMiniPlayerMode()) {
                toMiniPlayer()
            } else {
                isEnabled = false
            }
        }
    }

    /**
     * プレイヤー部分Viewを追加する関数
     * @param addView 追加したいView
     * */
    fun addPlayerFrameLayout(addView: View) {
        viewBinding.fragmentPlayerBasePlayerFrameLayout.addView(addView)
    }

    /** バックボタン押した時呼ばれる */
    override fun onBackButtonPress() {

    }

    /** ミニプレイヤー状態かどうかを返す */
    override fun isMiniPlayerMode(): Boolean {
        return bottomSheetPlayerBehavior.isMiniPlayerMode()
    }

    /** ミニプレイヤーモードへ */
    fun toMiniPlayer() {
        bottomSheetPlayerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /** 通常モードへ */
    fun toDefaultPlayer() {
        bottomSheetPlayerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /** 現在の状態（ミニプレイヤー等）に合わせたアイコンを返す */
    fun getCurrentStateIcon(): Drawable? {
        return if (isMiniPlayerMode()) {
            context?.getDrawable(R.drawable.ic_expand_less_black_24dp)
        } else {
            context?.getDrawable(R.drawable.ic_expand_more_24px)
        }
    }

    /** このFragmentを終了させるときに使う関数 */
    fun finishFragment() {
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    /** 画面が横かどうかを返す。横ならtrue */
    fun isLandscape(): Boolean {
        return requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * ドラッグを有効/無効にする
     * @param isDraggable ドラッグ禁止はfalse
     * */
    fun setDraggable(isDraggable: Boolean) {
        bottomSheetPlayerBehavior.isDraggable = isDraggable
    }

    /**
     * プレイヤーのサイズ変更（ドラッグ操作）をプレイヤー範囲に限定するかどうか
     * @param isPlayerOnly trueで限定する
     * */
    fun setDraggableAreaPlayerOnly(isPlayerOnly: Boolean) {
        bottomSheetPlayerBehavior.isDraggableAreaPlayerOnly = isPlayerOnly
    }

    /**
     * SnackBarを表示する関数。
     * @param message めっせーじ
     * @param click Snackbarのボタンを押した時
     * @param action Snackbarのボタンの本文
     * */
    fun showSnackBar(message: String, action: String?, click: (() -> Unit)?) {
        Snackbar.make(fragmentPlayerFrameLayout, message, Snackbar.LENGTH_SHORT).apply {
            if (action != null) {
                setAction(action) {
                    click?.invoke()
                }
            }
            anchorView = fragmentCommentFab
            //view.elevation = 30f
        }.show()
    }

    /**
     * 全画面プレイヤーへ切り替える。アスペクト比の調整などは各自やってな
     *
     * ステータスバーも非表示にする。画面も横に倒す。
     * */
    fun toFullScreen() {
        // 横画面にする。SENSOR版なので右に倒しても左に倒してもおｋだよ？
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // ステータスバー隠す
        SystemBarVisibility.hideSystemBar(requireActivity().window)
        // BottomSheet側も全画面に切り替える
        bottomSheetPlayerBehavior.toFullScreen()
    }

    /**
     * 全画面から通常画面へ。アスペクト比の調整などは各自やってな
     *
     * ステータスバーを表示、画面はセンサー次第
     * */
    fun toDefaultScreen() {
        // センサーの思いのままに
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        // ステータスバー表示
        SystemBarVisibility.showSystemBar(requireActivity().window)
        // BottomSheet側も全画面を無効にする
        bottomSheetPlayerBehavior.toDefaultScreen()
    }

    /**
     * BottomSheetの状態が変わったら呼ばれる関数。オーバーライドして使って
     * @param state [BottomSheetBehavior.state]の値
     * @param isMiniPlayer [isMiniPlayerMode]の値
     * */
    open fun onBottomSheetStateChane(state: Int, isMiniPlayer: Boolean) {

    }

    /**
     * BottomSheet操作中に呼ばれる
     * */
    open fun onBottomSheetProgress(progress: Float) {

    }

}