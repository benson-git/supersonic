package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.enums.QueryType;
import com.tencent.supersonic.headless.api.request.BatchDownloadReq;
import com.tencent.supersonic.headless.api.request.DownloadStructReq;
import com.tencent.supersonic.headless.api.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.request.ItemUseReq;
import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.request.QueryItemReq;
import com.tencent.supersonic.headless.api.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.ExplainResp;
import com.tencent.supersonic.headless.api.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.api.response.ItemUseResp;
import com.tencent.supersonic.headless.api.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.response.SqlParserResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.service.DownloadService;
import com.tencent.supersonic.headless.server.service.SemantciQueryEngine;
import com.tencent.supersonic.headless.server.service.QueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class QueryController {

    @Autowired
    private QueryService queryService;
    @Autowired
    private SemantciQueryEngine semantciQueryEngine;

    @Autowired
    private DownloadService downloadService;

    @PostMapping("/sql")
    public Object queryBySql(@RequestBody QuerySqlReq querySQLReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryBySql(querySQLReq, user);
    }

    @PostMapping("/struct")
    public Object queryByStruct(@RequestBody QueryStructReq queryStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByStructWithAuth(queryStructReq, user);
    }

    @PostMapping("/queryMetricDataById")
    public ItemQueryResultResp queryMetricDataById(@Valid @RequestBody QueryItemReq queryApiReq,
            HttpServletRequest request) throws Exception {
        return queryService.queryMetricDataById(queryApiReq, request);
    }

    @PostMapping("/download/struct")
    public void downloadByStruct(@RequestBody DownloadStructReq downloadStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.downloadByStruct(downloadStructReq, user, response);
    }

    @PostMapping("/download/batch")
    public void downloadBatch(@RequestBody BatchDownloadReq batchDownloadReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.batchDownload(batchDownloadReq, user, response);
    }

    @PostMapping("/queryStatement")
    public SemanticQueryResp queryStatement(@RequestBody QueryStatement queryStatement) throws Exception {
        return queryService.queryByQueryStatement(queryStatement);
    }

    @PostMapping("/struct/parse")
    public SqlParserResp parseByStruct(@RequestBody ParseSqlReq parseSqlReq) throws Exception {
        QueryStructReq queryStructCmd = new QueryStructReq();
        Set<Long> models = new HashSet<>();
        models.add(Long.valueOf(parseSqlReq.getRootPath()));
        queryStructCmd.setModelIds(models);
        QueryStatement queryStatement = semantciQueryEngine.physicalSql(queryStructCmd, parseSqlReq);
        SqlParserResp sqlParserResp = new SqlParserResp();
        BeanUtils.copyProperties(queryStatement, sqlParserResp);
        return sqlParserResp;
    }

    /**
     * queryByMultiStruct
     */
    @PostMapping("/multiStruct")
    public Object queryByMultiStruct(@RequestBody QueryMultiStructReq queryMultiStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByMultiStruct(queryMultiStructReq, user);
    }

    /**
     * getStatInfo
     * query the used frequency of the metric/dimension
     *
     * @param itemUseReq
     */
    @PostMapping("/stat")
    public List<ItemUseResp> getStatInfo(@RequestBody ItemUseReq itemUseReq) {
        return queryService.getStatInfo(itemUseReq);
    }

    @PostMapping("/queryDimValue")
    public SemanticQueryResp queryDimValue(@RequestBody QueryDimValueReq queryDimValueReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryService.queryDimValue(queryDimValueReq, user);
    }

    @PostMapping("/explain")
    public <T> ExplainResp explain(@RequestBody ExplainSqlReq<T> explainSqlReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        User user = UserHolder.findUser(request, response);
        String queryReqJson = JsonUtil.toString(explainSqlReq.getQueryReq());
        QueryType queryTypeEnum = explainSqlReq.getQueryTypeEnum();

        if (QueryType.SQL.equals(queryTypeEnum)) {
            QuerySqlReq querySQLReq = JsonUtil.toObject(queryReqJson, QuerySqlReq.class);
            ExplainSqlReq<QuerySqlReq> explainSqlReqNew = ExplainSqlReq.<QuerySqlReq>builder()
                    .queryReq(querySQLReq)
                    .queryTypeEnum(queryTypeEnum).build();
            return queryService.explain(explainSqlReqNew, user);
        }
        if (QueryType.STRUCT.equals(queryTypeEnum)) {
            QueryStructReq queryStructReq = JsonUtil.toObject(queryReqJson, QueryStructReq.class);
            ExplainSqlReq<QueryStructReq> explainSqlReqNew = ExplainSqlReq.<QueryStructReq>builder()
                    .queryReq(queryStructReq)
                    .queryTypeEnum(queryTypeEnum).build();
            return queryService.explain(explainSqlReqNew, user);
        }
        return null;
    }

}
