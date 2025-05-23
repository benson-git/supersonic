import request from 'umi-request';
import moment from 'moment';
import { DatePeridMap } from '@/pages/SemanticModel/constant';

const getRunningEnv = () => {
  return window.location.pathname.includes('/chatSetting/') ? 'chat' : 'semantic';
};

export function getDomainList(): Promise<any> {
  if (getRunningEnv() === 'chat') {
    return request.get(`${process.env.CHAT_API_BASE_URL}conf/domainList`);
  }
  return request.get(`${process.env.API_BASE_URL}domain/getDomainList`);
}

export function getDomainDetail(data: any): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}domain/getDomain/${data.modelId}`);
}

export function createDomain(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}domain/createDomain`, {
    data,
  });
}

export function updateDomain(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}domain/updateDomain`, {
    data,
  });
}

export function createDatasource(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}datasource/createDatasource`, {
    data,
  });
}

export function updateDatasource(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}datasource/updateDatasource`, {
    data,
  });
}

export function getDimensionList(data: any): Promise<any> {
  const { domainId, modelId } = data;
  const queryParams = {
    data: {
      current: 1,
      pageSize: 999999,
      ...data,
      ...(domainId ? { domainIds: [domainId] } : {}),
      ...(modelId ? { modelIds: [modelId] } : {}),
    },
  };
  if (getRunningEnv() === 'chat') {
    return request.post(`${process.env.CHAT_API_BASE_URL}conf/dimension/page`, queryParams);
  }
  return request.post(`${process.env.API_BASE_URL}dimension/queryDimension`, queryParams);
}

export function getCommonDimensionList(domainId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}commonDimension/getList?domainId=${domainId}`);
}

export function saveCommonDimension(data: any): Promise<any> {
  if (data.id) {
    return request(`${process.env.API_BASE_URL}commonDimension`, {
      method: 'PUT',
      data,
    });
  }
  return request.post(`${process.env.API_BASE_URL}commonDimension`, {
    data,
  });
}

export function deleteCommonDimension(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}commonDimension/${id}`, {
    method: 'DELETE',
  });
}

export function getDimensionInModelCluster(modelId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}dimension/getDimensionInModelCluster/${modelId}`);
}

export function createDimension(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/createDimension`, {
    data,
  });
}

export function updateDimension(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/updateDimension`, {
    data,
  });
}

export function mockDimensionAlias(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/mockDimensionAlias`, {
    data,
  });
}

export function mockDimensionValuesAlias(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/mockDimensionValuesAlias`, {
    data,
  });
}

export function queryMetric(data: any): Promise<any> {
  const { domainId, modelId } = data;
  const queryParams = {
    data: {
      current: 1,
      pageSize: 999999,
      ...data,
      ...(domainId ? { domainIds: [domainId] } : {}),
      ...(modelId ? { modelIds: [modelId] } : {}),
    },
  };
  if (getRunningEnv() === 'chat') {
    return request.post(`${process.env.CHAT_API_BASE_URL}conf/metric/page`, queryParams);
  }
  return request.post(`${process.env.API_BASE_URL}metric/queryMetric`, queryParams);
}

export function createMetric(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/createMetric`, {
    data,
  });
}

export function updateMetric(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/updateMetric`, {
    data,
  });
}

export function batchUpdateMetricStatus(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/batchUpdateStatus`, {
    data,
  });
}

export function batchUpdateDimensionStatus(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}dimension/batchUpdateStatus`, {
    data,
  });
}

export async function batchDownloadMetric(data: any): Promise<any> {
  const response = await request.post(`${process.env.API_BASE_URL}query/download/batch`, {
    responseType: 'blob',
    getResponse: true,
    data,
  });

  downloadStruct(response.data);
}

export function mockMetricAlias(data: any): Promise<any> {
  return request.post(`${process.env.API_BASE_URL}metric/mockMetricAlias`, {
    data,
  });
}

export function getMetricTags(): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}metric/getMetricTags`);
}

export function getMetricData(metricId: string | number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}metric/getMetric/${metricId}`);
}

export function getDrillDownDimension(metricId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}metric/getDrillDownDimension`, {
    params: { metricId },
  });
}

export function getMeasureListByModelId(modelId: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}datasource/getMeasureListOfModel/${modelId}`);
}

