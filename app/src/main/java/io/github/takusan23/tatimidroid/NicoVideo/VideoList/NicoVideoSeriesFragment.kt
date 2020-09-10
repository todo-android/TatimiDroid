package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSeriesAPI
import io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_series.*
import kotlinx.android.synthetic.main.include_playlist_button.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * シリーズFragment
 *
 * 入れてほしいもの
 * series_id    | String    | シリーズID。https://sp.nicovideo.jp/series/{ここの数字}
 * series_title | String    | シリーズタイトル
 * */
class NicoVideoSeriesFragment : Fragment() {

    val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    // Adapter
    val nicoVideoList = arrayListOf<NicoVideoData>()
    val nicoVideoListAdapter = NicoVideoListAdapter(nicoVideoList)

    val seriesTitle by lazy { arguments?.getString("series_title") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_series, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        // 画面回転復帰時
        if (savedInstanceState != null) {
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).forEach {
                nicoVideoList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
            include_playlist_button.isVisible = true
        } else {
            // シリーズ取得
            val seriesId = arguments?.getString("series_id") ?: return
            val userSession = prefSetting.getString("user_session", "") ?: return
            lifecycleScope.launch(Dispatchers.Default) {
                val seriesAPI = NicoVideoSeriesAPI()
                val response = seriesAPI.getSeriesVideoList(userSession, seriesId)
                seriesAPI.parseSeriesVideoList(response.body?.string()).forEach {
                    nicoVideoList.add(it)
                }
                withContext(Dispatchers.Main) {
                    // 反映
                    nicoVideoListAdapter.notifyDataSetChanged()
                    include_playlist_button.isVisible = true
                }
            }
        }

        // 連続再生
        include_playlist_button.setOnClickListener {
            val intent = Intent(requireContext(), NicoVideoPlayListActivity::class.java)
            // 中身を入れる
            intent.putExtra("video_list", nicoVideoList)
            intent.putExtra("name", seriesTitle)
            startActivity(intent)
            if (activity is NicoVideoActivity) {
                activity?.finish()
            }
        }

    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        fragment_nicovideo_series_recyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = nicoVideoListAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", nicoVideoList)
    }

}