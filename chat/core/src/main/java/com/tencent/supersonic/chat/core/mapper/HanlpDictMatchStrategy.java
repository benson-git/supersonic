package com.tencent.supersonic.chat.core.mapper;

import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.server.service.KnowledgeService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * HanlpDictMatchStrategy uses <a href="https://www.hanlp.com/">HanLP</a> to
 * match schema elements. It currently supports prefix and suffix matching
 * against names, values and aliases.
 */
@Service
@Slf4j
public class HanlpDictMatchStrategy extends BaseMatchStrategy<HanlpMapResult> {

    @Autowired
    private MapperHelper mapperHelper;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Autowired
    private KnowledgeService knowledgeService;

    @Override
    public Map<MatchText, List<HanlpMapResult>> match(QueryContext queryContext, List<S2Term> terms,
            Set<Long> detectViewIds) {
        String text = queryContext.getQueryText();
        if (Objects.isNull(terms) || StringUtils.isEmpty(text)) {
            return null;
        }

        log.debug("retryCount:{},terms:{},,detectModelIds:{}", terms, detectViewIds);

        List<HanlpMapResult> detects = detect(queryContext, terms, detectViewIds);
        Map<MatchText, List<HanlpMapResult>> result = new HashMap<>();

        result.put(MatchText.builder().regText(text).detectSegment(text).build(), detects);
        return result;
    }

    @Override
    public boolean needDelete(HanlpMapResult oneRoundResult, HanlpMapResult existResult) {
        return getMapKey(oneRoundResult).equals(getMapKey(existResult))
                && existResult.getDetectWord().length() < oneRoundResult.getDetectWord().length();
    }

    public void detectByStep(QueryContext queryContext, Set<HanlpMapResult> existResults, Set<Long> detectViewIds,
            String detectSegment, int offset) {
        // step1. pre search
        Integer oneDetectionMaxSize = optimizationConfig.getOneDetectionMaxSize();
        LinkedHashSet<HanlpMapResult> hanlpMapResults = knowledgeService.prefixSearch(detectSegment,
                oneDetectionMaxSize, detectViewIds).stream().collect(Collectors.toCollection(LinkedHashSet::new));
        // step2. suffix search
        LinkedHashSet<HanlpMapResult> suffixHanlpMapResults = knowledgeService.suffixSearch(detectSegment,
                oneDetectionMaxSize, detectViewIds).stream().collect(Collectors.toCollection(LinkedHashSet::new));

        hanlpMapResults.addAll(suffixHanlpMapResults);

        if (CollectionUtils.isEmpty(hanlpMapResults)) {
            return;
        }
        // step3. merge pre/suffix result
        hanlpMapResults = hanlpMapResults.stream().sorted((a, b) -> -(b.getName().length() - a.getName().length()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // step4. filter by similarity
        hanlpMapResults = hanlpMapResults.stream()
                .filter(term -> mapperHelper.getSimilarity(detectSegment, term.getName())
                        >= mapperHelper.getThresholdMatch(term.getNatures()))
                .filter(term -> CollectionUtils.isNotEmpty(term.getNatures()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("after isSimilarity parseResults:{}", hanlpMapResults);

        hanlpMapResults = hanlpMapResults.stream().map(parseResult -> {
            parseResult.setOffset(offset);
            parseResult.setSimilarity(mapperHelper.getSimilarity(detectSegment, parseResult.getName()));
            return parseResult;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        // step5. take only one dimension or 10 metric/dimension value per rond.
        List<HanlpMapResult> dimensionMetrics = hanlpMapResults.stream()
                .filter(entry -> mapperHelper.existDimensionValues(entry.getNatures()))
                .collect(Collectors.toList())
                .stream()
                .limit(1)
                .collect(Collectors.toList());

        Integer oneDetectionSize = optimizationConfig.getOneDetectionSize();
        List<HanlpMapResult> oneRoundResults = hanlpMapResults.stream().limit(oneDetectionSize)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(dimensionMetrics)) {
            oneRoundResults = dimensionMetrics;
        }
        // step6. select mapResul in one round
        selectResultInOneRound(existResults, oneRoundResults);
    }

    public String getMapKey(HanlpMapResult a) {
        return a.getName() + Constants.UNDERLINE + String.join(Constants.UNDERLINE, a.getNatures());
    }
}