export function deleteDatasource(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}datasource/deleteDatasource/${id}`, {
    method: 'DELETE',
  });
}

export function deleteDimension(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}dimension/deleteDimension/${id}`, {
    method: 'DELETE',
  });
}

export function deleteMetric(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}metric/deleteMetric/${id}`, {
    method: 'DELETE',
  });
}

export function deleteDomain(id: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}domain/deleteDomain/${id}`, {
    method: 'DELETE',
  });
}

export function getGroupAuthInfo(modelId: number): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}queryGroup`, {
    method: 'GET',
    params: { modelId },
  });
}

export function createGroupAuth(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}createGroup`, {
    method: 'POST',
    data,
  });
}

export function updateGroupAuth(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}updateGroup`, {
    method: 'POST',
    data,
  });
}

export function removeGroupAuth(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}removeGroup`, {
    method: 'POST',
    data,
  });
}

export function addDomainExtend(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf`, {
    method: 'POST',
    data,
  });
}

export function editDomainExtend(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf`, {
    method: 'PUT',
    data,
  });
}

export function getDomainExtendConfig(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf/search`, {
    method: 'POST',
    data,
  });
}

export function getDomainExtendDetailConfig(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}conf/richDesc/${data.modelId}`, {
    method: 'GET',
  });
}

export function getDatasourceRelaList(id?: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}datasource/getDatasourceRelaList/${id}`, {
    method: 'GET',
  });
}

export function createOrUpdateDatasourceRela(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/createOrUpdateDatasourceRela`, {
    method: 'POST',
    data,
  });
}

export function createOrUpdateModelRela(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}modelRela`, {
    method: data?.id ? 'PUT' : 'POST',
    data,
  });
}

export function deleteModelRela(id: any): Promise<any> {
  if (!id) {
    return;
  }
  return request(`${process.env.API_BASE_URL}modelRela/${id}`, {
    method: 'DELETE',
  });
}

export function getModelRelaList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}modelRela/list`, {
    method: 'GET',
    params: { domainId },
  });
}

export function createOrUpdateViewInfo(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/createOrUpdateViewInfo`, {
    method: 'POST',
    data,
  });
}

export function getViewInfoList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/getViewInfoList/${domainId}`, {
    method: 'GET',
  });
}

export function deleteViewInfo(recordId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/deleteViewInfo/${recordId}`, {
    method: 'DELETE',
  });
}

export function deleteDatasourceRela(domainId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/deleteDatasourceRela/${domainId}`, {
    method: 'DELETE',
  });
}

export function getDomainSchemaRela(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}viewInfo/getDomainSchemaRela/${domainId}`, {
    method: 'GET',
  });
}

export type SaveDatabaseParams = {
  domainId: number;
  name: string;
  type: string;
  host: string;
  port: string;
  username: string;
  password: string;
  database?: string;
  description?: string;
};

export function getDatabaseList(): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getDatabaseList`, {
    method: 'GET',
  });
}

export function deleteDatabase(domainId: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/${domainId}`, {
    method: 'DELETE',
  });
}

export function saveDatabase(data: SaveDatabaseParams): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/createOrUpdateDatabase`, {
    method: 'POST',
    data,
  });
}

export function testDatabaseConnect(data: SaveDatabaseParams): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/testConnect`, {
    method: 'POST',
    data,
  });
}

type ExcuteSqlParams = {
  sql: string;
  id: number;
};

// 执行脚本
export async function excuteSql(params: ExcuteSqlParams) {
  const data = { ...params };
  return request.post(`${process.env.API_BASE_URL}database/executeSql`, { data });
}

export function getDbNames(dbId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getDbNames/${dbId}`, {
    method: 'GET',
  });
}

export function getTables(dbId: number, dbName: string): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getTables/${dbId}/${dbName}`, {
    method: 'GET',
  });
}

export function getColumns(dbId: number, dbName: string, tableName: string): Promise<any> {
  return request(`${process.env.API_BASE_URL}database/getColumns/${dbId}/${dbName}/${tableName}`, {
    method: 'GET',
  });
}

export function getModelList(domainId: number): Promise<any> {
  if (getRunningEnv() === 'chat') {
    return request(`${process.env.CHAT_API_BASE_URL}conf/modelList/${domainId}`, {
      method: 'GET',
    });
  }
  return request(`${process.env.API_BASE_URL}model/getModelList/${domainId}`, {
    method: 'GET',
  });
}

export function createModel(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/createModel`, {
    method: 'POST',
    data,
  });
}
export function updateModel(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/updateModel`, {
    method: 'POST',
    data,
  });
}

export function deleteModel(modelId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/deleteModel/${modelId}`, {
    method: 'DELETE',
  });
}

