package com.zhiguan.gujian.community.application;

import com.zhiguan.gujian.community.interfaces.CommentRequest;
import com.zhiguan.gujian.community.interfaces.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse addComment(Long userId, Long postId, CommentRequest request);

    List<CommentResponse> getComments(Long postId);
}
