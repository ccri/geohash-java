package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 12/15/11
 * Time: 3:23 PM
 *
 * The GeoHash is a combination of hashes of its two dimensions, latitude and longitude; the
 * total precision of the GeoHash is defined as a fixed number of bits.
 *
 * Each bit represents an interval-halving decision.  The first longitude bit, then, is interpreted as
 * follows:  a 0 means that the target longitude is on the interval [0, 180); a 1 means its on [180, 360].
 * The first two bits together define a cell that is 90 degrees wide; the first three bits together define
 * a cell that is 45 degrees wide, etc.
 *
 * The bit-strings from the two dimensions are interleaved (they alternate), beginning with longitude,
 * so a GeoHash of 10110 (read Lon-Lat-Lon-Lat-Lon) consists of three bits of longitude (110) and two bits of latitude (01).
 * The following is an example of how the GeoHashes (at 5-bits precision) progress:
 *
 *                                                          Longitude
 *                000      001     010      011       100      101     110      111
 *             -----   -----   -----   -----    -----   -----   -----   -----
 * 	11 |  01010  01011  01110  01111  11010  11011  11110  11111
 * 	10 |  01000  01001  01100  01101  11000  11001  11100  11101
 * 	01 |  00010  00011  00110  00111  10010  10011  10110  10111
 * 	00 |  00000  00001  00100  00101  10000  10001  10100  10101
 *
 * Each cell in this example is 45 degrees wide and 45 degrees high (since longitude ranges over [0,360],
 * and latitude only goes from [-90,90]).
 *
 * Note that the dimension-specific bit-strings proceed in order (longitude from 0 to 7; latitude from 0 to 3)
 * along each axis.  That allows us to work on these bit-strings as coordinate indexes, making it simple to
 * iterate over the GeoHashes within a rectangle (and make some estimates about in-circle membership).
 */
public class RadialGeoHashIterator implements Iterator<GeoHash> {
    private double radiusMeters;

    private long latBits;

    private long lonBits;

    private GeoHash ghCurrent;

    private int deltaLatBits;

    private int deltaLonBits;

    private int dLat;

    private int dLon;

    private double distanceInMeters = Double.NaN;

    private double precisionInMetersLat;

    private double precisionInMetersLon;

    private boolean doesHaveNext;

    private int latPrecision;

    private int lonPrecision;

    public RadialGeoHashIterator(double latitude, double longitude, double radiusMeters, int precision) {
        this.radiusMeters = radiusMeters;

        this.latPrecision = (precision >> 1);
        this.lonPrecision = latPrecision + (precision % 2);

        // identify the center, and extract its latitude- and longitude-bits
        GeoHash ghCenter = GeoHash.withBitPrecision(latitude, longitude, precision);
        long[] lar = decomposeGeoHashBits(ghCenter);
        this.latBits = lar[0];
        this.lonBits = lar[1];

        // identify precision in each dimension (meters/bit)
        this.precisionInMetersLat = this.getDimensionPrecisionInMeters(true, latPrecision);
        this.precisionInMetersLon = this.getDimensionPrecisionInMeters(false, lonPrecision);

        // figure out the maximum distance (in cells) from the center cell we ought to range
        this.deltaLonBits = (int)Math.ceil((radiusMeters * 2.0 / this.precisionInMetersLon - 1.0) / 2.0);
        this.deltaLatBits = (int)Math.ceil((radiusMeters * 2.0 / this.precisionInMetersLat - 1.0) / 2.0);

        // initialize position:  always start in the LL (South-West) corner
        this.dLat = -deltaLatBits;
        this.dLon = -deltaLonBits;

        // initialize whether there are any qualifying GeoHashes to iterate over (there had better be!)
        this.doesHaveNext = advance();
    }

    public void remove() { }

    public GeoHash next() {
        GeoHash gh = ghCurrent;

        advance();

        return gh;
    }

    public boolean advance() {
        boolean isDone = false;

        // identify the next pair of coordinates that is likely to be inside the circle of interest
        while (!isDone) {
            if (dLat > deltaLatBits) {
                ghCurrent = null;
                distanceInMeters = Double.NaN;
                doesHaveNext = false;
                return false;
            }

            // determine whether this point is likely inside the circle of interest
            // (use a fudge factor to reduce the likelihood that we reject corner-only overlaps)
            double fudgeFactorLat = (dLat - 0.5 * Math.signum(dLat)) * precisionInMetersLat;
            double fudgeFactorLon = (dLon - 0.5 * Math.signum(dLon)) * precisionInMetersLon;
            if (Math.hypot(fudgeFactorLat, fudgeFactorLon) <= radiusMeters) {
                // be sure to set the flag so that we can terminate the loop
                isDone = true;

                // fetch this current GeoHash
                ghCurrent = composeGeoHashFromBits(latBits+dLat, lonBits+dLon, latPrecision, lonPrecision);

                // update the (ESTIMATED!) distance in meters from this point to the center
                distanceInMeters = Math.hypot(dLon*precisionInMetersLon, dLat*precisionInMetersLat);
            }

            // increment the counters in either case (found a keeper or not)
            if (++dLon > deltaLatBits) {
                dLat++;
                dLon = -deltaLonBits;
            }
        }

        doesHaveNext = (ghCurrent != null);

        return doesHaveNext;
    }

    /**
         * We iterate row by row, so as long as the latitude cell is valid, we still have more cells to return.
         *
         * @return whether there is another cell to return
         */
    public boolean hasNext() {
        return doesHaveNext;
    }

    public double getDistanceInMeters() {
        return distanceInMeters;
    }

    /**
         * Longitude ranges over the entire circumference of the Earth, while latitude only ranges over half.
         * Hence, the precision (in meters-per-bit) is twice as refined for latitude as it is for longitude.
         * (The first bit latitude represents ~10,000 Km, while the first bit longitude represents ~20,000 Km.)
         *
         * In addition, latitude spans a slightly smaller range due to the asymmetry of the Earth.
         *
         * @param isLatitude  whether the dimension requested is latitude
         * @param precision the number of bits used
         * @return how many meters each bit of this dimension represents
         */
    public double getDimensionPrecisionInMeters(boolean isLatitude, int precision) {
        if (isLatitude) return 40008000.0 / (double)(1 << precision);
        else return 40075160.0 / (double)(1 << (precision-1));
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
}

