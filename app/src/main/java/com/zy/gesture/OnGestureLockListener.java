package com.zy.gesture;

import java.util.ArrayList;

public interface OnGestureLockListener {
    void onError(String errorMsg);
    void onGestureSuccess(ArrayList<Integer> selectPoints);//绘制成功
    void onStartGestureLock();//开始绘制
}
