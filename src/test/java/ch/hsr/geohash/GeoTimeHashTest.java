package ch.hsr.geohash;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 2/23/12
 * Time: 3:50 PM
 * 
 * Tests the GeoTimeHash class.
 */
public class GeoTimeHashTest {
    @Test
    public void testRoundTripMultipleOfSix() {
        System.out.println("[GeoTimeHashTest.testRoundTripMultipleOfSix]");
        
        GeoTimeHash gthBegin = GeoTimeHash.withBitPrecision(31.4067, 60.0903, (new GregorianCalendar(2011, 8, 8, 21, 0, 0)).getTime(), 6*10);
        System.out.println("  begin:  " + gthBegin);
        
        String base64 = gthBegin.toBase64();
        
        GeoTimeHash gthEnd = GeoTimeHash.fromBase64String(base64);
        System.out.println("    end:  " + gthEnd);
        
        int comparison = gthBegin.toString().compareTo(gthEnd.toString());
        System.out.println("  comparison:  " + comparison);
        Assert.assertEquals("Could not complete round-trip base64-(en|de)coding", 0, comparison);
    }
    
    @Test
    public void testRoundTripNotMultipleOfSix() {
        System.out.println("[GeoTimeHashTest.testRoundTripNotMultipleOfSix]");

        GeoTimeHash gthBegin = GeoTimeHash.withBitPrecision(31.4067, 60.0903, (new GregorianCalendar(2011, 8, 8, 21, 0, 0)).getTime(), 6*10+4);
        System.out.println("  begin:  " + gthBegin);

        String base64 = gthBegin.toBase64();

        GeoTimeHash gthEnd = GeoTimeHash.fromBase64String(base64);
        System.out.println("    end:  " + gthEnd);

        int comparison = gthBegin.toString().compareTo(gthEnd.toString());
        System.out.println("  comparison:  " + comparison);
        Assert.assertEquals("Could not complete round-trip base64-(en|de)coding", 0, comparison);
    }

    @Test
    public void testHierarchy() {
        System.out.println("[GeoTimeHashTest.testHierarchy]");

        // the geo-time anchor to encode
        WGS84Point point = new WGS84Point(31.4067, 60.0903);
        Date date = (new GregorianCalendar(-1000, 1, 1, 0, 0, 0)).getTime();
        
        String lastBase64 = GeoTimeHash.withBitPrecision(point.getLatitude(), point.getLongitude(), date, 6).toBase64();
        for (int i=12; i<=126; i+=6) {
            String base64 = GeoTimeHash.withBitPrecision(point.getLatitude(), point.getLongitude(), date, i).toBase64();
            
            System.out.println("  " + lastBase64 + ", " + base64);
            
            Assert.assertTrue("Base64-encoding of geo-time at greater precision (" + i + ") is not include the encoding of the geo-time at lesser precision (" + (i-6) + ")", base64.startsWith(lastBase64));

            lastBase64 = base64;
        }
        
        System.out.println("Final encoding:  " + GeoTimeHash.fromBase64String(lastBase64));
    }
}
