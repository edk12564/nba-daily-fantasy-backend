package com.bigschlong.demo.interceptors;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import software.pando.crypto.nacl.Crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

@Component
public class SignatureVerificationInterceptor implements HandlerInterceptor {

}
