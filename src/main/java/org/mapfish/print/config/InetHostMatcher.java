/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Arrays;

/**
 * Allows to check that a given URL matches an IP address (numeric format).
 */
public abstract class InetHostMatcher extends HostMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(InetHostMatcher.class);

    /**
     * The ip addresses that are considered legal.
     * CSOFF: VisibilityModifier
     */
    protected byte[][] authorizedIPs = null;
    // CSON: VisibilityModifier

    @Override
    public boolean validate(final URI uri) throws UnknownHostException, SocketException, MalformedURLException {
        final InetAddress maskAddress = getMaskAddress();
        final InetAddress[] requestedIPs;
        try {
            requestedIPs = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException ex) {
            return false;
        }
        boolean oneMatching = false;
        for (int i = 0; i < requestedIPs.length; ++i) {
            InetAddress requestedIP = requestedIPs[i];
            if (isInAuthorized(requestedIP, maskAddress)) {
                oneMatching = true;
                break;
            }
        }
        return oneMatching && super.validate(uri);
    }

    private boolean isInAuthorized(final InetAddress requestedIP, final InetAddress mask) throws UnknownHostException,
            SocketException {
        byte[] rBytes = mask(requestedIP, mask);
        final byte[][] finalAuthorizedIPs = getAuthorizedIPs(mask);
        for (int i = 0; i < finalAuthorizedIPs.length; ++i) {
            byte[] authorizedIP = finalAuthorizedIPs[i];
            if (compareIP(rBytes, authorizedIP)) {
                return true;
            }
        }
        LOGGER.debug("Address not in the authorized set: " + requestedIP);
        return false;
    }

    private boolean compareIP(final byte[] rBytes, final byte[] authorizedIP) {
        if (rBytes.length != authorizedIP.length) {
            return false;
        }
        for (int j = 0; j < authorizedIP.length; ++j) {
            byte bA = authorizedIP[j];
            byte bR = rBytes[j];
            if (bA != bR) {
                return false;
            }
        }
        return true;
    }

    private byte[] mask(final InetAddress address, final InetAddress mask) {
        byte[] aBytes = address.getAddress();
        if (mask != null) {
            byte[] mBytes = mask.getAddress();
            if (aBytes.length != mBytes.length) {
                LOGGER.warn("Cannot mask address [" + address + "] with :" + mask);
                return aBytes;
            } else {
                final byte[] result = new byte[aBytes.length];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = (byte) (aBytes[i] & mBytes[i]);
                }
                return result;
            }
        } else {
            return aBytes;
        }
    }

    /**
     * Get the mask IP address.
     *
     * @return the ask addresses.
     */
    protected abstract InetAddress getMaskAddress() throws UnknownHostException;

    /**
     * calculate the authorized Ip addresses and assign them to the field.
     *
     * @param ips the addresses get the IP addresses from.
     */
    protected byte[][] buildMaskedAuthorizedIPs(final InetAddress[] ips) throws UnknownHostException {
        final InetAddress maskAddress = getMaskAddress();
        byte[][] tmpAuthorizedIPs = new byte[ips.length][];
        for (int i = 0; i < ips.length; ++i) {
            tmpAuthorizedIPs[i] = mask(ips[i], maskAddress);
        }

        return tmpAuthorizedIPs;
    }

    /**
     * Get the full list of authorized IP addresses for the provided mask.
     *
     * @param mask the mask address
     */
    protected abstract byte[][] getAuthorizedIPs(final InetAddress mask) throws UnknownHostException, SocketException;

    // CHECKSTYLE:OFF
    // Don't run checkstyle on generated methods
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(authorizedIPs);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InetHostMatcher other = (InetHostMatcher) obj;
        if (!Arrays.equals(authorizedIPs, other.authorizedIPs)) {
            return false;
        }
        return true;
    }
    // CHECKSTYLE:ON

}
