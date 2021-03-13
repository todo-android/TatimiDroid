package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.compositionContext
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.DarkColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.LightColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.NicoVideoLikeMessageScreen
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import java.util.concurrent.atomic.AtomicReference

/**
 * ニコ動のいいねのお礼メッセージ表示用BottomFragment
 *
 * レイアウトはComposeで
 * */
class NicoVideoLikeThanksMessageBottomFragment : BottomSheetDialogFragment() {

    /** [io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.JCNicoVideoFragment]のViewModel */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    @InternalComposeUiApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            ViewTreeLifecycleOwner.set(this, viewLifecycleOwner)
            val newRecomposer = AtomicReference(WindowRecomposerFactory.LifecycleAware).get().createRecomposer(rootView)
            compositionContext = newRecomposer

            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    // これでくくらないとなんかダークモード時に文字が白にならない
                    Surface {
                        NicoVideoLikeMessageScreen(nicoVideoViewModel = viewModel) {
                            // 二回目（画面回転時）に表示させない
                            viewModel.isAlreadyShowThanksMessage = true
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 閉じれんように
        isCancelable = false

    }

}