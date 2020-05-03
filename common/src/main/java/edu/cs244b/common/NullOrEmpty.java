package edu.cs244b.common;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class NullOrEmpty {
    static public boolean isTrue(@Nullable Number number) {
        return (number == null);
    }

    static public <T> boolean isTrue(@Nullable T[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable T[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable int[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable int[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable long[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable long[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable byte[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable byte[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable float[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable float[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable double[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable double[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable char[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable char[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable short[] t) {
        return (t == null) || (t.length == 0);
    }

    static public <T> boolean isFalse(@Nullable short[] t) {
        return !isTrue(t);
    }

    static public <T> boolean isTrue(@Nullable Iterable<T> iterable) {
        return (iterable == null) || (iterable.iterator() == null) || !iterable.iterator().hasNext();
    }

    static public <T> boolean isTrue(@Nullable Collection<T> list) { return list == null || list.size() == 0; }

    static public <T> boolean isFalse(@Nullable Collection<T> list) {
        return !isTrue(list);
    }

    static public <T> boolean isFalse(@Nullable Iterable<T> iterable) {
        return !isTrue(iterable);
    }

    static public <K, V> boolean isTrue(@Nullable Map<K, V> map) {
        return (map == null) || (map.size() == 0);
    }

    static public <K, V> boolean isFalse(@Nullable Map<K, V> map) {
        return !isTrue(map);
    }

    static public boolean isTrue(@Nullable String string) {
        return isTrue(string, false);
    }

    static public boolean isTrue(@Nullable Boolean bool) {
        return bool == null;
    }

    static public boolean isTrue(@Nullable String string, boolean trimString) {
        return (string == null) || (trimString ? string.trim().isEmpty() : string.isEmpty());
    }

    static public boolean isFalse(@Nullable String string) {
        return !isTrue(string);
    }

    static public boolean isFalse(@Nullable String string, boolean trimString) {
        return !isTrue(string, trimString);
    }

    static public boolean isFalse(@Nullable Boolean bool) {
        return !isTrue(bool);
    }

    static public <K, V> boolean isTrue(@Nullable Multimap<K, V> map) {
        return (map == null) || (map.size() == 0);
    }

    static public <K, V> boolean isFalse(@Nullable Multimap<K, V> map) {
        return !isTrue(map);
    }

    static public <R, C, V> boolean isTrue(Table<R, C, V> table) {
        return (table == null) || (table.size() == 0);
    }

    static public <R, C, V> boolean isFalse(Table<R, C, V> table) {
        return !isTrue(table);
    }
}
