package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMSqlQuery;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * SqlInfoProcessor adds S2SQL to the parsing results so that
 * technical users could verify SQL by themselves.
 **/
public class SqlInfoProcessor implements ParseResultProcessor {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(semanticQueries)) {
            return;
        }
        List<SemanticParseInfo> selectedParses = semanticQueries.stream().map(SemanticQuery::getParseInfo)
                .collect(Collectors.toList());
        long startTime = System.currentTimeMillis();
        addSqlInfo(queryContext, selectedParses);
        parseResp.getParseTimeCost().setSqlTime(System.currentTimeMillis() - startTime);
    }

    private void addSqlInfo(QueryContext queryContext, List<SemanticParseInfo> semanticParseInfos) {
        if (CollectionUtils.isEmpty(semanticParseInfos)) {
            return;
        }
        semanticParseInfos.forEach(parseInfo -> {
            addSqlInfo(queryContext, parseInfo);
        });
    }

    private void addSqlInfo(QueryContext queryContext, SemanticParseInfo parseInfo) {
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (Objects.isNull(semanticQuery)) {
            return;
        }
        semanticQuery.setParseInfo(parseInfo);
        String explainSql = semanticQuery.explain(queryContext.getUser());
        if (StringUtils.isBlank(explainSql)) {
            return;
        }
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (semanticQuery instanceof LLMSqlQuery) {
            keyPipelineLog.info("\ns2sql:{}\ncorrectS2SQL:{}\nquerySQL:{}", sqlInfo.getS2SQL(),
                    sqlInfo.getCorrectS2SQL(), explainSql);
        }
        sqlInfo.setQuerySQL(explainSql);
    }

}
