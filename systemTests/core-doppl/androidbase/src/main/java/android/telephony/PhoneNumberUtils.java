package android.telephony;

import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kgalligan on 8/1/16.
 */
public class PhoneNumberUtils
{
    /*
     * Special characters
     *
     * (See "What is a phone number?" doc)
     * 'p' --- GSM pause character, same as comma
     * 'n' --- GSM wild character
     * 'w' --- GSM wait character
     */
    public static final char PAUSE = ',';
    public static final char WAIT = ';';
    public static final char WILD = 'N';

    /*
     * TOA = TON + NPI
     * See TS 24.008 section 10.5.4.7 for details.
     * These are the only really useful TOA values
     */
    public static final int TOA_International = 0x91;
    public static final int TOA_Unknown = 0x81;

    static final String LOG_TAG = "PhoneNumberUtils";
    private static final boolean DBG = false;

    /*
     * global-phone-number = ["+"] 1*( DIGIT / written-sep )
     * written-sep         = ("-"/".")
     */
    private static final Pattern GLOBAL_PHONE_NUMBER_PATTERN =
            Pattern.compile("[\\+]?[0-9.-]+");

    /** True if c is ISO-LATIN characters 0-9 */
    public static boolean
    isISODigit (char c) {
        return c >= '0' && c <= '9';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # */
    public final static boolean
    is12Key(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD  */
    public final static boolean
    isDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD;
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , + (no WILD)  */
    public final static boolean
    isReallyDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE   */
    public final static boolean
    isNonSeparator(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+'
                || c == WILD || c == WAIT || c == PAUSE;
    }

    /** This any anything to the right of this char is part of the
     *  post-dial string (eg this is PAUSE or WAIT)
     */
    public final static boolean
    isStartsPostDial (char c) {
        return c == PAUSE || c == WAIT;
    }

    /** Returns true if ch is not dialable or alpha char */
    private static boolean isSeparator(char ch) {
        return !isDialable(ch) && !(('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z'));
    }

    /** Extracts the network address portion and canonicalizes
     *  (filters out separators.)
     *  Network address portion is everything up to DTMF control digit
     *  separators (pause or wait), but without non-dialable characters.
     *
     *  Please note that the GSM wild character is allowed in the result.
     *  This must be resolved before dialing.
     *
     *  Allows + only in the first  position in the result string.
     *
     *  Returns null if phoneNumber == null
     */
    public static String
    extractNetworkPortion(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        boolean firstCharAdded = false;

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (isDialable(c) && (c != '+' || !firstCharAdded)) {
                firstCharAdded = true;
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    /**
     * Extracts the network address portion and canonicalize.
     *
     * This function is equivalent to extractNetworkPortion(), except
     * for allowing the PLUS character to occur at arbitrary positions
     * in the address portion, not just the first position.
     *
     * @hide
     */
    public static String extractNetworkPortionAlt(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);
        boolean haveSeenPlus = false;

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (c == '+') {
                if (haveSeenPlus) {
                    continue;
                }
                haveSeenPlus = true;
            }
            if (isDialable(c)) {
                ret.append(c);
            } else if (isStartsPostDial (c)) {
                break;
            }
        }

        return ret.toString();
    }

    /**
     * Strips separators from a phone number string.
     * @param phoneNumber phone number to strip.
     * @return phone string stripped of separators.
     */
    public static String stripSeparators(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (isNonSeparator(c)) {
                ret.append(c);
            }
        }

        return ret.toString();
    }

    /** or -1 if both are negative */
    static private int
    minPositive (int a, int b) {
        if (a >= 0 && b >= 0) {
            return (a < b) ? a : b;
        } else if (a >= 0) { /* && b < 0 */
            return a;
        } else if (b >= 0) { /* && a < 0 */
            return b;
        } else { /* a < 0 && b < 0 */
            return -1;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
    /** index of the last character of the network portion
     *  (eg anything after is a post-dial string)
     */
    static private int
    indexOfLastNetworkChar(String a) {
        int pIndex, wIndex;
        int origLength;
        int trimIndex;

        origLength = a.length();

        pIndex = a.indexOf(PAUSE);
        wIndex = a.indexOf(WAIT);

        trimIndex = minPositive(pIndex, wIndex);

        if (trimIndex < 0) {
            return origLength - 1;
        } else {
            return trimIndex - 1;
        }
    }

    /**
     * Extracts the post-dial sequence of DTMF control digits, pauses, and
     * waits. Strips separators. This string may be empty, but will not be null
     * unless phoneNumber == null.
     *
     * Returns null if phoneNumber == null
     */

    public static String
    extractPostDialPortion(String phoneNumber) {
        if (phoneNumber == null) return null;

        int trimIndex;
        StringBuilder ret = new StringBuilder();

        trimIndex = indexOfLastNetworkChar (phoneNumber);

        for (int i = trimIndex + 1, s = phoneNumber.length()
                ; i < s; i++
                ) {
            char c = phoneNumber.charAt(i);
            if (isNonSeparator(c)) {
                ret.append(c);
            }
        }

        return ret.toString();
    }

    /**
     * Compare phone numbers a and b, return true if they're identical enough for caller ID purposes.
     */
    public static boolean compare(String a, String b) {
        // We've used loose comparation at least Eclair, which may change in the future.

        return compare(a, b, false);
    }

    /**
     * @hide only for testing.
     */
    public static boolean compare(String a, String b, boolean useStrictComparation) {
        return (useStrictComparation ? compareStrictly(a, b) : compareLoosely(a, b));
    }

    /**
     * Compare phone numbers a and b, return true if they're identical
     * enough for caller ID purposes.
     *
     * - Compares from right to left
     * - requires MIN_MATCH (7) characters to match
     * - handles common trunk prefixes and international prefixes
     *   (basically, everything except the Russian trunk prefix)
     *
     * Note that this method does not return false even when the two phone numbers
     * are not exactly same; rather; we can call this method "similar()", not "equals()".
     *
     * @hide
     */
    public static boolean
    compareLoosely(String a, String b) {
        int ia, ib;
        int matched;
        int numNonDialableCharsInA = 0;
        int numNonDialableCharsInB = 0;

        if (a == null || b == null) return a == b;

        if (a.length() == 0 || b.length() == 0) {
            return false;
        }

        ia = indexOfLastNetworkChar (a);
        ib = indexOfLastNetworkChar (b);
        matched = 0;

        while (ia >= 0 && ib >=0) {
            char ca, cb;
            boolean skipCmp = false;

            ca = a.charAt(ia);

            if (!isDialable(ca)) {
                ia--;
                skipCmp = true;
                numNonDialableCharsInA++;
            }

            cb = b.charAt(ib);

            if (!isDialable(cb)) {
                ib--;
                skipCmp = true;
                numNonDialableCharsInB++;
            }

            if (!skipCmp) {
                if (cb != ca && ca != WILD && cb != WILD) {
                    break;
                }
                ia--; ib--; matched++;
            }
        }

        if (matched < MIN_MATCH) {
            int effectiveALen = a.length() - numNonDialableCharsInA;
            int effectiveBLen = b.length() - numNonDialableCharsInB;


            // if the number of dialable chars in a and b match, but the matched chars < MIN_MATCH,
            // treat them as equal (i.e. 404-04 and 40404)
            if (effectiveALen == effectiveBLen && effectiveALen == matched) {
                return true;
            }

            return false;
        }

        // At least one string has matched completely;
        if (matched >= MIN_MATCH && (ia < 0 || ib < 0)) {
            return true;
        }

        /*
         * Now, what remains must be one of the following for a
         * match:
         *
         *  - a '+' on one and a '00' or a '011' on the other
         *  - a '0' on one and a (+,00)<country code> on the other
         *     (for this, a '0' and a '00' prefix would have succeeded above)
         */

        if (matchIntlPrefix(a, ia + 1)
                && matchIntlPrefix (b, ib +1)
                ) {
            return true;
        }

        if (matchTrunkPrefix(a, ia + 1)
                && matchIntlPrefixAndCC(b, ib +1)
                ) {
            return true;
        }

        if (matchTrunkPrefix(b, ib + 1)
                && matchIntlPrefixAndCC(a, ia +1)
                ) {
            return true;
        }

        return false;
    }

    /**
     * @hide
     */
    public static boolean
    compareStrictly(String a, String b) {
        return compareStrictly(a, b, true);
    }

    /**
     * @hide
     */
    public static boolean
    compareStrictly(String a, String b, boolean acceptInvalidCCCPrefix) {
        if (a == null || b == null) {
            return a == b;
        } else if (a.length() == 0 && b.length() == 0) {
            return false;
        }

        int forwardIndexA = 0;
        int forwardIndexB = 0;

        CountryCallingCodeAndNewIndex cccA =
                tryGetCountryCallingCodeAndNewIndex(a, acceptInvalidCCCPrefix);
        CountryCallingCodeAndNewIndex cccB =
                tryGetCountryCallingCodeAndNewIndex(b, acceptInvalidCCCPrefix);
        boolean bothHasCountryCallingCode = false;
        boolean okToIgnorePrefix = true;
        boolean trunkPrefixIsOmittedA = false;
        boolean trunkPrefixIsOmittedB = false;
        if (cccA != null && cccB != null) {
            if (cccA.countryCallingCode != cccB.countryCallingCode) {
                // Different Country Calling Code. Must be different phone number.
                return false;
            }
            // When both have ccc, do not ignore trunk prefix. Without this,
            // "+81123123" becomes same as "+810123123" (+81 == Japan)
            okToIgnorePrefix = false;
            bothHasCountryCallingCode = true;
            forwardIndexA = cccA.newIndex;
            forwardIndexB = cccB.newIndex;
        } else if (cccA == null && cccB == null) {
            // When both do not have ccc, do not ignore trunk prefix. Without this,
            // "123123" becomes same as "0123123"
            okToIgnorePrefix = false;
        } else {
            if (cccA != null) {
                forwardIndexA = cccA.newIndex;
            } else {
                int tmp = tryGetTrunkPrefixOmittedIndex(b, 0);
                if (tmp >= 0) {
                    forwardIndexA = tmp;
                    trunkPrefixIsOmittedA = true;
                }
            }
            if (cccB != null) {
                forwardIndexB = cccB.newIndex;
            } else {
                int tmp = tryGetTrunkPrefixOmittedIndex(b, 0);
                if (tmp >= 0) {
                    forwardIndexB = tmp;
                    trunkPrefixIsOmittedB = true;
                }
            }
        }

        int backwardIndexA = a.length() - 1;
        int backwardIndexB = b.length() - 1;
        while (backwardIndexA >= forwardIndexA && backwardIndexB >= forwardIndexB) {
            boolean skip_compare = false;
            final char chA = a.charAt(backwardIndexA);
            final char chB = b.charAt(backwardIndexB);
            if (isSeparator(chA)) {
                backwardIndexA--;
                skip_compare = true;
            }
            if (isSeparator(chB)) {
                backwardIndexB--;
                skip_compare = true;
            }

            if (!skip_compare) {
                if (chA != chB) {
                    return false;
                }
                backwardIndexA--;
                backwardIndexB--;
            }
        }

        if (okToIgnorePrefix) {
            if ((trunkPrefixIsOmittedA && forwardIndexA <= backwardIndexA) ||
                    !checkPrefixIsIgnorable(a, forwardIndexA, backwardIndexA)) {
                if (acceptInvalidCCCPrefix) {
                    // Maybe the code handling the special case for Thailand makes the
                    // result garbled, so disable the code and try again.
                    // e.g. "16610001234" must equal to "6610001234", but with
                    //      Thailand-case handling code, they become equal to each other.
                    //
                    // Note: we select simplicity rather than adding some complicated
                    //       logic here for performance(like "checking whether remaining
                    //       numbers are just 66 or not"), assuming inputs are small
                    //       enough.
                    return compare(a, b, false);
                } else {
                    return false;
                }
            }
            if ((trunkPrefixIsOmittedB && forwardIndexB <= backwardIndexB) ||
                    !checkPrefixIsIgnorable(b, forwardIndexA, backwardIndexB)) {
                if (acceptInvalidCCCPrefix) {
                    return compare(a, b, false);
                } else {
                    return false;
                }
            }
        } else {
            // In the US, 1-650-555-1234 must be equal to 650-555-1234,
            // while 090-1234-1234 must not be equalt to 90-1234-1234 in Japan.
            // This request exists just in US (with 1 trunk (NDD) prefix).
            // In addition, "011 11 7005554141" must not equal to "+17005554141",
            // while "011 1 7005554141" must equal to "+17005554141"
            //
            // In this comparison, we ignore the prefix '1' just once, when
            // - at least either does not have CCC, or
            // - the remaining non-separator number is 1
            boolean maybeNamp = !bothHasCountryCallingCode;
            while (backwardIndexA >= forwardIndexA) {
                final char chA = a.charAt(backwardIndexA);
                if (isDialable(chA)) {
                    if (maybeNamp && tryGetISODigit(chA) == 1) {
                        maybeNamp = false;
                    } else {
                        return false;
                    }
                }
                backwardIndexA--;
            }
            while (backwardIndexB >= forwardIndexB) {
                final char chB = b.charAt(backwardIndexB);
                if (isDialable(chB)) {
                    if (maybeNamp && tryGetISODigit(chB) == 1) {
                        maybeNamp = false;
                    } else {
                        return false;
                    }
                }
                backwardIndexB--;
            }
        }

        return true;
    }

    /**
     * Returns the rightmost MIN_MATCH (5) characters in the network portion
     * in *reversed* order
     *
     * This can be used to do a database lookup against the column
     * that stores getStrippedReversed()
     *
     * Returns null if phoneNumber == null
     */
    public static String
    toCallerIDMinMatch(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);
        return internalGetStrippedReversed(np, MIN_MATCH);
    }

    /**
     * Returns the network portion reversed.
     * This string is intended to go into an index column for a
     * database lookup.
     *
     * Returns null if phoneNumber == null
     */
    public static String
    getStrippedReversed(String phoneNumber) {
        String np = extractNetworkPortionAlt(phoneNumber);

        if (np == null) return null;

        return internalGetStrippedReversed(np, np.length());
    }

    /**
     * Returns the last numDigits of the reversed phone number
     * Returns null if np == null
     */
    private static String
    internalGetStrippedReversed(String np, int numDigits) {
        if (np == null) return null;

        StringBuilder ret = new StringBuilder(numDigits);
        int length = np.length();

        for (int i = length - 1, s = length
                ; i >= 0 && (s - i) <= numDigits ; i--
                ) {
            char c = np.charAt(i);

            ret.append(c);
        }

        return ret.toString();
    }

    /**
     * Basically: makes sure there's a + in front of a
     * TOA_International number
     *
     * Returns null if s == null
     */
    public static String
    stringFromStringAndTOA(String s, int TOA) {
        if (s == null) return null;

        if (TOA == TOA_International && s.length() > 0 && s.charAt(0) != '+') {
            return "+" + s;
        }

        return s;
    }

    /**
     * Returns the TOA for the given dial string
     * Basically, returns TOA_International if there's a + prefix
     */

    public static int
    toaFromString(String s) {
        if (s != null && s.length() > 0 && s.charAt(0) == '+') {
            return TOA_International;
        }

        return TOA_Unknown;
    }

    /**
     *  3GPP TS 24.008 10.5.4.7
     *  Called Party BCD Number
     *
     *  See Also TS 51.011 10.5.1 "dialing number/ssc string"
     *  and TS 11.11 "10.3.1 EF adn (Abbreviated dialing numbers)"
     *
     * @param bytes the data buffer
     * @param offset should point to the TOA (aka. TON/NPI) octet after the length byte
     * @param length is the number of bytes including TOA byte
     *                and must be at least 2
     *
     * @return partial string on invalid decode
     *
     * FIXME(mkf) support alphanumeric address type
     *  currently implemented in SMSMessage.getAddress()
     */
    public static String
    calledPartyBCDToString (byte[] bytes, int offset, int length) {
        boolean prependPlus = false;
        StringBuilder ret = new StringBuilder(1 + length * 2);

        if (length < 2) {
            return "";
        }

        //Only TON field should be taken in consideration
        if ((bytes[offset] & 0xf0) == (TOA_International & 0xf0)) {
            prependPlus = true;
        }

        internalCalledPartyBCDFragmentToString(
                ret, bytes, offset + 1, length - 1);

        if (prependPlus && ret.length() == 0) {
            // If the only thing there is a prepended plus, return ""
            return "";
        }

        if (prependPlus) {
            // This is an "international number" and should have
            // a plus prepended to the dialing number. But there
            // can also be Gsm MMI codes as defined in TS 22.030 6.5.2
            // so we need to handle those also.
            //
            // http://web.telia.com/~u47904776/gsmkode.htm is a
            // has a nice list of some of these GSM codes.
            //
            // Examples are:
            //   **21*+886988171479#
            //   **21*8311234567#
            //   *21#
            //   #21#
            //   *#21#
            //   *31#+11234567890
            //   #31#+18311234567
            //   #31#8311234567
            //   18311234567
            //   +18311234567#
            //   +18311234567
            // Odd ball cases that some phones handled
            // where there is no dialing number so they
            // append the "+"
            //   *21#+
            //   **21#+
            String retString = ret.toString();
            Pattern p = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$");
            Matcher m = p.matcher(retString);
            if (m.matches()) {
                if ("".equals(m.group(2))) {
                    // Started with two [#*] ends with #
                    // So no dialing number and we'll just
                    // append a +, this handles **21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(3));
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                    ret.append("+");
                } else {
                    // Starts with [#*] and ends with #
                    // Assume group 4 is a dialing number
                    // such as *21*+1234554#
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                    ret.append(m.group(5));
                }
            } else {
                p = Pattern.compile("(^[#*])(.*)([#*])(.*)");
                m = p.matcher(retString);
                if (m.matches()) {
                    // Starts with [#*] and only one other [#*]
                    // Assume the data after last [#*] is dialing
                    // number (i.e. group 4) such as *31#+11234567890.
                    // This also includes the odd ball *21#+
                    ret = new StringBuilder();
                    ret.append(m.group(1));
                    ret.append(m.group(2));
                    ret.append(m.group(3));
                    ret.append("+");
                    ret.append(m.group(4));
                } else {
                    // Does NOT start with [#*] just prepend '+'
                    ret = new StringBuilder();
                    ret.append('+');
                    ret.append(retString);
                }
            }
        }

        return ret.toString();
    }

    private static void
    internalCalledPartyBCDFragmentToString(
            StringBuilder sb, byte [] bytes, int offset, int length) {
        for (int i = offset ; i < length + offset ; i++) {
            byte b;
            char c;

            c = bcdToChar((byte)(bytes[i] & 0xf));

            if (c == 0) {
                return;
            }
            sb.append(c);

            // FIXME(mkf) TS 23.040 9.1.2.3 says
            // "if a mobile receives 1111 in a position prior to
            // the last semi-octet then processing shall commense with
            // the next semi-octet and the intervening
            // semi-octet shall be ignored"
            // How does this jive with 24,008 10.5.4.7

            b = (byte)((bytes[i] >> 4) & 0xf);

            if (b == 0xf && i + 1 == length + offset) {
                //ignore final 0xf
                break;
            }

            c = bcdToChar(b);
            if (c == 0) {
                return;
            }

            sb.append(c);
        }

    }

    /**
     * Like calledPartyBCDToString, but field does not start with a
     * TOA byte. For example: SIM ADN extension fields
     */

    public static String
    calledPartyBCDFragmentToString(byte [] bytes, int offset, int length) {
        StringBuilder ret = new StringBuilder(length * 2);

        internalCalledPartyBCDFragmentToString(ret, bytes, offset, length);

        return ret.toString();
    }

    /** returns 0 on invalid value */
    private static char
    bcdToChar(byte b) {
        if (b < 0xa) {
            return (char)('0' + b);
        } else switch (b) {
            case 0xa: return '*';
            case 0xb: return '#';
            case 0xc: return PAUSE;
            case 0xd: return WILD;

            default: return 0;
        }
    }

    private static int
    charToBCD(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c == '*') {
            return 0xa;
        } else if (c == '#') {
            return 0xb;
        } else if (c == PAUSE) {
            return 0xc;
        } else if (c == WILD) {
            return 0xd;
        } else {
            throw new RuntimeException ("invalid char for BCD " + c);
        }
    }

    /**
     * Return true iff the network portion of <code>address</code> is,
     * as far as we can tell on the device, suitable for use as an SMS
     * destination address.
     */
    public static boolean isWellFormedSmsAddress(String address) {
        String networkPortion =
                PhoneNumberUtils.extractNetworkPortion(address);

        return (!(networkPortion.equals("+")
                || TextUtils.isEmpty(networkPortion)))
                && isDialable(networkPortion);
    }

    public static boolean isGlobalPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        Matcher match = GLOBAL_PHONE_NUMBER_PATTERN.matcher(phoneNumber);
        return match.matches();
    }

