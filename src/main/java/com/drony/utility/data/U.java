package com.drony.utility.data;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class U {

    private final static Logger log = LoggerFactory.getLogger(U.class);

    public static String CSV_SEPARATOR = ";";

    public static Short multiplyMinusOne(Short a) {
        return (new Integer(a * -1)).shortValue();
    }

    public static Short absShort(Short a) {
        return a < 0 ? multiplyMinusOne(a) : a;
    }

    public static Integer randomInteger(Integer max) {
        return (int) (Math.random() * max);
    }

    public static Long randomLong(Long max) {
        return (long) (Math.random() * max);
    }

    public static boolean randomBoolean() {
        return Math.random() > 0.5;
    }

    public static BigDecimal randomBigDecimal(String range) {

        BigDecimal max = new BigDecimal(range + ".0");
        BigDecimal randFromDouble = new BigDecimal(Math.random());
        BigDecimal actualRandomDec = randFromDouble.multiply(max);

        return actualRandomDec.setScale(2, RoundingMode.CEILING);
    }

    public static boolean isEmptyOrNull(String string) {

        return string == null || string.trim().isEmpty();
    }

    public static boolean isEmptyOrNull(List<?> list) {

        return list == null || list.isEmpty();
    }

    public static <T> List<T> clean(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    public static boolean isNullOrEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {

        return map == null || map.isEmpty();
    }

    public static String cleanAndCapitalize(String s) {
        return WordUtils.capitalize(U.clean(s));
    }

    public static String capitalizeHard(String s) {
        s = s.trim();
        s = s.toLowerCase();
        return WordUtils.capitalize(s);
    }

    public static String clearStringRemoveEmptyStep(String path) {
        path = path.trim();
        path = path.replaceAll("\\s+", "");
        path = path.replaceAll("[^\\w\\s\\-_]", "");

        return path;
    }

    public static String clearStringForPath(String path) {
        path = clearStringRemoveEmptyStep(path);
        path = path.toLowerCase();

        return path;
    }

    public static String trimRemoveNull(String s) {
        if (s != null) {
            s = s.trim();
        } else {
            s = "";
        }
        return s;
    }

    public static String getLikePattern(final String searchTerm) {
        return U.isEmptyOrNull(searchTerm) ? "%" : "%" + searchTerm.trim().toLowerCase() + "%";
    }

    public static String getLikePatternOnlyRight(final String searchTerm) {
        return searchTerm.trim().toLowerCase() + "%";
    }

    public static String clearStringForPathUpper(String path) {
        path = clearStringForPath(path);

        return path.toUpperCase();
    }

    public static String clean(String string) {

        if (string == null) {
            return "";
        }
        return string.trim();
    }

    public static Long clean(Long num) {

        if (num == null) {
            return 0L;
        }
        return num;
    }

    public static BigDecimal clean(BigDecimal num) {

        if (num == null) {
            return BigDecimal.ZERO;
        }
        return num;
    }

    public static Integer clean(Integer num) {

        if (num == null) {
            return 0;
        }
        return num;
    }

    public static Short clean(Short num) {

        if (num == null) {
            return 0;
        }
        return num;
    }

    public static Integer cleanMax(Integer num) {

        if (num == null) {
            return Integer.MAX_VALUE;
        }
        return num;
    }

    public static Double clean(Double num) {

        if (num == null) {
            return 0.0;
        }
        return num;
    }

    public static Double cleanMax(Double num) {

        if (num == null) {
            return Double.MAX_VALUE;
        }
        return num;
    }

    public static String cleanNameForUrl(String name) {
        if (U.isEmptyOrNull(name)) {
            return "";
        }
        name = name.trim();
        name = name.replaceAll(" +", " ");
        name = name.replace(" ", "-");
        name = name.replace("_", "-");
        name = name.replace(".", "-");
        name = name.replace(",", "-");
        name = name.replace("+", "-");
        name = name.replace("'", "-");
        return name;
    }

    public static String cleanNameForDCOrder(String name) {
        if (U.isEmptyOrNull(name)) {
            return "";
        }
        name = name.trim();
        name = name.replaceAll(" +", "_");
        name = name.replace(" ", "_");
        name = name.replace(".", "_");
        name = name.replace(",", "_");
        name = name.replace("+", "_");
        name = name.replace("'", "_");
        name = name.replaceAll("[^a-zA-Z0-9_-]", "");
        return name;
    }

    public static String cleanNameForKey(String name) {
        return cleanNameForUrl(name);
    }

    public static boolean clean(Boolean b) {
        return b != null && b;
    }

    public static Integer toInteger(String s) {
        if (isEmptyOrNull(s)) {
            return null;
        }

        Integer num;
        try {
            num = Integer.parseInt(s);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
        return num;
    }

    public static Short toShort(String s) {
        if (isEmptyOrNull(s))
            return null;

        Short num;

        try {
            num = Short.parseShort(s);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }

        return num;
    }


    public static BigDecimal toBigDecimal(String s) {
        if (isEmptyOrNull(s))
            return null;

        BigDecimal num;

        try {
            num = new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }

        return num;
    }

    public static Boolean toBoolean(String s) {

        if (isEmptyOrNull(s))
            return null;

        try {
            return Boolean.parseBoolean(s);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    public static String truncateStr(String value, int length) {
        if (value == null) {
            return "";
        }
        value = value.trim();
        if (value.length() > length) {
            return value.substring(0, length);
        } else {
            return value;
        }
    }

   public static <T> List<List<T>> chopped(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<>(
                list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }

    public static String getSubString(String str, Integer size) {

        if (!isEmptyOrNull(str)) {
            str = str.trim();
            if (str.length() > size) {
                return str.substring(0, size);
            } else {
                return str;
            }
        }
        return "";
    }

    public static <T> T coalesce(T... ts) {
        for (T t : ts) {
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    public static boolean isZeroOrNull(BigDecimal num) {
        return num == null || num.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isAllZeroOrNull(BigDecimal... nums) {
        for (BigDecimal num : nums) {
            if (!isZeroOrNull(num)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllNull(Object... objs) {
        for (Object obj : objs) {
            if (obj != null) {
                return false;
            }
        }
        return true;
    }

    public static <T> List<T> getRandomSubList(List<T> list) {
        Random randomGenerator = new Random();

        int len = randomGenerator.nextInt(list.size() > 10 ? 10 : list.size());

        return getRandomSubList(list, len);
    }

    public static <T> List<T> getRandomSubList(List<T> list, int len) {
        Random randomGenerator = new Random();

        List<T> newList = new ArrayList<>();

        for (int i = 0; i < len; i++) {
            int index = randomGenerator.nextInt(list.size());
            T item = list.get(index);
            newList.add(item);
        }

        return newList;
    }

    public static boolean allTrue(List<Boolean> list) {
        return list.stream().noneMatch((l) -> (!l));
    }

    public static boolean allEqualOrMore(List<BigDecimal> list, BigDecimal value) {
        return list.stream().noneMatch((l) -> (l.compareTo(value) > 0));
    }

    public static boolean allEqualOrLess(List<BigDecimal> list, BigDecimal value) {

        for (BigDecimal l : list) {
            if (l.compareTo(value) < 0) {
                return false;
            }
        }

        return true;
    }

    public static <T, R extends Comparable<?>> T getIfNotNull(ImmutableRangeMap<R, T> rangeMap, R day) {
        if (rangeMap != null) {
            return rangeMap.get(day);
        }

        return null;
    }

    public static String truncateStrWithDot(String value, int length) {
        if (value == null) {
            return "";
        }
        value = value.trim();
        if (value.length() > length) {
            return value.substring(0, length - 3) + "...";
        } else {
            return value;
        }
    }


    public static BigDecimal cleanBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static String getLengthString(String value) {
        if (value == null) {
            return "[0]";
        }
        return "[" + value.length() + "]";
    }


    public static String createSimpleKey(Object... objs) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : objs) {
            if (obj != null) {
                sb.append(obj.toString());
            }
            sb.append("_");
        }

        return sb.toString();
    }

    public static Range<Integer> cloneRangeClose(Range<Integer> range) {
        Integer start = range.lowerBoundType() == BoundType.CLOSED ? range.lowerEndpoint() : range.lowerEndpoint() + 1;
        Integer end = range.upperBoundType() == BoundType.CLOSED ? range.upperEndpoint() : range.upperEndpoint() - 1;

        if (start > end)
            return null;
        else
            return Range.closed(start, end);
    }

    public static <T extends Comparable> Range<T> getSubRange(Range<T> range1, Range<T> range2) {
        if (range1.isConnected(range2))
            return range1.intersection(range2);
        return null;
    }

    public static <T> Set<T> cleanSetQuery(Set<T> ids, T i) {
        return
            ids == null ? ImmutableSet.of(i) :
                ids.isEmpty() ? ImmutableSet.of(i) : ids;
    }

    public static <T> List<T> filterListOfObjectsByType(List<Object> list, Class<T> type) {

        if (isEmptyOrNull(list))
            return new ArrayList<>();

        return
            list
                .parallelStream()
                .filter(Objects::nonNull)
                .filter(type::isInstance)
                .map(i -> (T) i)
                .collect(Collectors.toList());
    }

       public static Integer objectToInteger(Object obj) {
        return Objects.isNull(obj) ? null : (Integer) obj;
    }

    public static Short objectToShort(Object obj) {
        return Objects.isNull(obj) ? null : (Short) obj;
    }

    private static Object[] toObjectArray(Object array) {

        int length = Array.getLength(array);

        Object[] objects = new Object[length];

        for (int i = 0; i < length; i++)
            objects[i] = Array.get(array, i);

        return objects;
    }

    public static <T> T getListFirst(List<T> list) {

        if (isEmptyOrNull(list))
            return null;

        return list.stream().findFirst().orElse(null);
    }

    public static <T> T getListLast(List<T> list) {

        if (isEmptyOrNull(list))
            return null;

        return list.get(list.size() - 1);
    }

    public static <T> T searchInList(List<T> list, Predicate<? super T> predicate) {

        if (isEmptyOrNull(list))
            return null;

        return
            list
                .stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    public static ZonedDateTime setTimeToDate(ZonedDateTime date, ZonedDateTime time) {

        if (Objects.isNull(date))
            return null;

        if (Objects.isNull(time))
            return date;

        return
            date
                .truncatedTo(ChronoUnit.DAYS)
                .plusHours(time.getHour())
                .plusMinutes(time.getMinute());
    }

    // https://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
    public static String splitCamelCase(String s) {
        return s.replaceAll(
            String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
            ),
            " "
        );
    }

    // allows to get the same effect of distinct() in a stream by giving the property of a class
    // see https://stackoverflow.com/questions/23699371/java-8-distinct-by-property
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }



    public static <T> boolean isNullALLNull(List<T> list) {
        if (Objects.nonNull(list)) {
            for (T t : list) {
                if (Objects.isNull(t)) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    public static BigDecimal toBigDecimal(double value){
        return new BigDecimal(value, MathContext.DECIMAL64);
    }
}
