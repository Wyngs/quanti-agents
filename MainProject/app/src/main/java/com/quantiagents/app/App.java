package com.quantiagents.app;

import android.app.Application;
import androidx.annotation.Nullable;
import com.quantiagents.app.Services.ServiceLocator;

/**
 * Application class that provides dependency injection through ServiceLocator.
 * Supports both production and test environments by allowing test locator override.
 */
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
     * Sets a test locator for dependency injection during testing.
     * Tests call this in @Before (see TestDataSetup.seedData()).
     * When set, everything in the app will use this locator instead
     * of constructing the real one.
     *
     * @param locator The ServiceLocator instance to use for testing, or null to use production locator
     */
    public void setTestLocator(@Nullable ServiceLocator locator) {
        this.testLocator = locator;
    }

    /**
     * Single access point for dependency injection graph.
     * - If a test locator was set, use it.
     * - Otherwise, lazily construct the real one with a valid Application context.
     *
     * @return The ServiceLocator instance (test or production)
     */
    public synchronized ServiceLocator locator() {
        if (testLocator != null) return testLocator;
        if (realLocator == null) {
            realLocator = new ServiceLocator(this); // <-- valid, non-null context
        }
        return realLocator;
    }
}
