package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetricQueryDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricBaseReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;

import java.util.List;
import java.util.Set;

public interface MetricService {

    MetricResp createMetric(MetricReq metricReq, User user) throws Exception;

    void createMetricBatch(List<MetricReq> metricReqs, User user) throws Exception;

    MetricResp updateMetric(MetricReq metricReq, User user) throws Exception;

    void batchUpdateStatus(MetaBatchReq metaBatchReq, User user);

    void deleteMetric(Long id, User user) throws Exception;

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user);

    List<MetricResp> getMetrics(MetaFilter metaFilter);

    List<MetricResp> getMetricsToCreateNewMetric(Long modelId);

    MetricResp getMetric(Long modelId, String bizName);

    MetricResp getMetric(Long id, User user);

    MetricResp getMetric(Long id);

    List<String> mockAlias(MetricBaseReq metricReq, String mockType, User user);

    Set<String> getMetricTags();

    List<DrillDownDimension> getDrillDownDimension(Long metricId);

    void saveMetricQueryDefaultConfig(MetricQueryDefaultConfig defaultConfig, User user);

    MetricQueryDefaultConfig getMetricQueryDefaultConfig(Long metricId, User user);

    void sendMetricEventBatch(List<Long> modelIds, EventType eventType);
}
