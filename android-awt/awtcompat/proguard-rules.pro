-keep class org.apache.harmony.awt.gl.color.NativeImageFormat {
    private int cmmFormat;
    private int rows;
    private int cols;
    private int scanlineStride;
    private java.lang.Object imageData;
    private int dataOffset;
    private int alphaOffset;
}

-keep class org.apache.harmony.awt.gl.color.NativeCMM { *; }