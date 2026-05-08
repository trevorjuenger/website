package com.openclaw.facebookfocus

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

private const val TAB_MESSAGES = 1
private const val TAB_NOTIFICATIONS = 2
private const val TAB_MARKETPLACE = 3
private const val TAB_POST = 4

private const val HOME_URL = "https://m.facebook.com/messages"
private const val MESSAGES_URL = "https://www.facebook.com/messages/"
private const val MARKETPLACE_URL = "https://www.facebook.com/marketplace/"
private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
private val TAB_URLS = mapOf(
    TAB_MESSAGES to MESSAGES_URL,
    TAB_NOTIFICATIONS to "https://m.facebook.com/notifications.php",
    TAB_MARKETPLACE to MARKETPLACE_URL,
)

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var topBar: View
    private lateinit var defaultUserAgent: String
    private var currentTab: Int = TAB_MESSAGES
    private var messagesMode: Int = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.userAgentString = settings.userAgentString.replace("wv", "")
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = FocusWebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message,
                ): Boolean {
                    val child = WebView(this@MainActivity).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        settings.userAgentString = view.settings.userAgentString
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = FocusWebViewClient()
                    }
                    child.webChromeClient = this@MainActivity.webView.webChromeClient
                    child.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            this@MainActivity.webView.loadUrl(url)
                            return true
                        }
                    }
                    (resultMsg.obj as WebView.WebViewTransport).webView = child
                    resultMsg.sendToTarget()
                    return true
                }
            }
            setBackgroundColor(Color.WHITE)
        }
        defaultUserAgent = webView.settings.userAgentString

        topBar = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, dp(56)).apply {
                gravity = Gravity.TOP
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { refreshCurrentTab() }

            val logo = ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_focusbook_foreground_image)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                adjustViewBounds = true
            }
            addView(
                logo,
                FrameLayout.LayoutParams(dp(38), dp(38)).apply {
                    gravity = Gravity.CENTER
                }
            )
        }

        val nav = BottomNavigationView(this).apply {
            labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
            menu.add(0, TAB_MESSAGES, 0, "Messages").setIcon(android.R.drawable.ic_dialog_email)
            menu.add(0, TAB_NOTIFICATIONS, 1, "Alerts").setIcon(android.R.drawable.ic_dialog_info)
            menu.add(0, TAB_MARKETPLACE, 2, "Market").setIcon(android.R.drawable.ic_menu_view)
            menu.add(0, TAB_POST, 3, "Post").setIcon(android.R.drawable.ic_menu_edit)
            setOnItemSelectedListener { item ->
                openTab(item.itemId)
                true
            }
        }

        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

            addView(
                webView,
                FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    bottomMargin = dp(64)
                }
            )

            addView(topBar)

            addView(
                nav,
                FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM
                }
            )
        }

        setContentView(root)

        nav.selectedItemId = TAB_MESSAGES

        if (savedInstanceState == null) {
            openTab(TAB_MESSAGES)
        }
    }

    private fun openTab(tabId: Int) {
        currentTab = tabId
        if (tabId == TAB_POST) {
            topBar.visibility = View.GONE
            webView.settings.userAgentString = defaultUserAgent
            showComposer()
        } else {
            topBar.visibility = View.VISIBLE
            if (tabId == TAB_MESSAGES || tabId == TAB_MARKETPLACE) {
                messagesMode = 0
                webView.settings.userAgentString = DESKTOP_USER_AGENT
            } else {
                webView.settings.userAgentString = defaultUserAgent
            }
            webView.loadUrl(TAB_URLS[tabId] ?: HOME_URL)
        }
    }

    private fun refreshCurrentTab() {
        if (currentTab == TAB_POST) {
            showComposer()
            return
        }
        topBar.visibility = View.VISIBLE
        if (currentTab == TAB_MESSAGES || currentTab == TAB_MARKETPLACE) {
            messagesMode = 0
            webView.settings.userAgentString = DESKTOP_USER_AGENT
        }
        webView.loadUrl(TAB_URLS[currentTab] ?: HOME_URL)
    }

    private fun allowedHost(url: String): Boolean {
        return url.contains("facebook.com") || url.contains("messenger.com") || url.contains("fbcdn.net") || url.contains("fbsbx.com") || url.contains("facebook.net") || url.contains("fb.me") || url.contains("l.facebook.com") || url.contains("lm.facebook.com")
    }

    private fun blockedPath(url: String): Boolean {
        val lower = url.lowercase()
        return listOf(
            "/reel",
            "/reels",
            "/watch",
            "/video",
            "/videos",
            "/story",
            "/stories",
            "/home.php",
        ).any { token -> lower.contains(token) && !TAB_URLS.values.any { lower.startsWith(it.lowercase()) } }
    }

    private fun isMessengerDownload(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("messenger.com/download") || lower.contains("download-messenger") || lower.startsWith("fb-messenger://") || lower.startsWith("intent://") || lower.contains("play.google.com/store/apps/details?id=com.facebook.orca")
    }

    private fun openDesktopMessages(view: WebView?) {
        messagesMode = 1
        view?.settings?.userAgentString = DESKTOP_USER_AGENT
        view?.loadUrl(MESSAGES_URL)
    }

    private fun showMessagesFallback() {
        webView.loadDataWithBaseURL(
            "https://www.facebook.com/",
            messagesFallbackHtml(),
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun messagesFallbackHtml(): String = """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            body { margin:0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background:#f0f2f5; color:#1c1e21; }
            .wrap { padding:18px; }
            .card { background:#fff; border-radius:18px; box-shadow:0 4px 18px rgba(0,0,0,.08); padding:18px; }
            h1 { margin:0 0 8px; font-size:22px; }
            p { margin:0 0 14px; line-height:1.45; color:#65676b; }
            .btn { display:block; background:#1877f2; color:#fff; text-decoration:none; padding:14px 16px; border-radius:12px; font-weight:700; text-align:center; margin-top:10px; }
            .secondary { background:#e4e6eb; color:#1c1e21; }
          </style>
        </head>
        <body>
          <div class="wrap">
            <div class="card">
              <h1>Messages is blocked</h1>
              <p>Facebook keeps redirecting this app to the Messenger download flow.</p>
              <a class="btn" href="https://www.facebook.com/messages/">Try desktop Messages</a>
              <a class="btn secondary" href="https://mbasic.facebook.com/messages/">Try basic mobile Messages</a>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()

    private fun fallbackUrl(): String = TAB_URLS[currentTab] ?: HOME_URL

    private fun showComposer() {
        webView.loadDataWithBaseURL(
            "https://m.facebook.com/",
            composerHtml(),
            "text/html",
            "UTF-8",
            null,
        )
    }

    private fun composerHtml(): String = """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            :root { color-scheme: light; }
            body {
              margin: 0;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              background: #f0f2f5;
              color: #1c1e21;
            }
            .wrap {
              padding: 18px;
            }
            .card {
              background: #fff;
              border-radius: 18px;
              box-shadow: 0 4px 18px rgba(0,0,0,.08);
              padding: 18px;
            }
            h1 { margin: 0 0 8px; font-size: 22px; }
            p { margin: 0 0 14px; line-height: 1.45; color: #65676b; }
            a.btn {
              display: inline-block;
              background: #1877f2;
              color: #fff;
              text-decoration: none;
              padding: 14px 16px;
              border-radius: 12px;
              font-weight: 700;
              width: 100%;
              box-sizing: border-box;
              text-align: center;
            }
            .hint {
              margin-top: 12px;
              font-size: 13px;
              color: #8a8d91;
            }
          </style>
        </head>
        <body>
          <div class="wrap">
            <div class="card">
              <h1>New Post</h1>
              <p>This tab is now dedicated to creating a post instead of dumping you into the profile feed.</p>
              <a class="btn" href="https://m.facebook.com/composer/">Open Facebook composer</a>
              <div class="hint">If Facebook changes the composer URL again, I’ll swap in a better path.</div>
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()

    private inner class FocusWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            if (isMessengerDownload(url)) {
                if ((currentTab == TAB_MESSAGES || currentTab == TAB_MARKETPLACE) && messagesMode == 0) {
                    openDesktopMessages(view)
                } else {
                    showMessagesFallback()
                }
                return true
            }
            if (!allowedHost(url)) return true
            if (blockedPath(url)) {
                view?.loadUrl(fallbackUrl())
                return true
            }
            return false
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) {
                if ((currentTab == TAB_MESSAGES || currentTab == TAB_MARKETPLACE) && messagesMode == 0) {
                    openDesktopMessages(view)
                } else {
                    showMessagesFallback()
                }
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            if (request.isForMainFrame) {
                if ((currentTab == TAB_MESSAGES || currentTab == TAB_MARKETPLACE) && messagesMode == 0) {
                    openDesktopMessages(view)
                } else {
                    showMessagesFallback()
                }
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            if (currentTab == TAB_POST || currentTab == TAB_MESSAGES || currentTab == TAB_MARKETPLACE) return
            if (shouldInjectFocusScript(url)) {
                view.evaluateJavascript(FOCUS_SCRIPT, null)
            }
        }
    }

    companion object {
        private const val FOCUS_SCRIPT = """
            (function() {
              try {
                const styleId = 'focusbook-style';
                let style = document.getElementById(styleId);
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  style.textContent = `
                    a[href*='/reel/'], a[href*='/reels/'], a[href*='/watch/'], a[href*='/videos/'],
                    [aria-label*='Reels'], [aria-label*='Videos'],
                    div[data-pagelet*='FeedUnit'], div[role='feed'],
                    a[href*='home.php'], a[href*='story'], a[href*='stories'] {
                      display: none !important;
                      visibility: hidden !important;
                      height: 0 !important;
                      width: 0 !important;
                      overflow: hidden !important;
                    }
                    body { overflow-x: hidden !important; }
                  `;
                  document.head.appendChild(style);
                }

                const pruneSponsored = () => {
                  const nodes = Array.from(document.querySelectorAll('span, div'));
                  for (const node of nodes) {
                    const text = (node.textContent || '').trim();
                    if (text === 'Sponsored' || text === 'Ad') {
                      let target = node;
                      for (let i = 0; i < 4 && target; i++) target = target.parentElement;
                      if (target && target.remove) target.remove();
                    }
                  }
                };

                pruneSponsored();
                const obs = new MutationObserver(pruneSponsored);
                obs.observe(document.documentElement, { childList: true, subtree: true });
              } catch (e) {
                console.log('focusbook injection failed', e);
              }
            })();
        """
    }

    private fun shouldInjectFocusScript(url: String): Boolean {
        val lower = url.lowercase()
        if (currentTab == TAB_MARKETPLACE) return false
        return currentTab != TAB_POST && currentTab != TAB_MESSAGES
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