    private static boolean isDialable(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isDialable(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNonSeparator(String address) {
        for (int i = 0, count = address.length(); i < count; i++) {
            if (!isNonSeparator(address.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    /**
     * Note: calls extractNetworkPortion(), so do not use for
     * SIM EF[ADN] style records
     *
     * Returns null if network portion is empty.
     */
    public static byte[]
    networkPortionToCalledPartyBCD(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(networkPortion, false);
    }

    /**
     * Same as {@link #networkPortionToCalledPartyBCD}, but includes a
     * one-byte length prefix.
     */
    public static byte[]
    networkPortionToCalledPartyBCDWithLength(String s) {
        String networkPortion = extractNetworkPortion(s);
        return numberToCalledPartyBCDHelper(networkPortion, true);
    }

    /**
     * Convert a dialing number to BCD byte array
     *
     * @param number dialing number string
     *        if the dialing number starts with '+', set to internationl TOA
     * @return BCD byte array
     */
    public static byte[]
    numberToCalledPartyBCD(String number) {
        return numberToCalledPartyBCDHelper(number, false);
    }

    /**
     * If includeLength is true, prepend a one-byte length value to
     * the return array.
     */
    private static byte[]
    numberToCalledPartyBCDHelper(String number, boolean includeLength) {
        int numberLenReal = number.length();
        int numberLenEffective = numberLenReal;
        boolean hasPlus = number.indexOf('+') != -1;
        if (hasPlus) numberLenEffective--;

        if (numberLenEffective == 0) return null;

        int resultLen = (numberLenEffective + 1) / 2;  // Encoded numbers require only 4 bits each.
        int extraBytes = 1;                            // Prepended TOA byte.
        if (includeLength) extraBytes++;               // Optional prepended length byte.
        resultLen += extraBytes;

        byte[] result = new byte[resultLen];

        int digitCount = 0;
        for (int i = 0; i < numberLenReal; i++) {
            char c = number.charAt(i);
            if (c == '+') continue;
            int shift = ((digitCount & 0x01) == 1) ? 4 : 0;
            result[extraBytes + (digitCount >> 1)] |= (byte)((charToBCD(c) & 0x0F) << shift);
            digitCount++;
        }

        // 1-fill any trailing odd nibble/quartet.
        if ((digitCount & 0x01) == 1) result[extraBytes + (digitCount >> 1)] |= 0xF0;

        int offset = 0;
        if (includeLength) result[offset++] = (byte)(resultLen - 1);
        result[offset] = (byte)(hasPlus ? TOA_International : TOA_Unknown);

        return result;
    }

    //================ Number formatting =========================

    /** The current locale is unknown, look for a country code or don't format */
    public static final int FORMAT_UNKNOWN = 0;
    /** NANP formatting */
    public static final int FORMAT_NANP = 1;
    /** Japanese formatting */
    public static final int FORMAT_JAPAN = 2;

    /** List of country codes for countries that use the NANP */
    private static final String[] NANP_COUNTRIES = new String[] {
            "US", // United States
            "CA", // Canada
            "AS", // American Samoa
            "AI", // Anguilla
            "AG", // Antigua and Barbuda
            "BS", // Bahamas
            "BB", // Barbados
            "BM", // Bermuda
            "VG", // British Virgin Islands
            "KY", // Cayman Islands
            "DM", // Dominica
            "DO", // Dominican Republic
            "GD", // Grenada
            "GU", // Guam
            "JM", // Jamaica
            "PR", // Puerto Rico
            "MS", // Montserrat
            "MP", // Northern Mariana Islands
            "KN", // Saint Kitts and Nevis
            "LC", // Saint Lucia
            "VC", // Saint Vincent and the Grenadines
            "TT", // Trinidad and Tobago
            "TC", // Turks and Caicos Islands
            "VI", // U.S. Virgin Islands
    };

    /**
     * Breaks the given number down and formats it according to the rules
     * for the country the number is from.
     *
     * @param source The phone number to format
     * @return A locally acceptable formatting of the input, or the raw input if
     *  formatting rules aren't known for the number
     */
    public static String formatNumber(String source) {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, getFormatTypeForLocale(Locale.getDefault()));
        return text.toString();
    }

    /**
     * Formats the given number with the given formatting type. Currently
     * {@link #FORMAT_NANP} and {@link #FORMAT_JAPAN} are supported as a formating type.
     *
     * @param source the phone number to format
     * @param defaultFormattingType The default formatting rules to apply if the number does
     * not begin with +<country_code>
     * @return The phone number formatted with the given formatting type.
     *
     * @hide TODO:Shuold be unhidden.
     */
    public static String formatNumber(String source, int defaultFormattingType) {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, defaultFormattingType);
        return text.toString();
    }

    /**
     * Returns the phone number formatting type for the given locale.
     *
     * @param locale The locale of interest, usually {@link Locale#getDefault()}
     * @return The formatting type for the given locale, or FORMAT_UNKNOWN if the formatting
     * rules are not known for the given locale
     */
    public static int getFormatTypeForLocale(Locale locale) {
        String country = locale.getCountry();

        return getFormatTypeFromCountryCode(country);
    }

    /**
     * Formats a phone number in-place. Currently {@link #FORMAT_JAPAN} and {@link #FORMAT_NANP}
     * is supported as a second argument.
     *
     * @param text The number to be formatted, will be modified with the formatting
     * @param defaultFormattingType The default formatting rules to apply if the number does
     * not begin with +<country_code>
     */
    public static void formatNumber(Editable text, int defaultFormattingType) {
        int formatType = defaultFormattingType;

        if (text.length() > 2 && text.charAt(0) == '+') {
            if (text.charAt(1) == '1') {
                formatType = FORMAT_NANP;
            } else if (text.length() >= 3 && text.charAt(1) == '8'
                    && text.charAt(2) == '1') {
                formatType = FORMAT_JAPAN;
            } else {
                return;
            }
        }

        switch (formatType) {
            case FORMAT_NANP:
                formatNanpNumber(text);
                return;
            case FORMAT_JAPAN:
                formatJapaneseNumber(text);
                return;
        }
    }

    private static final int NANP_STATE_DIGIT = 1;
    private static final int NANP_STATE_PLUS = 2;
    private static final int NANP_STATE_ONE = 3;
    private static final int NANP_STATE_DASH = 4;

    /**
     * Formats a phone number in-place using the NANP formatting rules. Numbers will be formatted
     * as:
     *
     * <p><code>
     * xxxxx
     * xxx-xxxx
     * xxx-xxx-xxxx
     * 1-xxx-xxx-xxxx
     * +1-xxx-xxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with the formatting
     */
    public static void formatNanpNumber(Editable text) {
        int length = text.length();
        if (length > "+1-nnn-nnn-nnnn".length()) {
            // The string is too long to be formatted
            return;
        } else if (length <= 5) {
            // The string is either a shortcode or too short to be formatted
            return;
        }

        CharSequence saved = text.subSequence(0, length);

        // Strip the dashes first, as we're going to add them back
        int p = 0;
        while (p < text.length()) {
            if (text.charAt(p) == '-') {
                text.delete(p, p + 1);
            } else {
                p++;
            }
        }
        length = text.length();

        // When scanning the number we record where dashes need to be added,
        // if they're non-0 at the end of the scan the dashes will be added in
        // the proper places.
        int dashPositions[] = new int[3];
        int numDashes = 0;

        int state = NANP_STATE_DIGIT;
        int numDigits = 0;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '1':
                    if (numDigits == 0 || state == NANP_STATE_PLUS) {
                        state = NANP_STATE_ONE;
                        break;
                    }
                    // fall through
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                    if (state == NANP_STATE_PLUS) {
                        // Only NANP number supported for now
                        text.replace(0, length, saved);
                        return;
                    } else if (state == NANP_STATE_ONE) {
                        // Found either +1 or 1, follow it up with a dash
                        dashPositions[numDashes++] = i;
                    } else if (state != NANP_STATE_DASH && (numDigits == 3 || numDigits == 6)) {
                        // Found a digit that should be after a dash that isn't
                        dashPositions[numDashes++] = i;
                    }
                    state = NANP_STATE_DIGIT;
                    numDigits++;
                    break;

                case '-':
                    state = NANP_STATE_DASH;
                    break;

                case '+':
                    if (i == 0) {
                        // Plus is only allowed as the first character
                        state = NANP_STATE_PLUS;
                        break;
                    }
                    // Fall through
                default:
                    // Unknown character, bail on formatting
                    text.replace(0, length, saved);
                    return;
            }
        }

        if (numDigits == 7) {
            // With 7 digits we want xxx-xxxx, not xxx-xxx-x
            numDashes--;
        }

        // Actually put the dashes in place
        for (int i = 0; i < numDashes; i++) {
            int pos = dashPositions[i];
            text.replace(pos + i, pos + i, "-");
        }

        // Remove trailing dashes
        int len = text.length();
        while (len > 0) {
            if (text.charAt(len - 1) == '-') {
                text.delete(len - 1, len);
                len--;
            } else {
                break;
            }
        }
    }

    /**
     * Formats a phone number in-place using the Japanese formatting rules.
     * Numbers will be formatted as:
     *
     * <p><code>
     * 03-xxxx-xxxx
     * 090-xxxx-xxxx
     * 0120-xxx-xxx
     * +81-3-xxxx-xxxx
     * +81-90-xxxx-xxxx
     * </code></p>
     *
     * @param text the number to be formatted, will be modified with
     * the formatting
     */
    public static void formatJapaneseNumber(Editable text) {
        JapanesePhoneNumberFormatter.format(text);
    }

    // Three and four digit phone numbers for either special services,
    // or 3-6 digit addresses from the network (eg carrier-originated SMS messages) should
    // not match.
    //
    // This constant used to be 5, but SMS short codes has increased in length and
    // can be easily 6 digits now days. Most countries have SMS short code length between
    // 3 to 6 digits. The exceptions are
    //
    // Australia: Short codes are six or eight digits in length, starting with the prefix "19"
    //            followed by an additional four or six digits and two.
    // Czech Republic: Codes are seven digits in length for MO and five (not billed) or
    //            eight (billed) for MT direction
    //
    // see http://en.wikipedia.org/wiki/Short_code#Regional_differences for reference
    //
    // However, in order to loose match 650-555-1212 and 555-1212, we need to set the min match
    // to 7.
    static final int MIN_MATCH = 7;



    /**
     * Translates any alphabetic letters (i.e. [A-Za-z]) in the
     * specified phone number into the equivalent numeric digits,
     * according to the phone keypad letter mapping described in
     * ITU E.161 and ISO/IEC 9995-8.
     *
     * @return the input string, with alpha letters converted to numeric
     *         digits using the phone keypad letter mapping.  For example,
     *         an input of "1-800-GOOG-411" will return "1-800-4664-411".
     */
    public static String convertKeypadLettersToDigits(String input) {
        if (input == null) {
            return input;
        }
        int len = input.length();
        if (len == 0) {
            return input;
        }

        char[] out = input.toCharArray();

        for (int i = 0; i < len; i++) {
            char c = out[i];
            // If this char isn't in KEYPAD_MAP at all, just leave it alone.
            out[i] = (char) KEYPAD_MAP.get(c, c);
        }

        return new String(out);
    }

    /**
     * The phone keypad letter mapping (see ITU E.161 or ISO/IEC 9995-8.)
     * TODO: This should come from a resource.
     */
    private static final SparseIntArray KEYPAD_MAP = new SparseIntArray();
    static {
        KEYPAD_MAP.put('a', '2'); KEYPAD_MAP.put('b', '2'); KEYPAD_MAP.put('c', '2');
        KEYPAD_MAP.put('A', '2'); KEYPAD_MAP.put('B', '2'); KEYPAD_MAP.put('C', '2');

        KEYPAD_MAP.put('d', '3'); KEYPAD_MAP.put('e', '3'); KEYPAD_MAP.put('f', '3');
        KEYPAD_MAP.put('D', '3'); KEYPAD_MAP.put('E', '3'); KEYPAD_MAP.put('F', '3');

        KEYPAD_MAP.put('g', '4'); KEYPAD_MAP.put('h', '4'); KEYPAD_MAP.put('i', '4');
        KEYPAD_MAP.put('G', '4'); KEYPAD_MAP.put('H', '4'); KEYPAD_MAP.put('I', '4');

        KEYPAD_MAP.put('j', '5'); KEYPAD_MAP.put('k', '5'); KEYPAD_MAP.put('l', '5');
        KEYPAD_MAP.put('J', '5'); KEYPAD_MAP.put('K', '5'); KEYPAD_MAP.put('L', '5');

        KEYPAD_MAP.put('m', '6'); KEYPAD_MAP.put('n', '6'); KEYPAD_MAP.put('o', '6');
        KEYPAD_MAP.put('M', '6'); KEYPAD_MAP.put('N', '6'); KEYPAD_MAP.put('O', '6');

        KEYPAD_MAP.put('p', '7'); KEYPAD_MAP.put('q', '7'); KEYPAD_MAP.put('r', '7'); KEYPAD_MAP.put('s', '7');
        KEYPAD_MAP.put('P', '7'); KEYPAD_MAP.put('Q', '7'); KEYPAD_MAP.put('R', '7'); KEYPAD_MAP.put('S', '7');

        KEYPAD_MAP.put('t', '8'); KEYPAD_MAP.put('u', '8'); KEYPAD_MAP.put('v', '8');
        KEYPAD_MAP.put('T', '8'); KEYPAD_MAP.put('U', '8'); KEYPAD_MAP.put('V', '8');

        KEYPAD_MAP.put('w', '9'); KEYPAD_MAP.put('x', '9'); KEYPAD_MAP.put('y', '9'); KEYPAD_MAP.put('z', '9');
        KEYPAD_MAP.put('W', '9'); KEYPAD_MAP.put('X', '9'); KEYPAD_MAP.put('Y', '9'); KEYPAD_MAP.put('Z', '9');
    }

    //================ Plus Code formatting =========================
    private static final char PLUS_SIGN_CHAR = '+';
    private static final String PLUS_SIGN_STRING = "+";
    private static final String NANP_IDP_STRING = "011";
    private static final int NANP_LENGTH = 10;




    private static boolean isTwoToNine (char c) {
        if (c >= '2' && c <= '9') {
            return true;
        } else {
            return false;
        }
    }

    private static int getFormatTypeFromCountryCode (String country) {
        // Check for the NANP countries
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(country) == 0) {
                return FORMAT_NANP;
            }
        }
        if ("jp".compareToIgnoreCase(country) == 0) {
            return FORMAT_JAPAN;
        }
        return FORMAT_UNKNOWN;
    }

