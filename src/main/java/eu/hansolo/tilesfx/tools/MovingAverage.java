/*
 * Copyright (c) 2017 by Gerrit Grunwald
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

package eu.hansolo.tilesfx.tools;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by hansolo on 01.11.16.
 */
public class MovingAverage {
    public  static final int             MAX_PERIOD     = 2_073_600; // 24h in seconds
    private static final int             DEFAULT_PERIOD = 10;
    private        final Queue<TimeData> window;
    private              int             period;
    private              double          sum;


    // ******************** Constructors **************************************
    public MovingAverage() {
        this(DEFAULT_PERIOD);
    }
    public MovingAverage(final int PERIOD) {
        period         = Helper.clamp(0, MAX_PERIOD, PERIOD);
        window         = new ConcurrentLinkedQueue<>();
    }


    // ******************** Methods *******************************************
    public void addData(final TimeData DATA) {
        sum += DATA.getValue();
        window.add(DATA);
        if (window.size() > period) {
            sum -= window.remove().getValue();
        }
    }
    public void addValue(final double VALUE) {
        addData(new TimeData(VALUE));
    }
    public void addListOfData(final List<TimeData> LIST_OF_DATA) {
        for (TimeData data : LIST_OF_DATA) {
            addData(data);
        }
    }

    public Queue<TimeData> getWindow() {
        LinkedList<TimeData> result = new LinkedList<>();
        for (TimeData item : window) {
            TimeData timeData = new TimeData(item.getValue(), item.getTimestamp());
            result.add(timeData);
        }
        return result;
    }

    public TimeData getFirstEntry() { return window.peek(); }
    public TimeData getLastEntry() {
        boolean  seen = false;
        TimeData acc  = null;
        for (TimeData timeData : window) {
            if (!seen) {
                seen = true;
                acc = timeData;
            } else {
                acc = timeData;
            }
        }
        return seen ? acc : null; }

    public Instant getTimeSpan() {
        TimeData firstEntry = getFirstEntry();
        TimeData lastEntry  = getLastEntry();
        if (null == firstEntry || null == lastEntry) return null;
        return lastEntry.getTimestamp().minusSeconds(firstEntry.getTimestamp().getEpochSecond());
    }

    public double getAverage() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return (sum / window.size());
    }

    public double getTimeBasedAverageOf(final Duration DURATION) {
        assert !DURATION.isNegative() : "Time period must be positive";
        Instant now    = Instant.now();
        double  result = 0;
        long    count  = 0;
        for (TimeData v : window) {
            if (v.getTimestamp().isAfter(now.minus(DURATION))) {
                double value = v.getValue();
                result += value;
                count++;
            }
        }
        return (count > 0 ? OptionalDouble.of(result / count) : OptionalDouble.empty())
                     .getAsDouble();
    }

    public int getPeriod() { return period; }
    public void setPeriod(final int PERIOD) {
        period = Helper.clamp(0, MAX_PERIOD, PERIOD);
        reset();
    }

    public boolean isFilling() { return window.size() < period; }

    public void reset() { window.clear(); }
}
