/*
 * Copyright 2014 Parity authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kmfrog.martlet.book;

import static com.kmfrog.martlet.util.Strings.repeat;

import com.kmfrog.martlet.C;
import com.kmfrog.martlet.util.ASCII;
import com.typesafe.config.Config;

/**
 * An instrument configuration.
 */
public class Instrument {

    private final String asString;
    private final long   asLong;

    private final int priceFractionDigits;
    private final int sizeFractionDigits;

    private final long priceFactor;
    private final long sizeFactor;

    private String priceFormat;
    private String sizeFormat;

    public Instrument(String asString, int priceFractionDigits, int sizeFractionDigits) {
        this.asString = asString;
        this.asLong   = ASCII.packLong(asString);

        this.priceFractionDigits = priceFractionDigits;
        this.sizeFractionDigits  = sizeFractionDigits;

        this.priceFactor = C.POWERS_OF_TEN[priceFractionDigits];
        this.sizeFactor  = C.POWERS_OF_TEN[sizeFractionDigits];

        setPriceFormat(1, priceFractionDigits);
        setSizeFormat(1, sizeFractionDigits);
    }

    /**
     * Get this instrument as a string.
     *
     * @return this instrument as a string
     */
    public String asString() {
        return asString;
    }

    /**
     * Get this instrument as a long integer.
     *
     * @return this instrument as a long integer
     */
    public long asLong() {
        return asLong;
    }

    /**
     * Get the number of digits in the fractional part of a price.
     *
     * @return the number of digits in the fractional part of a price
     */
    public int getPriceFractionDigits() {
        return priceFractionDigits;
    }

    /**
     * Get the number of digits in the fractional part of a size.
     *
     * @return the number of digits in the fractional part of a size
     */
    public int getSizeFractionDigits() {
        return sizeFractionDigits;
    }

    /**
     * Get the multiplication factor for a price.
     *
     * @return the multiplication factor for a price
     */
    public long getPriceFactor() {
        return priceFactor;
    }

    /**
     * Get the multiplication factor for a size.
     *
     * @return the multiplication factor for a size
     */
    public long getSizeFactor() {
        return sizeFactor;
    }

    /**
     * Get a format string for a price.
     *
     * @return a format string for a price
     */
    public String getPriceFormat() {
        return priceFormat;
    }

    /**
     * Get a format string for a size.
     *
     * @return a format string for a size
     */
    public String getSizeFormat() {
        return sizeFormat;
    }

    public void setPriceFormat(int integerDigits, int maxFractionDigits) {
        priceFormat = getFormat(integerDigits, priceFractionDigits, maxFractionDigits);
    }

    public void setSizeFormat(int integerDigits, int maxFractionDigits) {
        sizeFormat = getFormat(integerDigits, sizeFractionDigits, maxFractionDigits);
    }

    public static Instrument fromConfig(Config config, String path) {
        int priceFractionDigits = config.getInt(path + ".price-fraction-digits");
        int sizeFractionDigits  = config.getInt(path + ".size-fraction-digits");

        return new Instrument(path, priceFractionDigits, sizeFractionDigits);
    }

    private static String getFormat(int integerDigits, int fractionDigits, int maxFractionDigits) {
        int width = integerDigits + (fractionDigits > 0 ? 1 : 0) + fractionDigits;

        int padding = maxFractionDigits - fractionDigits;

        if (maxFractionDigits > 0 && fractionDigits == 0)
            padding += 1;

        return "%" + width + "." + fractionDigits + "f" + repeat(' ', padding);
    }

}
