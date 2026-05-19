package com.stockpro.auth.service;

public interface JwtBlacklistService {

    void blacklist(String token);

    boolean isBlacklisted(String token);
}
