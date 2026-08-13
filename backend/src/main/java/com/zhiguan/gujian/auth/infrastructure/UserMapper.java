package com.zhiguan.gujian.auth.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiguan.gujian.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
}
