package com.tencent.supersonic.chat.core.parser;


import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * This checker can be used by semantic parsers to check if query intent
 * has already been satisfied by current candidate queries. If so, current
 * parser could be skipped.
 */
@Slf4j
public class SatisfactionChecker {

    // check all the parse info in candidate
    public static boolean isSkip(QueryContext queryContext) {
        for (SemanticQuery query : queryContext.getCandidateQueries()) {
            if (query.getQueryMode().equals(LLMSqlQuery.QUERY_MODE)) {
                continue;
            }
            if (checkThreshold(queryContext.getQueryText(), query.getParseInfo())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkThreshold(String queryText, SemanticParseInfo semanticParseInfo) {
        int queryTextLength = queryText.replaceAll(" ", "").length();
        double degree = semanticParseInfo.getScore() / queryTextLength;
        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        if (queryTextLength > optimizationConfig.getQueryTextLengthThreshold()) {
            if (degree < optimizationConfig.getLongTextThreshold()) {
                return false;
            }
        } else if (degree < optimizationConfig.getShortTextThreshold()) {
            return false;
        }
        log.info("queryMode:{}, degree:{}, parse info:{}",
                semanticParseInfo.getQueryMode(), degree, semanticParseInfo);
        return true;
    }

}
