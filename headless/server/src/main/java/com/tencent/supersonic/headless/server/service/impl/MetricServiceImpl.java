package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.ChatGptHelper;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetricParam;
import com.tencent.supersonic.headless.api.pojo.MetricQueryDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricBaseReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.CollectDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricQueryDefaultConfigDO;
import com.tencent.supersonic.headless.server.persistence.repository.MetricRepository;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.service.CollectService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.ViewService;
import com.tencent.supersonic.headless.server.utils.MetricCheckUtils;
import com.tencent.supersonic.headless.server.utils.MetricConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MetricServiceImpl implements MetricService {

    private MetricRepository metricRepository;

    private ModelService modelService;

    private DomainService domainService;

    private ChatGptHelper chatGptHelper;

    private CollectService collectService;

    private ViewService viewService;

    private ApplicationEventPublisher eventPublisher;

    public MetricServiceImpl(MetricRepository metricRepository,
            ModelService modelService,
            DomainService domainService,
            ChatGptHelper chatGptHelper,
            CollectService collectService,
            ViewService viewService,
            ApplicationEventPublisher eventPublisher) {
        this.domainService = domainService;
        this.metricRepository = metricRepository;
        this.modelService = modelService;
        this.chatGptHelper = chatGptHelper;
        this.eventPublisher = eventPublisher;
        this.collectService = collectService;
        this.viewService = viewService;
    }

    @Override
    public MetricResp createMetric(MetricReq metricReq, User user) {
        checkExist(Lists.newArrayList(metricReq));
        MetricCheckUtils.checkParam(metricReq);
        metricReq.createdBy(user.getName());
        MetricDO metricDO = MetricConverter.convert2MetricDO(metricReq);
        metricRepository.createMetric(metricDO);
        sendEventBatch(Lists.newArrayList(metricDO), EventType.ADD);
        return MetricConverter.convert2MetricResp(metricDO);
    }

    @Override
    public void createMetricBatch(List<MetricReq> metricReqs, User user) {
        if (CollectionUtils.isEmpty(metricReqs)) {
            return;
        }
        Long modelId = metricReqs.get(0).getModelId();
        List<MetricResp> metricResps = getMetrics(new MetaFilter(Lists.newArrayList(modelId)));
        Map<String, MetricResp> bizNameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, MetricResp> nameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getName, a -> a, (k1, k2) -> k1));
        List<MetricReq> metricToInsert = metricReqs.stream()
                .filter(metric -> !bizNameMap.containsKey(metric.getBizName())
                        && !nameMap.containsKey(metric.getName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(metricToInsert)) {
            return;
        }
        List<MetricDO> metricDOS = metricToInsert.stream().peek(metric -> metric.createdBy(user.getName()))
                .map(MetricConverter::convert2MetricDO).collect(Collectors.toList());
        metricRepository.createMetricBatch(metricDOS);
        sendEventBatch(metricDOS, EventType.ADD);
    }

    @Override
    public MetricResp updateMetric(MetricReq metricReq, User user) {
        MetricCheckUtils.checkParam(metricReq);
        checkExist(Lists.newArrayList(metricReq));
        metricReq.updatedBy(user.getName());
        MetricDO metricDO = metricRepository.getMetricById(metricReq.getId());
        String oldName = metricDO.getName();
        MetricConverter.convert(metricDO, metricReq);
        metricRepository.updateMetric(metricDO);
        if (!oldName.equals(metricDO.getName())) {
            DataItem dataItem = getDataItem(metricDO);
            dataItem.setName(oldName);
            dataItem.setNewName(metricDO.getName());
            sendEvent(getDataItem(metricDO), EventType.UPDATE);
        }
        return MetricConverter.convert2MetricResp(metricDO);
    }

    @Override
    public void batchUpdateStatus(MetaBatchReq metaBatchReq, User user) {
        if (CollectionUtils.isEmpty(metaBatchReq.getIds())) {
            return;
        }
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setIds(metaBatchReq.getIds());
        List<MetricDO> metricDOS = metricRepository.getMetric(metricFilter);
        if (CollectionUtils.isEmpty(metricDOS)) {
            return;
        }
        metricDOS = metricDOS.stream()
                .peek(metricDO -> {
                    metricDO.setStatus(metaBatchReq.getStatus());
                    metricDO.setUpdatedAt(new Date());
                    metricDO.setUpdatedBy(user.getName());
                })
                .collect(Collectors.toList());
        metricRepository.batchUpdateStatus(metricDOS);
        if (StatusEnum.OFFLINE.getCode().equals(metaBatchReq.getStatus())
                || StatusEnum.DELETED.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(metricDOS, EventType.DELETE);
        } else if (StatusEnum.ONLINE.getCode().equals(metaBatchReq.getStatus())) {
            sendEventBatch(metricDOS, EventType.ADD);
        }
    }

    @Override
    public void deleteMetric(Long id, User user) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            throw new RuntimeException(String.format("the metric %s not exist", id));
        }
        metricDO.setStatus(StatusEnum.DELETED.getCode());
        metricDO.setUpdatedAt(new Date());
        metricDO.setUpdatedBy(user.getName());
        metricRepository.updateMetric(metricDO);
        sendEventBatch(Lists.newArrayList(metricDO), EventType.DELETE);
    }

    @Override
    public PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user) {
        MetricFilter metricFilter = new MetricFilter();
        BeanUtils.copyProperties(pageMetricReq, metricFilter);
        Set<DomainResp> domainResps = domainService.getDomainChildren(pageMetricReq.getDomainIds());
        List<Long> domainIds = domainResps.stream().map(DomainResp::getId).collect(Collectors.toList());
        List<ModelResp> modelResps = modelService.getModelByDomainIds(domainIds);
        List<Long> modelIds = modelResps.stream().map(ModelResp::getId).collect(Collectors.toList());
        pageMetricReq.getModelIds().addAll(modelIds);
        metricFilter.setModelIds(pageMetricReq.getModelIds());
        List<CollectDO> collectList = collectService.getCollectList(user.getName());
        List<Long> collectIds = collectList.stream().map(CollectDO::getCollectId).collect(Collectors.toList());
        if (pageMetricReq.isHasCollect()) {
            if (CollectionUtils.isEmpty(collectIds)) {
                metricFilter.setIds(Lists.newArrayList(-1L));
            } else {
                metricFilter.setIds(collectIds);
            }
        }
        PageInfo<MetricDO> metricDOPageInfo = PageHelper.startPage(pageMetricReq.getCurrent(),
                        pageMetricReq.getPageSize())
                .doSelectPageInfo(() -> queryMetric(metricFilter));
        PageInfo<MetricResp> pageInfo = new PageInfo<>();
        BeanUtils.copyProperties(metricDOPageInfo, pageInfo);
        List<MetricResp> metricResps = convertList(metricDOPageInfo.getList(), collectIds);
        fillAdminRes(metricResps, user);
        pageInfo.setList(metricResps);
        return pageInfo;
    }

    protected List<MetricDO> queryMetric(MetricFilter metricFilter) {
        return metricRepository.getMetric(metricFilter);
    }

    @Override
    public List<MetricResp> getMetrics(MetaFilter metaFilter) {
        MetricFilter metricFilter = new MetricFilter();
        BeanUtils.copyProperties(metaFilter, metricFilter);
        List<MetricResp> metricResps = convertList(queryMetric(metricFilter));
        if (!CollectionUtils.isEmpty(metaFilter.getFieldsDepend())) {
            return filterByField(metricResps, metaFilter.getFieldsDepend());
        }
        if (metaFilter.getViewId() != null) {
            ViewResp viewResp = viewService.getView(metaFilter.getViewId());
            return MetricConverter.filterByView(metricResps, viewResp);
        }
        return metricResps;
    }

    private List<MetricResp> filterByField(List<MetricResp> metricResps, List<String> fields) {
        List<MetricResp> metricRespFiltered = Lists.newArrayList();
        for (MetricResp metricResp : metricResps) {
            for (String field : fields) {
                if (MetricDefineType.METRIC.equals(metricResp.getMetricDefineType())) {
                    List<Long> ids = metricResp.getMetricDefineByMetricParams().getMetrics()
                            .stream().map(MetricParam::getId).collect(Collectors.toList());
                    List<MetricResp> metricById = metricResps.stream()
                            .filter(metric -> ids.contains(metric.getId()))
                            .collect(Collectors.toList());
                    for (MetricResp metric : metricById) {
                        if (metric.getExpr().contains(field)) {
                            metricRespFiltered.add(metricResp);
                        }
                    }
                } else {
                    if (metricResp.getExpr().contains(field)) {
                        metricRespFiltered.add(metricResp);
                    }
                }
            }
        }
        return metricRespFiltered;
    }

    @Override
    public List<MetricResp> getMetricsToCreateNewMetric(Long modelId) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = getMetrics(metricFilter);
        return metricResps.stream().filter(metricResp ->
                MetricDefineType.FIELD.equals(metricResp.getMetricDefineType())
                        || MetricDefineType.MEASURE.equals(metricResp.getMetricDefineType()))
                .collect(Collectors.toList());
    }

    private void fillAdminRes(List<MetricResp> metricResps, User user) {
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        if (CollectionUtils.isEmpty(modelResps)) {
            return;
        }
        Set<Long> modelIdSet = modelResps.stream().map(ModelResp::getId).collect(Collectors.toSet());
        for (MetricResp metricResp : metricResps) {
            if (modelIdSet.contains(metricResp.getModelId())) {
                metricResp.setHasAdminRes(true);
            }
        }
    }

    @Deprecated
    @Override
    public MetricResp getMetric(Long modelId, String bizName) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setBizName(bizName);
        metricFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = getMetrics(metricFilter);
        MetricResp metricResp = null;
        if (CollectionUtils.isEmpty(metricResps)) {
            return metricResp;
        }
        return metricResps.get(0);
    }

    @Override
    public MetricResp getMetric(Long id, User user) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            return null;
        }
        Map<Long, ModelResp> modelMap = modelService.getModelMap();
        List<CollectDO> collectList = collectService.getCollectList(user.getName());
        List<Long> collect = collectList.stream().map(CollectDO::getCollectId).collect(Collectors.toList());
        MetricResp metricResp = MetricConverter.convert2MetricResp(metricDO, modelMap, collect);
        fillAdminRes(Lists.newArrayList(metricResp), user);
        return metricResp;
    }

    @Override
    public MetricResp getMetric(Long id) {
        MetricDO metricDO = metricRepository.getMetricById(id);
        if (metricDO == null) {
            return null;
        }
        return MetricConverter.convert2MetricResp(metricDO, new HashMap<>(), Lists.newArrayList());
    }

    @Override
    public List<String> mockAlias(MetricBaseReq metricReq, String mockType, User user) {

        String mockAlias = chatGptHelper.mockAlias(mockType, metricReq.getName(), metricReq.getBizName(), "",
                metricReq.getDescription(), !"".equals(metricReq.getDataFormatType()));
        return JSONObject.parseObject(mockAlias, new TypeReference<List<String>>() {
        });
    }

    @Override
    public Set<String> getMetricTags() {
        List<MetricResp> metricResps = getMetrics(new MetaFilter());
        if (CollectionUtils.isEmpty(metricResps)) {
            return new HashSet<>();
        }
        return metricResps.stream().flatMap(metricResp ->
                metricResp.getTags().stream()).collect(Collectors.toSet());
    }

    @Override
    public List<DrillDownDimension> getDrillDownDimension(Long metricId) {
        List<DrillDownDimension> drillDownDimensions = Lists.newArrayList();
        MetricResp metricResp = getMetric(metricId);
        if (metricResp == null) {
            return drillDownDimensions;
        }
        if (metricResp.getRelateDimension() != null
                && !CollectionUtils.isEmpty(metricResp.getRelateDimension().getDrillDownDimensions())) {
            drillDownDimensions.addAll(metricResp.getRelateDimension().getDrillDownDimensions());
        }
        ModelResp modelResp = modelService.getModel(metricResp.getModelId());
        if (modelResp.getDrillDownDimensions() == null) {
            return drillDownDimensions;
        }
        for (DrillDownDimension drillDownDimension : modelResp.getDrillDownDimensions()) {
            if (!drillDownDimensions.stream().map(DrillDownDimension::getDimensionId)
                    .collect(Collectors.toList()).contains(drillDownDimension.getDimensionId())) {
                drillDownDimension.setInheritedFromModel(true);
                drillDownDimensions.add(drillDownDimension);
            }
        }
        return drillDownDimensions;
    }

    @Override
    public void saveMetricQueryDefaultConfig(MetricQueryDefaultConfig defaultConfig, User user) {
        MetricQueryDefaultConfigDO defaultConfigDO =
                metricRepository.getDefaultQueryConfig(defaultConfig.getMetricId(), user.getName());
        if (defaultConfigDO == null) {
            defaultConfigDO = new MetricQueryDefaultConfigDO();
            defaultConfig.createdBy(user.getName());
            BeanMapper.mapper(defaultConfig, defaultConfigDO);
            metricRepository.saveDefaultQueryConfig(defaultConfigDO);
        } else {
            defaultConfig.setId(defaultConfigDO.getId());
            defaultConfig.updatedBy(user.getName());
            BeanMapper.mapper(defaultConfig, defaultConfigDO);
            metricRepository.updateDefaultQueryConfig(defaultConfigDO);
        }
    }

    @Override
    public MetricQueryDefaultConfig getMetricQueryDefaultConfig(Long metricId, User user) {
        MetricQueryDefaultConfigDO metricQueryDefaultConfigDO =
                metricRepository.getDefaultQueryConfig(metricId, user.getName());
        MetricQueryDefaultConfig metricQueryDefaultConfig = new MetricQueryDefaultConfig();
        BeanMapper.mapper(metricQueryDefaultConfigDO, metricQueryDefaultConfig);
        return metricQueryDefaultConfig;
    }

    private void checkExist(List<MetricBaseReq> metricReqs) {
        Long modelId = metricReqs.get(0).getModelId();
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setModelIds(Lists.newArrayList(modelId));
        List<MetricResp> metricResps = getMetrics(metaFilter);
        Map<String, MetricResp> bizNameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getBizName, a -> a, (k1, k2) -> k1));
        Map<String, MetricResp> nameMap = metricResps.stream()
                .collect(Collectors.toMap(MetricResp::getName, a -> a, (k1, k2) -> k1));
        for (MetricBaseReq metricReq : metricReqs) {
            if (bizNameMap.containsKey(metricReq.getBizName())) {
                MetricResp metricResp = bizNameMap.get(metricReq.getBizName());
                if (!metricResp.getId().equals(metricReq.getId())) {
                    throw new RuntimeException(String.format("该主题域下存在相同的指标字段名:%s 创建人:%s",
                            metricReq.getBizName(), metricResp.getCreatedBy()));
                }
            }
            if (nameMap.containsKey(metricReq.getName())) {
                MetricResp metricResp = nameMap.get(metricReq.getName());
                if (!metricResp.getId().equals(metricReq.getId())) {
                    throw new RuntimeException(String.format("该主题域下存在相同的指标名:%s 创建人:%s",
                            metricReq.getName(), metricResp.getCreatedBy()));
                }
            }
        }
    }

    private List<MetricResp> convertList(List<MetricDO> metricDOS) {
        return convertList(metricDOS, Lists.newArrayList());
    }

    private List<MetricResp> convertList(List<MetricDO> metricDOS, List<Long> collect) {
        List<MetricResp> metricResps = Lists.newArrayList();
        Map<Long, ModelResp> modelMap = modelService.getModelMap();
        if (!CollectionUtils.isEmpty(metricDOS)) {
            metricResps = metricDOS.stream()
                    .map(metricDO -> MetricConverter.convert2MetricResp(metricDO, modelMap, collect))
                    .collect(Collectors.toList());
        }
        return metricResps;
    }

    @Override
    public void sendMetricEventBatch(List<Long> modelIds, EventType eventType) {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setModelIds(modelIds);
        List<MetricDO> metricDOS = queryMetric(metricFilter);
        sendEventBatch(metricDOS, eventType);
    }

    private void sendEventBatch(List<MetricDO> metricDOS, EventType eventType) {
        List<DataItem> dataItems = metricDOS.stream().map(this::getDataItem)
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new DataEvent(this, dataItems, eventType));
    }

    private void sendEvent(DataItem dataItem, EventType eventType) {
        eventPublisher.publishEvent(new DataEvent(this,
                Lists.newArrayList(dataItem), eventType));
    }

    private DataItem getDataItem(MetricDO metricDO) {
        MetricResp metricResp = MetricConverter.convert2MetricResp(metricDO,
                new HashMap<>(), Lists.newArrayList());
        return DataItem.builder().id(metricDO.getId()).name(metricDO.getName())
                .bizName(metricDO.getBizName())
                .modelId(metricDO.getModelId()).type(TypeEnums.METRIC)
                .defaultAgg(metricResp.getDefaultAgg()).build();
    }

}
