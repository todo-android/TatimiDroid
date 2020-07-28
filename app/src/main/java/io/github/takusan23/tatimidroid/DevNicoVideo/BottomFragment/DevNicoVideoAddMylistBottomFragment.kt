package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoMylistAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_mylist.*
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * マイリスト追加BottomFragment
 * */
class DevNicoVideoAddMylistBottomFragment : BottomSheetDialogFragment() {

    // アダプター
    lateinit var nicoVideoMylistAdapter: DevNicoVideoMylistAdapter
    val recyclerViewList = arrayListOf<Pair<String, String>>()

    // ユーザーセッション
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // スマホ版マイリストAPI
    private val spMyListAPI = NicoVideoSPMyListAPI()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        // マイリスト取得
        coroutine()

    }


    fun coroutine() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable.message}")
        }
        GlobalScope.launch(errorHandler) {
            recyclerViewList.clear()
            // マイリスト一覧APIを叩く
            val myListListResponse = spMyListAPI.getMyListList(userSession)
            if (!myListListResponse.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${myListListResponse.code}")
                return@launch
            }
            // レスポンスをパースしてRecyclerViewに突っ込む
            spMyListAPI.parseMyListList(myListListResponse.body?.string()).forEach { myList ->
                val pair = Pair(myList.title, myList.id)
                recyclerViewList.add(pair)
            }
            // 一覧更新
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                nicoVideoMylistAdapter.notifyDataSetChanged()
            }
        }
    }

    fun initRecyclerView() {
        bottom_fragment_nicovideo_mylist_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoMylistAdapter = DevNicoVideoMylistAdapter(recyclerViewList)
            nicoVideoMylistAdapter.id = arguments?.getString("id", "") ?: ""
            nicoVideoMylistAdapter.mylistBottomFragment = this@DevNicoVideoAddMylistBottomFragment
            adapter = nicoVideoMylistAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}