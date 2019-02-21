package xyz.erupt.core.controller;


import xyz.erupt.annotation.sub_field.sub_edit.TabType;
import xyz.erupt.core.constant.HttpStatus;
import xyz.erupt.core.model.*;
import xyz.erupt.core.constant.RestPath;
import xyz.erupt.core.service.InitService;
import org.springframework.web.bind.annotation.*;
import xyz.erupt.core.util.ReflectUtil;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Erupt 页面结构构建信息
 * Created by liyuepeng on 9/28/18.
 */
@RestController
@RequestMapping(RestPath.ERUPT_BUILD)
public class EruptBuildController {

    @GetMapping("/list/{erupt}")
    @ResponseBody
    public EruptPageModel getEruptTableView(@PathVariable("erupt") String eruptName, HttpServletResponse response) {
        EruptPageModel eruptPageModel = new EruptPageModel();
        EruptModel eruptModel = InitService.ERUPTS.get(eruptName);
        if (null != eruptModel) {
            try {
                Object eruptInstance = eruptModel.getClazz().newInstance();
                for (EruptFieldModel fm : eruptModel.getEruptFieldModels()) {
                    Field field = fm.getField();
                    field.setAccessible(true);
                    fm.setValue(field.get(eruptInstance));
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            eruptPageModel.setEruptModel(eruptModel);
            List<EruptAndEruptFieldModel> eruptAndEruptFieldModels = new ArrayList<>();
            for (EruptFieldModel fieldModel : eruptModel.getEruptFieldModels()) {
                TabType[] tabType = fieldModel.getEruptField().edit().tabType();
                if (tabType.length > 0) {
                    String typeName = ReflectUtil.getFieldGenericName(fieldModel.getField()).get(0);
                    EruptModel subEruptModel = InitService.ERUPTS.get(typeName);
                    eruptAndEruptFieldModels.add(new EruptAndEruptFieldModel(fieldModel, subEruptModel));
                    if (null == subEruptModel) {
                        throw new RuntimeException("请使用Erupt注解管理" + typeName);
                    }
                }
            }
            eruptPageModel.setSubErupts(eruptAndEruptFieldModels);
        } else {
            response.setStatus(HttpStatus.NOT_FOUNT.code);
        }
        return eruptPageModel;
    }


}
