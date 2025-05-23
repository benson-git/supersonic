package com.tencent.supersonic.headless.server.pojo;

import com.google.common.base.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


@Data
@NoArgsConstructor
public class MetaFilter {

    private String id;

    private String name;

    private String bizName;

    private String createdBy;

    private List<Long> modelIds;

    private Long domainId;

    private Long viewId;

    private Integer sensitiveLevel;

    private Integer status;

    private String key;

    private List<Long> ids;

    private List<String> fieldsDepend;

    public MetaFilter(List<Long> modelIds) {
        this.modelIds = modelIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaFilter that = (MetaFilter) o;
        return Objects.equal(id, that.id) && Objects.equal(name, that.name)
                && Objects.equal(bizName, that.bizName) && Objects.equal(
                createdBy, that.createdBy) && Objects.equal(modelIds, that.modelIds)
                && Objects.equal(domainId, that.domainId) && Objects.equal(
                viewId, that.viewId) && Objects.equal(sensitiveLevel, that.sensitiveLevel)
                && Objects.equal(status, that.status) && Objects.equal(key,
                that.key) && Objects.equal(ids, that.ids) && Objects.equal(
                fieldsDepend, that.fieldsDepend);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, bizName, createdBy, modelIds, domainId, viewId, sensitiveLevel, status, key,
                ids, fieldsDepend);
    }
}
