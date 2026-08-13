package com.zhiguan.gujian.community.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiguan.gujian.community.domain.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
