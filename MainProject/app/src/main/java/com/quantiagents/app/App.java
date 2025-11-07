package com.quantiagents.app;

import android.app.Application;
import androidx.annotation.Nullable;
import com.quantiagents.app.Services.ServiceLocator;

public class App extends Application {

    // Real app graph (built on first access).
    private @Nullable ServiceLocator realLocator;

    // Test override graph.
    private @Nullable ServiceLocator testLocator;

//    @Override
//    public void onCreate() {
//        super.onCreate();
//        // Spin up my shared locator once.
//        serviceLocator = new ServiceLocator(this);
//    }

    // Hand this out so everything hits the same graph.
    /**
     * Tests call this in @Before (see TestDataSetup.seedData()).
     * When set, everything in the app will use this locator instead
     * of constructing the real one.
     */
    public void setTestLocator(@Nullable ServiceLocator locator) {
        this.testLocator = locator;
    }

    /**
     * Single access point for DI graph.
     * - If a test locator was set, use it.
     * - Otherwise, lazily construct the real one with a valid Application context.
     */
    public synchronized ServiceLocator locator() {
        if (testLocator != null) return testLocator;
        if (realLocator == null) {
            realLocator = new ServiceLocator(this); // <-- valid, non-null context
        }
        return realLocator;
    }
}
