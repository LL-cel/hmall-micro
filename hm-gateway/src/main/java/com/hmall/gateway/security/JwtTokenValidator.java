package com.hmall.gateway.security;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

@Component
public class JwtTokenValidator {
    private final JWTSigner jwtSigner;

    public JwtTokenValidator(@Value("${hm.jwt.public-key-location}") Resource certificateLocation)
            throws IOException, CertificateException {
        try (var input = certificateLocation.getInputStream()) {
            var certificate = CertificateFactory.getInstance("X.509").generateCertificate(input);
            this.jwtSigner = JWTSignerUtil.rs256(certificate.getPublicKey());
        }
    }

    public long validate(String token) {
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        JWT jwt;
        boolean validSignature;
        try {
            jwt = JWT.of(token).setSigner(jwtSigner);
            validSignature = jwt.verify();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的token", e);
        }
        if (!validSignature) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的token");
        }

        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token已经过期", e);
        }

        var user = jwt.getPayload("user");
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的token");
        }
        try {
            return Long.parseLong(user.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无效的token", e);
        }
    }
}
