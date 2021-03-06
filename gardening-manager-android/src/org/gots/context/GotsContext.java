package org.gots.context;

import android.content.Context;

import org.gots.preferences.GotsPreferences;
import org.nuxeo.android.context.NuxeoContext;

public class GotsContext extends NuxeoContext {

    public GotsContext(Context androidContext) {
        super(androidContext, new GotsPreferences(androidContext));
    }

    public static GotsContext get(Context gotsContextProvider) {
        if (gotsContextProvider != null && gotsContextProvider.getApplicationContext() instanceof GotsContextProvider) {
            GotsContextProvider nxApp = (GotsContextProvider) gotsContextProvider.getApplicationContext();
            return nxApp.getGotsContext();
        } else {
            throw new UnsupportedOperationException("Your application Context should implement GotsContextProvider !");
        }
    }

    @Override
    public GotsPreferences getServerConfig() {
        return (GotsPreferences) super.getServerConfig();
    }
}
