package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import java.util.ArrayList;

/**
 * 主题颜色选择适配器 (酷9特性: 彩虹渐变)
 */
public class ThemeColorAdapter extends BaseQuickAdapter<Integer, BaseViewHolder> {

    private int selectedPosition = -1;

    public ThemeColorAdapter() {
        super(R.layout.item_theme_colors, new ArrayList<>());
        // 预置彩虹色系
        addData(Color.parseColor("#FF0000")); // 红
        addData(Color.parseColor("#FF7F00")); // 橙
        addData(Color.parseColor("#FFFF00")); // 黄
        addData(Color.parseColor("#00FF00")); // 绿
        addData(Color.parseColor("#0000FF")); // 蓝
        addData(Color.parseColor("#4B0082")); // 靛
        addData(Color.parseColor("#9400D3")); // 紫
        addData(Color.parseColor("#FF1493")); // 深粉
        addData(Color.parseColor("#00CED1")); // 深青
        addData(Color.parseColor("#FF4500")); // 橙红
        addData(Color.parseColor("#1E90FF")); // 道奇蓝
        addData(Color.parseColor("#32CD32")); // 酸橙绿
    }

    @Override
    protected void convert(BaseViewHolder helper, Integer color) {
        View view = helper.getView(R.id.vThemeColor);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        if (helper.getAdapterPosition() == selectedPosition) {
            drawable.setStroke(4, Color.WHITE);
        } else {
            drawable.setStroke(2, Color.parseColor("#80FFFFFF"));
        }
        view.setBackground(drawable);
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }
}
