package com.joprelys.joprelys_mobile

import android.os.Bundle
import android.view.WindowManager
import io.flutter.embedding.android.FlutterFragmentActivity

class MainActivity : FlutterFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Clinical data: block screenshots, screen recording, casting to
        // non-secure displays and previews in the recent apps screen. Set
        // before the first frame is drawn.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
    }
}
