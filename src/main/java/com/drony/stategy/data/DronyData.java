package com.drony.stategy.data;

import java.time.ZonedDateTime;
import java.util.List;

public interface DronyData {

    List<List<String>> getMatrix();
    ZonedDateTime getCreatedDate();
}
