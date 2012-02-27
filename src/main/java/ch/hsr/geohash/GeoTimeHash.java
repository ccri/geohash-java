package ch.hsr.geohash;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 2/23/12
 * Time: 1:06 PM
 *
 * This is an experimental hash that incorporates time as well as location.
 */
public class GeoTimeHash {
    private final static char[] BASE64_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  // 10
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',      'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',  // 25
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',      'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',  // 25
            '_', '=', '~', '+'  // 4
    };
    private final static TreeMap<Character, Integer> tmCharToVal = new TreeMap<Character, Integer>();
    static {
        for (int i=0; i<BASE64_CHARS.length; i++) {
            tmCharToVal.put(BASE64_CHARS[i], i);
        }
    }
    
    public final static int BIT_CLASS__DATE = 1;
    public final static int BIT_CLASS__LATITUDE = 2;
    public final static int BIT_CLASS__LONGITUDE = 3;

    //private final static double MIN_DATE_SECONDS = Math.round(Double.MIN_VALUE);
    private final static Date MIN_DATE = (new GregorianCalendar(0, 1, 1, 0, 0, 0)).getTime();
    private final static double MIN_DATE_SECONDS = getDateAsDouble(MIN_DATE);

    //private final static double MAX_DATE_SECONDS = Math.round(Double.MAX_VALUE);
    private final static Date MAX_DATE = (new GregorianCalendar(3000, 12, 31, 23, 59, 59)).getTime();
    private final static double MAX_DATE_SECONDS = getDateAsDouble(MAX_DATE);

    private final static double DATE_RESOLUTION_MILLIS = 60.0 * 1000.0;  // one-minute

    private double[] rangeLatitude = {-90.0, 0.0, 90.0};

    private double[] rangeLongitude = {-180.0, 0.0, 180.0};

    private double[] rangeDateSeconds = {MIN_DATE_SECONDS, 0.5*(MIN_DATE_SECONDS+MAX_DATE_SECONDS), MAX_DATE_SECONDS};

    private final static int NUM_LONGS_DATA = 10;
    private long[] longs = new long[NUM_LONGS_DATA];
    
    private int precision = 0;

    protected GeoTimeHash() { }
    
    public static GeoTimeHash withBitPrecision(double latitude, double longitude, Date date, int precision) {
        GeoTimeHash gtHash = new GeoTimeHash();
        gtHash.encode(latitude, longitude, date, precision);

        return gtHash;
    }
    
    public static GeoTimeHash fromBase64String(String base64) {
        GeoTimeHash gtHash = new GeoTimeHash();
        gtHash.fromBase64(base64);

        return gtHash;
    }

    private int getBitClass(int n) {
        int r = n % 10;
        if (r < 4) return BIT_CLASS__DATE;
        if (r < 7) return BIT_CLASS__LONGITUDE;
        return BIT_CLASS__LATITUDE;
    }
    
    private void encode(double latitude, double longitude, Date date, int desiredPrecision) {
        int bit=0;

        double seconds = getDateAsDouble(date);

        for (int bitPos=0; bitPos<desiredPrecision; bitPos++) {
            switch (getBitClass(bitPos)) {
                case BIT_CLASS__DATE:
                    setBit(bitPos, seconds < rangeDateSeconds[1] ? 0 : 1);
                    break;
                case BIT_CLASS__LONGITUDE:
                    setBit(bitPos, longitude < rangeLongitude[1] ? 0 : 1);
                    break;
                case BIT_CLASS__LATITUDE:
                    setBit(bitPos, latitude < rangeLatitude[1] ? 0 : 1);
                    break;
            }
        }

        precision = desiredPrecision;
    }
    
    public static double getDateAsDouble(Date date) {
        if (date==null) throw new IllegalArgumentException("Invalid (NULL) date");
        return (double)(date.getTime()) / DATE_RESOLUTION_MILLIS;
    }
    
    public static Date getDateFromDouble(double reduced) {
        long millis = Math.round(reduced * DATE_RESOLUTION_MILLIS);
        return new Date(millis);
    }
    
    private void divideInterval(double[] range, boolean goRight) {
        if (!goRight) {
            // descend
            range[2] = range[1];
            range[1] = 0.5 * (range[0] + range[2]);
        } else {
            // ascend
            range[0] = range[1];
            range[1] = 0.5 * (range[0] + range[2]);
        }
    }

    private int getBit(int pos) {
        int longNum = 0;
        while (pos >= 64) {
            pos -= 64;
            longNum++;
        }
        if (longNum<0 | longNum>=NUM_LONGS_DATA) throw new IllegalArgumentException("Invalid bit position " + pos + " -> long " + longNum + " / " + NUM_LONGS_DATA);

        int offset = 63 - pos;

        return (int)((longs[longNum] >> offset) & 1L);
    }
    
    private void setBit(int pos, int bit) {
        switch (getBitClass(pos)) {
            case BIT_CLASS__DATE:
                divideInterval(rangeDateSeconds, bit!=0);
                break;
            case BIT_CLASS__LONGITUDE:
                divideInterval(rangeLongitude, bit!=0);
                break;
            case BIT_CLASS__LATITUDE:
                divideInterval(rangeLatitude, bit!=0);
                break;
        }

        if (bit != 0) {
            int longNum = 0;
            while (pos >= 64) {
                pos -= 64;
                longNum++;
            }
            if (longNum<0 | longNum>=NUM_LONGS_DATA) throw new IllegalArgumentException("Invalid bit position " + pos + " -> long " + longNum + " / " + NUM_LONGS_DATA);

            int offset = 63 - pos;

            longs[longNum] = longs[longNum] | (1L << offset);
        }
    }

    public String toBase64() {
        String b64 = "";

        int mask = 0;
        for (int bit=0; bit<precision; bit++) {
            mask = (mask<<1) | getBit(bit);
            if (bit%6==5) {
                b64 = b64 + BASE64_CHARS[mask];
                mask = 0;
            }
        }
        
        int leftoverBits = precision % 6;
        if (leftoverBits > 0) {
            b64 = b64 + "." + Integer.toString(leftoverBits) + BASE64_CHARS[mask];
        }

        return b64;
    }
    
    private void fromBase64(String b64) {
        String leftover = "";
        
        if (b64!=null && b64.length() > 0) {
            int period = b64.indexOf(".");
            
            if (period>=0) {
                leftover = b64.substring(period+1);
                b64 = b64.substring(0,period);
            }
            
            precision = b64.length() * 6;
            
            for (int i=0; i<b64.length(); i++) {
                int mask = 0;
                char c = b64.charAt(i);

                mask = tmCharToVal.get(c);
                //for (int k=0; k<BASE64_CHARS.length; k++)
                  //  if (c==BASE64_CHARS[k]) mask = k;
                
                for (int j=0; j<6; j++) {
                    setBit(6 * i + j, mask & (1 << (5 - j)));
                }
            }
            
            if (period>=0) {
                char c = leftover.charAt(1);
                int mask = 0;
                mask = tmCharToVal.get(c);
                //for (int k=0; k<64; k++)
                  //  if (c==BASE64_CHARS[k]) mask = k;

                int leftBits = Integer.valueOf(leftover.substring(0,1));
                for (int i=0; i<leftBits; i++) {
                    setBit(6*b64.length() + i, mask & (1 << (leftBits-1-i)));
                }
                precision += leftBits;
            }
        }
    }
    
    public String toBinaryString() {
        String s = "";

        for (int i=0; i<precision; i++) {
            s = s + (getBit(i) != 0 ? "1" : "0");
        }
        
        return s;
    }
    
    public String getRangeString(String label, double[] range) {
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("###,##0.0000");
        
        return label + "[" + df4.format(range[0]) + "," + df4.format(range[2]) + "]";
    }

    protected static String pad(String s, String padStr, int minLength, boolean isLeft) {
        while (s.length() <= (minLength-padStr.length())) {
            s = (isLeft ? padStr : "") + s + (!isLeft ? padStr : "");
        }
        
        return s;
    }
    
    protected String getSimpleDateString(Date date) {
        Calendar c = new GregorianCalendar();
        c.setTime(date);
        
        return pad(Integer.toString(c.get(Calendar.YEAR)), "0", 2, true) + "-" +
                pad(Integer.toString(c.get(Calendar.MONTH)), "0", 2, true) + "-" +
                pad(Integer.toString(c.get(Calendar.DAY_OF_MONTH)), "0", 2, true) + " " +
                pad(Integer.toString(c.get(Calendar.HOUR_OF_DAY)), "0", 2, true) + ":" +
                pad(Integer.toString(c.get(Calendar.MINUTE)), "0", 2, true) + ":" +
                pad(Integer.toString(c.get(Calendar.SECOND)), "0", 2, true)
        ;
    }
    
    public String getDateRangeString(String label, double[] range) {
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("0");

        Date dtMin = getDateFromDouble(range[0]);
        Date dtMax = getDateFromDouble(range[2]);
        
        return label + "["
                + df4.format(range[0]) + "=" + getSimpleDateString(dtMin)
                + ","
                + df4.format(range[2]) + "=" + getSimpleDateString(dtMax)
                + "]"
        ;
    }

    @Override
    public String toString() {
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("#,##0.0000");
        
        return "GeoTimeHash " + precision + " bits -> " +
                "(" + df4.format(rangeLatitude[1]) + ", " + df4.format(rangeLongitude[1]) + ", " + getSimpleDateString(getDateFromDouble(rangeDateSeconds[1])) + ") =>" +
                " 2[" + toBinaryString() + "]" +
                " 64[" + toBase64() + "]" +
                " " + getRangeString("LAT", rangeLatitude) +
                " " + getRangeString("LON", rangeLongitude) +
                " " + getDateRangeString("date", rangeDateSeconds)
        ;
    }
}
