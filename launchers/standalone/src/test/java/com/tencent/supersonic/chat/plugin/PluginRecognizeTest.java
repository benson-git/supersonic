package com.tencent.supersonic.chat.plugin;

import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.core.plugin.PluginManager;
import com.tencent.supersonic.chat.MockConfiguration;
import com.tencent.supersonic.util.DataUtils;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.QueryService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

public class PluginRecognizeTest extends BasePluginTest {

    @MockBean
    protected PluginManager pluginManager;

    @MockBean
    private EmbeddingConfig embeddingConfig;

    @MockBean
    private AgentService agentService;

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    public void webPageRecognize() throws Exception {
        MockConfiguration.mockEmbeddingRecognize(pluginManager, "alice最近的访问情况怎么样", "1");
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryContextReq = DataUtils.getQueryReqWithAgent(1000, "alice最近的访问情况怎么样", 1);

        ParseResp parseResp = queryService.performParsing(queryContextReq);
        ExecuteQueryReq executeReq = ExecuteQueryReq.builder().user(queryContextReq.getUser())
                .chatId(parseResp.getChatId())
                .queryId(parseResp.getQueryId())
                .queryText(parseResp.getQueryText())
                .parseInfo(parseResp.getSelectedParses().get(0))
                .build();
        QueryResult queryResult = queryService.performExecution(executeReq);

        assertPluginRecognizeResult(queryResult);
    }

    public void webPageRecognizeWithQueryFilter() throws Exception {
        MockConfiguration.mockEmbeddingRecognize(pluginManager, "在超音数最近的情况怎么样", "1");
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryRequest = DataUtils.getQueryReqWithAgent(1000, "在超音数最近的情况怎么样", 1);
        QueryFilters queryFilters = new QueryFilters();
        QueryFilter queryFilter = new QueryFilter();
        queryFilter.setElementID(2L);
        queryFilter.setValue("alice");
        queryRequest.setModelId(1L);
        queryFilters.getFilters().add(queryFilter);
        queryRequest.setQueryFilters(queryFilters);

        ParseResp parseResp = queryService.performParsing(queryRequest);
        ExecuteQueryReq executeReq = ExecuteQueryReq.builder().user(queryRequest.getUser())
                .chatId(parseResp.getChatId())
                .queryId(parseResp.getQueryId())
                .queryText(parseResp.getQueryText())
                .parseInfo(parseResp.getSelectedParses().get(0))
                .build();
        QueryResult queryResult = queryService.performExecution(executeReq);

        assertPluginRecognizeResult(queryResult);
    }

    public void pluginRecognizeWithAgent() {
        MockConfiguration.mockEmbeddingRecognize(pluginManager, "alice最近的访问情况怎么样", "1");
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        MockConfiguration.mockMetricAgent(agentService);
        QueryReq queryContextReq = DataUtils.getQueryReqWithAgent(1000, "alice最近的访问情况怎么样",
                DataUtils.getMetricAgent().getId());
        ParseResp parseResp = queryService.performParsing(queryContextReq);
        Assert.assertTrue(parseResp.getSelectedParses() != null
                && parseResp.getSelectedParses().size() > 0);
    }

}