    /**
     * This function checks if the passed in string conforms to the NANP format
     * i.e. NXX-NXX-XXXX, N is any digit 2-9 and X is any digit 0-9
     */
    private static boolean isNanp (String dialStr) {
        boolean retVal = false;
        if (dialStr != null) {
            if (dialStr.length() == NANP_LENGTH) {
                if (isTwoToNine(dialStr.charAt(0)) &&
                        isTwoToNine(dialStr.charAt(3))) {
                    retVal = true;
                    for (int i=1; i<NANP_LENGTH; i++ ) {
                        char c=dialStr.charAt(i);
                        if (!PhoneNumberUtils.isISODigit(c)) {
                            retVal = false;
                            break;
                        }
                    }
                }
            }
        } else {
            Log.e("isNanp: null dialStr passed in", dialStr);
        }
        return retVal;
    }

    /**
     * This function checks if the passed in string conforms to 1-NANP format
     */
    private static boolean isOneNanp(String dialStr) {
        boolean retVal = false;
        if (dialStr != null) {
            String newDialStr = dialStr.substring(1);
            if ((dialStr.charAt(0) == '1') && isNanp(newDialStr)) {
                retVal = true;
            }
        } else {
            Log.e("isOneNanp: null dialStr passed in", dialStr);
        }
        return retVal;
    }

