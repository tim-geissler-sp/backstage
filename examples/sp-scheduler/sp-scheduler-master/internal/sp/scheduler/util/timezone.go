package util

import (
	"fmt"
	"regexp"
	"strconv"
	"time"

	"github.com/gorhill/cronexpr"
)

// TimezoneOffsetRegEx is the expected format for a cron-based schedule's timezone Offset: a '+' OR '-', followed by
//at least 1 digit, a colon, and exactly two digits.
var TimezoneOffsetRegEx, _ = regexp.Compile(`(?P<direction>[\+-])(?P<hour>\d+):(?P<minute>\d\d)`)

// AdjustTimeWithTimeLocationOrOffset takes a date time (timeValue) and converts it to corresponding time in the time location or time zone offset.  Time
// location takes priority, but if it is missing then it will use offset.  If location is invalid, then an error is returned and a
//  zero time is returned.  If location is not given, but an offset is given and that offset is not valid -- then a zero time
// is returned and an error is given.
func AdjustTimeWithTimeLocationOrOffset(timeValue time.Time, timezoneLocation string, timezoneOffset string) (time.Time, error) {
	var adjustedTimeValue time.Time

	// use location info if present
	if timezoneLocation != "" {
		loadedLocation, err := time.LoadLocation(timezoneLocation)
		if err != nil {
			return time.Time{}, fmt.Errorf("cannot use %s as a timezone location -- see the IANA Time Zone database for valid values", timezoneLocation)
		}
		adjustedTimeValue = timeValue.In(loadedLocation)
		return adjustedTimeValue, nil
	}

	// use timezone offset since location info is not present
	if (timezoneOffset != "") && TimezoneOffsetRegEx.MatchString(timezoneOffset) {

		offsetComponents := TimezoneOffsetRegEx.FindStringSubmatch(timezoneOffset)

		directionInd := TimezoneOffsetRegEx.SubexpIndex("direction")
		if directionInd == -1 {
			return time.Time{}, fmt.Errorf("leading '+' or '-' sign is required for cron timezone offset")
		}

		hourInd := TimezoneOffsetRegEx.SubexpIndex("hour")
		if hourInd == -1 {
			return time.Time{}, fmt.Errorf("cannot parse/find hour component of cron timezone offset")
		}

		minuteInd := TimezoneOffsetRegEx.SubexpIndex("minute")
		if minuteInd == -1 {
			return time.Time{}, fmt.Errorf("cannot parse/find minute component of cron timezone offset")
		}

		direction := offsetComponents[directionInd]
		hour := offsetComponents[hourInd]
		minute := offsetComponents[minuteInd]

		hrs, err := strconv.Atoi(hour)
		if err != nil {
			return time.Time{}, err
		}
		mins, err := strconv.Atoi(minute)
		if err != nil {
			return time.Time{}, err
		}

		offsetAmt := (time.Hour * time.Duration(hrs)) + (time.Minute * time.Duration(mins))
		if direction == "-" {
			offsetAmt *= -1
		}
		adjustedTime := timeValue.Add(offsetAmt)

		// Now that the clock time has been adjusted on the time.time object, we need to adjust the timezone attribute
		userOffset := time.FixedZone("", int(offsetAmt.Seconds()))
		adjustedTimeValue = time.Date(adjustedTime.Year(), adjustedTime.Month(), adjustedTime.Day(), adjustedTime.Hour(), adjustedTime.Minute(), adjustedTime.Second(), adjustedTime.Nanosecond(), userOffset)

		return adjustedTimeValue, nil
	}

	// if no location or offset was given, then return the original timeValue
	return timeValue, nil

}

// GetNextCronValueUsingTimezone takes a time value (now), the respective time zone information, and a cron expression to find the
// next time in the respective timezone.  This method converts to the timezone, gets the next time, and returns the time in UTC
// If this cannot be done, then return a non-nil error and "zero" time
func GetNextCronValueUsingTimezone(now time.Time, timezoneLocation string, timezoneOffset string, expr *cronexpr.Expression) (time.Time, error) {
	nowInDiffTimezone, err := AdjustTimeWithTimeLocationOrOffset(now, timezoneLocation, timezoneOffset)
	if err != nil {
		return time.Time{}, err
	}
	return expr.Next(nowInDiffTimezone).UTC(), nil
}
