package com.zhiguan.gujian.community.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiguan.gujian.community.domain.LikeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    /**
     * 分页查询某用户点赞的帖子记录（created_at 倒序 — 与旧内存分页排序语义一致）。
     * IPage 必须为第一参数，PaginationInnerInterceptor 自动追加 COUNT + LIMIT。
     */
    @Select("SELECT * FROM like_record WHERE user_id = #{userId} AND target_type = 'POST' ORDER BY created_at DESC")
    IPage<LikeRecord> selectPageByUser(Page<LikeRecord> page, @Param("userId") Long userId);
}
