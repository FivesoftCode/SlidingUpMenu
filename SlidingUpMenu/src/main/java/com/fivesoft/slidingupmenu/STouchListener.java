package com.fivesoft.slidingupmenu;

/*
Class which makes single-finger and multi-finger gestures
detecting easy.
 */

import android.graphics.PointF;
import android.os.Build;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public class STouchListener {

    private final SparseArray<PointF> activePoints = new SparseArray<>();
    private float d = 0;
    private int f = 0;
    private int fs = 0;
    private long t = System.currentTimeMillis();
    private OnTouchEvent onTouchEvent;
    private boolean longClickPerformed = false;
    protected View view;
    private boolean returnRawCoordinates = false;

    private static final long LONG_CLICK_TIME = 300;

    private int requiredFingerCount = -1;

    public static final int MAX_FINGER_COUNT = 20;

    public void setRequiredFingerCount(int i){
        this.requiredFingerCount = i;
    }

    public void setOnTouchEventListener(OnTouchEvent onTouchEventListener){
        this.onTouchEvent = onTouchEventListener;
    }

    public STouchListener(View view, OnTouchEvent onTouchEvent){
        this.onTouchEvent = onTouchEvent;
        this.view = view;
        setOnTouchListener(view);
    }

    private void setOnTouchListener(View view){
        view.setOnTouchListener((v, event) -> {
            // get pointer index from the event object
            int pointerIndex = event.getActionIndex();

            // get pointer ID
            int pointerId = event.getPointerId(pointerIndex);

            // get masked (not specific to a pointer) action
            int maskedAction = event.getActionMasked();

            switch (maskedAction) {

                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    // We have a new pointer. Lets add it to the list of pointers
                    PointF f = new PointF();
                    f.x = getPointerX(event, pointerIndex);
                    f.y = getPointerY(event, pointerIndex);
                    activePoints.put(pointerId, f);
                    //User started touch
                    t = System.currentTimeMillis();
                    longClickPerformed = false;
                    break;
                }
                case MotionEvent.ACTION_MOVE: { // a pointer was moved

                    for (int i = 0; i < event.getPointerCount(); i++) {
                        PointF point = activePoints.get(event.getPointerId(i));
                        if (point != null) {
                            d += (point.x - getPointerX(event, i) + point.y - getPointerY(event, i)) / 2;

                            if(Math.abs(t - System.currentTimeMillis()) > LONG_CLICK_TIME && !longClickPerformed && f == 1 && view.isLongClickable() && Math.abs(d) < 20){
                                //Perform long click
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    longClickPerformed = view.performLongClick(point.x, point.y);
                                else
                                    longClickPerformed = view.performLongClick();
                            }

                            if(Math.abs(d) > 3 && f != event.getPointerCount() && Math.abs(t - System.currentTimeMillis()) > 100 && fs == 0) {
                                d = 0;
                                f = event.getPointerCount();
                                fs = f;
                                if(requiredFingerCount == f || requiredFingerCount == -1)
                                    onTouchEvent.onTouchStarted(f, getActivePoints());

                            }
                        }
                        activePoints.put(event.getPointerId(i), new PointF(getPointerX(event, i), getPointerY(event, i)));
                    }

                    if(fs == event.getPointerCount() || fs == 0)
                        if(activePoints.size() == f)
                            if(requiredFingerCount == f || requiredFingerCount == -1)
                                onTouchEvent.onMove(f, getActivePoints());

                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if(f != 0 && activePoints.size() - 1 <= 0) {
                        if(requiredFingerCount == f || requiredFingerCount == -1)
                            onTouchEvent.onTouchEnd(f, getActivePoints());
                        view.performClick();
                        f = 0;
                        fs = 0;
                    }
                    activePoints.remove(pointerId);
                    t = System.currentTimeMillis();
                    longClickPerformed = false;
                    break;
                }
            }
            return true;
        });
    }

    private PointF[] getActivePoints(){
        PointF[] res = new PointF[MAX_FINGER_COUNT];
        for(int i = 0; i < activePoints.size(); i++){
            if(i >= MAX_FINGER_COUNT)
                break;
            res[i] = activePoints.get(activePoints.keyAt(i));
        }
        return res;
    }

    public interface OnTouchEvent{
        void onTouchStarted(int fingers, PointF[] points);
        void onMove(int fingers, PointF[] points);
        void onTouchEnd(int fingers, PointF[] points);
    }

    public void setReturnRawCoordinates(boolean b){
        this.returnRawCoordinates = b;
    }

    private float getPointerX(MotionEvent motionEvent, int pointer){

        if(!returnRawCoordinates)
            return motionEvent.getX(pointer);

        final int[] location = { 0, 0 };
        view.getLocationOnScreen(location);
        return (int) motionEvent.getX(pointer) + location[0];
    }

    private float getPointerY(MotionEvent motionEvent, int pointer){

        if(!returnRawCoordinates)
            return motionEvent.getY(pointer);

        final int[] location = { 0, 0 };
        view.getLocationOnScreen(location);
        return motionEvent.getY(pointer) + location[1];
    }

}
