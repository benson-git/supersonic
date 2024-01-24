package com.tencent.supersonic.chat.integration;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;

import com.tencent.supersonic.chat.integration.util.DataUtils;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.core.query.rule.metric.MetricFilterQuery;
import com.tencent.supersonic.chat.core.query.rule.metric.MetricGroupByQuery;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.junit.Test;
import org.junit.jupiter.api.Order;

public class MultiTurnsTest extends BaseTest {

    @Test
    @Order(1)
    public void queryTest_01() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        QueryResult actualResult = submitMultiTurnChat("alice的访问次数", DataUtils.metricAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户名", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(2)
    public void queryTest_02() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        QueryResult actualResult = submitMultiTurnChat("停留时长呢", DataUtils.metricAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户名", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(3)
    public void queryTest_03() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        QueryResult actualResult = submitMultiTurnChat("lucy的如何", DataUtils.metricAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "lucy", "用户名", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(4)
    public void queryTest_04() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        QueryResult actualResult = submitMultiTurnChat("按部门统计", DataUtils.metricAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(5)
    public void queryTest_05() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        DateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        DateFormat textFormat = new SimpleDateFormat("yyyy年mm月dd日");
        QueryResult actualResult = submitMultiTurnChat(textFormat.format(format.parse(startDay)),
                DataUtils.metricAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, 1, period, startDay, startDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(6)
    public void queryTest_06() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        QueryResult actualResult = submitMultiTurnChat("近30天", DataUtils.metricAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(30, DateConf.DateMode.RECENT, "DAY"));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

}
