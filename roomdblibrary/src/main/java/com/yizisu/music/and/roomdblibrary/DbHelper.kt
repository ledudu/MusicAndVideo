package com.yizisu.music.and.roomdblibrary

import android.content.Context
import com.github.yuweiguocn.library.greendao.MigrationHelper
import com.greendao.gen.*
import com.yizisu.music.and.roomdblibrary.bean.*

object DbHelper {
    private var session: DaoSession? = null
    fun init(context: Context, dbName: String) {
        MigrationHelper.DEBUG = BuildConfig.DEBUG //如果你想查看日志信息，请将DEBUG设置为true
        val updateHelper = UpdateDbHelper(context, dbName, null)
        session = DaoMaster(updateHelper.writableDatabase).newSession()
        checkAndInitDefaultAlbum()
    }

    private val albumInfoTableDao by lazy { session?.albumInfoTableDao }
    private val songInfoTableDao by lazy { session?.songInfoTableDao }
    private val singerInfoTableDao by lazy { session?.singerInfoTableDao }
    private val songWithSingerDao by lazy { session?.songWithSingerDao }
    private val songWithAlbumDao by lazy { session?.songWithAlbumDao }


    /**************************************************************************************/

    /**
     * 通过歌单id查询歌单
     */
    fun queryAlbumByDbId(dbId: Long): AlbumInfoTable? {
        val dao = albumInfoTableDao ?: return null
        return dao.loadByRowId(dbId)
    }

    /**
     * 查询所有歌单
     */
    fun queryAllAlbumByDbId(): MutableList<AlbumInfoTable>? {
        val dao = albumInfoTableDao ?: return null
        return dao.loadAll()
    }

    /**
     * 插入单个歌曲到歌单
     */
    @Synchronized
    fun addSongToAlbum(song: SongInfoTable, album: AlbumInfoTable?) {
        album ?: return
        val songId = insetSong(song)
        if (!song.coverUrlPath.isNullOrBlank()) {
            album.urlPath = song.coverUrlPath
        }
        if (!song.coverFilePath.isNullOrBlank()) {
            album.filePath = song.coverFilePath
        }
        val albumId = insetOrUpdateAlbum(album)
        //歌曲和歌单做关联
        if (!isSongInAlbum(song, album)) {
            withSongAndAlbum(songId, albumId)
        }
    }

    @Synchronized
    fun addDownloadSongToAlbum(song: SongInfoTable, albumDbId: Long?) {
        albumDbId ?: return
        val songId = insetSong(song)
        //歌曲和歌单做关联
        if (!isSongInAlbum(song, albumDbId)) {
            withSongAndAlbum(songId, albumDbId)
        }
    }

    @Synchronized
    fun addSongToAlbum(song: SongInfoTable, albumId: Long?) {
        albumId ?: return
        addSongToAlbum(
            song, albumInfoTableDao?.load(albumId)
        )
    }

    /**
     * 批量的将歌曲插入歌单
     */
    @Synchronized
    fun addSongToAlbum(song: MutableList<SongInfoTable>?, album: AlbumInfoTable) {
        val songWithAlbums = mutableListOf<SongWithAlbum>()
        val albumId = insetOrUpdateAlbum(album)
        if (song.isNullOrEmpty()) {
            return
        }
        song.forEach {
            val songId = insetSong(it)
            if (!it.coverUrlPath.isNullOrBlank()) {
                album.urlPath = it.coverUrlPath
            }
            if (!it.coverFilePath.isNullOrBlank()) {
                album.filePath = it.coverFilePath
            }
            if (!isSongInAlbum(it, album)) {
                songWithAlbums.add(SongWithAlbum(null, albumId, songId, null, null))
            }
        }
        songWithAlbumDao?.insertOrReplaceInTx(songWithAlbums)
    }

    /**
     * 查询某个歌曲是否在歌单
     */
    fun isSongInAlbum(song: SongInfoTable, album: AlbumInfoTable): Boolean {
        song.dbId ?: return false
        return isSongInAlbum(song, album.dbId)
    }

