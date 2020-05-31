package nl.frankkie.webawake

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val TAG = "WebAwake"
    //Change this to the actual URL
    //private val initialUrl = "file:///android_asset/index.html"
    //private val initialUrl = "https://test.example.org/"
    private val initialUrl = "https://test.webrtc.org"


    private val urlContainsForInternalBrowser = "webrtc.org"

    private val requiredPermission = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private val reqCodePermission = 1337

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Keep screen on; using WindowMnager
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initUI()
        askForPermissions()
        askForWakeLock()
        setWebViewSettings()

        //Set URL
        mainWebView.loadUrl(initialUrl)
    }

    private fun initUI() {
        setContentView(R.layout.activity_main)
    }

    private fun askForPermissions() {
        requestPermissions(requiredPermission, reqCodePermission)
    }

    private fun askForWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WebAwake:PartialWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebViewSettings(){
        //Keep the screen on; via View
        mainWebView.keepScreenOn = true

        //WebViewClient
        val myWebViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (!url.contains(urlContainsForInternalBrowser)) {
                    //Open in external browser
                    view.context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                    return true
                }
                //Load here, in this app
                return false
            }
        }
        mainWebView.webViewClient = myWebViewClient

        //WebChromeClient
        val myWebChromeClient = object : WebChromeClient() {
            // Grant permissions for cam
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    Log.d(TAG, request.origin.toString())
                    if (request.origin.toString().startsWith("file:///")
                        || request.origin.toString().contains(urlContainsForInternalBrowser)) {
                        Log.d(TAG, "GRANTED")
                        request.grant(request.resources)
                    } else {
                        Log.d(TAG, "DENIED")
                        request.deny()
                    }
                }
            }
        }
        mainWebView.webChromeClient = myWebChromeClient

        //WebSettings
        //if needed; uncomment and change some settings
        mainWebView.settings.apply {
            javaScriptEnabled = true
//            allowContentAccess = true
//            allowFileAccess = true
//            allowFileAccessFromFileURLs = true
//            allowUniversalAccessFromFileURLs = true
//            javaScriptCanOpenWindowsAutomatically = true
//            blockNetworkImage = false
//            blockNetworkLoads = false
//            builtInZoomControls = true
//            databaseEnabled = true
//            domStorageEnabled = true
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                forceDark = WebSettings.FORCE_DARK_AUTO
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                safeBrowsingEnabled = false
//            }
        }
    }

    ///System callbacks
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //TODO: check if permission are granted.
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && mainWebView.canGoBack()) {
            mainWebView.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
    }
}