export function getUnAvailableItem(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}model/getUnAvailableItem`, {
    method: 'POST',
    data,
  });
}

export function getModelDetail(data: any): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}model/getModel/${data.modelId}`);
}

export function getMetricsToCreateNewMetric(data: any): Promise<any> {
  return request.get(
    `${process.env.API_BASE_URL}metric/getMetricsToCreateNewMetric/${data.modelId}`,
  );
}

export function createDictTask(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}dict/task`, {
    method: 'POST',
    data,
  });
}

export function deleteDictTask(data: any): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}dict/task/delete`, {
    method: 'POST',
    data,
  });
}

export function searchDictLatestTaskList(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/task/search`, {
    method: 'POST',
    data,
  });
}

export function searchKnowledgeConfigQuery(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}knowledge/conf/query`, {
    method: 'POST',
    data,
  });
}

const downloadStruct = (blob: Blob) => {
  const fieldName = `supersonic_${moment().format('YYYYMMDDhhmmss')}.xlsx`;
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob([blob]));
  link.download = fieldName;
  document.body.appendChild(link);
  link.click();
  URL.revokeObjectURL(link.href);
  document.body.removeChild(link);
};

export function queryDimValue(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}query/queryDimValue`, {
    method: 'POST',
    data,
  });
}

export async function queryStruct({
  modelIds,
  bizName,
  dateField = 'sys_imp_date',
  startDate,
  endDate,
  download = false,
  groups = [],
  dimensionFilters = [],
  isTransform,
}: {
  modelIds: number[];
  bizName: string;
  dateField: string;
  startDate: string;
  endDate: string;
  download?: boolean;
  groups?: string[];
  dimensionFilters?: string[];
  isTransform: boolean;
}): Promise<any> {
  const response = await request(
    `${process.env.API_BASE_URL}query/${download ? 'download/' : ''}struct`,
    {
      method: 'POST',
      ...(download ? { responseType: 'blob', getResponse: true } : {}),
      data: {
        modelIds,
        groups: [dateField, ...groups],
        dimensionFilters,
        isTransform,
        aggregators: [
          {
            column: bizName,
            // func: 'SUM',
            nameCh: 'null',
            args: null,
          },
        ],
        orders: [{ column: dateField, direction: 'desc' }],
        metricFilters: [],
        params: [],
        dateInfo: {
          dateMode: 'BETWEEN',
          startDate,
          endDate,
          dateList: [],
          unit: 7,
          // period: 'DAY',
          period: DatePeridMap[dateField],
          text: 'null',
        },
        limit: 2000,
        nativeQuery: false,
      },
    },
  );
  if (download) {
    downloadStruct(response.data);
  } else {
    return response;
  }
}

export function metricStarState(data: { id: number; state: boolean }): Promise<any> {
  const { id, state } = data;
  if (state) {
    return request(`${process.env.API_BASE_URL}collect/createCollectionIndicators`, {
      method: 'POST',
      data: { id },
    });
  } else {
    return request(`${process.env.API_BASE_URL}collect/deleteCollectionIndicators/${id}`, {
      method: 'DELETE',
    });
  }
}

export function getDatabaseParameters(): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}database/getDatabaseParameters`);
}

export function getDatabaseDetail(id: number): Promise<any> {
  return request.get(`${process.env.API_BASE_URL}database/${id}`);
}

export function getViewList(domainId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}view/getViewList`, {
    method: 'GET',
    params: { domainId },
  });
}

export function createView(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}view`, {
    method: 'POST',
    data,
  });
}
export function updateView(data: any): Promise<any> {
  return request(`${process.env.API_BASE_URL}view`, {
    method: 'PUT',
    data,
  });
}

export function deleteView(viewId: number): Promise<any> {
  return request(`${process.env.API_BASE_URL}view/${viewId}`, {
    method: 'DELETE',
  });
}
