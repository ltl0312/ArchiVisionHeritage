package com.zhiguan.gujian.service;

import com.zhiguan.gujian.dto.request.LoginRequest;
import com.zhiguan.gujian.dto.request.RegisterRequest;

public interface AuthService {

    String login(LoginRequest request);

    void register(RegisterRequest request);
}
