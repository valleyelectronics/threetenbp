/*
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package javax.time.builder;

import static javax.time.builder.LocalDateUnit.DAYS;
import static javax.time.builder.LocalDateUnit.MONTHS;
import static javax.time.builder.LocalDateUnit.QUARTER_YEARS;
import static javax.time.builder.LocalDateUnit.YEARS;

import javax.time.CalendricalException;
import javax.time.LocalDate;
import javax.time.LocalDateTime;
import javax.time.MathUtils;
import javax.time.calendrical.DateTimeRuleRange;

/**
 * A field of date/time.
 */
public enum QuarterYearField implements DateField {

    DAY_OF_QUARTER("DayOfQuarter", DAYS, QUARTER_YEARS, DateTimeRuleRange.of(1, 90, 92)),
    MONTH_OF_QUARTER("MonthOfQuarter", MONTHS, QUARTER_YEARS,  DateTimeRuleRange.of(1, 3)),
    QUARTER_OF_YEAR("QuarterOfYear", QUARTER_YEARS, YEARS,  DateTimeRuleRange.of(1, 4));

    private final String name;
    private final PeriodUnit baseUnit;
    private final PeriodUnit rangeUnit;
    private final DRules dRules;
    private final DateTimeRules<LocalDateTime> dtRules;
    private final DateTimeRuleRange range;

    private QuarterYearField(String name, PeriodUnit baseUnit, PeriodUnit rangeUnit, DateTimeRuleRange range) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.dRules = new DRules(this);
        this.dtRules = new DateBasedDateTimeRules(this);
        this.range = range;
    }

    //-----------------------------------------------------------------------
    @Override
    public String getName() {
        return name;
    }

    @Override
    public PeriodUnit getBaseUnit() {
        return baseUnit;
    }

    @Override
    public PeriodUnit getRangeUnit() {
        return rangeUnit;
    }

    @Override
    public DateTimeRules<LocalDate> getDateRules() {
        return dRules;
    }

    @Override
    public DateTimeRules<LocalDateTime> getDateTimeRules() {
        return dtRules;
    }

    @Override
    public DateTimeRuleRange getValueRange() {
        return range;
    }

    @Override
    public long getValueFrom(CalendricalObject calendrical) {
        return getDateRules().get(calendrical.extract(LocalDate.class));
    }

    @Override
    public String toString() {
        return getName();
    }

    //-------------------------------------------------------------------------
    /**
     * Date rules for the field.
     */
    private static final class DRules implements DateTimeRules<LocalDate> {
        private static final DateTimeRuleRange RANGE_DOQ_90 = DateTimeRuleRange.of(1, 90);
        private static final DateTimeRuleRange RANGE_DOQ_91 = DateTimeRuleRange.of(1, 91);
        private static final DateTimeRuleRange RANGE_DOQ_92 = DateTimeRuleRange.of(1, 92);
        private static final int[] QUARTER_DAYS = {0, 90, 181, 273, 0, 91, 182, 274};

        private final QuarterYearField field;
        private DRules(QuarterYearField field) {
            this.field = field;
        }
        @Override
        public DateTimeRuleRange range(LocalDate date) {
            if (field == DAY_OF_QUARTER) {
                switch (date.getMonthOfYear().ordinal() / 3) {
                    case 0: return (date.isLeapYear() ? RANGE_DOQ_91 : RANGE_DOQ_90);
                    case 1: return RANGE_DOQ_91;
                    case 2: return RANGE_DOQ_92;
                    case 3: return RANGE_DOQ_92;
                }
            }
            return field.getValueRange();
        }
        @Override
        public long get(LocalDate date) {
            switch (field) {
                case DAY_OF_QUARTER: return doq(date);
                case MONTH_OF_QUARTER: return (date.getMonthOfYear().ordinal() % 3) + 1;
                case QUARTER_OF_YEAR: return (date.getMonthOfYear().ordinal() / 3) + 1;
            }
            throw new CalendricalException("Unsupported field");
        }
        @Override
        public LocalDate set(LocalDate date, long newValue) {
            if (range(date).isValidValue(newValue) == false) {
                throw new CalendricalException("Invalid value: " + field + " " + newValue);
            }
            long value0 = newValue - 1;
            switch (field) {
                case DAY_OF_QUARTER: return date.plusDays(value0 - (doq(date) - 1));
                case MONTH_OF_QUARTER: return date.plusMonths(value0 - (date.getMonthOfYear().ordinal() % 3));
                case QUARTER_OF_YEAR: return date.plusMonths((value0 - (date.getMonthOfYear().ordinal() / 3)) * 3);
            }
            throw new CalendricalException("Unsupported field");
        }
        @Override
        public LocalDate setLenient(LocalDate date, long newValue) {
            long value0 = MathUtils.safeDecrement(newValue);
            switch (field) {
                case DAY_OF_QUARTER: return date.plusDays(value0 - (doq(date) - 1));
                case MONTH_OF_QUARTER: return date.plusMonths(value0 - (date.getMonthOfYear().ordinal() % 3));
                case QUARTER_OF_YEAR: return date.plusMonths(MathUtils.safeMultiply(value0 - (date.getMonthOfYear().ordinal() / 3), 3));
            }
            throw new CalendricalException("Unsupported field");
        }
        @Override
        public LocalDate roll(LocalDate date, long roll) {
            DateTimeRuleRange range = range(date);
            long valueRange = (range.getMaximum() - range.getMinimum()) + 1;
            long curValue0 = get(date) - 1;
            long newValue = ((curValue0 + (roll % valueRange)) % valueRange) + 1;
            return set(date, newValue);
        }

        private static int doq(LocalDate date) {
            return date.getDayOfYear() - QUARTER_DAYS[(date.getMonthOfYear().ordinal() / 3) + (date.isLeapYear() ? 4 : 0)];
        }
    }

}
