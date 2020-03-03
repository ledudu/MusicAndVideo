package com.yizisu.music.and.video.module.fragment


import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.yizisu.basemvvm.mvvm.mvvm_helper.LiveBean
import com.yizisu.basemvvm.mvvm.mvvm_helper.LiveBeanStatus
import com.yizisu.basemvvm.utils.launchThread
import com.yizisu.basemvvm.view.simpleTextRcvAdater
import com.yizisu.music.and.video.AppData


import com.yizisu.music.and.video.R
import com.yizisu.music.and.video.baselib.base.BaseFragment
import com.yizisu.music.and.video.bean.LocalMusicBean
import com.yizisu.music.and.video.bean.SongModel
import com.yizisu.music.and.video.module.fragment.adapter.LocalMusicAdapter
import com.yizisu.music.and.video.module.full_video.FullVideoActivity
import com.yizisu.music.and.video.module.local_music.adapter.LocalPagerAdapter
import com.yizisu.music.and.video.service.music.MusicService
import com.yizisu.music.and.video.viewmodel.LocalMusicViewModel
import com.yizisu.playerlibrary.helper.PlayerModel
import kotlinx.android.synthetic.main.fragment_local_music.*

class LocalVideoFragment : BaseFragment() {
    companion object {
        fun create(): LocalVideoFragment {
            return LocalVideoFragment()
        }
    }

    private val adapter = LocalMusicAdapter()
    override fun getContentResOrView(inflater: LayoutInflater): Any? {
        return R.layout.fragment_local_music
    }

    override fun initViewModel() {
        super.initViewModel()
        AppData.localVideoData.register(::onQueryLocalMusic)
    }

    override fun initUi(savedInstanceState: Bundle?) {
        super.initUi(savedInstanceState)
        adapter.setOnItemClickListener { itemView, position, itemData ->
            FullVideoActivity.start(
                appCompatActivity, FullVideoActivity.FullVideoData(
                    itemData.path
                )
            )
        }
        localMusicRcv.adapter = adapter
    }

    override fun initData() {
        super.initData()
        getPermission(
            mutableListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            if (it) {
                getActivityViewModel<LocalMusicViewModel>()?.queryLocalVideo()
            }
        }
    }

    /**
     * 查询本地音乐
     */
    private fun onQueryLocalMusic(bean: LiveBean<MutableList<LocalMusicBean>>) {
        when (bean.status) {
            LiveBeanStatus.START -> {

            }
            LiveBeanStatus.SUCCESS -> {
                val musics = bean.data
                if (!musics.isNullOrEmpty()) {
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV1"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV2"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv2hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV3"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv3hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV4"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv4hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV5"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv5hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV6"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv6hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV7"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv7hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV8"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv8hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV9"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv9hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV10"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv10hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV11"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv11hd.m3u8"
                    })
                    musics.add(LocalMusicBean().apply {
                        title = "CCTV12"
                        singer = "直播"
                        path = "http://ivi.bupt.edu.cn/hls/cctv12hd.m3u8"
                    })
                    adapter.refreshList(musics)
                }
            }
            LiveBeanStatus.FAIL -> {

            }
        }
    }
}
