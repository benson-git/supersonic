package com.tencent.supersonic.chat.core.query.llm.s2sql;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.core.knowledge.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.core.utils.QueryReqBuilder;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.llm.LLMSemanticQuery;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class LLMSqlQuery extends LLMSemanticQuery {

    public static final String QUERY_MODE = "LLM_S2SQL";
    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    public LLMSqlQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {

        long startTime = System.currentTimeMillis();
        String querySql = parseInfo.getSqlInfo().getCorrectS2SQL();
        QuerySqlReq querySqlReq = QueryReqBuilder.buildS2SQLReq(querySql, parseInfo.getModel().getModelIds());
        SemanticQueryResp queryResp = semanticInterpreter.queryByS2SQL(querySqlReq, user);

        log.info("queryByS2SQL cost:{},querySql:{}", System.currentTimeMillis() - startTime, querySql);

        QueryResult queryResult = new QueryResult();
        if (Objects.nonNull(queryResp)) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }
        String resultQql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>() : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(resultQql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(QUERY_MODE);
        queryResult.setQueryState(QueryState.SUCCESS);

        parseInfo.setProperties(null);
        return queryResult;
    }

    @Override
    public void initS2Sql(SemanticSchema semanticSchema, User user) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        sqlInfo.setCorrectS2SQL(sqlInfo.getS2SQL());
    }
}
