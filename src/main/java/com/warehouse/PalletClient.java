package com.warehouse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PalletClient implements Closeable, AutoCloseable {

    URI root;
    CloseableHttpClient httpclient;
    BasicCookieStore cookieStore;
    String _csrf;

    public Parameters in, out;

    static Logger log = Logger.getLogger(PalletClient.class.getName());

    public PalletClient(String root) throws URISyntaxException {
        this.root = new URI(root);
        // connection manager
        BasicHttpClientConnectionManager connectionManager;
        try {
            TrustStrategy trustStrategy = (cert, auth) -> true;
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", factory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();
            connectionManager = new BasicHttpClientConnectionManager(registry);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        // cookie
        cookieStore = new BasicCookieStore();
        // client
        httpclient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)
                .disableRedirectHandling()
                .build();
        in = new Parameters();
        out = new Parameters();
    }

    public void login(String username, String password) throws IOException, ProtocolException, URISyntaxException {
        URI uri = root.resolve("login");
        // get
        HttpGet get = new HttpGet(uri);
        CloseableHttpResponse response = httpclient.execute(get);
        String html = new BasicHttpClientResponseHandler().handleResponse(response);
        response.close();
        Document document = Jsoup.parse(html);
        _csrf = document.forms().get(0)
                .select("input[type='hidden'][name='_csrf']").first()
                .attr("value");
        // post
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("password", password));
        nameValuePairs.add(new BasicNameValuePair("_csrf", _csrf));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs);
        HttpPost post = new HttpPost(uri);
        post.setEntity(entity);
        response = httpclient.execute(post);
        response.close();
        URI location = new URI(response.getHeader("Location").getValue());
        if (!location.getPath().equals("/")) {
            throw new RuntimeException("Login failed with response code: " + response.getCode());
        }
        // csrf
        get = new HttpGet(root);
        response = httpclient.execute(get);
        html = new BasicHttpClientResponseHandler().handleResponse(response);
        response.close();
        document = Jsoup.parse(html);
        _csrf = document.head()
                .select("meta[name='_csrf']").first()
                .attr("content");
        log.info("Login success for " + uri);
    }

    public void post(String path) throws IOException {
        URI uri = root.resolve(path);
        HttpPost post = new HttpPost(root.resolve(path));
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        in.addAll(entityBuilder);
        in.clear();
        entityBuilder.addTextBody("_csrf", _csrf);
        HttpEntity entity = entityBuilder.build();
        post.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(post);
        response.close();
        if (response.getCode() != 200) {
            throw new RuntimeException("POST failed with response code: " + response.getCode());
        }
        log.info("POST success for " + uri);
    }

    public void get(String path) throws IOException, ParseException {
        URI uri = root.resolve(path);
        HttpGet get = new HttpGet(root.resolve(path));
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        in.addAll(entityBuilder);
        in.clear();
        HttpEntity entity = entityBuilder.build();
        get.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(get);
        String content = EntityUtils.toString(response.getEntity());
        response.close();
        if (response.getCode() != 200) {
            throw new RuntimeException("GET failed with response code: " + response.getCode() + " and content: " + content);
        }
        out.writeAll(content);
        log.info("GET success for " + uri);
    }

    @Override
    public void close() throws IOException {
        if (httpclient != null) {
            httpclient.close();
        }
    }
}
