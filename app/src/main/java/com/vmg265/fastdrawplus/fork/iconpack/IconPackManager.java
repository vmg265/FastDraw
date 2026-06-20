package com.vmg265.fastdrawplus.fork.iconpack;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPackManager {
    private static final String TAG = "IconPackManager";
    private final Context context;
    private final PackageManager pm;
    private final LruCache<String, Drawable> iconCache;
    private final Map<String, Map<String, String>> iconPacksAppFilters;

    private static IconPackManager instance;

    public static synchronized IconPackManager getInstance(Context context) {
        if (instance == null) {
            instance = new IconPackManager(context.getApplicationContext());
        }
        return instance;
    }

    private IconPackManager(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
        
        // Cache up to 10MB or use max memory fraction. Let's use maxMemory / 8
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        
        this.iconCache = new LruCache<String, Drawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, Drawable drawable) {
                // Return size in KB (approximated based on bounds if bitmap not easily accessible, or just use 1 as count)
                // Using simple count for simplicity
                return 1;
            }
        };
        this.iconPacksAppFilters = new HashMap<>();
    }

    public List<String> getAvailableIconPacks() {
        List<String> iconPacks = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory("com.anddoes.launcher.THEME");
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : resolveInfos) {
            iconPacks.add(info.activityInfo.packageName);
        }
        // Also add Apex intent
        Intent apexIntent = new Intent(Intent.ACTION_MAIN);
        apexIntent.addCategory("net.apexlauncher.THEME");
        List<ResolveInfo> apexInfos = pm.queryIntentActivities(apexIntent, PackageManager.GET_META_DATA);
        for (ResolveInfo info : apexInfos) {
            if (!iconPacks.contains(info.activityInfo.packageName)) {
                iconPacks.add(info.activityInfo.packageName);
            }
        }
        return iconPacks;
    }

    public String getIconPackLabel(String packageName) {
        try {
            return pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private Map<String, String> getAppFilter(String packageName) {
        if (iconPacksAppFilters.containsKey(packageName)) {
            return iconPacksAppFilters.get(packageName);
        }

        Map<String, String> appFilter = new HashMap<>();
        try {
            Resources res = pm.getResourcesForApplication(packageName);
            int resId = res.getIdentifier("appfilter", "xml", packageName);
            if (resId != 0) {
                XmlPullParser xpp = res.getXml(resId);
                parseAppFilter(xpp, appFilter);
            } else {
                // Try from assets
                try (InputStream is = pm.getResourcesForApplication(packageName).getAssets().open("appfilter.xml")) {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(is, "UTF-8");
                    parseAppFilter(xpp, appFilter);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing appfilter.xml from assets", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting appfilter for " + packageName, e);
        }

        iconPacksAppFilters.put(packageName, appFilter);
        return appFilter;
    }

    private void parseAppFilter(XmlPullParser xpp, Map<String, String> appFilter) throws Exception {
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "item".equals(xpp.getName())) {
                String component = xpp.getAttributeValue(null, "component");
                String drawable = xpp.getAttributeValue(null, "drawable");
                if (component != null && drawable != null) {
                    appFilter.put(component, drawable);
                }
            }
            eventType = xpp.next();
        }
    }

    public Drawable getDrawable(String iconPackPackage, String drawableName) {
        String cacheKey = iconPackPackage + ":" + drawableName;
        Drawable cached = iconCache.get(cacheKey);
        if (cached != null) {
            return cached.getConstantState().newDrawable().mutate();
        }

        try {
            Resources res = pm.getResourcesForApplication(iconPackPackage);
            int resId = res.getIdentifier(drawableName, "drawable", iconPackPackage);
            if (resId != 0) {
                Drawable drawable = res.getDrawable(resId, null);
                if (drawable != null) {
                    iconCache.put(cacheKey, drawable);
                    return drawable.getConstantState().newDrawable().mutate();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting drawable " + drawableName + " from " + iconPackPackage, e);
        }
        return null;
    }

    public Drawable getIcon(String iconPackPackage, ComponentName componentName) {
        if (iconPackPackage == null || iconPackPackage.isEmpty() || "system".equals(iconPackPackage)) {
            return null;
        }

        Map<String, String> appFilter = getAppFilter(iconPackPackage);
        String componentStr = "ComponentInfo{" + componentName.flattenToString() + "}";
        String drawableName = appFilter.get(componentStr);
        if (drawableName != null) {
            return getDrawable(iconPackPackage, drawableName);
        }
        return null;
    }

    public void clearCache() {
        iconCache.evictAll();
        iconPacksAppFilters.clear();
    }
    
    public List<String> getAllDrawables(String iconPackPackage) {
        List<String> drawables = new ArrayList<>();
        try {
            Resources res = pm.getResourcesForApplication(iconPackPackage);
            int resId = res.getIdentifier("drawable", "xml", iconPackPackage);
            if (resId != 0) {
                XmlPullParser xpp = res.getXml(resId);
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "item".equals(xpp.getName())) {
                        String drawable = xpp.getAttributeValue(null, "drawable");
                        if (drawable != null && !drawables.contains(drawable)) {
                            drawables.add(drawable);
                        }
                    }
                    eventType = xpp.next();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting drawables from " + iconPackPackage, e);
        }
        return drawables;
    }
}
