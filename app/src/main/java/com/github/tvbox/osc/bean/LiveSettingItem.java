package com.github.tvbox.osc.bean;

public class LiveSettingItem {
    private int itemIndex;
    private String itemName;
    private boolean itemSelected = false;
    private int itemValue = 0;

    public int getItemIndex() {
        return itemIndex;
    }

    public void setItemIndex(int itemIndex) {
        this.itemIndex = itemIndex;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public boolean isItemSelected() {
        return itemSelected;
    }

    public void setItemSelected(boolean itemSelected) {
        this.itemSelected = itemSelected;
    }

    // ========== 酷9兼容方法 ==========
    public void setItemValue(int itemValue) {
        this.itemValue = itemValue;
    }

    public int getItemValue() {
        return itemValue;
    }
    // ========== 兼容方法结束 ==========
}
