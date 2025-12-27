package com.picknroll.demo.interceptors;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CheckSignatureTest {

    @Test
    @DisplayName("convertToString reads input stream correctly")
    void convertToString_readsStream() throws IOException {
        String content = "{\"test\": \"value\"}";
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ServletInputStream sis = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bais.read();
            }
        };

        String result = CheckSignature.convertToString(sis);
        assertEquals(content, result);
    }

    @Test
    @DisplayName("convertToString handles empty stream")
    void convertToString_emptyStream() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        ServletInputStream sis = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bais.read();
            }
        };

        String result = CheckSignature.convertToString(sis);
        assertEquals("", result);
    }

    @Test
    @DisplayName("convertToString handles multiline content")
    void convertToString_multilineContent() throws IOException {
        String content = "line1\nline2\nline3";
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ServletInputStream sis = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bais.read();
            }
        };

        String result = CheckSignature.convertToString(sis);
        assertEquals("line1line2line3", result); // newlines are stripped by readLine
    }
}
