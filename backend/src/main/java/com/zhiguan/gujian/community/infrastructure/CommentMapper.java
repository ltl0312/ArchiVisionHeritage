package com.zhiguan.gujian.community.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiguan.gujian.community.domain.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
