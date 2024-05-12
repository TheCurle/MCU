package io.github.xfacthd.rsctrlunit.common.util.property;

public final class PropertyHolder
{
    public static final RedstoneTypeProperty RS_CON_0 = RedstoneTypeProperty.create("rs_con_0");
    public static final RedstoneTypeProperty RS_CON_1 = RedstoneTypeProperty.create("rs_con_1");
    public static final RedstoneTypeProperty RS_CON_2 = RedstoneTypeProperty.create("rs_con_2");
    public static final RedstoneTypeProperty RS_CON_3 = RedstoneTypeProperty.create("rs_con_3");
    public static final RedstoneTypeProperty[] RS_CON_PROPS = new RedstoneTypeProperty[] {
            RS_CON_0, RS_CON_1, RS_CON_2, RS_CON_3
    };

    private PropertyHolder() { }
}
