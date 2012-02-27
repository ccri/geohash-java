package ch.hsr.geohash;

import ch.hsr.geohash.util.VincentyGeodesy;

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

    private final static double REFERENCE_DATE_MILLIS = (double)(new GregorianCalendar(3000, 1, 1, 0, 0, 0)).getTime().getTime();
    private final static double DATE_SIGMOID_K = -1.0/REFERENCE_DATE_MILLIS * Math.log(2.0/(0.8+1.0)-1.0);
    
    private double[] rangeLatitude = {-90.0, 0.0, 90.0};

    private double[] rangeLongitude = {-180.0, 0.0, 180.0};

    private double[] rangeDateSignal = {-1.0, 0.0, +1.0};

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
        int r = n % 3;
        if (r < 1) return BIT_CLASS__DATE;
        if (r < 2) return BIT_CLASS__LONGITUDE;
        return BIT_CLASS__LATITUDE;
    }
    
    private void encode(double latitude, double longitude, Date date, int desiredPrecision) {
        int bit=0;

        double signal = getSignalFromDate(date);

        for (int bitPos=0; bitPos<desiredPrecision; bitPos++) {
            switch (getBitClass(bitPos)) {
                case BIT_CLASS__DATE:
                    setBit(bitPos, signal < rangeDateSignal[1] ? 0 : 1);
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
    
    public static double getSignalFromDate(Date date) {
        return 2.0 / (1.0+Math.exp(-1.0*DATE_SIGMOID_K*date.getTime())) - 1.0;
    }
    
    public static Date getDateFromSignal(double signal) {
        if (signal <= -1.0) return new Date(Long.MIN_VALUE);
        if (signal >= +1.0) return new Date(Long.MAX_VALUE);

        double millis = -1.0/DATE_SIGMOID_K * Math.log(2.0/(signal+1.0)-1.0);
        return new Date(Math.round(millis));
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
                divideInterval(rangeDateSignal, bit!=0);
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
    
    public String getLongitudeCellSizeString() {
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("###,##0.0000");

        double distanceMeters = VincentyGeodesy.distanceInMeters(
                new WGS84Point(rangeLatitude[1], rangeLongitude[0]),
                new WGS84Point(rangeLatitude[1], rangeLongitude[2])
        );

        return "dLon("
            + df4.format(rangeLongitude[2]-rangeLongitude[0]) + " deg, "
            + df4.format(distanceMeters) + " m"
            + ")"
        ;
    }

    public String getLatitudeCellSizeString() {
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("###,##0.0000");

        double distanceMeters = VincentyGeodesy.distanceInMeters(
                new WGS84Point(rangeLatitude[0], rangeLongitude[1]),
                new WGS84Point(rangeLatitude[2], rangeLongitude[1])
        );

        return "dLat("
                + df4.format(rangeLatitude[2]-rangeLatitude[0]) + " deg, "
                + df4.format(distanceMeters) + " m"
                + ")"
                ;
    }
    
    public String getDateCellSizeString() {
        java.text.DecimalFormat df2 = new java.text.DecimalFormat("###,##0.00");

        Date dtEarly = getDateFromSignal(rangeDateSignal[0]);
        Date dtLate = getDateFromSignal(rangeDateSignal[2]);
        long dMillis = dtLate.getTime() - dtEarly.getTime();
        
        double seconds = dMillis / 1000.0;
        double minutes = seconds / 60.0;
        double hours = minutes / 60.0;
        double days = hours / 24.0;
        double weeks = days / 7.0;
        double months = weeks / (52.0/12.0);
        double years = days / 365.24;
        
        String s = df2.format(seconds) + " seconds";
        if (years >= 1.0) s = df2.format(years) + " years";
        else if (months >= 1.0) s = df2.format(months) + " months";
        else if (weeks >= 1.0) s = df2.format(weeks) + " weeks";
        else if (days >= 1.0) s = df2.format(days) + " days";
        else if (hours >= 1.0) s = df2.format(hours) + " hours";
        else if (minutes >= 1.0) s = df2.format(minutes) + " minutes";

        return "dDate("
            + s
            + ")"
        ;
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
        java.text.DecimalFormat df4 = new java.text.DecimalFormat("0.0000");

        Date dtMin = getDateFromSignal(range[0]);
        Date dtMax = getDateFromSignal(range[2]);
        
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
                "(" + df4.format(rangeLatitude[1]) + ", " + df4.format(rangeLongitude[1]) + ", " + getSimpleDateString(getDateFromSignal(rangeDateSignal[1])) + ") =>" +
                " cellSize(" +
                getLatitudeCellSizeString() + "; " +
                getLongitudeCellSizeString() + "; " +
                getDateCellSizeString() +
                ")" +
                " 2[" + toBinaryString() + "]" +
                " 64[" + toBase64() + "]" +
                " " + getRangeString("LAT", rangeLatitude) +
                " " + getRangeString("LON", rangeLongitude) +
                " " + getDateRangeString("date", rangeDateSignal)
        ;
    }
}
