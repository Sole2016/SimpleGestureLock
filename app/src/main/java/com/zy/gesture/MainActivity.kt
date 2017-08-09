package com.zy.gesture

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val gestureView = findViewById(R.id.simple_gesture) as LockPatternView
        gestureView.setGestureLockListener(object:OnGestureLockListener{
            override fun onError(errorMsg: String?) {
                Toast.makeText(this@MainActivity,errorMsg,Toast.LENGTH_SHORT).show()
            }

            override fun onGestureSuccess(selectPoints: ArrayList<Int>?) {
                Toast.makeText(this@MainActivity,selectPoints!!.toString(),Toast.LENGTH_SHORT).show()
            }

            override fun onStartGestureLock() {
                println("开始绘制")
            }

        })
    }
}
