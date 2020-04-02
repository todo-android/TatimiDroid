package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoHistoryFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoMyListFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoPOSTFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoRankingFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_select.*


class DevNicoVideoSelectFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // インターネット接続確認
        if (isConnectionInternet()) {
            // ランキング
            setFragment(DevNicoVideoRankingFragment())
        } else {
            // キャッシュ一覧

        }

        fragment_nicovideo_ranking.setOnClickListener {
            setFragment(DevNicoVideoRankingFragment())
        }

        fragment_nicovideo_post.setOnClickListener {
            setFragment(DevNicoVideoPOSTFragment())
        }

        fragment_nicovideo_mylist.setOnClickListener {
            setFragment(DevNicoVideoMyListFragment())
        }

        fragment_nicovideo_history.setOnClickListener {
            setFragment(DevNicoVideoHistoryFragment())
        }

    }

    fun setFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(fragment_video_list_linearlayout.id, fragment)?.commit()
    }

    /**
     * ネットワーク接続確認
     * https://stackoverflow.com/questions/57277759/getactivenetworkinfo-is-deprecated-in-api-29
     * */
    fun isConnectionInternet(): Boolean {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10時代のネットワーク接続チェック
            val network = connectivityManager?.activeNetwork
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
            return when {
                networkCapabilities == null -> false
                // Wi-Fi / MobileData / EtherNet / Bluetooth のどれかでつながっているか
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            // 今までのネットワーク接続チェック
            return connectivityManager?.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnected
        }
    }

}