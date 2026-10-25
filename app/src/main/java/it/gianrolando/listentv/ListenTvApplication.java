package it.gianrolando.listentv;

public final class ListenTvApplication extends android.app.Application {
    @Override public void onCreate() {
        super.onCreate();
        Diagnostics.install(this);
    }
}
