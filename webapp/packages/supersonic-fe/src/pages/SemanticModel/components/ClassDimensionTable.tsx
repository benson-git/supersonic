import type { ActionType, ProColumns } from '@ant-design/pro-table';
import ProTable from '@ant-design/pro-table';
import { message, Button, Space, Popconfirm, Input, Tag, Select } from 'antd';
import React, { useRef, useState, useEffect } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../model';
import { StatusEnum } from '../enum';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_OPTIONS } from '../constant';
import {
  getModelList,
  getDimensionList,
  deleteDimension,
  batchUpdateDimensionStatus,
} from '../service';
import DimensionInfoModal from './DimensionInfoModal';
import DimensionValueSettingModal from './DimensionValueSettingModal';
import { ISemantic, IDataSource } from '../data';
import TableHeaderFilter from './TableHeaderFilter';
import moment from 'moment';
import BatchCtrlDropDownButton from '@/components/BatchCtrlDropDownButton';
import { ColumnsConfig } from './MetricTableColumnRender';
import styles from './style.less';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const ClassDimensionTable: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectModelId: modelId, selectDomainId: domainId } = domainManger;
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dimensionItem, setDimensionItem] = useState<ISemantic.IDimensionItem>();
  const [dataSourceList, setDataSourceList] = useState<IDataSource.IDataSourceItem[]>([]);
  const [tableData, setTableData] = useState<ISemantic.IMetricItem[]>([]);
  const [filterParams, setFilterParams] = useState<Record<string, any>>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [dimensionValueSettingList, setDimensionValueSettingList] = useState<
    ISemantic.IDimensionValueSettingItem[]
  >([]);
  const [dimensionValueSettingModalVisible, setDimensionValueSettingModalVisible] =
    useState<boolean>(false);

  const defaultPagination = {
    current: 1,
    pageSize: 20,
    total: 0,
  };
  const [pagination, setPagination] = useState(defaultPagination);

  const actionRef = useRef<ActionType>();

  const queryDimensionList = async (params: any) => {
    setLoading(true);
    const { code, data, msg } = await getDimensionList({
      ...pagination,
      ...params,
      modelId,
    });
    setLoading(false);
    const { list, pageSize, pageNum, total } = data || {};
    if (code === 200) {
      setPagination({
        ...pagination,
        pageSize: Math.min(pageSize, 100),
        current: pageNum,
        total,
      });
      setTableData(list);
    } else {
      message.error(msg);
      setTableData([]);
    }
  };

  const queryDataSourceList = async () => {
    const { code, data, msg } = await getModelList(domainId);
    if (code === 200) {
      setDataSourceList(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDimensionList({ ...filterParams, ...defaultPagination });
  }, [filterParams]);

  useEffect(() => {
    queryDataSourceList();
  }, [modelId]);

  const queryBatchUpdateStatus = async (ids: React.Key[], status: StatusEnum) => {
    if (Array.isArray(ids) && ids.length === 0) {
      return;
    }
    setLoading(true);
    const { code, msg } = await batchUpdateDimensionStatus({
      ids,
      status,
    });
    setLoading(false);
    if (code === 200) {
      queryDimensionList({ ...filterParams, ...defaultPagination });
      dispatch({
        type: 'domainManger/queryDimensionList',
        payload: {
          modelId,
        },
      });
      return;
    }
    message.error(msg);
  };

  const columns: ProColumns[] = [
    {
      dataIndex: 'id',
      title: 'ID',
      fixed: 'left',
      width: 80,
      order: 100,
      search: false,
    },
    {
      dataIndex: 'key',
      title: '维度搜索',
      hideInTable: true,
    },
    {
      dataIndex: 'name',
      title: '维度',
      fixed: 'left',
      width: 280,
      render: ColumnsConfig.dimensionInfo.render,
      search: false,
    },
    {
      dataIndex: 'sensitiveLevel',
      title: '敏感度',
      width: 150,
      valueEnum: SENSITIVE_LEVEL_ENUM,
      render: ColumnsConfig.sensitiveLevel.render,
    },
    {
      dataIndex: 'isTag',
      title: '是否为标签',
      width: 120,
      render: (isTag) => {
        switch (isTag) {
          case 0:
            return '否';
          case 1:
            return <span style={{ color: '#1677ff' }}>是</span>;
          default:
            return <Tag color="default">未知</Tag>;
        }
      },
    },
    {
      dataIndex: 'status',
      title: '状态',
      width: 150,
      search: false,
      render: ColumnsConfig.state.render,
    },
    {
      dataIndex: 'createdBy',
      title: '创建人',
      width: 180,
      search: false,
    },

    {
      dataIndex: 'description',
      title: '描述',
      search: false,
      render: ColumnsConfig.description.render,
    },

    {
      dataIndex: 'updatedAt',
      title: '更新时间',
      width: 180,
      search: false,
      render: (value: any) => {
        return value && value !== '-' ? moment(value).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },

    {
      title: '操作',
      dataIndex: 'x',
      valueType: 'option',
      width: 250,
      render: (_, record) => {
        return (
          <Space className={styles.ctrlBtnContainer}>
            <Button
              key="dimensionEditBtn"
              type="link"
              onClick={() => {
                setDimensionItem(record);
                setCreateModalVisible(true);
              }}
            >
              编辑
            </Button>
            <Button
              key="dimensionValueEditBtn"
              type="link"
              onClick={() => {
                setDimensionItem(record);
                setDimensionValueSettingModalVisible(true);
                if (Array.isArray(record.dimValueMaps)) {
                  setDimensionValueSettingList(record.dimValueMaps);
                } else {
                  setDimensionValueSettingList([]);
                }
              }}
            >
              维度值设置
            </Button>
            {record.status === StatusEnum.ONLINE ? (
              <Button
                type="link"
                key="editStatusOfflineBtn"
                onClick={() => {
                  queryBatchUpdateStatus([record.id], StatusEnum.OFFLINE);
                }}
              >
                停用
              </Button>
            ) : (
              <Button
                type="link"
                key="editStatusOnlineBtn"
                onClick={() => {
                  queryBatchUpdateStatus([record.id], StatusEnum.ONLINE);
                }}
              >
                启用
              </Button>
            )}
            <Popconfirm
              title="确认删除？"
              okText="是"
              cancelText="否"
              placement="left"
              onConfirm={async () => {
                const { code, msg } = await deleteDimension(record.id);
                if (code === 200) {
                  setDimensionItem(undefined);
                  queryDimensionList({ ...filterParams, ...defaultPagination });
                } else {
                  message.error(msg);
                }
              }}
            >
              <Button
                type="link"
                key="dimensionDeleteEditBtn"
                onClick={() => {
                  setDimensionItem(record);
                }}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  const rowSelection = {
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(selectedRowKeys);
    },
  };

  const onMenuClick = (key: string) => {
    switch (key) {
      case 'batchStart':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.ONLINE);
        break;
      case 'batchStop':
        queryBatchUpdateStatus(selectedRowKeys, StatusEnum.OFFLINE);
        break;
      default:
        break;
    }
  };

  return (
    <>
      <ProTable
        className={`${styles.classTable} ${styles.classTableSelectColumnAlignLeft}  ${styles.disabledSearchTable}`}
        actionRef={actionRef}
        rowKey="id"
        columns={columns}
        loading={loading}
        headerTitle={
          <TableHeaderFilter
            components={[
              {
                label: '维度搜索',
                component: (
                  <Input.Search
                    style={{ width: 280 }}
                    placeholder="请输入ID/维度名称/英文名称"
                    onSearch={(value) => {
                      setFilterParams((preState) => {
                        return {
                          ...preState,
                          key: value,
                        };
                      });
                    }}
                  />
                ),
              },
              {
                label: '敏感度',
                component: (
                  <Select
                    style={{ width: 140 }}
                    options={SENSITIVE_LEVEL_OPTIONS}
                    placeholder="请选择敏感度"
                    allowClear
                    onChange={(value) => {
                      setFilterParams((preState) => {
                        return {
                          ...preState,
                          sensitiveLevel: value,
                        };
                      });
                    }}
                  />
                ),
              },
              {
                label: '标签状态',
                component: (
                  <Select
                    style={{ width: 145 }}
                    placeholder="请选择标签状态"
                    allowClear
                    onChange={(value) => {
                      setFilterParams((preState) => {
                        return {
                          ...preState,
                          isTag: value,
                        };
                      });
                    }}
                    options={[
                      { value: 1, label: '是' },
                      { value: 0, label: '否' },
                    ]}
                  />
                ),
              },
            ]}
          />
        }
        search={false}
        // search={{
        //   optionRender: false,
        //   collapsed: false,
        // }}
        rowSelection={{
          type: 'checkbox',
          ...rowSelection,
        }}
        dataSource={tableData}
        pagination={pagination}
        tableAlertRender={() => {
          return false;
        }}
        size="large"
        scroll={{ x: 1500 }}
        options={{ reload: false, density: false, fullScreen: false }}
        onChange={(data: any) => {
          const { current, pageSize, total } = data;
          const currentPagin = {
            current,
            pageSize,
            total,
          };
          setPagination(currentPagin);
          queryDimensionList({ ...filterParams, ...currentPagin });
        }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            onClick={() => {
              setDimensionItem(undefined);
              setCreateModalVisible(true);
            }}
          >
            创建维度
          </Button>,
          <BatchCtrlDropDownButton
            key="ctrlBtnList"
            onDeleteConfirm={() => {
              queryBatchUpdateStatus(selectedRowKeys, StatusEnum.DELETED);
            }}
            hiddenList={['batchDownload']}
            onMenuClick={onMenuClick}
          />,
        ]}
      />

      {createModalVisible && (
        <DimensionInfoModal
          modelId={modelId}
          domainId={domainId}
          bindModalVisible={createModalVisible}
          dimensionItem={dimensionItem}
          dataSourceList={dataSourceList}
          onSubmit={() => {
            setCreateModalVisible(false);
            queryDimensionList({ ...filterParams, ...defaultPagination });
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
              },
            });
            return;
          }}
          onCancel={() => {
            setCreateModalVisible(false);
          }}
        />
      )}
      {dimensionValueSettingModalVisible && (
        <DimensionValueSettingModal
          dimensionValueSettingList={dimensionValueSettingList}
          open={dimensionValueSettingModalVisible}
          dimensionItem={dimensionItem}
          onCancel={() => {
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
              },
            });
            setDimensionValueSettingModalVisible(false);
          }}
          onSubmit={() => {
            queryDimensionList({ ...filterParams, ...defaultPagination });
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
              },
            });
            setDimensionValueSettingModalVisible(false);
          }}
        />
      )}
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ClassDimensionTable);