    fun isSongInAlbum(song: SongInfoTable, albumId: Long?): Boolean {
        val dao = songWithAlbumDao ?: return false
        song.dbId ?: return false
        albumId ?: return false
        //判断歌曲是否存在
        val old = dao.queryBuilder().where(
            SongWithAlbumDao.Properties.SongId.eq(song.dbId),
            SongWithAlbumDao.Properties.AlbumId.eq(albumId)
        ).unique()
        return old != null
    }

    /**
     * 歌单是否存在
     */
    fun queryAlbum(id: Long, source: Int): List<AlbumInfoTable>? {
        val dao = albumInfoTableDao ?: return null
        //判断专辑是否存在
        val list = dao.queryBuilder().where(
            AlbumInfoTableDao.Properties.Id.eq(id),
            AlbumInfoTableDao.Properties.Source.eq(source)
        ).list()
        return list
    }

    /**
     * 数据库查询歌曲
     */
    fun queryAllSongByKeyword(keywords: String): List<SongInfoTable>? {
        val dao = songInfoTableDao ?: return null
        //判断专辑是否存在
        val qb = dao.queryBuilder()
        return qb.where(
            qb.or(
                SongInfoTableDao.Properties.Name.like("%${keywords}%"),
                SongInfoTableDao.Properties.Des.like("%${keywords}%")
            )
        ).list()
    }

    /**
     * 查询所有已下载歌曲
     */
    @Synchronized
    fun queryAllSongIfFilePathNotEmpty(): List<SongInfoTable>? {
        val dao = songInfoTableDao ?: return null
        //判断专辑是否存在
        return dao.loadAll().filterNot {
            it.playFilePath.isNullOrEmpty() || it.source == DbCons.SOURCE_LOCAL
        }
    }

    /**
     * 从歌单移除单个歌曲
     */
    @Synchronized
    fun removeSongFromAlbum(song: SongInfoTable, album: AlbumInfoTable) {
        val dao = songWithAlbumDao ?: return
        song.dbId ?: return
        album.dbId ?: return
        //判断歌曲是否存在
        val old = dao.queryBuilder().where(
            SongWithAlbumDao.Properties.SongId.eq(song.dbId),
            SongWithAlbumDao.Properties.AlbumId.eq(album.dbId)
        ).unique()
        if (old != null) {
            dao.delete(old)
        }
    }

    /**
     * 将歌曲从歌单移除
     */
    @Synchronized
    fun removeSongsFromAlbum(songs: MutableList<SongInfoTable>, album: AlbumInfoTable) {
        val dao = songWithAlbumDao ?: return
        album.dbId ?: return
        //判断歌曲是否存在
        val songWithAlbums = mutableListOf<SongWithAlbum>()
        songs.forEach {
            dao.queryBuilder().where(
                SongWithAlbumDao.Properties.SongId.eq(it.dbId),
                SongWithAlbumDao.Properties.AlbumId.eq(album.dbId)
            ).unique()?.let {
                songWithAlbums.add(it)
            }
        }
        if (songWithAlbums.isNotEmpty()) {
            dao.deleteInTx(songWithAlbums)
        }
    }

    /**
     * 删除歌单
     */
    fun deleteAlbum(id: Long) {
        removeAllSongsFromAlbum(id)
        albumInfoTableDao?.deleteByKey(id)
    }

    /**
     * 从歌单中移除全部歌曲
     */
    @Synchronized
    fun removeAllSongsFromAlbum(albumDbId: Long) {
        val dao = songWithAlbumDao ?: return
        val list = dao.queryBuilder().where(
            SongWithAlbumDao.Properties.AlbumId.eq(albumDbId)
        ).list()
        if (!list.isNullOrEmpty()) {
            dao.deleteInTx(list)
        }
    }

    /**
     * 通过dbId更新或者插入歌曲
     */
    @Synchronized
    private fun insetSong(song: SongInfoTable): Long? {
        val dao = songInfoTableDao ?: return null
        //判断歌曲是否存在
        val old = if (song.dbId != null) {
            dao.loadByRowId(song.dbId)
        } else {
            dao.queryBuilder().where(
                SongInfoTableDao.Properties.Id.eq(song.id),
                SongInfoTableDao.Properties.Source.eq(song.source)
            ).unique()
        }
        //由于更新导致耗时太长，目前处理，如不存在只新增
        return if (old == null) {
            dao.insert(song)
        } else {
            old.dbId
        }
    }

