package com.tencent.supersonic;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.DefaultDisplayInfo;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.api.pojo.ViewDetail;
import com.tencent.supersonic.headless.api.pojo.ViewModelConfig;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.MetricTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.request.ViewReq;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.ViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class BenchMarkDemoDataLoader {

    private User user = User.getFakeUser();

    @Autowired
    private DomainService domainService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelRelaService modelRelaService;

    @Autowired
    private ViewService viewService;

    public void doRun() {
        try {
            addDomain();
            addModel_1();
            addModel_2();
            addModel_3();
            addModel_4();
            addView_1();
            addModelRela_1();
            addModelRela_2();
            addModelRela_3();
            addModelRela_4();
            addModelRela_5();
        } catch (Exception e) {
            log.error("Failed to add bench mark demo data", e);
        }

    }

    public void addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("测评数据_音乐");
        domainReq.setBizName("music");
        domainReq.setParentId(0L);
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Collections.singletonList("admin"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainService.createDomain(domainReq, user);
    }

    public void addModel_1() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setDomainId(3L);
        modelReq.setName("艺术类型");
        modelReq.setBizName("genre");
        modelReq.setDescription("艺术类型");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(3L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        dimensions.add(new Dim("活跃区域", "most_popular_in", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("音乐类型名称", "g_name", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("音乐类型名称", IdentifyType.primary.name(), "g_name"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        Measure measure = new Measure("评分", "rating", AggOperatorEnum.SUM.name(), 0);
        measures.add(measure);
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT g_name, rating, most_popular_in FROM genre");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void addModel_2() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setDomainId(3L);
        modelReq.setName("艺术家");
        modelReq.setBizName("artist");
        modelReq.setDescription("艺术家");
        modelReq.setDatabaseId(1L);
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        dimensions.add(new Dim("艺术家名称", "artist_name", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("国籍", "country", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("性别", "gender", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("艺术家名称", IdentifyType.primary.name(), "artist_name"));
        identifiers.add(new Identify("音乐类型名称", IdentifyType.foreign.name(), "g_name"));
        modelDetail.setIdentifiers(identifiers);

        modelDetail.setMeasures(Collections.emptyList());

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT artist_name, country, gender, g_name FROM artist");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void addModel_3() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setDomainId(3L);
        modelReq.setName("文件");
        modelReq.setBizName("files");
        modelReq.setDescription("文件");
        modelReq.setDatabaseId(1L);
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        dimensions.add(new Dim("持续时间", "duration", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("文件格式", "formats", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("艺术家名称", "artist_name", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("歌曲ID", IdentifyType.primary.name(), "f_id"));
        identifiers.add(new Identify("艺术家名称", IdentifyType.foreign.name(), "artist_name"));
        modelDetail.setIdentifiers(identifiers);

        modelDetail.setMeasures(Collections.emptyList());

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT f_id, artist_name, file_size, duration, formats FROM files");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void addModel_4() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setDomainId(3L);
        modelReq.setName("歌曲");
        modelReq.setBizName("song");
        modelReq.setDescription("歌曲");
        modelReq.setDatabaseId(1L);
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        dimensions.add(new Dim("歌曲名称", "song_name", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("国家", "country", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("语种", "languages", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("发行时间", "releasedate", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("歌曲名称", IdentifyType.primary.name(), "song_name"));
        identifiers.add(new Identify("歌曲ID", IdentifyType.foreign.name(), "f_id"));
        identifiers.add(new Identify("艺术家名称", IdentifyType.foreign.name(), "artist_name"));
        identifiers.add(new Identify("艺术家名称", IdentifyType.foreign.name(), "artist_name"));

        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("分辨率", "resolution", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("评分", "rating", AggOperatorEnum.SUM.name(), 1));
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT imp_date, song_name, artist_name, country, f_id, g_name, "
                + " rating, languages, releasedate, resolution FROM song");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void addView_1() {
        ViewReq viewReq = new ViewReq();
        viewReq.setName("cspider");
        viewReq.setBizName("singer");
        viewReq.setDomainId(3L);
        viewReq.setDescription("包含cspider数据集相关标签和指标信息");
        viewReq.setAdmins(Lists.newArrayList("admin"));
        List<ViewModelConfig> viewModelConfigs = Lists.newArrayList(
                new ViewModelConfig(5L, Lists.newArrayList(8L), Lists.newArrayList()),
                new ViewModelConfig(6L, Lists.newArrayList(9L, 10L), Lists.newArrayList()),
                new ViewModelConfig(7L, Lists.newArrayList(11L, 12L), Lists.newArrayList()),
                new ViewModelConfig(8L, Lists.newArrayList(13L, 14L), Lists.newArrayList(8L, 9L))
        );
        ViewDetail viewDetail = new ViewDetail();
        viewDetail.setViewModelConfigs(viewModelConfigs);
        viewReq.setViewDetail(viewDetail);
        viewReq.setTypeEnum(TypeEnums.VIEW);
        QueryConfig queryConfig = new QueryConfig();
        TagTypeDefaultConfig tagTypeDefaultConfig = new TagTypeDefaultConfig();
        TimeDefaultConfig tagTimeDefaultConfig = new TimeDefaultConfig();
        tagTimeDefaultConfig.setTimeMode(TimeMode.LAST);
        tagTimeDefaultConfig.setUnit(7);
        tagTypeDefaultConfig.setTimeDefaultConfig(tagTimeDefaultConfig);
        DefaultDisplayInfo defaultDisplayInfo = new DefaultDisplayInfo();
        defaultDisplayInfo.setDimensionIds(Lists.newArrayList());
        defaultDisplayInfo.setMetricIds(Lists.newArrayList());
        tagTypeDefaultConfig.setDefaultDisplayInfo(defaultDisplayInfo);
        MetricTypeDefaultConfig metricTypeDefaultConfig = new MetricTypeDefaultConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        timeDefaultConfig.setUnit(7);
        metricTypeDefaultConfig.setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.setTagTypeDefaultConfig(tagTypeDefaultConfig);
        queryConfig.setMetricTypeDefaultConfig(metricTypeDefaultConfig);
        viewReq.setQueryConfig(queryConfig);
        viewService.save(viewReq, User.getFakeUser());
    }

    public void addModelRela_1() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("g_name", "g_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(3L);
        modelRelaReq.setFromModelId(6L);
        modelRelaReq.setToModelId(5L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_2() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("artist_name", "artist_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(3L);
        modelRelaReq.setFromModelId(7L);
        modelRelaReq.setToModelId(6L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_3() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("artist_name", "artist_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(3L);
        modelRelaReq.setFromModelId(8L);
        modelRelaReq.setToModelId(6L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_4() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("g_name", "g_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(3L);
        modelRelaReq.setFromModelId(8L);
        modelRelaReq.setToModelId(5L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_5() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("f_id", "f_id", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(3L);
        modelRelaReq.setFromModelId(8L);
        modelRelaReq.setToModelId(7L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }
}
