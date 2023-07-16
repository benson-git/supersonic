package com.tencent.supersonic.semantic.query.application.parser;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.core.domain.Catalog;

public interface SemanticConverter {

    boolean accept(QueryStructReq queryStructCmd);

    void converter(Catalog catalog, QueryStructReq queryStructCmd, ParseSqlReq sqlCommend, MetricReq metricCommand)
            throws Exception;

}
