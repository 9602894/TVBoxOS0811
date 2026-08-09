package com.github.tvbox.osc.bean;

public class ShortcutsBean {
    private String name;
    private int iconRes;

    public ShortcutsBean(String name, int iconRes) {
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
}
