package ch.hsr.geohash.util;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;

/**
 * Created by IntelliJ IDEA. User: kevin Date: Jan 17, 2011 Time: 12:03:47 PM
 */
public class TwoGeoHashBoundingBox {
    private BoundingBox boundingBox;
    private GeoHash bottomLeft;
    private GeoHash topRight;

    public static TwoGeoHashBoundingBox withCharacterPrecision(BoundingBox bbox, int numberOfCharacters) {
        GeoHash bottomLeft = GeoHash.withCharacterPrecision(bbox.getMinLat(), bbox.getMinLon(), numberOfCharacters);
        GeoHash topRight = GeoHash.withCharacterPrecision(bbox.getMaxLat(), bbox.getMaxLon(), numberOfCharacters);
        return new TwoGeoHashBoundingBox(bottomLeft, topRight);
    }

    public static TwoGeoHashBoundingBox withBitPrecision(BoundingBox bbox, int numberOfBits) {
        GeoHash bottomLeft = GeoHash.withBitPrecision(bbox.getMinLat(), bbox.getMinLon(), numberOfBits);
        GeoHash topRight = GeoHash.withBitPrecision(bbox.getMaxLat(), bbox.getMaxLon(), numberOfBits);
        return new TwoGeoHashBoundingBox(bottomLeft, topRight);
    }

    /**
     * Convenience method so as not to break backward-compatibility with old calls
     * after we have added support for non-5-bit boundaries.
     *
     * @param base32 the double base-32 GeoHash string to decode
     * @return the decoded GeoHash pair (as one object)
     */
    public static TwoGeoHashBoundingBox fromBase32(String base32) {
        return fromBase32(base32, 5*(base32.length()>>1));
    }

    public static TwoGeoHashBoundingBox fromBase32(String base32, int bitsPrecision) {
        // simple validation
        if (base32 == null) throw new IllegalArgumentException("Invalid (null) base32-string");

        // if the input string does not have an even length, it is ill-formed
        int n = base32.length();
        if ((n % 2) != 0)
            throw new IllegalArgumentException("The base32-string must have an even number of characters, but does not");

        String bottomLeft = base32.substring(0, n >> 1);
        String topRight = base32.substring(n >> 1);
        return new TwoGeoHashBoundingBox(
                GeoHash.fromGeohashString(bottomLeft, bitsPrecision),
                GeoHash.fromGeohashString(topRight, bitsPrecision)
        );
    }

    public TwoGeoHashBoundingBox(GeoHash bottomLeft, GeoHash topRight) {
        if (bottomLeft.significantBits() != topRight.significantBits()) {
            throw new IllegalArgumentException(
                    "Does it make sense to iterate between hashes that have different precisions?");
        }
        this.bottomLeft = GeoHash.fromLongValue(bottomLeft.longValue(), bottomLeft.significantBits());
        this.topRight = GeoHash.fromLongValue(topRight.longValue(), topRight.significantBits());
        this.boundingBox = this.bottomLeft.getBoundingBox();
        this.boundingBox.expandToInclude(this.topRight.getBoundingBox());
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public GeoHash getBottomLeft() {
        return bottomLeft;
    }

    public GeoHash getTopRight() {
        return topRight;
    }

    public String toBase32() {
        return bottomLeft.toBase32() + topRight.toBase32();
    }

    public String toString() {
        return "TwoGeoHashBoundingBox(" + bottomLeft.toString() + ", " + topRight.toString();
    }
}
