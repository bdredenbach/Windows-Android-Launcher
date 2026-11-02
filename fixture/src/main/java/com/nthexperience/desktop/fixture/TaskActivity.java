package com.nthexperience.desktop.fixture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import java.util.UUID;

/** A separate UID/task with unsaved state, used only in emulator tests. */
public final class TaskActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);if(getIntent().getBooleanExtra("finish-task",false)){finishAndRemoveTask();return;}LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(30,60,30,30);TextView token=new TextView(this);token.setText("Instance "+UUID.randomUUID());token.setContentDescription("Task probe instance");root.addView(token);EditText note=new EditText(this);note.setContentDescription("Task probe note");note.setHint("Unsaved note");root.addView(note);setContentView(root);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);if(intent.getBooleanExtra("finish-task",false))finishAndRemoveTask();}
}
