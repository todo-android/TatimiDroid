package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRankingHTML
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoRankingViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_ranking.*
import kotlinx.coroutines.Job

/**
 * ランキングFragment
 * */
class NicoVideoRankingFragment : Fragment() {

    lateinit var nicoVideoListAdapter: NicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var prefSetting: SharedPreferences

    lateinit var launch: Job

    // メニュー選んだ位置
    var rankingGenrePos = 0
    var rankingTimePos = 0

    /** ランキングのジャンル一覧。[NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE] のURL一覧と一致している */
    private val RANKING_GENRE = arrayListOf(
        "全ジャンル",
        "話題",
        "エンターテインメント",
        "ラジオ",
        "音楽・サウンド",
        "ダンス",
        "動物",
        "自然",
        "料理",
        "旅行・アウトドア",
        "乗り物",
        "スポーツ",
        "社会・政治・時事",
        "技術・工作",
        "解説・講座",
        "アニメ",
        "ゲーム",
        "その他"
    )

    /** ランキングの集計時間。基本いじらない。[NicoVideoRankingHTML.NICOVIDEO_RANKING_TIME] の配列の中身と一致している。 */
    private val RANKING_TIME = arrayListOf(
        "毎時",
        "２４時間",
        "週間",
        "月間",
        "全期間"
    )

    // ViewModel
    private val viewModel by viewModels<NicoVideoRankingViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        // ViewModel監視
        viewModel.rankingVideoList.observe(viewLifecycleOwner) { videoList ->
            // 動画一覧更新
            fragment_video_ranking_swipe.isRefreshing = false
            recyclerViewList.clear()
            videoList.forEach { video -> recyclerViewList.add(video) }
        }
        viewModel.rankingTagList.observe(viewLifecycleOwner) { tagList ->
            fragment_nicovideo_ranking_tag.removeAllViews()
            // ランキングのタグ
            tagList.forEach { genreTag ->
                val chip = (layoutInflater.inflate(R.layout.include_chip, fragment_nicovideo_ranking_tag, false) as Chip).apply {
                    text = genreTag
                    // 押したら読み込み
                    setOnClickListener {
                        // 全てのときはnullを指定する
                        if (genreTag == "すべて") {
                            getRanking(null)
                        } else {
                            getRanking(genreTag)
                        }
                    }
                }
                fragment_nicovideo_ranking_tag.addView(chip)
            }
        }

        if (savedInstanceState == null) {
            // しょかい
            // 前回開いてたランキングを開く
            val lastOpenGenre = prefSetting.getString("nicovideo_ranking_genre", RANKING_GENRE[0])
            val lastOpenTime = prefSetting.getString("nicovideo_ranking_time", RANKING_TIME[0])
            // 配列の位置を探す
            rankingGenrePos = RANKING_GENRE.indexOf(lastOpenGenre)
            rankingTimePos = RANKING_TIME.indexOf(lastOpenTime)
            // Chipに入れる
            fragment_nicovideo_ranking_genre.text = lastOpenGenre
            fragment_nicovideo_ranking_time.text = lastOpenTime
            // データ取得
            getRanking()
        }

        // Swipe To Refresh
        fragment_video_ranking_swipe.setOnRefreshListener {
            getRanking()
        }
    }

    /**
     * ランキングを取得する
     * @param tag 音楽ジャンルならVOCALOIDなど。無くても良い
     * */
    private fun getRanking(tag: String? = null) {
        // 消す
        recyclerViewList.clear()
        nicoVideoListAdapter.notifyDataSetChanged()
        fragment_nicovideo_ranking_tag.removeAllViews()
        fragment_video_ranking_swipe.isRefreshing = true
        // 次表示するときのために今選んでいるジャンル・集計時間を記録しておく
        prefSetting.edit {
            putString("nicovideo_ranking_genre", fragment_nicovideo_ranking_genre.text.toString())
            putString("nicovideo_ranking_time", fragment_nicovideo_ranking_time.text.toString())
        }
        // ジャンル
        val genre = NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE[rankingGenrePos]
        // 集計期間
        val time = NicoVideoRankingHTML.NICOVIDEO_RANKING_TIME[rankingTimePos]
        // ランキング更新
        viewModel.loadRanking(genre, time, tag)
    }

    private fun initDropDownMenu() {
        // ジャンル選択
        val genrePopupMenu = PopupMenu(requireContext(), fragment_nicovideo_ranking_genre)
        RANKING_GENRE.forEach { genre -> genrePopupMenu.menu.add(genre) }
        fragment_nicovideo_ranking_genre.setOnClickListener {
            genrePopupMenu.show()
        }
        // クリックイベント
        genrePopupMenu.setOnMenuItemClickListener { item ->
            fragment_nicovideo_ranking_genre.text = item.title
            rankingGenrePos = RANKING_GENRE.indexOf(item.title)
            // データ再取得
            getRanking()
            true
        }
        // 集計時間選択
        val timePopupMenu = PopupMenu(requireContext(), fragment_nicovideo_ranking_time)
        RANKING_TIME.forEach { time -> timePopupMenu.menu.add(time) }
        fragment_nicovideo_ranking_time.setOnClickListener {
            timePopupMenu.show()
        }
        timePopupMenu.setOnMenuItemClickListener { item ->
            fragment_nicovideo_ranking_time.text = item.title
            rankingTimePos = RANKING_TIME.indexOf(item.title)
            // データ再取得
            getRanking()
            true
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_video_ranking_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // ViewModelにしたい
        outState.putSerializable("list", recyclerViewList)
        outState.putStringArrayList("tag", fragment_nicovideo_ranking_tag.children.toList().map { view -> (view as Chip).text.toString() } as java.util.ArrayList<String>)
    }

}