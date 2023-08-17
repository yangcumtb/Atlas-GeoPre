package com.example.atlasgeopre.tools;

public enum FormatEum {
    TIFF("GTIFF", ".tif"),
    JPEG("JPEG", ".jpg"),
    BMP("BMP", ".bmp"),
    ENVI_BSQ("ENVI_BSQ", ".bsq"),
    ENVI_BIL("ENVI_BIL", ".bil"),
    ENVI_BIP("ENVI_BIP", ".bip"),
    GRID("AAIGrid", ".dat"),
    PNG("PNG", ".png"),
    GIF("GIF", ".gif");

    private final String value;

    private final String name;

    FormatEum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 获取格式后缀
     *
     * @param name 格式名称
     * @return
     */
    public static String getSuffixValue(String name) {
        for (FormatEum c : FormatEum.values()) {
            if (c.getName().equals(name)) {
                return c.getValue();
            }
        }
        return null;
    }

    public String getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

}
