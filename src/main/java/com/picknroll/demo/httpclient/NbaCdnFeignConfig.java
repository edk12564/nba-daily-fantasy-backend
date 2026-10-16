package com.picknroll.demo.httpclient;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Headers matching {@code scripts/src/utils/nba_headers.py}. Required by cdn.nba.com
 * (Akamai); without them responses are 403. Not a {@code @Configuration} so these
 * beans apply only to Feign clients that reference this class.
 */
public class NbaCdnFeignConfig {

    public static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36";

    @Bean
    public RequestInterceptor nbaCdnHeadersInterceptor() {
        return template -> {
            template.header("User-Agent", USER_AGENT);
            template.header("Referer", "https://www.nba.com/");
            template.header("Origin", "https://www.nba.com");
            template.header("Accept", "application/json, text/plain, */*");
            template.header("Accept-Language", "en-US,en;q=0.9");
            template.header("Connection", "keep-alive");
            template.header("sec-ch-ua-platform", "\"macOS\"");
            template.header(
                    "sec-ch-ua",
                    "\"Brave\";v=\"153\", \"Not_A Brand\";v=\"8\", \"Chromium\";v=\"153\"");
            template.header("sec-ch-ua-mobile", "?0");
        };
    }
}
