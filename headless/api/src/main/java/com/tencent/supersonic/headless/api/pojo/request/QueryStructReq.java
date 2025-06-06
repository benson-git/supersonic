package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;


@Data
@Slf4j
public class QueryStructReq extends SemanticQueryReq {

    private List<String> groups = new ArrayList<>();
    private List<Aggregator> aggregators = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private List<Filter> dimensionFilters = new ArrayList<>();
    private List<Filter> metricFilters = new ArrayList<>();
    private DateConf dateInfo;
    private Long limit = 2000L;
    private QueryType queryType = QueryType.ID;


    /**
     * Later deleted for compatibility only
     */
    private String s2SQL;
    /**
     * Later deleted for compatibility only
     */
    private String correctS2SQL;

    public List<String> getGroups() {
        if (!CollectionUtils.isEmpty(this.groups)) {
            this.groups = groups.stream().filter(group -> !Strings.isEmpty(group)).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(this.groups)) {
            this.groups = Lists.newArrayList();
        }

        return this.groups;
    }

    public List<String> getMetrics() {
        List<String> metrics = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(this.aggregators)) {
            metrics = aggregators.stream().map(Aggregator::getColumn).collect(Collectors.toList());
        }
        return metrics;
    }

    public List<Order> getOrders() {
        if (orders == null) {
            return Lists.newArrayList();
        }
        return orders;
    }

    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"viewId\":")
                .append(viewId);
        stringBuilder.append("\"modelIds\":")
                .append(modelIds);
        stringBuilder.append(",\"groups\":")
                .append(groups);
        stringBuilder.append(",\"aggregators\":")
                .append(aggregators);
        stringBuilder.append(",\"orders\":")
                .append(orders);
        stringBuilder.append(",\"filters\":")
                .append(dimensionFilters);
        stringBuilder.append(",\"dateInfo\":")
                .append(dateInfo);
        stringBuilder.append(",\"params\":")
                .append(params);
        stringBuilder.append(",\"limit\":")
                .append(limit);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public String generateCommandMd5() {
        return DigestUtils.md5Hex(this.toCustomizedString());
    }

    public List<Filter> getOriginalFilter() {
        return dimensionFilters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"viewId\":")
                .append(viewId);
        sb.append("\"modelIds\":")
                .append(modelIds);
        sb.append(",\"groups\":")
                .append(groups);
        sb.append(",\"aggregators\":")
                .append(aggregators);
        sb.append(",\"orders\":")
                .append(orders);
        sb.append(",\"dimensionFilters\":")
                .append(dimensionFilters);
        sb.append(",\"metricFilters\":")
                .append(metricFilters);
        sb.append(",\"params\":")
                .append(params);
        sb.append(",\"dateInfo\":")
                .append(dateInfo);
        sb.append(",\"limit\":")
                .append(limit);
        sb.append(",\"cacheInfo\":")
                .append(cacheInfo);
        sb.append('}');
        return sb.toString();
    }

    public QuerySqlReq convert() {
        return convert(false);
    }

    /**
     * convert queryStructReq to QueryS2SQLReq
     *
     * @return
     */
    public QuerySqlReq convert(boolean isBizName) {
        String sql = null;
        try {
            sql = buildSql(this, isBizName);
        } catch (Exception e) {
            log.error("buildSql error", e);
        }

        QuerySqlReq result = new QuerySqlReq();
        result.setSql(sql);
        result.setViewId(this.getViewId());
        result.setModelIds(this.getModelIdSet());
        result.setParams(new ArrayList<>());
        return result;
    }

    private String buildSql(QueryStructReq queryStructReq, boolean isBizName) throws JSQLParserException {
        Select select = new Select();
        //1.Set the select items (columns)
        PlainSelect plainSelect = new PlainSelect();
        List<SelectItem> selectItems = new ArrayList<>();
        List<String> groups = queryStructReq.getGroups();
        if (!CollectionUtils.isEmpty(groups)) {
            for (String group : groups) {
                selectItems.add(new SelectExpressionItem(new Column(group)));
            }
        }
        List<Aggregator> aggregators = queryStructReq.getAggregators();
        if (!CollectionUtils.isEmpty(aggregators)) {
            for (Aggregator aggregator : aggregators) {
                String columnName = aggregator.getColumn();
                if (queryStructReq.getQueryType().isNativeAggQuery()) {
                    selectItems.add(new SelectExpressionItem(new Column(columnName)));
                } else {
                    Function sumFunction = new Function();
                    AggOperatorEnum func = aggregator.getFunc();
                    if (AggOperatorEnum.UNKNOWN.equals(func)) {
                        func = AggOperatorEnum.SUM;
                    }
                    sumFunction.setName(func.getOperator());
                    if (AggOperatorEnum.COUNT_DISTINCT.equals(func)) {
                        sumFunction.setName("count");
                        sumFunction.setDistinct(true);
                    }
                    sumFunction.setParameters(new ExpressionList(new Column(columnName)));
                    SelectExpressionItem selectExpressionItem = new SelectExpressionItem(sumFunction);
                    String alias = StringUtils.isNotBlank(aggregator.getAlias()) ? aggregator.getAlias() : columnName;
                    selectExpressionItem.setAlias(new Alias(alias));
                    selectItems.add(selectExpressionItem);
                }
            }
        }
        plainSelect.setSelectItems(selectItems);
        //2.Set the table name
        Table table = new Table(queryStructReq.getTableName());
        plainSelect.setFromItem(table);

        //3.Set the order by clause
        List<Order> orders = queryStructReq.getOrders();
        if (!CollectionUtils.isEmpty(orders)) {
            List<OrderByElement> orderByElements = new ArrayList<>();
            for (Order order : orders) {
                if (StringUtils.isBlank(order.getColumn())) {
                    continue;
                }
                OrderByElement orderByElement = new OrderByElement();
                orderByElement.setExpression(new Column(order.getColumn()));
                orderByElement.setAsc(false);
                if (Constants.ASC_UPPER.equalsIgnoreCase(order.getDirection())) {
                    orderByElement.setAsc(true);
                }
                orderByElements.add(orderByElement);
            }
            plainSelect.setOrderByElements(orderByElements);
        }

        //4.Set the group by clause
        if (!CollectionUtils.isEmpty(groups) && !queryStructReq.getQueryType().isNativeAggQuery()) {
            GroupByElement groupByElement = new GroupByElement();
            for (String group : groups) {
                groupByElement.addGroupByExpression(new Column(group));
            }
            plainSelect.setGroupByElement(groupByElement);
        }

        //5.Set the limit clause
        if (Objects.nonNull(queryStructReq.getLimit())) {
            Limit limit = new Limit();
            limit.setRowCount(new LongValue(queryStructReq.getLimit()));
            plainSelect.setLimit(limit);
        }
        select.setSelectBody(plainSelect);

        //6.Set where
        List<Filter> dimensionFilters = queryStructReq.getDimensionFilters();
        SqlFilterUtils sqlFilterUtils = ContextUtils.getBean(SqlFilterUtils.class);
        String whereClause = sqlFilterUtils.getWhereClause(dimensionFilters, isBizName);

        String sql = select.toString();
        if (StringUtils.isNotBlank(whereClause)) {
            Expression expression = CCJSqlParserUtil.parseCondExpression(whereClause);
            sql = SqlAddHelper.addWhere(sql, expression);
        }

        //7.Set DateInfo
        DateModeUtils dateModeUtils = ContextUtils.getBean(DateModeUtils.class);
        String dateWhereStr = dateModeUtils.getDateWhereStr(queryStructReq.getDateInfo());
        if (StringUtils.isNotBlank(dateWhereStr)) {
            Expression expression = CCJSqlParserUtil.parseCondExpression(dateWhereStr);
            sql = SqlAddHelper.addWhere(sql, expression);
        }
        return sql;
    }

    public String getTableName() {
        if (StringUtils.isNotBlank(viewName)) {
            return viewName;
        }
        if (viewId != null) {
            return Constants.TABLE_PREFIX + viewId;
        }
        return Constants.TABLE_PREFIX + StringUtils.join(modelIds, "_");
    }

}
