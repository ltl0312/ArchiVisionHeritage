package com.zhiguan.gujian.auth.application;

import com.zhiguan.gujian.auth.interfaces.LoginRequest;
import com.zhiguan.gujian.auth.interfaces.RegisterRequest;

public interface AuthService {

    String login(LoginRequest request);

    void register(RegisterRequest request);
}
