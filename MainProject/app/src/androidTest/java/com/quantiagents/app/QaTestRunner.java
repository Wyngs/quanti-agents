package com.quantiagents.app;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

/** Forces instrumentation to use TestApp instead of the real App. */
public class QaTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // Use our TestApp for the target apk under test
        return super.newApplication(cl, TestApp.class.getName(), context);
    }
}
