package io.github.takusan23.tatimidroid.NicoLive

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKData
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKHTML
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoJKProgramAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_jk_program_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ニコニコ実況チャンネル一覧
 * */
class NicoJKProgramListFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    lateinit var nicoJKProgramAdapter: NicoJKProgramAdapter
    var recyclerViewList = arrayListOf<NicoJKData>()

    // API叩いたりスクレイピングしたり
    val nicoJKHTML = NicoJKHTML()

    var userSession = ""

    // 読み込む種類
    var type = "tv" // tv か radio か bs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_jk_program_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""
        type = arguments?.getString("type") ?: "tv"

        initRecyclerView()

        if (savedInstanceState == null) {
            coroutine()
        } else {
            (savedInstanceState.getSerializable("list") as ArrayList<NicoJKData>).forEach {
                recyclerViewList.add(it)
            }
            nicoJKProgramAdapter.notifyDataSetChanged()
        }

        // 引っ張って更新
        fragment_jk_program_list_swipe.setOnRefreshListener {
            coroutine()
        }

    }

    // データ取得
    fun coroutine() {
        // 配列クリア
        recyclerViewList.clear()
        nicoJKProgramAdapter.notifyDataSetChanged()
        fragment_jk_program_list_swipe.isRefreshing = true
        lifecycleScope.launch {
            val listResponse = nicoJKHTML.getChannelListHTML(type, userSession)
            // 取得できんときは落とす
            if (!listResponse.isSuccessful) {
                return@launch
            }
            recyclerViewList = withContext(Dispatchers.Default) {
                ArrayList(nicoJKHTML.parseChannelListHTML(listResponse.body?.string()))
            }
            // RecyclerView更新
            initRecyclerView()
            fragment_jk_program_list_swipe.isRefreshing = false
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        if (isAdded) {
            fragment_jk_program_list_recyclerview.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                nicoJKProgramAdapter = NicoJKProgramAdapter(recyclerViewList)
                adapter = nicoJKProgramAdapter
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
    }

}