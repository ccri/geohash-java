package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 1/31/12
 * Time: 3:56 PM
 * 
 * Base class for all iterators that range over,and return, GeoHashes.
 */
public abstract class GeoHashIterator implements Iterator<GeoHash> {
    protected long latBitsLL;

    protected long lonBitsLL;

    protected long latBitsUR;

    protected long lonBitsUR;

    private GeoHash ghCurrent;

    private GeoHash ghPrevious;

    protected long latPosition;

    protected long lonPosition;

    protected boolean doesHaveNext;
    
    protected int precision;

    protected int latPrecision;

    protected int lonPrecision;

    private double precisionInMetersLat;

    private double precisionInMetersLon;

    private long spanBitsLat;

    private long spanBitsLon;
    
    protected GeoHash[] ghBounds;

    /**
     * Increment of latitude in degrees.
     */
    protected double incLatitudeDegrees;

    /**
     * Increment of longitude in degrees.
     */
    protected double incLongitudeDegrees;

    /**
     *
     */
    protected double midLatitude;

    /**
     * Utility storage for converting degrees to meters.
     */

    private static final TreeMap<Double,Double> mapDegreesToMeters = new TreeMap<Double, Double>();
    private static final double precisionDegreesToMeters = 1e5;

    /**
     * Empty constructor.
     */
    public GeoHashIterator() {
        super();
    }

    /**
     * Set up routine to initialize the iterator.
     * 
     * @param ghBounds a 2-element array, (LL, UR), defining the MBR over which this iterator is defined
     * @param precision the precision, in bits, of the GeoHashes sought
     */
    protected void initialize(GeoHash[] ghBounds, int precision, boolean shouldAdvance) {
        this.ghBounds = ghBounds;

        this.precision = precision;
        this.latPrecision = (precision >> 1);
        this.lonPrecision = latPrecision + (precision % 2);

        // identify the lower-left corner, and extract its latitude- and longitude-bits
        long[] lar = decomposeGeoHashBits(ghBounds[0]);
        this.latBitsLL = lar[0];
        this.lonBitsLL = lar[1];

        // identify the upper-right corner, and extract its latitude- and longitude-bits
        lar = decomposeGeoHashBits(ghBounds[1]);
        this.latBitsUR = lar[0];
        this.lonBitsUR = lar[1];

        // initialize position:  always start in the LL (South-West) corner
        this.latPosition = latBitsLL;
        this.lonPosition = lonBitsLL;

        // identify precision in each dimension (meters/bit)
        this.midLatitude = 0.5*(ghBounds[0].getPoint().getLatitude()+ghBounds[1].getPoint().getLatitude());
        this.precisionInMetersLat = getDimensionPrecisionInMeters(midLatitude, true, latPrecision);
        this.precisionInMetersLon = getDimensionPrecisionInMeters(midLatitude, false, lonPrecision);

        // increments for longitude and latitude, in degrees
        incLatitudeDegrees = 180.0 / Math.pow(2.0, latPrecision);
        incLongitudeDegrees = 360.0 / Math.pow(2.0, lonPrecision);

        // count the span (in bits) for each dimension
        this.spanBitsLat = latBitsUR - latBitsLL + 1;
        this.spanBitsLon = lonBitsUR - lonBitsLL + 1;

        // initialize whether there are any qualifying GeoHashes to iterate over (there had better be!)
        if (shouldAdvance) this.doesHaveNext = advance();
    }

    /**
     * Given points, return the two GeoHashes that bound the rectangle that are suitable for
     * iteration.
     *
     * @param points a collection of points for which a minimum-bounding-rectangle (MBR) is sought
     * @param precision the precision, in bits, of the GeoHashes sought
     * @param radiusInMeters the buffer distance in meters
     * @return the lower-left and upper-right corners of the bounding box, as GeoHashes at the specified precision
     */
    public static GeoHash[] getBoundingGeoHashes(List<WGS84Point> points, int precision, double radiusInMeters) {
        long latPrecision = (precision >> 1);
        long lonPrecision = latPrecision + (precision % 2);

        GeoHash gh;
        long[] lar = null;
        long lonMin=Long.MAX_VALUE, lonMax=Long.MIN_VALUE, latMin=Long.MAX_VALUE, latMax=Long.MIN_VALUE;
        
        for (WGS84Point point : points) {
            lar = decomposeGeoHashBits(GeoHash.withBitPrecision(point.getLatitude(), point.getLongitude(), precision));
            latMin = Math.min(latMin, lar[0]);
            lonMin = Math.min(lonMin, lar[1]);
            latMax = Math.max(latMax, lar[0]);
            lonMax = Math.max(lonMax, lar[1]);
        }

        // apply the buffer distance to these corners

        GeoHash ghLL = composeGeoHashFromBits(latMin, lonMin, (int)latPrecision, (int)lonPrecision);
        WGS84Point ptLL = VincentyGeodesy.moveInDirection(
                VincentyGeodesy.moveInDirection(ghLL.getPoint(), 270, radiusInMeters),
                180, radiusInMeters
        );

        GeoHash ghUR = composeGeoHashFromBits(latMax, lonMax, (int)latPrecision, (int)lonPrecision);
        WGS84Point ptUR = VincentyGeodesy.moveInDirection(
                VincentyGeodesy.moveInDirection(ghUR.getPoint(), 0, radiusInMeters),
                90, radiusInMeters
        );

        return new GeoHash[]{
                GeoHash.withBitPrecision(ptLL.getLatitude(), ptLL.getLongitude(), precision),
                GeoHash.withBitPrecision(ptUR.getLatitude(), ptUR.getLongitude(), precision)
        };
    }

