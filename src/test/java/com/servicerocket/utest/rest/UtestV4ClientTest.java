package com.servicerocket.utest.rest;

import com.google.common.base.Predicate;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.System.getProperty;
import static java.lang.System.out;
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
            new URL("https://mytest.utest.com/platform/services/v4/rest/auth/login?username=" + getUsername() + "&password=" + getPassword() + "&_method=POST").openConnection().getContent();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        List<HttpCookie> cookies = ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();
        return from(cookies).firstMatch(/* (Predicate) (cookie) -> { */new Predicate<HttpCookie>() { @Override public boolean apply(@javax.annotation.Nullable HttpCookie cookie) {
            return "UTEST_XREF".equals(cookie.getName());
        }}/* } */).get().toString();

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

        out.println("Supported message converters are: ");
        for (HttpMessageConverter<?> c: client.getMessageConverters()) {
            out.println("\t" + c.getClass().toString() + ": " + c.getSupportedMediaTypes());
        }
    }

    @Test public void testHttp() throws Exception {

        final List<Integer> bugIds = new ArrayList<Integer>();
        bugIds.add(725819);

        Object r = client.exchange(
            "https://mytest.utest.com/platform/services/v4/rest/bugs/bfv",
            POST,
            new HttpEntity<MultiValueMap<String, Object>>(
                new LinkedMultiValueMap<String, Object>() {{
                    add("file", new FileSystemResource(new File(getFilePath())));
                    add("bugId", bugIds);
                    add("comment", random(10));
                }},
                new LinkedMultiValueMap<String, String>() {{
                    add("Content-Type", MULTIPART_FORM_DATA.toString());
                    add("Accept", APPLICATION_JSON_VALUE);
                }}
            ),
            Map.class
        );

        out.println(r);

    }
}
