package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QuerySqlReq extends SemanticQueryReq {

    private String sql;

    @Override
    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"viewId\":")
                .append(viewId);
        stringBuilder.append("\"modelIds\":")
                .append(modelIds);
        stringBuilder.append(",\"params\":")
                .append(params);
        stringBuilder.append(",\"cacheInfo\":")
                .append(cacheInfo);
        stringBuilder.append(",\"sql\":")
                .append(sql);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

}