    /**
     * This is a degenerate method, present as a requirement of the <code>Iterator</code>
     * interface.
     */
    public void remove() { }

    /**
     * Fetch the current result, and advance to the next (at least internally).
     *
     * @return the current GeoHash result
     */
    public GeoHash next() {
        GeoHash gh = ghCurrent;

        doesHaveNext = advance();

        return gh;
    }

    /**
     * Internal method that figures out whether the iterator is finished, and if not, updates the
     * current GeoHash and advances the counters.
     *
     * As a general scheme, we start in the lower-left corner, and iterate in a row-major way
     * until we exceed the upper-right corner of the rectangle.
     *
     * @return whether the iteration is over
     */
    protected abstract boolean advance();


    /**
     * Allows the user to query whether there is another GeoHash cell to return.
     *
     * @return whether there is another cell to return
     */
    public boolean hasNext() {
        return doesHaveNext;
    }

    /**
     * Longitude ranges over the entire circumference of the Earth, while latitude only ranges over half.
     * Hence, the precision (in meters-per-bit) is twice as refined for latitude as it is for longitude.
     * (The first bit latitude represents ~10,000 Km, while the first bit longitude represents ~20,000 Km.)
     *
     * In addition, latitude spans a slightly smaller range due to the asymmetry of the Earth.
     *
     * @param isLatitude  whether the dimension requested is latitude
     * @param dimensionBits the number of bits used
     * @return how many meters each bit of this dimension represents
     */
    public static double getDimensionPrecisionInMeters(double nearLatitude, boolean isLatitude, int dimensionBits) {
        if (isLatitude) return 20004000.0 / (double)(1 << dimensionBits);
        else {
            double radiusAtEquator = 40075160.0;
            double radiusNearLatitude = radiusAtEquator * Math.cos(nearLatitude * Math.PI / 180.0);
            double circumferenceNearLatitude = radiusNearLatitude * 2.0 * Math.PI;
            return circumferenceNearLatitude / (double)(1 << dimensionBits);
        }
    }

    /**
     * Decomposes the GeoHash into its dimension-specific bit-sequences.
     *
     * @param gh the source GeoHash
     * @return a 2-element array, wherein the first element is latitude and the second is longitude
     */
    public static long[] decomposeGeoHashBits(GeoHash gh) {
        long bitsLat = 0;
        long bitsLon = 0;
        int bitPos;
        long bit;
        boolean isEven = true;
        long bits = gh.longValue();
        int precision = gh.significantBits();
        for (bitPos=63; bitPos>(63-precision); bitPos--) {
            bit = (bits >> bitPos) & 1L;
            if (isEven) {
                bitsLon = (bitsLon << 1) | bit;
            } else {
                bitsLat = (bitsLat << 1) | bit;
            }

            isEven = !isEven;
        }

        return new long[]{bitsLat, bitsLon};
    }

    public static GeoHash composeGeoHashFromBits(long bitsLat, long bitsLon, int idxLat, int idxLon) {
        long bit;
        long bits = 0;
        boolean isEven = true;
        int precision = idxLat + idxLon;

        for (int i=0; i<precision; i++) {
            if (isEven) {
                bit = 1L & (bitsLon >> --idxLon);
            } else {
                bit = 1L & (bitsLat >> --idxLat);
            }

            bits = (bits << 1) | bit;

            isEven = !isEven;
        }

        bits = bits << (64-precision);

        return GeoHash.fromLongValue(bits, precision);
    }

    public static String getBinaryString(long bits, int size){
        String s = Long.toBinaryString(bits);

        while (s.length() < size) s = "0" + s;

        if (s.length() > size) s = s.substring(0, size);

        return s;
    }

    protected long getOffsetBitsLongitude() {
        return lonPosition - lonBitsLL;
    }

    protected long getOffsetBitsLatitude() {
        return latPosition - latBitsLL;
    }

    public long getSpanBitsLatitude() {
        return spanBitsLat;
    }

    public long getSpanBitsLongitude() {
        return spanBitsLon;
    }

