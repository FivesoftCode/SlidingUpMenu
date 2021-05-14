package com.fivesoft.slidingupmenu;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fivesoft.smartutil.Metrics;
import com.google.android.material.card.MaterialCardView;

public class SlideUpMenu extends LinearLayout {

    private View header;
    private View body;
    private MaterialCardView menu;

    private ValueAnimator animator = new ValueAnimator();
    private PointF touchStartPoint = new PointF();
    private PointF startPosition = new PointF();

    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_EXPANDED = 3;

    //Settings

    private int collapsedMenuOffset = 0;
    private int expandedMenuOffset = 0;

    private int cornerRadius = 0;
    private int elevation = 0;
    private int menuBackgroundColor = Color.WHITE;

    private SlideUpMenuListener listener;

    public SlideUpMenu(@NonNull Context context) {
        super(context);
        init();
    }

    public SlideUpMenu(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupAttrs(attrs);
        init();
    }

    public SlideUpMenu(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupAttrs(attrs);
        init();
    }

    private void setupAttrs(AttributeSet attributeSet){

        TypedArray a = getContext().getTheme().obtainStyledAttributes(attributeSet, R.styleable.SlideUpMenu, 0, 0);

        try{ header = LayoutInflater.from(getContext()).inflate(a.getResourceId(R.styleable.SlideUpMenu_headerView, -1), this, false); } catch (Exception e){ e.printStackTrace(); }
        try{ body = LayoutInflater.from(getContext()).inflate(a.getResourceId(R.styleable.SlideUpMenu_bodyView, -1), this, false); } catch (Exception e){ e.printStackTrace(); }

        collapsedMenuOffset = a.getDimensionPixelSize(R.styleable.SlideUpMenu_collapsedMenuOffset, 0);
        expandedMenuOffset = a.getDimensionPixelSize(R.styleable.SlideUpMenu_expandedMenuOffset, 0);

        elevation = a.getDimensionPixelSize(R.styleable.SlideUpMenu_menuElevation, 0);
        cornerRadius = a.getDimensionPixelSize(R.styleable.SlideUpMenu_cornerRadius, 0);

        menuBackgroundColor = a.getColor(R.styleable.SlideUpMenu_menuBackgroundColor, Color.WHITE);

        a.recycle();

        if(header == null || body == null)
            throw new RuntimeException("You must pass 'headerView' and 'bodyView' attributes in your xml layout file.");

    }

    private MaterialCardView createMenu(){
        MaterialCardView cardView = new MaterialCardView(getContext());
        cardView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        cardView.setCardElevation(elevation);
        cardView.setRadius(cornerRadius);
        cardView.setCardBackgroundColor(menuBackgroundColor);
        LinearLayout menu = new LinearLayout(getContext());
        menu.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.addView(header);
        menu.addView(body);
        menu.requestLayout();
        cardView.addView(menu);
        return cardView;
    }

    private void init(){
        menu = createMenu();

        setGravity(Gravity.BOTTOM);
        removeAllViews();
        addView(menu);
        initMenu();

        menu.setVisibility(INVISIBLE);
        header.post(() -> {
            moveMenu(maxY(), false);
            if(listener != null)
                listener.onSlide(menuHeight(), factor());
            menu.setVisibility(View.VISIBLE);
        });
    }


    private void initMenu(){
        STouchListener touchListener = new STouchListener(menu, new STouchListener.OnTouchEvent() {
            @Override
            public void onTouchStarted(int fingers, PointF[] points) {
                touchStartPoint = points[0];
                startPosition = new PointF(menu.getX(), menu.getY());
            }

            @Override
            public void onMove(int fingers, PointF[] points) {
                float y = Math.min(maxY(), Math.max(minY(), startPosition.y + points[0].y - touchStartPoint.y));
                moveMenu(y, false);
                if(listener != null)
                    listener.onSlide(menuHeight(),factor());
            }

            @Override
            public void onTouchEnd(int fingers, PointF[] points) {
                float y = Math.min(maxY(), Math.max(minY(), startPosition.y + points[0].y - touchStartPoint.y));
                if(Math.abs(startPosition.y - y) < Metrics.dpToPx(5, getContext())) {
                    setState(getStateForY(y), true);
                    return;
                }

                if(startPosition.y < y){
                    setState(STATE_COLLAPSED, true);
                } else {
                    setState(STATE_EXPANDED, true);
                }
            }
        });
        touchListener.setRequiredFingerCount(1);
        touchListener.setReturnRawCoordinates(true);
    }

    private void moveMenu(float y, boolean animate){
        if(animate){
            animator.setFloatValues(menu.getY(), y);
            animator.addUpdateListener(animation -> {
                menu.setY((float) animation.getAnimatedValue());

                if(listener == null)
                    return;

                listener.onSlide(menuHeight(), factor());
                if((float) animation.getAnimatedValue() == y)
                    listener.onStateChanged(getState());
            });
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(300);
            animator.start();
        } else {
            menu.setY(y);
        }
    }

    private float getYForState(int state){
        return state == STATE_COLLAPSED ? maxY() : minY();
    }

    private int getStateForY(float y){
        return ((float)(maxY() + minY()) / 2 > y) ? STATE_EXPANDED : STATE_COLLAPSED;
    }

    private float factor(){
        return 1 - (float) (menu.getY() - minY()) / (maxY() - minY());
    }

    private int minY(){
        return getHeight() - header.getHeight() - body.getHeight() - expandedMenuOffset;
    }

    private int maxY(){
        return getHeight() - header.getHeight() - collapsedMenuOffset;
    }

    private int menuHeight(){
        return getHeight() - (int) menu.getY();
    }

    public int getState(){
        return getStateForY(menu.getY());
    }

    public void setState(int state, boolean animate){
        moveMenu(getYForState(state), animate);
    }

    public void setCollapsedMenuOffset(int offset){
        this.collapsedMenuOffset = offset;
        moveMenu(maxY(), false);
    }

    public void setExpandedMenuOffset(int offset){
        this.expandedMenuOffset = offset;
        moveMenu(minY(), false);
    }

    public void setListener(SlideUpMenuListener listener){
        this.listener = listener;
    }

    public View getHeader(){
        return header;
    }

    public View getBody(){
        return body;
    }

    public interface SlideUpMenuListener {
        void onSlide(int menuHeight, float factor);
        void onStateChanged(int newState);
    }
}
