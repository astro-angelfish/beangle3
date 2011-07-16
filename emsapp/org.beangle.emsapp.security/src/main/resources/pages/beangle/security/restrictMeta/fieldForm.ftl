[#ftl]
[@b.toolbar title="修改参数信息"]bar.addBack("${b.text("action.back")}");[/@]
[@b.form action="!saveField" title="数据限制参数" theme="list"]
	[@b.textfield label="common.name" name="field.name" value="${field.name!}" required="true" maxlength="50"/]
	[@b.textfield label="标题" name="field.remark" value="${field.remark!}" required="true" maxlength="50"/]
	[@b.textfield label="类型" name="field.type" value="${field.type!}" required="true" maxlength="100"/]
	[@b.textfield label="数据源" name="field.source" value="${(field.source)!}" style="width:400px;" maxlength="100" comment="java.lang中的基本类型，此处可以为空"/]
	[@b.textfield label="主键属性" name="field.keyName" value="${(field.keyName)!}" maxlength="40"/]
	[@b.textfield label="其它显示属性" name="field.propertyNames" value="${(field.propertyNames)!}" maxlength="100"  /]
	[@b.radios label="是否允许多值" name="field.multiple"  value=field.multiple/]
	[@b.select2 label="restriction.entities" required="true" name1st="Entitys" name2nd="entityId" items1st=entities items2nd=field.entities /]
	[@b.formfoot]
		<input type="hidden" name="field.id" value="${(field.id)!}" style="width:200px;" />
		[@b.submit value="action.submit" /]
		<input type="reset"  name="reset1" value="${b.text("action.reset")}" />
	[/@]
</table>
[/@]