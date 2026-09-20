package io.bgplayground.cooljava;
import io.bgplayground.cooljava.guest.ImageProcessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {
    @Test void grayscalePreservesAlpha() {
        assertEquals("76,76,76,17,149,149,149,255", new ImageProcessor().apply("255,0,0,17,0,255,0,255",2,1,"gray"));
    }
    @Test void sobelDetectsVerticalEdge() {
        String black="0,0,0,255", white="255,255,255,255";
        String row=black+","+black+","+white;
        String result=new ImageProcessor().apply(row+","+row+","+row,3,3,"sobel");
        String[] values=result.split(",");
        assertEquals("255",values[16]);
        assertEquals("0",values[0]);
    }
    @Test void rejectsMalformedAndOversizedInput() {
        var p=new ImageProcessor();
        assertThrows(IllegalArgumentException.class,()->p.apply("0",1,1,"gray"));
        assertThrows(IllegalArgumentException.class,()->p.apply("",100000,100000,"gray"));
    }
}