    protected void setCurrentGeoHash(GeoHash newCurrentGeoHash) {
        ghPrevious = ghCurrent;
        ghCurrent = newCurrentGeoHash;
    }
    
    protected GeoHash getCurrentGeoHash() {
        return ghCurrent;
    }

    protected GeoHash getPreviousGeoHash() {
        return ghCurrent;
    }

    public double getPrecisionInMetersLatitude() {
        return precisionInMetersLat;
    }

    public double getPrecisionInMetersLongitude() {
        return precisionInMetersLon;
    }

    protected double getPrecisionInMetersLatitude(int precision) {
        return getDimensionPrecisionInMeters(midLatitude, true, precision >> 1);
    }

    protected double getPrecisionInMetersLongitude(int precision) {
        return getDimensionPrecisionInMeters(midLatitude, false, (1+precision) >> 1);
    }

    protected double getSpanAspectRatioSkew() {
        double nx = (double)spanBitsLon;
        double ny = (double)spanBitsLat;

        return Math.min(nx, ny) / (nx+ny);
    }

    protected void setDoesHaveNext(boolean newDoesHaveNext) {
        doesHaveNext = newDoesHaveNext;
    }

    /**
     * Given a radius in meters, what is the worst-case size in degrees that it might represent?
     * Note:  This is almost entirely useless as a measure.
     *
     * @param meters a distance in meters
     * @return a distance in degrees
     */
    public static double convertRadiusInMetersToDegrees(double meters) {
        WGS84Point a = new WGS84Point(35.0, 67.5);
        WGS84Point b = VincentyGeodesy.moveInDirection(a, 45.0, meters);

        return getSegmentLengthInDegrees(a, b);
    }

    /**
     * Given a radius in degrees, what is a blended-estimate size in meters that it might represent?
     * Note:  This is almost entirely useless as a measure.
     *
     * @param degrees a distance in degrees
     * @return a distance in meters
     */
    public static double convertRadiusInDegreesToMeters(double degrees) {
        // round this value, because beneath some tolerance they should be considered equal
        degrees = Math.round(degrees * precisionDegreesToMeters) / precisionDegreesToMeters;

        if (mapDegreesToMeters.containsKey(degrees)) {
            return mapDegreesToMeters.get(degrees);
        }
        
        double meters = convertRadiusInDegreesToMetersViaIntervalHalving(degrees, 45.0);
        
        mapDegreesToMeters.put(degrees, meters);
        
        return meters;
    }
    
    /**
     * Given a radius in degrees, what is a blended-estimate size in meters that it might represent?
     * Note:  This is almost entirely useless as a measure.
     *
     * @param degrees a distance in degrees
     * @return a distance in meters
     */
    public static double convertRadiusInDegreesToMetersViaIntervalHalving(double degrees, double azimuth) {
        WGS84Point a = new WGS84Point(35.0, 67.5);

        double minMeters = 0.01;
        double maxMeters = 10000000.0;
        double midMeters = 0.5 * (minMeters + maxMeters);

        double minDegrees = getSegmentLengthInDegrees(a, VincentyGeodesy.moveInDirection(a, azimuth, minMeters));
        double maxDegrees = getSegmentLengthInDegrees(a, VincentyGeodesy.moveInDirection(a, azimuth, maxMeters));
        double midDegrees = getSegmentLengthInDegrees(a, VincentyGeodesy.moveInDirection(a, azimuth, midMeters));

        while (Math.abs(midMeters-minMeters) > 0.01) {
            if (midDegrees==degrees) return midMeters;

            if (midDegrees > degrees) {
                maxMeters = midMeters;
                maxDegrees = midDegrees;
            }
            else if (midDegrees < degrees) {
                minMeters = midMeters;
                minDegrees = midDegrees;
            }

            midMeters = 0.5 * (minMeters+maxMeters);
            midDegrees = getSegmentLengthInDegrees(a, VincentyGeodesy.moveInDirection(a, azimuth, midMeters));
        }

        return midMeters;
    }

    /**
     * Utility function to express the distance between two points in degrees.
     * Note:  This is almost entirely useless as a measure.
     *
     * @param a one segment end-point
     * @param b one segment end-point
     * @return the distance in degrees between these two points; note that this can only be
     * an estimate, as horizontal and vertical degrees do not represent equal distances
     */
    public static double getSegmentLengthInDegrees(WGS84Point a, WGS84Point b) {
        return Math.hypot(a.getLongitude()-b.getLongitude(), a.getLatitude()-b.getLatitude());
    }

    /**
     * Utility function to express the distance between two points in meters.
     *
     * @param a one segment end-point
     * @param b one segment end-point
     * @return the distance in meters between these two points
     */
    public static double getSegmentLengthInMeters(WGS84Point a, WGS84Point b) {
        return VincentyGeodesy.distanceInMeters(a, b);
    }
}