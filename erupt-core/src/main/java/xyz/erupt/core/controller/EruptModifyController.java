package xyz.erupt.core.controller;import com.google.gson.Gson;import com.google.gson.JsonElement;import com.google.gson.JsonObject;import lombok.extern.java.Log;import org.apache.commons.lang3.StringUtils;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.web.bind.annotation.*;import xyz.erupt.annotation.EruptField;import xyz.erupt.annotation.sub_erupt.LinkTree;import xyz.erupt.core.annotation.EruptRecordOperate;import xyz.erupt.core.annotation.EruptRouter;import xyz.erupt.core.constant.RestPath;import xyz.erupt.core.exception.EruptNoLegalPowerException;import xyz.erupt.core.service.EruptCoreService;import xyz.erupt.core.service.EruptDataService;import xyz.erupt.core.util.AnnotationUtil;import xyz.erupt.core.util.EruptUtil;import xyz.erupt.core.util.ReflectUtil;import xyz.erupt.core.view.EruptApiModel;import xyz.erupt.core.view.EruptModel;import javax.transaction.Transactional;import java.lang.reflect.Field;/** * Erupt 对数据的增删改查 * * @author liyuepeng * @date 9/28/18. */@RestController@RequestMapping(RestPath.ERUPT_DATA)@Logpublic class EruptModifyController {    @Autowired    private Gson gson;    @Autowired    private EruptService eruptService;    @PostMapping({"/{erupt}"})    @ResponseBody    @EruptRecordOperate(desc = "新增")    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    @Transactional    public EruptApiModel addEruptData(@PathVariable("erupt") String erupt,                                      @RequestBody JsonObject data, JsonObject jsonObject) throws IllegalAccessException, InstantiationException {        EruptModel eruptModel = EruptCoreService.getErupt(erupt);        if (EruptUtil.getPowerObject(eruptModel).isAdd()) {            LinkTree dependTree = eruptModel.getErupt().linkTree();            if (StringUtils.isNotBlank(dependTree.field()) && dependTree.dependNode()) {                JsonElement jsonElement = data.remove("$" + dependTree.field());                //必须是强依赖才会自动注入值                if (dependTree.dependNode()) {                    if (null == jsonElement || jsonElement.isJsonNull()) {                        return EruptApiModel.errorApi("请选择树节点");                    } else {                        if (null == jsonObject) {                            jsonObject = new JsonObject();                        }                        String rm = ReflectUtil.findClassField(eruptModel.getClazz(), dependTree.field()).getType().getSimpleName();                        JsonObject sub = new JsonObject();                        sub.addProperty(EruptCoreService.getErupt(rm).getErupt().primaryKeyCol(), jsonElement.getAsString());                        jsonObject.add(dependTree.field(), sub);                    }                }            }            EruptApiModel eruptApiModel = EruptUtil.validateEruptValue(eruptModel, data);            if (eruptApiModel.getStatus() == EruptApiModel.Status.ERROR) {                return eruptApiModel;            }            Object o = gson.fromJson(data.toString(), eruptModel.getClazz());            this.clearObjectDefaultValueByJson(o, data);            Object obj = this.dataTarget(eruptModel, o, eruptModel.getClazz().newInstance());            if (null != jsonObject) {                for (String key : jsonObject.keySet()) {                    Field field = ReflectUtil.findClassField(eruptModel.getClazz(), key);                    field.setAccessible(true);                    field.set(obj, gson.fromJson(jsonObject.get(key).toString(), field.getType()));                }            }            EruptUtil.handlerDataProxy(eruptModel, (dataProxy -> dataProxy.beforeAdd(obj)));            AnnotationUtil.getEruptDataProcessor(eruptModel.getClazz()).addData(eruptModel, obj);            this.modifyLog(eruptModel, "ADD", data.toString());            EruptUtil.handlerDataProxy(eruptModel, (dataProxy -> dataProxy.afterAdd(obj)));            return EruptApiModel.successApi();        } else {            throw new EruptNoLegalPowerException();        }    }    @PutMapping("/{erupt}")    @ResponseBody    @EruptRecordOperate(desc = "修改")    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    @Transactional    public EruptApiModel editEruptData(@PathVariable("erupt") String erupt, @RequestBody JsonObject data) throws IllegalAccessException {        EruptModel eruptModel = EruptCoreService.getErupt(erupt);        if (EruptUtil.getPowerObject(eruptModel).isEdit()) {            EruptApiModel eruptApiModel = EruptUtil.validateEruptValue(eruptModel, data);            if (eruptApiModel.getStatus() == EruptApiModel.Status.ERROR) {                return eruptApiModel;            }            if (!eruptService.verifyIdPermissions(eruptModel, data.get(eruptModel.getErupt().primaryKeyCol()).getAsString())) {                throw new EruptNoLegalPowerException();            }//            LinkTree linkTree = eruptModel.getErupt().linkTree();//            if (linkTree.dependNode()) {//                data.remove(eruptModel.getErupt().linkTree().field());//            }            Object o = this.gson.fromJson(data.toString(), eruptModel.getClazz());            this.clearObjectDefaultValueByJson(o, data);            Object obj = this.dataTarget(eruptModel, o, AnnotationUtil.getEruptDataProcessor(eruptModel.getClazz())                    .findDataById(eruptModel, ReflectUtil.findClassField(eruptModel.getClazz(), eruptModel.getErupt().primaryKeyCol()).get(o)));            EruptUtil.handlerDataProxy(eruptModel, (dataProxy -> dataProxy.beforeUpdate(obj)));            AnnotationUtil.getEruptDataProcessor(eruptModel.getClazz()).editData(eruptModel, obj);            this.modifyLog(eruptModel, "EDIT", data.toString());            EruptUtil.handlerDataProxy(eruptModel, (dataProxy -> dataProxy.afterUpdate(obj)));            return EruptApiModel.successApi();        } else {            throw new EruptNoLegalPowerException();        }    }    private Object dataTarget(EruptModel eruptModel, Object data, Object target) {        ReflectUtil.findClassAllFields(eruptModel.getClazz(), (field) -> {            EruptField eruptField = field.getAnnotation(EruptField.class);            if (null != eruptField) {                if (StringUtils.isNotBlank(eruptField.edit().title()) && !eruptField.edit().readOnly()) {                    try {                        Field f = ReflectUtil.findClassField(eruptModel.getClazz(), field.getName());                        f.set(target, f.get(data));                    } catch (IllegalAccessException e) {                        e.printStackTrace();                    }                }            }        });        return target;    }    //清理序列化后对象所产生的默认值（通过json串进行校验）    private void clearObjectDefaultValueByJson(Object obj, JsonObject data) {        ReflectUtil.findClassAllFields(obj.getClass(), field -> {            try {                field.setAccessible(true);                if (null != field.get(obj)) {                    if (!data.has(field.getName())) {                        field.set(obj, null);                    }                }            } catch (IllegalAccessException e) {                e.printStackTrace();            }        });    }    @DeleteMapping("/{erupt}/{id}")    @ResponseBody    @EruptRecordOperate(desc = "删除")    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    @Transactional    public EruptApiModel deleteEruptData(@PathVariable("erupt") String erupt, @PathVariable("id") String id) {        EruptModel eruptModel = EruptCoreService.getErupt(erupt);        if (EruptUtil.getPowerObject(eruptModel).isDelete()) {            if (!eruptService.verifyIdPermissions(eruptModel, id)) {                throw new EruptNoLegalPowerException();            }            EruptDataService dataService = AnnotationUtil.getEruptDataProcessor(eruptModel.getClazz());            //获取对象数据信息用于DataProxy函数中            Object obj = dataService.findDataById(eruptModel, EruptUtil.toEruptId(eruptModel, id));            EruptUtil.handlerDataProxy(eruptModel, (dataProxy -> dataProxy.beforeDelete(obj)));            dataService.deleteData(eruptModel, obj);            this.modifyLog(eruptModel, "DELETE", id);            EruptUtil.handlerDataProxy(eruptModel, (dataProxy -> dataProxy.afterDelete(obj)));            return EruptApiModel.successApi();        } else {            throw new EruptNoLegalPowerException();        }    }    //为了事务性考虑所以增加了批量删除功能    @Transactional    @DeleteMapping("/{erupt}")    @ResponseBody    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    @EruptRecordOperate(desc = "批量删除")    public EruptApiModel deleteEruptDataList(@PathVariable("erupt") String erupt, @RequestParam("ids") String[] ids) {        EruptApiModel eruptApiModel = EruptApiModel.successApi();        for (String id : ids) {            eruptApiModel = this.deleteEruptData(erupt, id);            if (eruptApiModel.getStatus() == EruptApiModel.Status.ERROR) {                break;            }        }        return eruptApiModel;    }    private void modifyLog(EruptModel eruptModel, String placeholder, String content) {        log.info("[" + eruptModel.getEruptName() + " -> " + placeholder + "]:" + content);    }}