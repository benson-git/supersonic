package com.tencent.supersonic.chat.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.DAY;
import static com.tencent.supersonic.common.constant.Constants.UNDERLINE;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.application.ConfigServiceImpl;
import com.tencent.supersonic.chat.domain.pojo.config.*;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.chat.domain.dataobject.DimValueDO;
import com.tencent.supersonic.knowledge.domain.pojo.DictUpdateMode;
import com.tencent.supersonic.knowledge.domain.pojo.DimValue2DictCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DictMetaUtils {

    @Autowired
    private ConfigServiceImpl configService;

    @Value("${model.internal.metric.suffix:internal_cnt}")
    private String internalMetricNameSuffix;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public DictMetaUtils() {

    }


    public List<DimValueDO> generateDimValueInfo(DimValue2DictCommand dimValue2DictCommend) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        DictUpdateMode updateMode = dimValue2DictCommend.getUpdateMode();
        Set<Long> domainIds = new HashSet<>();
        switch (updateMode) {
            case OFFLINE_DOMAIN:
                domainIds.addAll(dimValue2DictCommend.getDomainIds());
                dimValueDOList = generateDimValueInfoByDomain(domainIds);
                break;
            case OFFLINE_FULL:
                List<DomainSchemaResp> domainSchemaDescList = semanticLayer.getDomainSchemaInfo(new ArrayList<>());
                if (CollectionUtils.isEmpty(domainSchemaDescList)) {
                    break;
                }

                Map<Long, DomainSchemaResp> domainIdAndDescPair = domainSchemaDescList.stream()
                        .collect(Collectors.toMap(DomainSchemaResp::getId, domainSchemaDesc -> domainSchemaDesc));
                if (!CollectionUtils.isEmpty(domainIdAndDescPair)) {
                    domainIds.addAll(domainIdAndDescPair.keySet());
                    dimValueDOList = generateDimValueInfoByDomain(domainIds);
                    break;
                }
                break;
            case REALTIME_ADD:
                dimValueDOList = generateDimValueInfoByDomainAndDim(dimValue2DictCommend.getDomainAndDimPair());
                break;
            case NOT_SUPPORT:
                throw new RuntimeException("illegal parameter for updateMode");
            default:
                break;
        }

        return dimValueDOList;
    }

    private List<DimValueDO> generateDimValueInfoByDomainAndDim(Map<Long, List<Long>> domainAndDimMap) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(domainAndDimMap)) {
            return dimValueDOList;
        }

        List<DomainSchemaResp> domainSchemaDescList = semanticLayer.getDomainSchemaInfo(new ArrayList<>());
        if (CollectionUtils.isEmpty(domainSchemaDescList)) {
            return dimValueDOList;
        }
        Map<Long, DomainSchemaResp> domainIdAndDescPair = domainSchemaDescList.stream()
                .collect(Collectors.toMap(DomainSchemaResp::getId, domainSchemaDesc -> domainSchemaDesc));

        for (Long domainId : domainAndDimMap.keySet()) {
            if (!domainIdAndDescPair.containsKey(domainId)) {
                continue;
            }
            Map<Long, DimSchemaResp> dimIdAndDescPairAll;
            dimIdAndDescPairAll = domainIdAndDescPair.get(domainId).getDimensions().stream()
                    .collect(Collectors.toMap(DimSchemaResp::getId, dimSchemaDesc -> dimSchemaDesc));
            List<Long> dimIdReq = domainAndDimMap.get(domainId);
            Map<Long, DimSchemaResp> dimIdAndDescPairReq = new HashMap<>();
            for (Long dimId : dimIdReq) {
                if (dimIdAndDescPairAll.containsKey(dimId)) {
                    dimIdAndDescPairReq.put(dimId, dimIdAndDescPairAll.get(dimId));
                }
            }
            fillDimValueDOList(dimValueDOList, domainId, dimIdAndDescPairReq);
        }

        return dimValueDOList;
    }

    private List<DimValueDO> generateDimValueInfoByDomain(Set<Long> domainIds) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        List<DomainSchemaResp> domainSchemaDescList = semanticLayer.getDomainSchemaInfo(new ArrayList<>(domainIds));
        if (CollectionUtils.isEmpty(domainSchemaDescList)) {
            return dimValueDOList;
        }

        domainSchemaDescList.forEach(domainSchemaDesc -> {
            Map<Long, DimSchemaResp> dimIdAndDescPair = domainSchemaDesc.getDimensions().stream()
                    .collect(Collectors.toMap(DimSchemaResp::getId, dimSchemaDesc -> dimSchemaDesc));
            fillDimValueDOList(dimValueDOList, domainSchemaDesc.getId(), dimIdAndDescPair);

        });

        return dimValueDOList;
    }

    private void fillDimValueDOList(List<DimValueDO> dimValueDOList, Long domainId,
                                    Map<Long, DimSchemaResp> dimIdAndDescPair) {
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(domainId);
        if (Objects.nonNull(chaConfigRichDesc) && Objects.nonNull(chaConfigRichDesc.getChatAggRichConfig())) {

            ChatDefaultRichConfig chatDefaultConfig = chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig();
            List<KnowledgeInfo> knowledgeAggInfo = chaConfigRichDesc.getChatAggRichConfig().getKnowledgeInfos();

            List<KnowledgeInfo> knowledgeDetailInfo = chaConfigRichDesc.getChatDetailRichConfig().getKnowledgeInfos();

            fillKnowledgeDimValue(knowledgeDetailInfo, chatDefaultConfig, dimValueDOList, dimIdAndDescPair, domainId);
            fillKnowledgeDimValue(knowledgeAggInfo, chatDefaultConfig, dimValueDOList, dimIdAndDescPair, domainId);


        }
    }

    private void fillKnowledgeDimValue(List<KnowledgeInfo> knowledgeInfos, ChatDefaultRichConfig chatDefaultConfig,
                                       List<DimValueDO> dimValueDOList, Map<Long, DimSchemaResp> dimIdAndDescPair, Long domainId) {
        if (!CollectionUtils.isEmpty(knowledgeInfos)) {
            List<Dim4Dict> dimensions = new ArrayList<>();
            List<DefaultMetric> defaultMetricDescList = new ArrayList<>();
            knowledgeInfos.stream()
                    .filter(knowledgeInfo -> knowledgeInfo.getSearchEnable() && !CollectionUtils.isEmpty(dimIdAndDescPair)
                            && dimIdAndDescPair.containsKey(knowledgeInfo.getItemId()))
                    .forEach(knowledgeInfo -> {
                        if (dimIdAndDescPair.containsKey(knowledgeInfo.getItemId())) {
                            DimSchemaResp dimensionDesc = dimIdAndDescPair.get(knowledgeInfo.getItemId());

                            //default cnt
                            if (Objects.isNull(chatDefaultConfig) || CollectionUtils.isEmpty(chatDefaultConfig.getMetrics())) {
                                String datasourceBizName = dimensionDesc.getDatasourceBizName();
                                if (Strings.isNotEmpty(datasourceBizName)) {
                                    String internalMetricName =
                                            datasourceBizName + UNDERLINE + internalMetricNameSuffix;
                                    defaultMetricDescList.add(new DefaultMetric(internalMetricName, 2, DAY));
                                }
                            } else {
                                SchemaItem schemaItem = chatDefaultConfig.getMetrics().get(0);
                                defaultMetricDescList.add(new DefaultMetric(schemaItem.getBizName(), chatDefaultConfig.getUnit(), chatDefaultConfig.getPeriod()));

                            }

                            String bizName = dimensionDesc.getBizName();
                            Dim4Dict dim4Dict = new Dim4Dict();
                            dim4Dict.setDimId(knowledgeInfo.getItemId());
                            dim4Dict.setBizName(bizName);
                            if(Objects.nonNull(knowledgeInfo.getKnowledgeAdvancedConfig())){
                                KnowledgeAdvancedConfig knowledgeAdvancedConfig = knowledgeInfo.getKnowledgeAdvancedConfig();
                                BeanUtils.copyProperties(knowledgeAdvancedConfig, dim4Dict);
                            }
                            dimensions.add(dim4Dict);
                        }
                    });

            if (!CollectionUtils.isEmpty(dimensions)) {
                DimValueDO dimValueDO = new DimValueDO()
                        .setDomainId(domainId)
                        .setDefaultMetricIds(defaultMetricDescList)
                        .setDimensions(dimensions);
                dimValueDOList.add(dimValueDO);
            }
        }
    }

    private boolean isVisibleDim(KnowledgeInfo knowledgeInfo, ItemVisibilityInfo itemVisibilityDesc) {
        if (Objects.isNull(itemVisibilityDesc) || CollectionUtils.isEmpty(itemVisibilityDesc.getBlackDimIdList())) {
            return true;
        }
        return !itemVisibilityDesc.getBlackDimIdList().contains(knowledgeInfo.getItemId());
    }
}
