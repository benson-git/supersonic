package com.tencent.supersonic.chat.server.listener;

import com.tencent.supersonic.headless.core.knowledge.DictWord;
import com.tencent.supersonic.chat.server.service.impl.SchemaService;
import com.tencent.supersonic.chat.server.service.impl.WordService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.tencent.supersonic.headless.server.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
public class ApplicationStartedListener implements CommandLineRunner {

    @Autowired
    private KnowledgeService knowledgeService;
    @Autowired
    private WordService wordService;
    @Autowired
    private SchemaService schemaService;

    @Override
    public void run(String... args) {
        updateKnowledgeDimValue();
    }

    public Boolean updateKnowledgeDimValue() {
        Boolean isOk = false;
        try {
            log.debug("ApplicationStartedInit start");

            List<DictWord> dictWords = wordService.getAllDictWords();
            wordService.setPreDictWords(dictWords);
            knowledgeService.reloadAllData(dictWords);

            log.debug("ApplicationStartedInit end");
            isOk = true;
        } catch (Exception e) {
            log.error("ApplicationStartedInit error", e);
        }
        return isOk;
    }

    public Boolean updateKnowledgeDimValueAsync() {
        CompletableFuture.supplyAsync(() -> {
            updateKnowledgeDimValue();
            return null;
        });
        return true;
    }

    /***
     * reload knowledge task
     */
    @Scheduled(cron = "${reload.knowledge.corn:0 0/1 * * * ?}")
    public void reloadKnowledge() {
        log.debug("reloadKnowledge start");

        try {
            List<DictWord> dictWords = wordService.getAllDictWords();
            List<DictWord> preDictWords = wordService.getPreDictWords();

            if (CollectionUtils.isEqualCollection(dictWords, preDictWords)) {
                log.debug("dictWords has not changed, reloadKnowledge end");
                return;
            }
            log.info("dictWords has changed");
            wordService.setPreDictWords(dictWords);
            knowledgeService.updateOnlineKnowledge(wordService.getAllDictWords());
            schemaService.getCache().refresh(SchemaService.ALL_CACHE);

        } catch (Exception e) {
            log.error("reloadKnowledge error", e);
        }

        log.debug("reloadKnowledge end");
    }
}
