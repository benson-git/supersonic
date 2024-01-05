package com.tencent.supersonic.chat.core.knowledge;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class ModelInfoStat implements Serializable {

    private long modelCount;

    private long metricModelCount;

    private long dimensionModelCount;

    private long dimensionValueModelCount;

}