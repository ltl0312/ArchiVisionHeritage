package com.zhiguan.gujian.notification.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiguan.gujian.notification.domain.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
