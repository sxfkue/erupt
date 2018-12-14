package com.erupt.entity;

import com.erupt.annotation.Erupt;
import com.erupt.annotation.EruptField;
import com.erupt.annotation.sub_field.Edit;
import com.erupt.annotation.sub_field.EditType;
import com.erupt.annotation.sub_field.View;
import com.erupt.annotation.sub_field.sub_edit.ReferenceType;
import com.erupt.model.BaseModel;

import javax.persistence.*;

/**
 * Created by liyuepeng on 12/7/18.
 */
@Entity
@Table(name = "E_DICT")
@Erupt(name = "数据字典")
public class EruptDict extends BaseModel {

    @EruptField(
            views = @View(title = "编码"),
            edit = @Edit(title = "编码")
    )
    @Column(name = "CODE")
    private String code;

    @EruptField(
            views = @View(title = "名称"),
            edit = @Edit(title = "名称")
    )
    @Column(name = "NAME")
    private String name;

    @EruptField(
            views = @View(title = "上级菜单", column = "name"),
            edit = @Edit(
                    title = "上级菜单",
                    type = EditType.REFERENCE,
                    referenceType = @ReferenceType(id = "id", label = "name")
            )
    )
    @ManyToOne
    @JoinColumn(name = "PARENT_DICT_ID")
    private EruptDict eruptDict;

    @EruptField(
            views = @View(title = "顺序"),
            edit = @Edit(title = "顺序")
    )
    @Column(name = "SORT")
    private Integer sort;

    @EruptField(
            views = @View(title = "备注"),
            edit = @Edit(title = "备注")
    )
    @Column(name = "REMARK")
    private String remark;
}
