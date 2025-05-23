package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * basic semantic correction functionality, offering common methods and an
 * abstract method called doCorrect
 */
@Slf4j
public abstract class BaseSemanticCorrector implements SemanticCorrector {

    public void correct(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        try {
            if (StringUtils.isBlank(semanticParseInfo.getSqlInfo().getCorrectS2SQL())) {
                return;
            }
            doCorrect(queryContext, semanticParseInfo);
            log.info("sqlCorrection:{} sql:{}", this.getClass().getSimpleName(), semanticParseInfo.getSqlInfo());
        } catch (Exception e) {
            log.error(String.format("correct error,sqlInfo:%s", semanticParseInfo.getSqlInfo()), e);
        }
    }

    public abstract void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo);

    protected Map<String, String> getFieldNameMap(QueryContext queryContext, Long viewId) {

        SemanticSchema semanticSchema = queryContext.getSemanticSchema();

        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        // support fieldName and field alias
        Map<String, String> result = dbAllFields.stream()
                .filter(entry -> viewId.equals(entry.getView()))
                .flatMap(schemaElement -> {
                    Set<String> elements = new HashSet<>();
                    elements.add(schemaElement.getName());
                    if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                        elements.addAll(schemaElement.getAlias());
                    }
                    return elements.stream();
                })
                .collect(Collectors.toMap(a -> a, a -> a, (k1, k2) -> k1));
        result.put(TimeDimensionEnum.DAY.getChName(), TimeDimensionEnum.DAY.getChName());
        result.put(TimeDimensionEnum.MONTH.getChName(), TimeDimensionEnum.MONTH.getChName());
        result.put(TimeDimensionEnum.WEEK.getChName(), TimeDimensionEnum.WEEK.getChName());

        result.put(TimeDimensionEnum.DAY.getName(), TimeDimensionEnum.DAY.getChName());
        result.put(TimeDimensionEnum.MONTH.getName(), TimeDimensionEnum.MONTH.getChName());
        result.put(TimeDimensionEnum.WEEK.getName(), TimeDimensionEnum.WEEK.getChName());

        return result;
    }

    protected void addFieldsToSelect(SemanticParseInfo semanticParseInfo, String correctS2SQL) {
        Set<String> selectFields = new HashSet<>(SqlSelectHelper.getSelectFields(correctS2SQL));
        Set<String> needAddFields = new HashSet<>(SqlSelectHelper.getGroupByFields(correctS2SQL));

        //decide whether add order by expression field to select
        Environment environment = ContextUtils.getBean(Environment.class);
        String correctorAdditionalInfo = environment.getProperty("corrector.additional.information");
        if (StringUtils.isNotBlank(correctorAdditionalInfo) && Boolean.parseBoolean(correctorAdditionalInfo)) {
            needAddFields.addAll(SqlSelectHelper.getOrderByFields(correctS2SQL));
        }

        // If there is no aggregate function in the S2SQL statement and
        // there is a data field in 'WHERE' statement, add the field to the 'SELECT' statement.
        if (!SqlSelectFunctionHelper.hasAggregateFunction(correctS2SQL)) {
            List<String> whereFields = SqlSelectHelper.getWhereFields(correctS2SQL);
            List<String> timeChNameList = TimeDimensionEnum.getChNameList();
            Set<String> timeFields = whereFields.stream().filter(field -> timeChNameList.contains(field))
                    .collect(Collectors.toSet());
            needAddFields.addAll(timeFields);
        }

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(needAddFields)) {
            return;
        }

        needAddFields.removeAll(selectFields);
        String replaceFields = SqlAddHelper.addFieldsToSelect(correctS2SQL, new ArrayList<>(needAddFields));
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(replaceFields);
    }

    protected void addAggregateToMetric(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        //add aggregate to all metric
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        Long viewId = semanticParseInfo.getView().getView();
        List<SchemaElement> metrics = getMetricElements(queryContext, viewId);

        Map<String, String> metricToAggregate = metrics.stream()
                .map(schemaElement -> {
                    if (Objects.isNull(schemaElement.getDefaultAgg())) {
                        schemaElement.setDefaultAgg(AggregateTypeEnum.SUM.name());
                    }
                    return schemaElement;
                }).flatMap(schemaElement -> {
                    Set<String> elements = new HashSet<>();
                    elements.add(schemaElement.getName());
                    if (!CollectionUtils.isEmpty(schemaElement.getAlias())) {
                        elements.addAll(schemaElement.getAlias());
                    }
                    return elements.stream().map(element -> Pair.of(element, schemaElement.getDefaultAgg())
                    );
                }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        if (CollectionUtils.isEmpty(metricToAggregate)) {
            return;
        }
        String aggregateSql = SqlAddHelper.addAggregateToField(correctS2SQL, metricToAggregate);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(aggregateSql);
    }

    protected List<SchemaElement> getMetricElements(QueryContext queryContext, Long viewId) {
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        return semanticSchema.getMetrics(viewId);
    }

}
