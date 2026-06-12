package com.drony.stategy.utility;

import com.drony.stategy.data.ParamDrony;
import com.dukascopy.api.IBar;
import com.dukascopy.api.Period;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public class TimeUtility {

  public static boolean checkTradingTimeLimit(ParamDrony paramDrony, IBar bar) {
    return TimeUtility.checkTradingTimeLimit(paramDrony, bar.getTime());
  }


  public static boolean checkTradingTimeLimit(ParamDrony paramDrony, Long epochMilli) {

    if (!paramDrony.getSelectedPeriod().isSmallerThan(Period.DAILY)) {
      return true;
    }

    if (paramDrony.getStartTradingTime() == null && paramDrony.getEndTradingTime() == null) {
      return true;
    }

    LocalTime barTime = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.of("UTC")).toLocalTime();
    return barTime.compareTo(paramDrony.getStartTradingTime()) > 0
        && barTime.compareTo(paramDrony.getEndTradingTime()) < 0;
  }

}
