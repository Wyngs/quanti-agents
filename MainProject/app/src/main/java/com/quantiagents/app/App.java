package com.quantiagents.app;

import android.app.Application;

import com.quantiagents.app.Services.ServiceLocator;

public class App extends Application {

    private ServiceLocator serviceLocator;

    @Override
    public void onCreate() {
        super.onCreate();
        // Spin up my shared locator once.
        serviceLocator = new ServiceLocator(this);
    }

    // Hand this out so everything hits the same graph.
    public ServiceLocator locator() {
        return serviceLocator;
    }
}
