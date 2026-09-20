package io.bgplayground.cooljava.guest;

/** Pure bytecode: RGBA integers in/out; no AWT, ImageIO, filesystem or JNI. */
public final class ImageProcessor {
    public String apply(String csv, int width, int height, String effect) {
        if (width < 1 || height < 1 || (long)width * height > 262144)
            throw new IllegalArgumentException("Image exceeds 262144 pixels");
        String[] parts = csv.split(",");
        if (parts.length != width * height * 4) throw new IllegalArgumentException("Invalid RGBA length");
        int[] gray = new int[width * height];
        int[] alpha = new int[gray.length];
        for (int i = 0; i < gray.length; i++) {
            int r = Integer.parseInt(parts[4*i]), g = Integer.parseInt(parts[4*i+1]), b = Integer.parseInt(parts[4*i+2]);
            gray[i] = (77*r + 150*g + 29*b) >> 8;
            alpha[i] = Integer.parseInt(parts[4*i+3]);
        }
        if (!effect.equals("gray") && !effect.equals("sobel")) throw new IllegalArgumentException("Unknown filter");
        StringBuilder out = new StringBuilder(parts.length * 4);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int i = y*width+x, v = gray[i];
            if (effect.equals("sobel")) {
                v = 0;
                if (x > 0 && y > 0 && x+1 < width && y+1 < height) {
                    int a=gray[i-width-1], b=gray[i-width], c=gray[i-width+1];
                    int d=gray[i-1], f=gray[i+1], g=gray[i+width-1], h=gray[i+width], j=gray[i+width+1];
                    int gx=-a+c-2*d+2*f-g+j, gy=-a-2*b-c+g+2*h+j;
                    v=Math.min(255, (int)Math.sqrt(gx*gx+gy*gy));
                }
            }
            if (i > 0) out.append(',');
            out.append(v).append(',').append(v).append(',').append(v).append(',').append(alpha[i]);
        }
        return out.toString();
    }
}
