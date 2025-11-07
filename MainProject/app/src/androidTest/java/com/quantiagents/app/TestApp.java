package com.quantiagents.app;

import com.quantiagents.app.Services.ServiceLocator;

/** Application used only during androidTest runs. */
public class TestApp extends App {
    @Override
    public void onCreate() {
        super.onCreate();
        // Build an isolated locator tied to a real instrumentation context
        ServiceLocator testLocator = new ServiceLocator(this);
        // Make tests use this locator (your TestDataSetup will swap in fakes)
        setTestLocator(testLocator);
    }
}
