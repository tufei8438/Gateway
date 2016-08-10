package com.hurray.gateway;

import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class RequestProxy {

    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private HttpUrl proxyUrl;

    private OkHttpClient httpClient = new OkHttpClient();

    public RequestProxy(String proxyUrl, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.proxyUrl = HttpUrl.parse(proxyUrl);
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    public HttpServletResponse doProxy() throws IOException {
        Response response = httpClient.newCall(buildRequest()).execute();
        buildServletResponse(response);
        return servletResponse;
    }

    private Request buildRequest() throws IOException {
        Request.Builder builder = new Request.Builder();
        builder.url(buildUrl());
        builder.headers(buildHeaders());
        builder.method(servletRequest.getMethod(), buildBody());
        return builder.build();
    }

    private String buildUrl() {
        HttpUrl.Builder builder = new HttpUrl.Builder();
        builder.scheme(proxyUrl.scheme());
        builder.host(proxyUrl.host());
        builder.encodedPath(servletRequest.getRequestURI());
        if (StringUtils.isNotEmpty(servletRequest.getQueryString())) {
            builder.encodedQuery(servletRequest.getQueryString());
        }
        return builder.build().toString();
    }

    private Headers buildHeaders() {
        Headers.Builder builder = new Headers.Builder();
        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.equalsIgnoreCase("Host")) {
                builder.add(headerName, proxyUrl.host());
            } else if (headerName.equalsIgnoreCase("Referer")) {
                HttpUrl srcRefererUrl = HttpUrl.parse(servletRequest.getHeader(headerName));
                HttpUrl.Builder dstRefererUrlBuilder = new HttpUrl.Builder();
                dstRefererUrlBuilder.scheme(srcRefererUrl.scheme());
                dstRefererUrlBuilder.host(proxyUrl.host());
                dstRefererUrlBuilder.encodedPath(srcRefererUrl.encodedPath());
                dstRefererUrlBuilder.encodedQuery(srcRefererUrl.encodedQuery());
                builder.add(headerName, dstRefererUrlBuilder.build().toString());
            } else {
                builder.add(headerName, servletRequest.getHeader(headerName));
            }
        }
        return builder.build();
    }

    private RequestBody buildBody() throws IOException {
        if (servletRequest.getContentLength() > 0) {
            MediaType contentType = MediaType.parse(servletRequest.getContentType());
            return RequestBody.create(contentType, getBytes(servletRequest));
        } else {
            return null;
        }
    }

    private byte[] getBytes(HttpServletRequest request) throws IOException {
        return IOUtils.toByteArray(request.getInputStream());
    }

    private void buildServletResponse(Response response) throws IOException {
        Headers headers = response.headers();
        for (String headerName : headers.names()) {
            servletResponse.addHeader(headerName, headers.get(headerName));
        }

        ResponseBody responseBody = response.body();
        InputStream in = responseBody.byteStream();
        ServletOutputStream out = servletResponse.getOutputStream();
        try {
            IOUtils.copy(responseBody.byteStream(), out);
            out.flush();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }


    }
}
