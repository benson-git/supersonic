package com.tencent.supersonic.chat.core.parser.plugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.core.parser.SemanticParser;
import com.tencent.supersonic.chat.core.plugin.Plugin;
import com.tencent.supersonic.chat.core.plugin.PluginManager;
import com.tencent.supersonic.chat.core.plugin.PluginParseResult;
import com.tencent.supersonic.chat.core.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.chat.core.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.ModelCluster;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.CollectionUtils;


/**
 * PluginParser defines the basic process and common methods for recalling plugins.
 */
public abstract class PluginParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        for (SemanticQuery semanticQuery : queryContext.getCandidateQueries()) {
            if (queryContext.getQueryText().length() <= semanticQuery.getParseInfo().getScore()
                    && (QueryManager.getPluginQueryModes().contains(semanticQuery.getQueryMode()))) {
                return;
            }
        }
        if (!checkPreCondition(queryContext)) {
            return;
        }
        PluginRecallResult pluginRecallResult = recallPlugin(queryContext);
        if (pluginRecallResult == null) {
            return;
        }
        buildQuery(queryContext, pluginRecallResult);
    }

    public abstract boolean checkPreCondition(QueryContext queryContext);

    public abstract PluginRecallResult recallPlugin(QueryContext queryContext);

    public void buildQuery(QueryContext queryContext, PluginRecallResult pluginRecallResult) {
        Plugin plugin = pluginRecallResult.getPlugin();
        Set<Long> modelIds = pluginRecallResult.getModelIds();
        if (plugin.isContainsAllModel()) {
            modelIds = Sets.newHashSet(-1L);
        }
        for (Long modelId : modelIds) {
            PluginSemanticQuery pluginQuery = QueryManager.createPluginQuery(plugin.getType());
            SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(modelId, plugin,
                    queryContext.getQueryFilters(), queryContext.getModelClusterMapInfo().getMatchedElements(modelId),
                    pluginRecallResult.getDistance());
            semanticParseInfo.setQueryMode(pluginQuery.getQueryMode());
            semanticParseInfo.setScore(pluginRecallResult.getScore());
            pluginQuery.setParseInfo(semanticParseInfo);
            queryContext.getCandidateQueries().add(pluginQuery);
        }
    }

    protected List<Plugin> getPluginList(QueryContext queryContext) {
        return PluginManager.getPluginAgentCanSupport(queryContext);
    }

    protected SemanticParseInfo buildSemanticParseInfo(Long modelId, Plugin plugin, QueryFilters queryFilters,
            List<SchemaElementMatch> schemaElementMatches, double distance) {
        if (modelId == null && !CollectionUtils.isEmpty(plugin.getModelList())) {
            modelId = plugin.getModelList().get(0);
        }
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
        }
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setElementMatches(schemaElementMatches);
        semanticParseInfo.setModel(ModelCluster.build(Sets.newHashSet(modelId)));
        Map<String, Object> properties = new HashMap<>();
        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setQueryFilters(queryFilters);
        pluginParseResult.setDistance(distance);
        properties.put(Constants.CONTEXT, pluginParseResult);
        properties.put("type", "plugin");
        properties.put("name", plugin.getName());
        semanticParseInfo.setProperties(properties);
        semanticParseInfo.setScore(distance);
        fillSemanticParseInfo(semanticParseInfo);
        return semanticParseInfo;
    }

    private void fillSemanticParseInfo(SemanticParseInfo semanticParseInfo) {
        List<SchemaElementMatch> schemaElementMatches = semanticParseInfo.getElementMatches();
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return;
        }
        schemaElementMatches.stream().filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                                || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                .forEach(schemaElementMatch -> {
                    QueryFilter queryFilter = new QueryFilter();
                    queryFilter.setValue(schemaElementMatch.getWord());
                    queryFilter.setElementID(schemaElementMatch.getElement().getId());
                    queryFilter.setName(schemaElementMatch.getElement().getName());
                    queryFilter.setOperator(FilterOperatorEnum.EQUALS);
                    queryFilter.setBizName(schemaElementMatch.getElement().getBizName());
                    semanticParseInfo.getDimensionFilters().add(queryFilter);
                });
    }
}
