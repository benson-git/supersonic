
package com.tencent.supersonic.chat.core.query.rule;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.query.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.BaseSemanticQuery;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.core.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ToString
public abstract class RuleSemanticQuery extends BaseSemanticQuery {

    protected QueryMatcher queryMatcher = new QueryMatcher();
    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();

    public RuleSemanticQuery() {
        QueryManager.register(this);
    }

    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
            QueryContext queryCtx) {
        return queryMatcher.match(candidateElementMatches);
    }

    @Override
    public void initS2Sql(SemanticSchema semanticSchema, User user) {
        initS2SqlByStruct(semanticSchema);
    }

    public void fillParseInfo(QueryContext queryContext, ChatContext chatContext) {
        parseInfo.setQueryMode(getQueryMode());
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();

        fillSchemaElement(parseInfo, semanticSchema);
        fillScore(parseInfo);
        fillDateConf(parseInfo, chatContext.getParseInfo());
    }

    private void fillDateConf(SemanticParseInfo queryParseInfo, SemanticParseInfo chatParseInfo) {
        if (queryParseInfo.getDateInfo() != null || chatParseInfo.getDateInfo() == null) {
            return;
        }

        if ((QueryManager.isTagQuery(queryParseInfo.getQueryMode())
                && QueryManager.isTagQuery(chatParseInfo.getQueryMode()))
                || (QueryManager.isMetricQuery(queryParseInfo.getQueryMode())
                && QueryManager.isMetricQuery(chatParseInfo.getQueryMode()))) {
            // inherit date info from context
            queryParseInfo.setDateInfo(chatParseInfo.getDateInfo());
            queryParseInfo.getDateInfo().setInherited(true);
        }
    }

    private void fillScore(SemanticParseInfo parseInfo) {
        double totalScore = 0;

        Map<SchemaElementType, SchemaElementMatch> maxSimilarityMatch = new HashMap<>();
        for (SchemaElementMatch match : parseInfo.getElementMatches()) {
            SchemaElementType type = match.getElement().getType();
            if (!maxSimilarityMatch.containsKey(type)
                    || match.getSimilarity() > maxSimilarityMatch.get(type).getSimilarity()) {
                maxSimilarityMatch.put(type, match);
            }
        }

        for (SchemaElementMatch match : maxSimilarityMatch.values()) {
            totalScore += match.getDetectWord().length() * match.getSimilarity();
        }

        parseInfo.setScore(parseInfo.getScore() + totalScore);
    }

    private void fillSchemaElement(SemanticParseInfo parseInfo, SemanticSchema semanticSchema) {
        Set<Long> viewIds = parseInfo.getElementMatches().stream().map(SchemaElementMatch::getElement)
                .map(SchemaElement::getView).collect(Collectors.toSet());
        parseInfo.setView(semanticSchema.getView(viewIds.iterator().next()));
        Map<Long, List<SchemaElementMatch>> dim2Values = new HashMap<>();
        Map<Long, List<SchemaElementMatch>> id2Values = new HashMap<>();

        for (SchemaElementMatch schemaMatch : parseInfo.getElementMatches()) {
            SchemaElement element = schemaMatch.getElement();
            element.setOrder(1 - schemaMatch.getSimilarity());
            switch (element.getType()) {
                case ID:
                    SchemaElement entityElement = semanticSchema.getElement(SchemaElementType.ENTITY, element.getId());
                    if (entityElement != null) {
                        if (id2Values.containsKey(element.getId())) {
                            id2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            id2Values.put(element.getId(), new ArrayList<>(Arrays.asList(schemaMatch)));
                        }
                    }
                    break;
                case VALUE:
                    SchemaElement dimElement = semanticSchema.getElement(SchemaElementType.DIMENSION, element.getId());
                    if (dimElement != null) {
                        if (dim2Values.containsKey(element.getId())) {
                            dim2Values.get(element.getId()).add(schemaMatch);
                        } else {
                            dim2Values.put(element.getId(), new ArrayList<>(Arrays.asList(schemaMatch)));
                        }
                    }
                    break;
                case DIMENSION:
                    parseInfo.getDimensions().add(element);
                    break;
                case METRIC:
                    parseInfo.getMetrics().add(element);
                    break;
                case ENTITY:
                    parseInfo.setEntity(element);
                    break;
                default:
            }
        }

        if (!id2Values.isEmpty()) {
            for (Map.Entry<Long, List<SchemaElementMatch>> entry : id2Values.entrySet()) {
                addFilters(parseInfo, semanticSchema, entry, SchemaElementType.ENTITY);
            }
        }

        if (!dim2Values.isEmpty()) {
            for (Map.Entry<Long, List<SchemaElementMatch>> entry : dim2Values.entrySet()) {
                addFilters(parseInfo, semanticSchema, entry, SchemaElementType.DIMENSION);
            }
        }
    }

    private void addFilters(SemanticParseInfo parseInfo, SemanticSchema semanticSchema,
            Entry<Long, List<SchemaElementMatch>> entry, SchemaElementType elementType) {
        SchemaElement dimension = semanticSchema.getElement(elementType, entry.getKey());

        if (entry.getValue().size() == 1) {
            SchemaElementMatch schemaMatch = entry.getValue().get(0);
            QueryFilter dimensionFilter = new QueryFilter();
            dimensionFilter.setValue(schemaMatch.getWord());
            dimensionFilter.setBizName(dimension.getBizName());
            dimensionFilter.setName(dimension.getName());
            dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
            dimensionFilter.setElementID(schemaMatch.getElement().getId());
            parseInfo.getDimensionFilters().add(dimensionFilter);
            parseInfo.setEntity(semanticSchema.getElement(SchemaElementType.ENTITY, entry.getKey()));
        } else {
            QueryFilter dimensionFilter = new QueryFilter();
            List<String> vals = new ArrayList<>();
            entry.getValue().stream().forEach(i -> vals.add(i.getWord()));
            dimensionFilter.setValue(vals);
            dimensionFilter.setBizName(dimension.getBizName());
            dimensionFilter.setName(dimension.getName());
            dimensionFilter.setOperator(FilterOperatorEnum.IN);
            dimensionFilter.setElementID(entry.getKey());
            parseInfo.getDimensionFilters().add(dimensionFilter);
        }
    }

    @Override
    public QueryResult execute(User user) {
        String queryMode = parseInfo.getQueryMode();

        if (parseInfo.getViewId() == null || StringUtils.isEmpty(queryMode)
                || !QueryManager.containsRuleQuery(queryMode)) {
            // reach here some error may happen
            log.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        QueryResult queryResult = new QueryResult();
        QueryStructReq queryStructReq = convertQueryStruct();

        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        if (optimizationConfig.isUseS2SqlSwitch()) {
            queryStructReq.setS2SQL(parseInfo.getSqlInfo().getS2SQL());
            queryStructReq.setCorrectS2SQL(parseInfo.getSqlInfo().getCorrectS2SQL());
        }
        SemanticQueryResp queryResp = semanticInterpreter.queryByStruct(queryStructReq, user);

        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }

        String sql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>()
                : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(sql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(queryMode);
        queryResult.setQueryState(QueryState.SUCCESS);

        return queryResult;
    }

    protected boolean isMultiStructQuery() {
        return false;
    }

    public QueryResult multiStructExecute(User user) {
        String queryMode = parseInfo.getQueryMode();

        if (parseInfo.getViewId() != null || StringUtils.isEmpty(queryMode)
                || !QueryManager.containsRuleQuery(queryMode)) {
            // reach here some error may happen
            log.error("not find QueryMode");
            throw new RuntimeException("not find QueryMode");
        }

        QueryResult queryResult = new QueryResult();
        QueryMultiStructReq queryMultiStructReq = convertQueryMultiStruct();
        SemanticQueryResp queryResp = semanticInterpreter.queryByMultiStruct(queryMultiStructReq, user);
        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }
        String sql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>()
                : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(sql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(queryMode);
        queryResult.setQueryState(QueryState.SUCCESS);

        return queryResult;
    }

    @Override
    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }

    public static List<RuleSemanticQuery> resolve(List<SchemaElementMatch> candidateElementMatches,
            QueryContext queryContext) {
        List<RuleSemanticQuery> matchedQueries = new ArrayList<>();

        for (RuleSemanticQuery semanticQuery : QueryManager.getRuleQueries()) {
            List<SchemaElementMatch> matches = semanticQuery.match(candidateElementMatches, queryContext);

            if (matches.size() > 0) {
                RuleSemanticQuery query = QueryManager.createRuleQuery(semanticQuery.getQueryMode());
                query.getParseInfo().getElementMatches().addAll(matches);
                matchedQueries.add(query);
            }
        }

        return matchedQueries;
    }

    protected QueryStructReq convertQueryStruct() {
        return QueryReqBuilder.buildStructReq(parseInfo);
    }

    protected QueryMultiStructReq convertQueryMultiStruct() {
        return QueryReqBuilder.buildMultiStructReq(parseInfo);
    }

}
