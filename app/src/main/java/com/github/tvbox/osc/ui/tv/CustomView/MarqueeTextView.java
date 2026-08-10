package com.github.tvbox.osc.ui.tv.CustomView;

import android.content.Context;
import android.util.AttributeSet;

public class MarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {
    public MarqueeTextView(Context context) {
        super(context);
        init();
    }
    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setSingleLine(true);
    }
    @Override
    public boolean isFocused() {
        return true;
    }
}
