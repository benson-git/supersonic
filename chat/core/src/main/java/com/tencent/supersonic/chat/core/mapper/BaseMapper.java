package com.tencent.supersonic.chat.core.mapper;

import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {

        String simpleName = this.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        log.debug("before {},mapInfo:{}", simpleName, queryContext.getMapInfo().getViewElementMatches());

        try {
            doMap(queryContext);
        } catch (Exception e) {
            log.error("work error", e);
        }

        long cost = System.currentTimeMillis() - startTime;
        log.debug("after {},cost:{},mapInfo:{}", simpleName, cost, queryContext.getMapInfo().getViewElementMatches());
    }

    public abstract void doMap(QueryContext queryContext);

    public void addToSchemaMap(SchemaMapInfo schemaMap, Long modelId, SchemaElementMatch newElementMatch) {
        Map<Long, List<SchemaElementMatch>> modelElementMatches = schemaMap.getViewElementMatches();
        List<SchemaElementMatch> schemaElementMatches = modelElementMatches.putIfAbsent(modelId, new ArrayList<>());
        if (schemaElementMatches == null) {
            schemaElementMatches = modelElementMatches.get(modelId);
        }
        //remove duplication
        AtomicBoolean needAddNew = new AtomicBoolean(true);
        schemaElementMatches.removeIf(
                existElementMatch -> {
                    SchemaElement existElement = existElementMatch.getElement();
                    SchemaElement newElement = newElementMatch.getElement();
                    if (existElement.equals(newElement)) {
                        if (newElementMatch.getSimilarity() > existElementMatch.getSimilarity()) {
                            return true;
                        } else {
                            needAddNew.set(false);
                        }
                    }
                    return false;
                }
        );
        if (needAddNew.get()) {
            schemaElementMatches.add(newElementMatch);
        }
    }

    public SchemaElement getSchemaElement(Long viewId, SchemaElementType elementType, Long elementID,
            SemanticSchema semanticSchema) {
        SchemaElement element = new SchemaElement();
        ViewSchema viewSchema = semanticSchema.getViewSchemaMap().get(viewId);
        if (Objects.isNull(viewSchema)) {
            return null;
        }
        SchemaElement elementDb = viewSchema.getElement(elementType, elementID);
        if (Objects.isNull(elementDb)) {
            log.info("element is null, elementType:{},elementID:{}", elementType, elementID);
            return null;
        }
        BeanUtils.copyProperties(elementDb, element);
        element.setAlias(getAlias(elementDb));
        return element;
    }

    public List<String> getAlias(SchemaElement element) {
        if (!SchemaElementType.VALUE.equals(element.getType())) {
            return element.getAlias();
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(element.getAlias()) && StringUtils.isNotEmpty(
                element.getName())) {
            return element.getAlias().stream()
                    .filter(aliasItem -> aliasItem.contains(element.getName()))
                    .collect(Collectors.toList());
        }
        return element.getAlias();
    }
}
