package com.github.tvbox.osc.ui.adapter;

import android.widget.ImageView;
import android.widget.TextView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.ShortcutsBean;
import java.util.ArrayList;

public class ShortcutsMenuAdapter extends BaseQuickAdapter<ShortcutsBean, BaseViewHolder> {
    public ShortcutsMenuAdapter() {
        super(R.layout.item_shortcuts_menu, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, ShortcutsBean item) {
        helper.setText(R.id.tvShortcutName, item.getName());
        ImageView iv = helper.getView(R.id.ivShortcutIcon);
        iv.setImageResource(item.getIconRes());
    }
}
