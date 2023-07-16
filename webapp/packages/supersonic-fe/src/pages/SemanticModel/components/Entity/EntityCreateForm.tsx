import { useEffect, useState, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button } from 'antd';
import { addDomainExtend, editDomainExtend } from '../../service';
import type { ISemantic, IChatConfig } from '../../data';
import { formLayout } from '@/components/FormHelper/utils';
import { formatRichEntityDataListToIds } from './utils';
import styles from '../style.less';

type Props = {
  entityData: IChatConfig.IChatRichConfig;
  dimensionList: ISemantic.IDimensionList;
  domainId: number;
  onSubmit: () => void;
};

const FormItem = Form.Item;

const EntityCreateForm: ForwardRefRenderFunction<any, Props> = (
  { entityData, dimensionList, domainId, onSubmit },
  ref,
) => {
  const [form] = Form.useForm();
  const formatEntityData = formatRichEntityDataListToIds(entityData);
  const [dimensionListOptions, setDimensionListOptions] = useState<any>([]);

  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useEffect(() => {
    form.resetFields();
    if (!entityData?.entity) {
      return;
    }

    form.setFieldsValue({
      ...formatEntityData.entity,
      id: formatEntityData.id,
    });
  }, [entityData]);

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));

  useEffect(() => {
    const dimensionEnum = dimensionList.map((item: ISemantic.IDimensionItem) => {
      return {
        label: item.name,
        value: item.id,
      };
    });
    setDimensionListOptions(dimensionEnum);
  }, [dimensionList]);

  const saveEntity = async () => {
    const values = await form.validateFields();
    const { id, name } = values;
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }
    const { code, msg, data } = await saveDomainExtendQuery({
      chatDetailConfig: {
        ...formatEntityData,
        entity: {
          ...values,
          names: name.split(','),
        },
      },
      id,
      domainId,
    });

    if (code === 200) {
      form.setFieldValue('id', data);
      onSubmit?.();
      message.success('保存成功');
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <Form {...formLayout} form={form} layout="vertical" className={styles.form}>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name="name"
          label="实体别名"
          // rules={[{ required: true, message: '请输入实体别名' }]}
        >
          <Input placeholder="请输入实体别名,多个名称以英文逗号分隔" />
        </FormItem>
        <FormItem
          name="entityId"
          label="唯一标识"
          // rules={[{ required: true, message: '请选择实体标识' }]}
        >
          <Select
            // mode="multiple"
            allowClear
            style={{ width: '100%' }}
            // filterOption={(inputValue: string, item: any) => {
            //   const { label } = item;
            //   if (label.includes(inputValue)) {
            //     return true;
            //   }
            //   return false;
            // }}
            placeholder="请选择主体标识"
            options={dimensionListOptions}
          />
        </FormItem>
        <FormItem>
          <Button
            type="primary"
            onClick={() => {
              saveEntity();
            }}
          >
            保 存
          </Button>
        </FormItem>
      </Form>
    </>
  );
};

export default forwardRef(EntityCreateForm);
