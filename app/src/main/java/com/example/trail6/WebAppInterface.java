package com.example.trail6;


import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

public class WebAppInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void fetchHospitalsFromLocation(double latitude, double longitude) {
        if (mContext instanceof MainActivity) {
            String latStr = String.valueOf(latitude);
            String lonStr = String.valueOf(longitude);
            ((MainActivity) mContext).fetchNearbyHospitals(latStr, lonStr);
        } else {
            // Handle the case where the context is not MainActivity
            // For example, show a toast or log an error
            Toast.makeText(mContext, "Context is not MainActivity", Toast.LENGTH_SHORT).show();
        }
    }

    @JavascriptInterface
    public void addMarker(double latitude, double longitude, String name) {
        if (mContext instanceof MainActivity) {
            String javascriptToAddMarker = "addMarker(" + latitude + ", " + longitude + ", '" + name + "')";
            ((MainActivity) mContext).runOnUiThread(() -> {
                WebView mapView = ((MainActivity) mContext).getMapView();
                if (mapView != null) {
                    mapView.loadUrl("javascript:" + javascriptToAddMarker);
                } else {
                    // Handle the case where mapView is null
                    // For example, show a toast or log an error
                    Toast.makeText(mContext, "MapView is null", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Handle the case where the context is not MainActivity
            // For example, show a toast or log an error
            Toast.makeText(mContext, "Context is not MainActivity", Toast.LENGTH_SHORT).show();
        }
    }
}