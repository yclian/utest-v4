package com.servicerocket.utest.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.System.getProperty;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * @author yclian
 * @since 20130813
 */
public class UtestV4ClientTest {

    RestTemplate client;

    static String getFilePath() {
        return getProperty("utest.file");
    }

    static String getUsername() {
        return getProperty("utest.username");
    }

    static String getPassword() {
        return getProperty("utest.password");
    }


    static String getXrefToken()  {

        CookieHandler.setDefault(new CookieManager());

        try {
            new URL("https://mytest.utest.com/platform/services/v4/rest/auth/login?username=" + getUsername() + "&password=" + getPassword()).openConnection().getContent();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        List<HttpCookie> cookies = ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();
        return FluentIterable.from(cookies).firstMatch(new Predicate<HttpCookie>() {
            @Override
            public boolean apply(@javax.annotation.Nullable HttpCookie input) {
                return "UTEST_XREF".equals(input.getName());
            }
        }).get().toString();

    }

    @Before public void setUp() throws Exception {

        client = new RestTemplate() {{
            setRequestFactory(new HttpComponentsClientHttpRequestFactory() {

                @Override
                protected void postProcessHttpRequest(HttpUriRequest request) {
                    if (null == request.getFirstHeader("Cookie") || !request.getFirstHeader("Cookie").getName().contains("UTEST_XREF")) {
                        request.setHeader("Cookie", getXrefToken());
                    }
                    super.postProcessHttpRequest(request);
                }
            });
        }};
    }

    @Test public void testHttp() throws Exception {
        Object r = client.exchange(
            "https://mytest.utest.com/platform/services/v4/rest/bugs/bfv",
            POST,
            new HttpEntity<MultiValueMap<String, Object>>(
                new LinkedMultiValueMap<String, Object>() {{
                    add("bugId", "725819");
                    add("file", new FileSystemResource(new File(getFilePath())));
                    add("comment", random(10));
                }},
                new HttpHeaders() {{
                    setContentType(MULTIPART_FORM_DATA);
                    add("Accept", APPLICATION_JSON_VALUE);
                }}
            ),
            Map.class
        );
    }
}
