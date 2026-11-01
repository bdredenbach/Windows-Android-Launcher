package com.nthexperience.desktop;

/** An actual second APK/task for lifecycle checks; never packaged in the launcher APK. */
public final class TestAwayActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state){super.onCreate(state);android.widget.TextView text=new android.widget.TextView(this);text.setText("Temporary external app for lifecycle checks");text.setTextSize(20);text.setGravity(android.view.Gravity.CENTER);setContentView(text);}
}
