package com.lenovo.way.newgesturetest;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * @author way
 * @data 2017/6/2
 * @description Data receive.
 */

public interface DataReceive {


    void setGestureInfo(final int fps, final int exp, final int longshort, final int fingercnt, final int eventCode);

    void setSetInfo(final int set_fps, final int set_exp, final int set_range);

    void setDepthImageData(int[] imgData);

    void setIRImageData(int[] imgData);

    void delFiles();

}
