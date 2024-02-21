package com.tencent.supersonic.chat.core.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.core.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class QueryFilterMapper implements SchemaMapper {

    private double similarity = 1.0;

    @Override
    public void map(QueryContext queryContext) {
        Long viewId = queryContext.getViewId();
        if (viewId == null || viewId <= 0) {
            return;
        }
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        clearOtherSchemaElementMatch(viewId, schemaMapInfo);
        List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(viewId);
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
            schemaMapInfo.setMatchedElements(viewId, schemaElementMatches);
        }
        addValueSchemaElementMatch(queryContext, schemaElementMatches);
    }

    private void clearOtherSchemaElementMatch(Long modelId, SchemaMapInfo schemaMapInfo) {
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMapInfo.getViewElementMatches().entrySet()) {
            if (!entry.getKey().equals(modelId)) {
                entry.getValue().clear();
            }
        }
    }

    private List<SchemaElementMatch> addValueSchemaElementMatch(QueryContext queryContext,
            List<SchemaElementMatch> candidateElementMatches) {
        QueryFilters queryFilters = queryContext.getQueryFilters();
        if (queryFilters == null || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return candidateElementMatches;
        }
        for (QueryFilter filter : queryFilters.getFilters()) {
            if (checkExistSameValueSchemaElementMatch(filter, candidateElementMatches)) {
                continue;
            }
            SchemaElement element = SchemaElement.builder()
                    .id(filter.getElementID())
                    .name(String.valueOf(filter.getValue()))
                    .type(SchemaElementType.VALUE)
                    .bizName(filter.getBizName())
                    .view(queryContext.getViewId())
                    .build();
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(element)
                    .frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .word(String.valueOf(filter.getValue()))
                    .similarity(similarity)
                    .detectWord(Constants.EMPTY)
                    .build();
            candidateElementMatches.add(schemaElementMatch);
        }
        return candidateElementMatches;
    }

    private boolean checkExistSameValueSchemaElementMatch(QueryFilter queryFilter,
            List<SchemaElementMatch> schemaElementMatches) {
        List<SchemaElementMatch> valueSchemaElements = schemaElementMatches.stream().filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : valueSchemaElements) {
            if (schemaElementMatch.getElement().getId().equals(queryFilter.getElementID())
                    && schemaElementMatch.getWord().equals(String.valueOf(queryFilter.getValue()))) {
                return true;
            }
        }
        return false;
    }
}
