package com.tencent.supersonic.chat.core.query.semantic;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.RelatedSchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaValueMap;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewSchemaResp;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewSchemaBuilder {

    public static ViewSchema build(ViewSchemaResp resp) {
        ViewSchema viewSchema = new ViewSchema();
        viewSchema.setQueryConfig(resp.getQueryConfig());
        SchemaElement model = SchemaElement.builder()
                .view(resp.getId())
                .id(resp.getId())
                .name(resp.getName())
                .bizName(resp.getBizName())
                .type(SchemaElementType.VIEW)
                .build();
        viewSchema.setView(model);

        Set<SchemaElement> metrics = new HashSet<>();
        for (MetricSchemaResp metric : resp.getMetrics()) {

            List<String> alias = SchemaItem.getAliasList(metric.getAlias());

            SchemaElement metricToAdd = SchemaElement.builder()
                    .view(resp.getId())
                    .id(metric.getId())
                    .name(metric.getName())
                    .bizName(metric.getBizName())
                    .type(SchemaElementType.METRIC)
                    .useCnt(metric.getUseCnt())
                    .alias(alias)
                    .relatedSchemaElements(getRelateSchemaElement(metric))
                    .defaultAgg(metric.getDefaultAgg())
                    .build();
            metrics.add(metricToAdd);

        }
        viewSchema.getMetrics().addAll(metrics);

        Set<SchemaElement> dimensions = new HashSet<>();
        Set<SchemaElement> dimensionValues = new HashSet<>();
        Set<SchemaElement> tags = new HashSet<>();
        for (DimSchemaResp dim : resp.getDimensions()) {

            List<String> alias = SchemaItem.getAliasList(dim.getAlias());
            Set<String> dimValueAlias = new HashSet<>();
            List<DimValueMap> dimValueMaps = dim.getDimValueMaps();
            List<SchemaValueMap> schemaValueMaps = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dimValueMaps)) {

                for (DimValueMap dimValueMap : dimValueMaps) {
                    if (Strings.isNotEmpty(dimValueMap.getBizName())) {
                        dimValueAlias.add(dimValueMap.getBizName());
                    }
                    if (!CollectionUtils.isEmpty(dimValueMap.getAlias())) {
                        dimValueAlias.addAll(dimValueMap.getAlias());
                    }
                    SchemaValueMap schemaValueMap = new SchemaValueMap();
                    BeanUtils.copyProperties(dimValueMap, schemaValueMap);
                    schemaValueMaps.add(schemaValueMap);
                }

            }
            SchemaElement dimToAdd = SchemaElement.builder()
                    .view(resp.getId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.DIMENSION)
                    .useCnt(dim.getUseCnt())
                    .alias(alias)
                    .schemaValueMaps(schemaValueMaps)
                    .build();
            dimensions.add(dimToAdd);

            SchemaElement dimValueToAdd = SchemaElement.builder()
                    .view(resp.getId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.VALUE)
                    .useCnt(dim.getUseCnt())
                    .alias(new ArrayList<>(Arrays.asList(dimValueAlias.toArray(new String[0]))))
                    .build();
            dimensionValues.add(dimValueToAdd);
            if (dim.getIsTag() == 1) {
                SchemaElement tagToAdd = SchemaElement.builder()
                        .view(resp.getId())
                        .id(dim.getId())
                        .name(dim.getName())
                        .bizName(dim.getBizName())
                        .type(SchemaElementType.TAG)
                        .useCnt(dim.getUseCnt())
                        .alias(alias)
                        .schemaValueMaps(schemaValueMaps)
                        .build();
                tags.add(tagToAdd);
            }
        }
        viewSchema.getDimensions().addAll(dimensions);
        viewSchema.getDimensionValues().addAll(dimensionValues);
        viewSchema.getTags().addAll(tags);

        DimSchemaResp dim = resp.getPrimaryKey();
        if (dim != null) {
            SchemaElement entity = SchemaElement.builder()
                    .view(resp.getId())
                    .id(dim.getId())
                    .name(dim.getName())
                    .bizName(dim.getBizName())
                    .type(SchemaElementType.ENTITY)
                    .useCnt(dim.getUseCnt())
                    .alias(dim.getEntityAlias())
                    .build();
            viewSchema.setEntity(entity);
        }
        return viewSchema;
    }

    private static List<RelatedSchemaElement> getRelateSchemaElement(MetricSchemaResp metricSchemaResp) {
        RelateDimension relateDimension = metricSchemaResp.getRelateDimension();
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            return Lists.newArrayList();
        }
        return relateDimension.getDrillDownDimensions().stream().map(dimension -> {
            RelatedSchemaElement relateSchemaElement = new RelatedSchemaElement();
            BeanUtils.copyProperties(dimension, relateSchemaElement);
            return relateSchemaElement;
        }).collect(Collectors.toList());
    }

}
