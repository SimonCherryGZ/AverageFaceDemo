package com.simoncherry.averageface;

import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by Simon on 2017/1/15.
 */

public class BitmapUtil {

    public static Bitmap getViewBitmap(View view){
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache().copy(Bitmap.Config.RGB_565, false);
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }
}
