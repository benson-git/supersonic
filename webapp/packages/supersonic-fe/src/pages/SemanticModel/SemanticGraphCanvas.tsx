import { Radio } from 'antd';
import React, { useState } from 'react';
import { connect } from 'umi';
import styles from './components/style.less';
import type { StateType } from './model';
import { SemanticNodeType } from './enum';
import SemanticFlow from './SemanticFlows';
import SemanticGraph from './SemanticGraph';

type Props = {
  domainManger: StateType;
};

const SemanticGraphCanvas: React.FC<Props> = ({ domainManger }) => {
  const [graphShowType, setGraphShowType] = useState<SemanticNodeType>(SemanticNodeType.DIMENSION);
  const { selectDomainId } = domainManger;
  return (
    <div className={styles.semanticGraphCanvas}>
      {/* <div className={styles.toolbar}>
        <Radio.Group
          buttonStyle="solid"
          value={graphShowType}
          onChange={(e) => {
            const { value } = e.target;
            setGraphShowType(value);
          }}
        >
          <Radio.Button value={SemanticNodeType.DATASOURCE}>数据源</Radio.Button>
          <Radio.Button value={SemanticNodeType.DIMENSION}>维度</Radio.Button>
          <Radio.Button value={SemanticNodeType.METRIC}>指标</Radio.Button>
        </Radio.Group>
      </div> */}

      <div className={styles.canvasContainer}>
        {graphShowType === SemanticNodeType.DATASOURCE ? (
          <div style={{ width: '100%', height: 'calc(100vh - 200px)' }}>
            <SemanticFlow />
          </div>
        ) : (
          <div style={{ width: '100%' }}>
            <SemanticGraph domainId={selectDomainId} />
          </div>
        )}
      </div>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(SemanticGraphCanvas);
