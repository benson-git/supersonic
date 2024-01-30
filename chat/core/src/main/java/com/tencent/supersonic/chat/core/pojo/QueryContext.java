package com.tencent.supersonic.chat.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaModelClusterMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.plugin.Plugin;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryContext {

    private String queryText;
    private Integer chatId;
    private Long modelId;
    private User user;
    private boolean saveAnswer = true;
    private Integer agentId;
    private QueryFilters queryFilters;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();
    private SchemaModelClusterMapInfo modelClusterMapInfo = new SchemaModelClusterMapInfo();
    @JsonIgnore
    private SemanticSchema semanticSchema;
    @JsonIgnore
    private Agent agent;
    @JsonIgnore
    private Map<Long, ChatConfigRichResp> modelIdToChatRichConfig;
    @JsonIgnore
    private Map<String, Plugin> nameToPlugin;
    @JsonIgnore
    private List<Plugin> pluginList;

    public List<SemanticQuery> getCandidateQueries() {
        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);
        Integer parseShowCount = optimizationConfig.getParseShowCount();
        candidateQueries = candidateQueries.stream()
                .sorted(Comparator.comparing(semanticQuery -> semanticQuery.getParseInfo().getScore(),
                        Comparator.reverseOrder()))
                .limit(parseShowCount)
                .collect(Collectors.toList());
        return candidateQueries;
    }
}
