package com.tencent.supersonic.chat;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.core.chat.query.rule.metric.MetricTagQuery;
import com.tencent.supersonic.headless.core.chat.query.rule.tag.TagFilterQuery;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TagTest extends BaseTest {

    @Test
    public void queryTest_metric_tag_query() throws Exception {
        MockConfiguration.mockTagAgent(agentService);
        QueryResult actualResult = submitNewChat("艺人周杰伦的播放量", DataUtils.tagAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricTagQuery.QUERY_MODE);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        QueryFilter dimensionFilter = DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS, "周杰伦", "歌手名", 7L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        SchemaElement metric = SchemaElement.builder().name("播放量").build();
        expectedParseInfo.getMetrics().add(metric);

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateMode.RECENT, 7, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_tag_list_filter() throws Exception {
        MockConfiguration.mockTagAgent(agentService);
        QueryResult actualResult = submitNewChat("爱情、流行类型的艺人", DataUtils.tagAgentId);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(TagFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(AggregateTypeEnum.NONE);

        List<String> list = new ArrayList<>();
        list.add("流行");
        QueryFilter dimensionFilter = DataUtils.getFilter("genre", FilterOperatorEnum.EQUALS,
                "流行", "风格", 2L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        SchemaElement metric = SchemaElement.builder().name("播放量").build();
        expectedParseInfo.getMetrics().add(metric);

        SchemaElement dim1 = SchemaElement.builder().name("歌手名").build();
        SchemaElement dim2 = SchemaElement.builder().name("活跃区域").build();
        SchemaElement dim3 = SchemaElement.builder().name("风格").build();
        SchemaElement dim4 = SchemaElement.builder().name("代表作").build();
        expectedParseInfo.getDimensions().add(dim1);
        expectedParseInfo.getDimensions().add(dim2);
        expectedParseInfo.getDimensions().add(dim3);
        expectedParseInfo.getDimensions().add(dim4);

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, startDay, startDay, 7));
        expectedParseInfo.setQueryType(QueryType.TAG);

        assertQueryResult(expectedResult, actualResult);
    }

}
