package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSearchHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.*

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSearchFragment]で使うViewModel
 *
 * なんかきれいに書けなかった
 * */
class NicoVideoSearchViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** ニコ動検索とスクレイピング */
    private val searchHTML = NicoVideoSearchHTML()

    /** 検索結果を送信するLiveData */
    val searchResultNicoVideoDataListLiveData = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 関連タグを送信するLiveData */
    val searchResultTagListLiveData = MutableLiveData<List<String>>()

    /** 読み込み中LiveData */
    val isLoadingLiveData = MutableLiveData(false)

    /** 終了（動画がもう取れない）ならtrue */
    var isEnd = false

    /** コルーチンキャンセル用 */
    private val coroutineJob = Job()

    /** 現在の位置 */
    var currentSearchPage = 1
        private set

    /** 検索ワード */
    var currentSearchWord: String? = null
        private set

    /** 並び順 */
    var currentSearchSortName: String? = null
        private set

    /** タグ検索かどうか */
    var currentSearchIsTagSearch: Boolean? = null
        private set

    /**
     * 検索する関数
     *
     * [searchText]と[sortName]と[isTagSearch]が前回の検索と同じ時のみ、[page]引数が適用されます。
     *
     * 前回と検索内容が違う場合は、[page]（[currentSearchPage]）は1にリセットされます
     *
     * @param searchText 検索ワード。なお文字列０([String.isEmpty])ならこの関数は終了します
     * @param page ページ（上の説明見て）
     * @param isTagSearch タグ検索の場合はtrue。キーワード検索ならfalse
     * @param sortName 並び順。入れられる文字は[NicoVideoSearchHTML.NICOVIDEO_SEARCH_ORDER]の配列を参照
     * */
    fun search(searchText: String, page: Int = 1, isTagSearch: Boolean = true, sortName: String = NicoVideoSearchHTML.NICOVIDEO_SEARCH_ORDER[0]) {

        if (searchText.isEmpty()) return

        // すでに検索してたならキャンセル
        coroutineJob.cancelChildren()
        // くるくる
        isLoadingLiveData.postValue(true)

        // 検索ワードが切り替わった時
        if (searchText != currentSearchWord && currentSearchSortName != sortName && currentSearchIsTagSearch != isTagSearch) {
            // ページを1にする
            currentSearchPage = 1
        } else {
            // 引数に従う
            currentSearchPage = page
        }

        // 控える
        currentSearchWord = searchText
        currentSearchSortName = sortName
        currentSearchIsTagSearch = isTagSearch

        // 例外処理。コルーチン内で例外出るとここに来るようになるらしい。あたまいい
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}${throwable}")
            isLoadingLiveData.postValue(false)
        }
        viewModelScope.launch(errorHandler + coroutineJob) {
            // ソート条件生成
            val sort = searchHTML.makeSortOrder(currentSearchSortName!!)
            // タグかキーワードか
            val tagOrKeyword = if (currentSearchIsTagSearch!!) "tag" else "search"

            // 検索結果html取りに行く
            val html = withContext(Dispatchers.Default) {
                val response = searchHTML.getHTML(
                    userSession = userSession,
                    searchText = currentSearchWord!!,
                    tagOrSearch = tagOrKeyword,
                    sort = sort.first,
                    order = sort.second,
                    page = currentSearchPage.toString()
                )
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${context.getString(R.string.error)}\n${response.code}")
                    // もう読み込まない
                    isEnd = response.code == 404 // 404でもうページが存在しない
                    isLoadingLiveData.postValue(false)
                    return@withContext null
                }
                response.body?.string()
            } ?: return@launch // nullなら終了

            // スクレイピングしてLiveDataへ送信
            searchResultTagListLiveData.postValue(searchHTML.parseTag(html))
            // ページが2ページ以降の場合はこれまでの検索結果を保持する
            if (currentSearchPage >= 2) {
                // 保持する設定。次のページ機能で使う
                searchResultNicoVideoDataListLiveData.value!!.addAll(searchHTML.parseHTML(html))
                searchResultNicoVideoDataListLiveData.postValue(searchResultNicoVideoDataListLiveData.value!!)
            } else {
                // 1ページ目
                searchResultNicoVideoDataListLiveData.postValue(searchHTML.parseHTML(html))
            }
            // くるくるもどす
            isLoadingLiveData.postValue(false)
        }
    }

    /** 次のページのデータをリクエスト */
    fun getNextPage() {
        if (currentSearchWord != null && currentSearchIsTagSearch != null && currentSearchSortName != null) {
            if (!isEnd) {
                currentSearchPage++
                search(currentSearchWord!!, currentSearchPage, currentSearchIsTagSearch!!, currentSearchSortName!!)
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}