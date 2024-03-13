package com.example.webviewapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

public class WebViewActivity extends Activity {
    private WebView webView;
    private ImageView image_back;
    private String url;
    private String title;

    public static void jumpToWebViewActivity(Context context, String webUrl) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("webUrl", webUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_webview);
        initData();
        initView();
        initWebView();
    }

    private void initData() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            url = getIntent().getExtras().getString("webUrl");
            Log.d("WebViewActivity", "webUrl = " + url);
        }
    }

    private void initView() {
        webView = (WebView) findViewById(R.id.webView);
        image_back = findViewById(R.id.image_back);
        image_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void initWebView() {
        loadWebView(webView);
        if (!TextUtils.isEmpty(url)) {
            webView.loadUrl(url);
        }
    }

    private void loadWebView(WebView webView) {
        webView.setBackgroundColor(0);
        WebSettings webSettings = webView.getSettings();
        webSettings.setUserAgentString(webSettings.getUserAgentString());
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setDomStorageEnabled(true);
        // webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        /* Enable zooming */
        webSettings.setSupportZoom(false);
        webSettings.setSavePassword(false);
        // 设置可以访问文件
        webSettings.setAllowFileAccess(true);
        // 设置支持缩放
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        WebChromeClient webChromeClient = new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                Log.d("onReceivedTitle", "title = " + title);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }
        };
        webView.setWebChromeClient(webChromeClient);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}