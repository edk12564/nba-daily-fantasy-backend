package com.bigschlong.demo.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import software.pando.crypto.nacl.Crypto;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

@Component
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    private static final String PUBLIC_KEY = "455238ff8387a5d4731f3907d06331456ed3be385552a44c82e8f9efb23eb3e0";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            System.out.println("headers " + Collections.list(request.getHeaderNames()));
            // Extract necessary parameters from the request (assuming they are in headers or body)
            String timestamp = request.getHeader("X-Signature-Timestamp"); // Extract timestamp from headers (example)
            String body = extractRequestBody(request); // Utility method to extract the request body as a string
            String signature = request.getHeader("X-Signature-Ed25519"); // Extract signature from headers (example)

            System.out.println("timestamp:" + timestamp);
            System.out.println("body:" + body);
            System.out.println("signature:" + signature);

            // Perform signature verification
            boolean isVerified = Crypto.signVerify(
                    Crypto.signingPublicKey(parseHexBinary(PUBLIC_KEY)),
                    (timestamp + body).getBytes(StandardCharsets.UTF_8),
                    parseHexBinary(signature));

            // If the verification fails, reject the request
            if (!isVerified) {
                System.out.println("unverified");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Signature verification failed.");
                return false; // Prevent further handling of the request
            } else {
                // If verification succeeds, allow the request to proceed
                return true;
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Signature verification failed.");
            return false; // Prevent further handling of the request
        }
    }

    // Utility method to extract request body as a string (optional, modify as per your use case)
    private String extractRequestBody(HttpServletRequest request) throws Exception {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        }

        return null;
    }
}
