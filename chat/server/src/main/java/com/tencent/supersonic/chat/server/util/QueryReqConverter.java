package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.pojo.enums.Text2SQLType;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import org.apache.commons.collections.MapUtils;

import java.util.Objects;

public class QueryReqConverter {

    public static QueryNLReq buildText2SqlQueryReq(ChatParseContext chatParseContext) {
        QueryNLReq queryNLReq = new QueryNLReq();
        BeanMapper.mapper(chatParseContext, queryNLReq);
        Agent agent = chatParseContext.getAgent();
        if (agent == null) {
            return queryNLReq;
        }
        if (agent.containsLLMParserTool() && agent.containsRuleTool()) {
            queryNLReq.setText2SQLType(Text2SQLType.RULE_AND_LLM);
        } else if (agent.containsLLMParserTool()) {
            queryNLReq.setText2SQLType(Text2SQLType.ONLY_LLM);
        } else if (agent.containsRuleTool()) {
            queryNLReq.setText2SQLType(Text2SQLType.ONLY_RULE);
        }
        queryNLReq.setDataSetIds(agent.getDataSetIds());
        if (Objects.nonNull(queryNLReq.getMapInfo())
                && MapUtils.isNotEmpty(queryNLReq.getMapInfo().getDataSetElementMatches())) {
            queryNLReq.setMapInfo(queryNLReq.getMapInfo());
        }
        queryNLReq.setModelConfig(agent.getModelConfig());
        queryNLReq.setPromptConfig(agent.getPromptConfig());
        return queryNLReq;
    }

}