    @Synchronized
    fun insetOrUpdateSong(song: SongInfoTable): Long? {
        val dao = songInfoTableDao ?: return null
        //判断歌曲是否存在
        val old = if (song.dbId != null) {
            dao.loadByRowId(song.dbId)
        } else {
            dao.queryBuilder().where(
                SongInfoTableDao.Properties.Id.eq(song.id),
                SongInfoTableDao.Properties.Source.eq(song.source)
            ).unique()
        }
        if (old != null) {
            song.dbId = old.dbId
        }
        return dao.insertOrReplace(song)
    }

    /**
     * 插入或者替换歌手
     */
    @Synchronized
    private fun insetOrUpdateSinger(singer: SingerInfoTable): Long? {
        val dao = singerInfoTableDao ?: return null
        //判断歌曲是否存在
        val old = if (singer.dbId != null) {
            dao.loadByRowId(singer.dbId)
        } else {
            dao.queryBuilder().where(
                SingerInfoTableDao.Properties.Id.eq(singer.id),
                SingerInfoTableDao.Properties.Source.eq(singer.source)
            ).unique()
        }
        if (old != null) {
            singer.dbId = old.dbId
        }
        return dao.insertOrReplace(singer)
    }


    /**
     * 通过dbId更新或者插入歌单
     */
    @Synchronized
    fun insetOrUpdateAlbum(album: AlbumInfoTable): Long? {
        val dao = albumInfoTableDao ?: return null
        //判断专辑是否存在
        return if (album.dbId == null) {
            dao.insert(album)
        } else {
            dao.loadByRowId(album.dbId)?.apply {
                album.createTime = createTime
                album.dbId = dbId
            }
            if (album.des.isNullOrBlank()) {
                album.des = "暂无描述"
            }
            dao.insertOrReplace(album)
        }
    }

    /**
     * 直接插入，albumId是一样的
     */
    @Synchronized
    fun insetAlbum(album: AlbumInfoTable): Long? {
        val dao = albumInfoTableDao ?: return null
        if (album.des.isNullOrBlank()) {
            album.des = "暂无描述"
        }
        return dao.insertOrReplace(album)
    }

    /**
     * 通过歌曲id和歌单id，插入关联
     * 如果存在不更新
     */
    @Synchronized
    fun withSongAndAlbum(songId: Long?, albumId: Long?): Long? {
        val dao = songWithAlbumDao ?: return null
        songId ?: return null
        albumId ?: return null
        //判断歌曲是否存在
        val old = dao.queryBuilder().where(
            SongWithAlbumDao.Properties.SongId.eq(songId),
            SongWithAlbumDao.Properties.AlbumId.eq(albumId)
        ).unique()
        if (old != null) {
            return old.id
        }
        val time = System.currentTimeMillis()
        return dao.insert(SongWithAlbum(null, albumId, songId, time, time))
    }

    /**
     * 通过歌曲id和歌单id，插入关联
     * 如果存在不更新
     */
    @Synchronized
    fun withSongAndSinger(song: SongInfoTable, singers: MutableList<SingerInfoTable>?) {
        val dao = songWithSingerDao ?: return
        song.dbId = insetOrUpdateSong(song)
        val songAndSingerInfoTable = mutableListOf<SongWithSinger>()
        singers?.forEach {
            it.dbId = insetOrUpdateSinger(it)
            //判断歌曲是否存在
            val old = dao.queryBuilder().where(
                SongWithSingerDao.Properties.SongId.eq(song.dbId),
                SongWithSingerDao.Properties.SingerId.eq(it.dbId)
            ).unique()
            if (old != null) {
                songAndSingerInfoTable.add(old)
            } else {
                val time = System.currentTimeMillis()

                songAndSingerInfoTable.add(SongWithSinger(null, it.dbId, song.dbId, time, time))
            }
        }
        if (songAndSingerInfoTable.isNotEmpty()) {
            dao.insertOrReplaceInTx(songAndSingerInfoTable)
        }
    }
}