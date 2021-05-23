package com.hera.bangkit.data.repositories

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.hera.bangkit.data.entity.ReportEntity
import com.hera.bangkit.data.entity.StoryEntity
import com.idm.moviedb.vo.Resource

interface DataSource {
    fun insertStory(story : StoryEntity)
    fun getListStory(): LiveData<Resource<PagedList<StoryEntity>>>

    fun insertReport(report : ReportEntity)
    fun getListReport(): LiveData<Resource<PagedList<ReportEntity>>>
}