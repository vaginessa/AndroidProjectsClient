/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.contributionsview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.VolleyError;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by theo on 12/01/17.
 */

//https://github.com/javierugarte/GithubContributionsView/blob/master/githubcontributionsview/src/main/java/com/github/javierugarte/GitHubContributionsView.java
public class ContributionsView extends View implements ContributionsLoader.ContributionsRequestListener {
    private static final String TAG = ContributionsView.class.getSimpleName();

    private boolean shouldDisplayMonths;
    private boolean shouldDisplayDays;
    private int textColor;
    private int textSize;
    private int backGroundColor;
    private int weekCount;


    private List<ContributionsLoader.GitDay> contribs = new ArrayList<>();

    private Paint dayPainter;
    private Paint textPainter;

    private Rect rect;
    private final Rect textBounds = new Rect();
    private float gridY;

    public ContributionsView(Context context) {
        super(context);
        initView(context, null, 0, 0);
    }

    public ContributionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0, 0);
    }

    public ContributionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ContributionsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs, defStyleAttr, defStyleRes);
    }

    //TODO Calculate a minimum height when wrap content is used

    private void initView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        rect = new Rect();

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ContributionsView, defStyleAttr, defStyleRes);
        useAttributes(attributes);

        textPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayPainter.setStyle(Paint.Style.FILL);
    }

    private void useAttributes(TypedArray ta) {
        shouldDisplayMonths = ta.getBoolean(R.styleable.ContributionsView_showMonths, true);
        shouldDisplayDays = ta.getBoolean(R.styleable.ContributionsView_showDays, true);
        backGroundColor = ta.getColor(R.styleable.ContributionsView_backgroundColor, 0xD6E685); //GitHub default color
        textColor = ta.getColor(R.styleable.ContributionsView_textColor, Color.BLACK);
        textSize = ta.getDimensionPixelSize(R.styleable.ContributionsView_textSize, 7);
        if(ta.getString(R.styleable.ContributionsView_username) != null && !isInEditMode()) {
            loadUser(ta.getString(R.styleable.ContributionsView_username));
        }
    }

    public void loadUser(String user) {
        new ContributionsLoader(this).beginRequest(getContext(), user);
    }

    //TODO Allow setting text size

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(rect);

        //TODO Draw placeholder squares
        //TODO Draw day text

        final int w = rect.width();
        final int h = rect.height();

        final int hnum = contribs.size() == 0 ? 52 : contribs.size() / 7; //The number of days to show horizontally

        final float bd = (w / (float) hnum) * 0.9f; //The dimension of a single block
        final float m = (w / (float) hnum) - bd; //The margin around a block

        final float tm = shouldDisplayMonths ? textSize : 0; //Top margin if we are displaying months
        final float mth = shouldDisplayMonths ? textSize : 0; //Height of month text

        //Draw the background
        dayPainter.setColor(backGroundColor);
        canvas.drawRect(0, (tm * mth), w, h + mth, dayPainter);

        textPainter.setColor(textColor);
        textPainter.setTextSize(mth);
        float x = 0;
        if(contribs.size() > 0) {
            int dow = getDayOfWeek(contribs.get(0).date) - 1;
            float y = (dow * (bd + m)) + tm + mth;
            gridY = y;

            for(ContributionsLoader.GitDay d : contribs) {
                dayPainter.setColor(d.color);

                canvas.drawRect(x, y, x + bd, y + bd, dayPainter);
                dow = getDayOfWeek(d.date) - 1;
                if(dow == 6) { //We just drew the last day of the week
                    x += bd + m;
                    y = tm + mth;
                } else {
                    y += bd + m;
                }

            }
        } else {
            int dow = 0;
            float y = tm + mth;
            gridY = y;
            dayPainter.setColor(Color.parseColor("#EEEEEE"));
            for(int i = 0; i < 364; i++) {
                canvas.drawRect(x, y, x + bd, y + bd, dayPainter);
                if(dow == 6) { //We just drew the last day of the week
                    x += bd + m;
                    y = tm + mth;
                    dow = 0;
                } else {
                    y += bd + m;
                    dow++;
                }
            }
        }
        if(shouldDisplayMonths) {
            if(contribs.size() > 0) {
                cal.setTimeInMillis(contribs.get(0).date);
            } else {
                cal.setTimeInMillis(System.currentTimeMillis());
                cal.add(Calendar.MONTH, -12);
            }
            x = 0;
            for(int i = 0; i < 12; i++) {
                final String month = getMonthName(cal.getTimeInMillis());
                textPainter.getTextBounds(month, 0, month.length(), textBounds);
                if(w > x + textBounds.width()) {
                    canvas.drawText(
                            month,
                            x,
                            mth,
                            textPainter
                    );
                }
                cal.add(Calendar.MONTH, 1);
                x += w / 12;
            }
        }
        final ViewGroup.LayoutParams lp =  getLayoutParams();
        lp.height = h;
        setLayoutParams(lp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i(TAG, "onTouchEvent: " + event.getAction());
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            final float cols = contribs.size() / 7f;
            final float pcr = event.getX() / rect.width();
            final int col = (int) (pcr * cols);
            if(event.getY() > textSize) {
                final float pcc = (event.getY() - gridY) / (rect.height() - gridY);
                final int row = (int) (pcc * 7);
                final int pos = (7 * col) + row;
                Log.i(TAG, "onTouchEvent: (" + row + ", " + col + ")");
                Log.i(TAG, "onTouchEvent: " + pos);
                if(pos < contribs.size() && pos >= 0) {
                    final String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date(contribs.get(pos).date));
                    Toast.makeText(getContext(), String.format("%1$d contributions on %2$s", contribs.get(pos).contributions, date), Toast.LENGTH_SHORT).show();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private static final Calendar cal = Calendar.getInstance();
    private int getDayOfWeek(long stamp) {
        cal.setTimeInMillis(stamp);
        //Day of week is indexed 1 to 7
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private boolean isFirstDayOfMonth(long stamp) {
        cal.setTimeInMillis(stamp);
        return (cal.get(Calendar.DAY_OF_MONTH)) == 1;
    }

    private static final SimpleDateFormat month = new SimpleDateFormat("MMM");
    private String getMonthName(long stamp) {
        return month.format(stamp);
    }

    public void setShouldDisplayMonths(boolean shouldDisplayMonths) {
        this.shouldDisplayMonths = shouldDisplayMonths;
    }

    public void setShouldDisplayDays(boolean shouldDisplayDays) {
        this.shouldDisplayDays = shouldDisplayDays;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setBackGroundColor(int backGroundColor) {
        this.backGroundColor = backGroundColor;
    }

    @Override
    public void onResponse(List<ContributionsLoader.GitDay> contributions) {
        contribs = contributions;
        invalidate();
    }

    @Override
    public void onError(VolleyError error) {

    }
}