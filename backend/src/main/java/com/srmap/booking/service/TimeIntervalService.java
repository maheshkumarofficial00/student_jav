package com.srmap.booking.service;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Pure time-interval arithmetic – mirrors the browser-side Slots.js logic.
 * All times are "HH:mm" strings on a single day.
 * Overlap rule: A overlaps B when A.start < B.end AND B.start < A.end
 * (back-to-back bookings are therefore allowed).
 */
@Component
public class TimeIntervalService {

    public record Interval(int start, int end) {
        public boolean overlaps(Interval other) {
            return this.start < other.end && other.start < this.end;
        }
    }

    public record TimeRange(String start, String end) {}

    // ---- conversion helpers ----
    public int toMin(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public String toTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private List<Interval> toNumeric(List<TimeRange> ranges) {
        List<Interval> out = new ArrayList<>();
        for (TimeRange r : ranges) {
            int s = toMin(r.start()), e = toMin(r.end());
            if (e > s) out.add(new Interval(s, e));
        }
        out.sort(Comparator.comparingInt(Interval::start));
        return out;
    }

    private List<TimeRange> stringify(List<Interval> intervals) {
        List<TimeRange> out = new ArrayList<>();
        for (Interval i : intervals) out.add(new TimeRange(toTime(i.start()), toTime(i.end())));
        return out;
    }

    private List<Interval> mergeNumeric(List<Interval> list) {
        List<Interval> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt(Interval::start));
        List<Interval> out = new ArrayList<>();
        for (Interval cur : sorted) {
            if (out.isEmpty()) { out.add(cur); continue; }
            Interval last = out.get(out.size() - 1);
            if (cur.start() <= last.end()) out.set(out.size() - 1, new Interval(last.start(), Math.max(last.end(), cur.end())));
            else out.add(cur);
        }
        return out;
    }

    /** Subtract busy intervals from windows. */
    public List<TimeRange> subtract(List<TimeRange> windows, List<TimeRange> busy) {
        List<Interval> out = mergeNumeric(toNumeric(windows));
        for (Interval b : mergeNumeric(toNumeric(busy))) {
            List<Interval> next = new ArrayList<>();
            for (Interval w : out) {
                if (b.end() <= w.start() || b.start() >= w.end()) { next.add(w); continue; }
                if (b.start() > w.start()) next.add(new Interval(w.start(), b.start()));
                if (b.end() < w.end())   next.add(new Interval(b.end(), w.end()));
            }
            out = next;
        }
        return stringify(out);
    }

    /** Intersect two sets of windows. */
    public List<TimeRange> intersect(List<TimeRange> a, List<TimeRange> b) {
        List<Interval> A = mergeNumeric(toNumeric(a));
        List<Interval> B = mergeNumeric(toNumeric(b));
        List<Interval> result = new ArrayList<>();
        for (Interval x : A) {
            for (Interval y : B) {
                int s = Math.max(x.start(), y.start());
                int e = Math.min(x.end(), y.end());
                if (s < e) result.add(new Interval(s, e));
            }
        }
        result.sort(Comparator.comparingInt(Interval::start));
        return stringify(mergeNumeric(result));
    }

    /** True if [start, end] fits entirely inside at least one free window. */
    public boolean isFreeSlot(List<TimeRange> freeWindows, String start, String end) {
        int s = toMin(start), e = toMin(end);
        for (TimeRange w : freeWindows) {
            if (toMin(w.start()) <= s && e <= toMin(w.end())) return true;
        }
        return false;
    }

    /** Duration in minutes of a window. */
    public int duration(TimeRange w) {
        return toMin(w.end()) - toMin(w.start());
    }

    public boolean overlaps(String s1, String e1, String s2, String e2) {
        return toMin(s1) < toMin(e2) && toMin(s2) < toMin(e1);
    }
}
