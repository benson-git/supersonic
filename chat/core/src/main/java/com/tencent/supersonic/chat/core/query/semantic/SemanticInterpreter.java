package com.tencent.supersonic.chat.core.query.semantic;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewSchemaResp;

import java.util.List;

/**
 * A semantic layer provides a simplified and consistent view of data from multiple sources.
 * It abstracts away the complexity of the underlying data sources and provides a unified view
 * of the data that is easier to understand and use.
 * <p>
 * The interface defines methods for getting metadata as well as querying data in the semantic layer.
 * Implementations of this interface should provide concrete implementations that interact with the
 * underlying data sources and return results in a consistent format. Or it can be implemented
 * as proxy to a remote semantic service.
 * </p>
 */
public interface SemanticInterpreter {

    SemanticQueryResp queryByStruct(QueryStructReq queryStructReq, User user);

    SemanticQueryResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user);

    SemanticQueryResp queryByS2SQL(QuerySqlReq querySQLReq, User user);

    List<ViewSchema> getViewSchema();

    List<ViewSchema> getViewSchema(List<Long> ids);

    ViewSchema getViewSchema(Long model, Boolean cacheEnable);

    PageInfo<DimensionResp> getDimensionPage(PageDimensionReq pageDimensionReq);

    PageInfo<MetricResp> getMetricPage(PageMetricReq pageDimensionReq, User user);

    List<DomainResp> getDomainList(User user);

    <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception;

    List<ViewSchemaResp> fetchViewSchema(List<Long> ids, Boolean cacheEnable);

    List<ViewResp> getViewList(Long domainId);

    List<ItemResp> getDomainViewTree();

}
