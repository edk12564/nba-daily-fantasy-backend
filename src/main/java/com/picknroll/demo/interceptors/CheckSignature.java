package com.picknroll.demo.interceptors;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import software.pando.crypto.nacl.Crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;

public class CheckSignature {
    @Value("${DISCORD_API_PUBLIC_KEY}")
    private static String PUBLIC_KEY;

    public static String checkSignature(HttpServletRequest request) throws Exception {
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
                return null;
            } else {
                // If verification succeeds, allow the request to proceed
                return body;
            }

        } catch (Exception e) {
            return null; // Prevent further handling of the request
        }
    }

    // Utility method to extract request body as a string (optional, modify as per your use case)
    private static String extractRequestBody(HttpServletRequest request) throws Exception {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return convertToString(request.getInputStream());
        }

        return null;
    }

    public static String convertToString(ServletInputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }
}