    /**
     * Determines if the specified number is actually a URI
     * (i.e. a SIP address) rather than a regular PSTN phone number,
     * based on whether or not the number contains an "@" character.
     *
     * @hide
     * @param number
     * @return true if number contains @
     */
    public static boolean isUriNumber(String number) {
        // Note we allow either "@" or "%40" to indicate a URI, in case
        // the passed-in string is URI-escaped.  (Neither "@" nor "%40"
        // will ever be found in a legal PSTN number.)
        return number != null && (number.contains("@") || number.contains("%40"));
    }



    // This function finds the index of the dialable character(s)
    // in the post dial string
    private static int findDialableIndexFromPostDialStr(String postDialStr) {
        for (int index = 0;index < postDialStr.length();index++) {
            char c = postDialStr.charAt(index);
            if (isReallyDialable(c)) {
                return index;
            }
        }
        return -1;
    }

    // This function appends the non-diablable P/W character to the original
    // dial string based on the dialable index passed in
    private static String
    appendPwCharBackToOrigDialStr(int dialableIndex,String origStr, String dialStr) {
        String retStr;

        // There is only 1 P/W character before the dialable characters
        if (dialableIndex == 1) {
            StringBuilder ret = new StringBuilder(origStr);
            ret = ret.append(dialStr.charAt(0));
            retStr = ret.toString();
        } else {
            // It means more than 1 P/W characters in the post dial string,
            // appends to retStr
            String nonDigitStr = dialStr.substring(0,dialableIndex);
            retStr = origStr.concat(nonDigitStr);
        }
        return retStr;
    }

