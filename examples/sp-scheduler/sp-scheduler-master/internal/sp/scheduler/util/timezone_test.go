package util

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestAdjustTimeWithTimeLocationOrOffsetWithBothGiven(t *testing.T) {

	testTime := time.Date(2022, 3, 21, 10, 0, 0, 0, time.UTC)

	loc, _ := time.LoadLocation("MST")
	expectedTime := testTime.In(loc)

	adjustedTime, err := AdjustTimeWithTimeLocationOrOffset(testTime, "MST", "+10:00")

	assert.Equal(t, expectedTime, adjustedTime)
	assert.Nil(t, err)
}

func TestAdjustTimeWhenOffsetIsGiven(t *testing.T) {

	testTime := time.Date(2022, 3, 21, 10, 0, 0, 0, time.UTC)

	loc := time.FixedZone("", 10*60*60)
	expectedTime := testTime.In(loc)

	adjustedTime, err := AdjustTimeWithTimeLocationOrOffset(testTime, "", "+10:00")

	assert.Equal(t, expectedTime, adjustedTime)
	assert.Nil(t, err)
}

func TestAdjustTimeWithTimeLocationOrOffset_CompletelyUnusableTimeZoneInfo(t *testing.T) {

	testTime := time.Date(2022, 3, 21, 10, 0, 0, 0, time.UTC)
	zeroTime := time.Time{}

	adjustedTime1, err := AdjustTimeWithTimeLocationOrOffset(testTime, "\"clearly_not_a_timezone\"", "+10:00")

	assert.Equal(t, zeroTime, adjustedTime1)
	assert.NotNilf(t, err, "cannot use 'clearly_not_a_timezone' as a timezone location -- see the IANA Time Zone database for valid values")

	adjustedTime2, err := AdjustTimeWithTimeLocationOrOffset(testTime, "", "5")

	assert.Equal(t, testTime, adjustedTime2)
	assert.Nil(t, err)
}

func TestAdjustTimeWithTimeLocationOrOffset_SomeUsefulTimeZoneInfo(t *testing.T) {

	testTime := time.Date(2022, 3, 21, 10, 0, 0, 0, time.UTC)

	loc, _ := time.LoadLocation("MST")
	expectedTime := testTime.In(loc)

	adjustedTime, err := AdjustTimeWithTimeLocationOrOffset(testTime, "MST", "")

	assert.Equal(t, expectedTime, adjustedTime)
	assert.Nil(t, err)

	loc = time.FixedZone("", -2*60*60)
	expectedTime = testTime.In(loc)

	adjustedTime, err = AdjustTimeWithTimeLocationOrOffset(testTime, "", "-2:00")

	assert.Equal(t, expectedTime, adjustedTime)
	assert.Nil(t, err)

}
