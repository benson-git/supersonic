import React, { ReactNode } from 'react';
import { AGG_TYPE_MAP, PREFIX_CLS } from '../../common/constants';
import { ChatContextType } from '../../common/type';
import Text from './Text';
import Typing from './Typing';
import classNames from 'classnames';

type Props = {
  parseLoading: boolean;
  parseInfoOptions: ChatContextType[];
  parseTip: string;
  currentParseInfo?: ChatContextType;
  onSelectParseInfo: (parseInfo: ChatContextType) => void;
};

const MAX_OPTION_VALUES_COUNT = 2;

const ParseTip: React.FC<Props> = ({
  parseLoading,
  parseInfoOptions,
  parseTip,
  currentParseInfo,
  onSelectParseInfo,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;

  if (parseLoading) {
    return <Typing />;
  }

  if (parseTip) {
    return <Text data={parseTip} />;
  }

  if (parseInfoOptions.length === 0) {
    return null;
  }

  const getTipNode = (parseInfo: ChatContextType, isOptions?: boolean, index?: number) => {
    const {
      domainName,
      dateInfo,
      dimensionFilters,
      dimensions,
      metrics,
      aggType,
      queryMode,
      properties,
      entity,
      elementMatches,
    } = parseInfo || {};
    const { startDate, endDate } = dateInfo || {};
    const dimensionItems = dimensions?.filter(item => item.type === 'DIMENSION');
    const metric = metrics?.[0];

    const tipContentClass = classNames(`${prefixCls}-tip-content`, {
      [`${prefixCls}-tip-content-option`]: isOptions,
      [`${prefixCls}-tip-content-option-active`]:
        isOptions &&
        currentParseInfo &&
        JSON.stringify(currentParseInfo) === JSON.stringify(parseInfo),
      [`${prefixCls}-tip-content-option-disabled`]:
        isOptions &&
        currentParseInfo !== undefined &&
        JSON.stringify(currentParseInfo) !== JSON.stringify(parseInfo),
    });

    const itemValueClass = classNames({
      [`${prefixCls}-tip-item-value`]: !isOptions,
      [`${prefixCls}-tip-item-option`]: isOptions,
    });

    const entityAlias = entity?.alias?.[0]?.split('.')?.[0];
    const entityName = elementMatches?.find(item => item.element?.type === 'ID')?.element.name;

    const pluginName = properties?.CONTEXT?.plugin?.name;

    const modeName = pluginName
      ? '调插件'
      : queryMode.includes('METRIC')
      ? '算指标'
      : queryMode === 'ENTITY_DETAIL'
      ? '查明细'
      : queryMode === 'ENTITY_LIST_FILTER'
      ? '做圈选'
      : '';

    const fields =
      queryMode === 'ENTITY_DETAIL' ? dimensionItems?.concat(metrics || []) : dimensionItems;

    return (
      <div
        className={tipContentClass}
        onClick={() => {
          if (isOptions && currentParseInfo === undefined) {
            onSelectParseInfo(parseInfo);
          }
        }}
      >
        {index !== undefined && <div>{index + 1}.</div>}
        {!pluginName && isOptions && <div className={`${prefixCls}-mode-name`}>{modeName}：</div>}
        {!!pluginName ? (
          <div className={`${prefixCls}-tip-item`}>
            将由问答插件
            <span className={itemValueClass}>{pluginName}</span>来解答
          </div>
        ) : (
          <>
            {queryMode === 'METRIC_ENTITY' || queryMode === 'ENTITY_DETAIL' ? (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>{entityAlias}：</div>
                <div className={itemValueClass}>{entityName}</div>
              </div>
            ) : (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>主题域：</div>
                <div className={itemValueClass}>{domainName}</div>
              </div>
            )}
            {modeName === '算指标' && metric && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>指标：</div>
                <div className={itemValueClass}>{metric.name}</div>
              </div>
            )}
            {!isOptions && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>时间：</div>
                <div className={itemValueClass}>
                  {startDate === endDate ? startDate : `${startDate} ~ ${endDate}`}
                </div>
              </div>
            )}
            {['METRIC_GROUPBY', 'METRIC_ORDERBY', 'ENTITY_DETAIL'].includes(queryMode) &&
              fields &&
              fields.length > 0 && (
                <div className={`${prefixCls}-tip-item`}>
                  <div className={`${prefixCls}-tip-item-name`}>
                    {queryMode === 'ENTITY_DETAIL' ? '查询字段' : '下钻维度'}：
                  </div>
                  <div className={itemValueClass}>
                    {fields
                      .slice(0, MAX_OPTION_VALUES_COUNT)
                      .map(field => field.name)
                      .join('、')}
                    {fields.length > MAX_OPTION_VALUES_COUNT && '...'}
                  </div>
                </div>
              )}
            {['METRIC_FILTER', 'METRIC_ENTITY', 'ENTITY_DETAIL', 'ENTITY_LIST_FILTER'].includes(
              queryMode
            ) &&
              dimensionFilters &&
              dimensionFilters?.length > 0 && (
                <div className={`${prefixCls}-tip-item`}>
                  <div className={`${prefixCls}-tip-item-name`}>筛选条件：</div>
                  {dimensionFilters.slice(0, MAX_OPTION_VALUES_COUNT).map((filter, index) => (
                    <div className={itemValueClass}>
                      <span>{filter.name}：</span>
                      <span>
                        {Array.isArray(filter.value) ? filter.value.join('、') : filter.value}
                      </span>
                      {index !== dimensionFilters.length - 1 && <span>、</span>}
                    </div>
                  ))}
                  {dimensionFilters.length > MAX_OPTION_VALUES_COUNT && '...'}
                </div>
              )}
            {queryMode === 'METRIC_ORDERBY' && aggType && aggType !== 'NONE' && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>聚合方式：</div>
                <div className={itemValueClass}>{AGG_TYPE_MAP[aggType]}</div>
              </div>
            )}
          </>
        )}
      </div>
    );
  };

  let tipNode: ReactNode;

  if (parseInfoOptions.length > 1) {
    tipNode = (
      <div className={`${prefixCls}-multi-options`}>
        <div>您的问题解析为以下几项，请您点击确认</div>
        <div className={`${prefixCls}-options`}>
          {parseInfoOptions.map((item, index) => getTipNode(item, true, index))}
        </div>
      </div>
    );
  } else {
    const pluginName = parseInfoOptions[0]?.properties?.CONTEXT?.plugin?.name;
    tipNode = (
      <div className={`${prefixCls}-tip`}>
        <div>{!!pluginName ? '您的问题' : '您的问题解析为：'}</div>
        {getTipNode(parseInfoOptions[0])}
      </div>
    );
  }

  return <Text data={tipNode} />;
};

export default ParseTip;