    //===== Begining of utility methods used in compareLoosely() =====

    /**
     * Phone numbers are stored in "lookup" form in the database
     * as reversed strings to allow for caller ID lookup
     *
     * This method takes a phone number and makes a valid SQL "LIKE"
     * string that will match the lookup form
     *
     */
    /** all of a up to len must be an international prefix or
     *  separators/non-dialing digits
     */
    private static boolean
    matchIntlPrefix(String a, int len) {
        /* '([^0-9*#+pwn]\+[^0-9*#+pwn] | [^0-9*#+pwn]0(0|11)[^0-9*#+pwn] )$' */
        /*        0       1                           2 3 45               */

        int state = 0;
        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                    break;

                default:
                    if (isNonSeparator(c)) return false;
                    break;

            }
        }

        return state == 1 || state == 3 || state == 5;
    }

    /** all of 'a' up to len must be a (+|00|011)country code)
     *  We're fast and loose with the country code. Any \d{1,3} matches */
    private static boolean
    matchIntlPrefixAndCC(String a, int len) {
        /*  [^0-9*#+pwn]*(\+|0(0|11)\d\d?\d? [^0-9*#+pwn] $ */
        /*      0          1 2 3 45  6 7  8                 */

        int state = 0;
        for (int i = 0 ; i < len ; i++ ) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 1:
                case 3:
                case 5:
                    if      (isISODigit(c)) state = 6;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 6:
                case 7:
                    if      (isISODigit(c)) state++;
                    else if (isNonSeparator(c)) return false;
                    break;

                default:
                    if (isNonSeparator(c)) return false;
            }
        }

        return state == 6 || state == 7 || state == 8;
    }

    /** all of 'a' up to len must match non-US trunk prefix ('0') */
    private static boolean
    matchTrunkPrefix(String a, int len) {
        boolean found;

        found = false;

        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            if (c == '0' && !found) {
                found = true;
            } else if (isNonSeparator(c)) {
                return false;
            }
        }

        return found;
    }

    //===== End of utility methods used only in compareLoosely() =====

    //===== Beggining of utility methods used only in compareStrictly() ====

    /*
     * If true, the number is country calling code.
     */
    private static final boolean COUNTLY_CALLING_CALL[] = {
            true, true, false, false, false, false, false, true, false, false,
            false, false, false, false, false, false, false, false, false, false,
            true, false, false, false, false, false, false, true, true, false,
            true, true, true, true, true, false, true, false, false, true,
            true, false, false, true, true, true, true, true, true, true,
            false, true, true, true, true, true, true, true, true, false,
            true, true, true, true, true, true, true, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, true, true, true, true, false, true, false, false, true,
            true, true, true, true, true, true, false, false, true, false,
    };
    private static final int CCC_LENGTH = COUNTLY_CALLING_CALL.length;

    /**
     * @return true when input is valid Country Calling Code.
     */
    private static boolean isCountryCallingCode(int countryCallingCodeCandidate) {
        return countryCallingCodeCandidate > 0 && countryCallingCodeCandidate < CCC_LENGTH &&
                COUNTLY_CALLING_CALL[countryCallingCodeCandidate];
    }

    /**
     * Returns interger corresponding to the input if input "ch" is
     * ISO-LATIN characters 0-9.
     * Returns -1 otherwise
     */
    private static int tryGetISODigit(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        } else {
            return -1;
        }
    }

    private static class CountryCallingCodeAndNewIndex {
        public final int countryCallingCode;
        public final int newIndex;
        public CountryCallingCodeAndNewIndex(int countryCode, int newIndex) {
            this.countryCallingCode = countryCode;
            this.newIndex = newIndex;
        }
    }

    /*
     * Note that this function does not strictly care the country calling code with
     * 3 length (like Morocco: +212), assuming it is enough to use the first two
     * digit to compare two phone numbers.
     */
    private static CountryCallingCodeAndNewIndex tryGetCountryCallingCodeAndNewIndex(
            String str, boolean acceptThailandCase) {
        // Rough regexp:
        //  ^[^0-9*#+]*((\+|0(0|11)\d\d?|166) [^0-9*#+] $
        //         0        1 2 3 45  6 7  89
        //
        // In all the states, this function ignores separator characters.
        // "166" is the special case for the call from Thailand to the US. Uguu!
        int state = 0;
        int ccc = 0;
        final int length = str.length();
        for (int i = 0 ; i < length ; i++ ) {
            char ch = str.charAt(i);
            switch (state) {
                case 0:
                    if      (ch == '+') state = 1;
                    else if (ch == '0') state = 2;
                    else if (ch == '1') {
                        if (acceptThailandCase) {
                            state = 8;
                        } else {
                            return null;
                        }
                    } else if (isDialable(ch)) {
                        return null;
                    }
                    break;

                case 2:
                    if      (ch == '0') state = 3;
                    else if (ch == '1') state = 4;
                    else if (isDialable(ch)) {
                        return null;
                    }
                    break;

                case 4:
                    if      (ch == '1') state = 5;
                    else if (isDialable(ch)) {
                        return null;
                    }
                    break;

                case 1:
                case 3:
                case 5:
                case 6:
                case 7:
                {
                    int ret = tryGetISODigit(ch);
                    if (ret > 0) {
                        ccc = ccc * 10 + ret;
                        if (ccc >= 100 || isCountryCallingCode(ccc)) {
                            return new CountryCallingCodeAndNewIndex(ccc, i + 1);
                        }
                        if (state == 1 || state == 3 || state == 5) {
                            state = 6;
                        } else {
                            state++;
                        }
                    } else if (isDialable(ch)) {
                        return null;
                    }
                }
                break;
                case 8:
                    if (ch == '6') state = 9;
                    else if (isDialable(ch)) {
                        return null;
                    }
                    break;
                case 9:
                    if (ch == '6') {
                        return new CountryCallingCodeAndNewIndex(66, i + 1);
                    } else {
                        return null;
                    }
                default:
                    return null;
            }
        }

        return null;
    }

    /**
     * Currently this function simply ignore the first digit assuming it is
     * trunk prefix. Actually trunk prefix is different in each country.
     *
     * e.g.
     * "+79161234567" equals "89161234567" (Russian trunk digit is 8)
     * "+33123456789" equals "0123456789" (French trunk digit is 0)
     *
     */
    private static int tryGetTrunkPrefixOmittedIndex(String str, int currentIndex) {
        int length = str.length();
        for (int i = currentIndex ; i < length ; i++) {
            final char ch = str.charAt(i);
            if (tryGetISODigit(ch) >= 0) {
                return i + 1;
            } else if (isDialable(ch)) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Return true if the prefix of "str" is "ignorable". Here, "ignorable" means
     * that "str" has only one digit and separater characters. The one digit is
     * assumed to be trunk prefix.
     */
    private static boolean checkPrefixIsIgnorable(final String str,
            int forwardIndex, int backwardIndex) {
        boolean trunk_prefix_was_read = false;
        while (backwardIndex >= forwardIndex) {
            if (tryGetISODigit(str.charAt(backwardIndex)) >= 0) {
                if (trunk_prefix_was_read) {
                    // More than one digit appeared, meaning that "a" and "b"
                    // is different.
                    return false;
                } else {
                    // Ignore just one digit, assuming it is trunk prefix.
                    trunk_prefix_was_read = true;
                }
            } else if (isDialable(str.charAt(backwardIndex))) {
                // Trunk prefix is a digit, not "*", "#"...
                return false;
            }
            backwardIndex--;
        }

        return true;
    }

    //==== End of utility methods used only in compareStrictly() =====
}
